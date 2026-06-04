# Migrate to Supabase Online Database (Offline-First)

This plan outlines the steps to remove the Google Drive backup dependency and integrate an online database (Supabase). The application will maintain its offline-first architecture using the existing local SQLite database, but will gain a login system and an automated daily sync mechanism.

## Goal

- Strip out Google Drive backup logic entirely.
- Add an admin-provided login system for sellers.
- Embed `seller_id` in all records locally and remotely to partition data.
- **Two-way Sync**: Sync offline changes to Supabase at 2 AM, and pull any new changes made from other devices.
- **Precision Data Migration**: Migrate all floating-point numbers (`double`) to `BigDecimal` mapped as `TEXT` in SQLite to ensure 100% precision for large financial calculations.
- **Data Preservation & CSV Backup**: Safely preserve all legacy data. Add a CSV export feature as a failsafe before any migration occurs.

> [!TIP]
> **Database Safety Assurance**: 
> SQLite uses *Dynamic Typing*. This means a column defined as `REAL` can actually store `TEXT` safely without breaking the table. However, to guarantee 0% risk of data loss, we will add a **CSV Export feature** so you can download all your raw data to your phone's storage before the new updates take effect. 

> [!WARNING]
> **User Review Required**:
> Please review the updated plan which now includes the CSV Export feature as a safety net.

---

## Proposed Changes

### 1. Failsafe CSV Export (New Feature)
Before we run any migrations, we will add a utility to export the entire database (Customers, Transactions, Payments) into CSV files in the Android `Downloads` folder. 
- **[NEW] `CsvExportManager.java`**: Reads all data via raw SQL queries and writes it to `.csv` files.
- This gives you a permanent, readable backup of your data on your phone before the Supabase sync begins.

### 2. High-Precision Data Migration (BigDecimal)
Currently, amounts and quantities are likely stored as `double` or `REAL` in the DB. To prevent floating-point errors on very large numbers:
- **Models**: Refactor `Customer`, `DailyTransaction`, `Payment` to use `java.math.BigDecimal` for all monetary amounts and quantities.
- **Database Schema**: Because SQLite uses dynamic typing, we can continue using the existing columns but will insert `BigDecimal` as `TEXT` strings to retain absolute precision. The DAOs will handle the conversion (`new BigDecimal(cursor.getString())` and `bigDecimal.toPlainString()`).

### 3. Dependency Updates
Remove Google Drive dependencies and add Retrofit.

#### [MODIFY] `gradle/libs.versions.toml` & `app/build.gradle.kts`
- Remove `googleAuth`, `googleDrive`, `googleApiClient`, `httpClient`.
- Add `androidx.work:work-runtime:2.9.0` for background jobs.
- Add `com.squareup.retrofit2:retrofit:2.9.0` and `converter-gson` for pure Java API calls.

---

### 4. Google Drive Removal

#### [DELETE] `app/src/main/java/com/ignishers/milkmanager2/utils/GoogleDriveHelper.java`
- We no longer need this helper.

#### [MODIFY] `app/src/main/java/com/ignishers/milkmanager2/activities/SettingsActivity.java` & `activity_settings.xml`
- Remove Google Sign-in logic and UI.
- Replace with "Export Data to CSV", "Manual Sync", and "Logout" buttons.

---

### 5. Login System & Safe Data Preservation

#### [NEW] `app/src/main/java/com/ignishers/milkmanager2/activities/LoginActivity.java`
- A new screen for email/password authentication using Supabase.
- **Data Preservation Strategy**: On the *first* successful login, the app will scan the entire local SQLite database. Any record (Customer, Transaction, Payment, Route) that does not have a `seller_id` will be immediately updated to the newly logged-in `seller_id` and marked as `is_synced = 0`. 

---

### 6. Database Schema Update (DBHelper v7)

#### [MODIFY] `app/src/main/java/com/ignishers/milkmanager2/database/DBHelper.java`
Upgrade database version to 7:
- Add `seller_id` (TEXT) and `is_synced` (INTEGER DEFAULT 0) to all data tables.
- Add `updated_at` (TEXT, ISO-8601 timestamp) to resolve two-way sync conflicts.

#### [MODIFY] DAOs (`CustomerDAO`, `DailyTransactionDAO`, etc.)
- Update inserts/updates to append the logged-in `seller_id` and the current `updated_at` timestamp.
- Migrate the `REAL` columns to be treated as `TEXT` fields for `BigDecimal` conversions.

---

### 7. Supabase Integration & Two-Way Sync Worker

#### [NEW] `app/src/main/java/com/ignishers/milkmanager2/api/SupabaseService.java`
- Retrofit interface mapping to Supabase REST endpoints.

#### [NEW] `app/src/main/java/com/ignishers/milkmanager2/managers/SyncManager.java`
- **Two-Way Sync Logic**:
  1. **Pull**: Fetch records from Supabase where `updated_at` > `local_last_sync_time`.
  2. **Push**: Fetch local records where `is_synced = 0`. Push them to Supabase in batches.
  3. **Update Timestamp**: Save the current timestamp as `local_last_sync_time` in `SharedPreferences`.

#### [NEW] `app/src/main/java/com/ignishers/milkmanager2/workers/NightlySyncWorker.java`
- Periodically runs the `SyncManager` two-way sync at 2 AM using `WorkManager`.

---

## Verification Plan

### Automated / Unit Verification
- Unit test the `BigDecimal` string conversions to ensure exact precision is maintained without rounding errors.
- Verify `DBHelper` successfully runs the `v7` migration without dropping tables.

### Manual Verification
1. Launch the app, navigate to Settings, and click **Export Data to CSV**. Verify CSV files are safely created in the Downloads folder.
2. Log in using a test seller credential.
3. Verify that all legacy local data now appears under the new `seller_id`.
4. Add large monetary values (e.g. `9999999999999.99`) and verify calculations are perfectly exact.
5. Trigger a Manual Sync and verify data uploads to Supabase.
