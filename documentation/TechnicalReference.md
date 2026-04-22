# MilkManager2 – Complete Technical Reference

> **Package root:** `com.ignishers.milkmanager2`  
> **Database:** `milky_mist_db` (SQLite, schema version 6)  
> **Min SDK:** 30 · **Target SDK / Compile SDK:** 36  
> **Language:** Java 11  
> **Architecture:** Activity-based MVC with DAO data layer and MVVM ViewModel bridge

---

## Table of Contents

1. [Project Layout](#1-project-layout)
2. [Build Configuration](#2-build-configuration)
3. [AndroidManifest](#3-androidmanifest)
4. [Database Layer](#4-database-layer)
   - 4.1 [DBHelper](#41-dbhelper)
   - 4.2 [CustomerDAO](#42-customerdao)
   - 4.3 [DailyTransactionDAO](#43-dailytransactiondao)
   - 4.4 [MilkRateDAO](#44-milkratedao)
   - 4.5 [RouteGroupDAO](#45-routegroupdao)
   - 4.6 [AppSettingsDAO](#46-appsettingsdao)
5. [Models](#5-models)
   - 5.1 [Customer](#51-customer)
   - 5.2 [DailyTransaction](#52-dailytransaction)
   - 5.3 [RouteGroup](#53-routegroup)
   - 5.4 [NavItem](#54-navitem)
6. [Activities](#6-activities)
   - 6.1 [DashboardActivity](#61-dashboardactivity)
   - 6.2 [PointOfSaleActivity](#62-pointofsaleactivity)
   - 6.3 [BillActivity](#63-billactivity)
   - 6.4 [SalesReportActivity](#64-salesreportactivity)
   - 6.5 [SettingsActivity](#65-settingsactivity)
7. [Receivers](#7-receivers)
   - 7.1 [AutoEntryReceiver](#71-autoentryreceiver)
   - 7.2 [AutoEntryScheduler](#72-autoentryscheduler)
   - 7.3 [BootReceiver](#73-bootreceiver)
8. [ViewModels](#8-viewmodels)
   - 8.1 [MilkEntryViewModel](#81-milkentryviewmodel)
9. [Adapters](#9-adapters)
   - 9.1 [NavigationAdapter](#91-navigationadapter)
   - 9.2 [DailyTransactionAdapter](#92-dailytransactionadapter)
10. [Fragments](#10-fragments)
    - 10.1 [MilkEntryFragment](#101-milkentryfragment)
11. [Bottom Sheets](#11-bottom-sheets)
    - 11.1 [MainMenuBottomSheet](#111-mainmenubottomsheet)
    - 11.2 [ManualEntryBottomSheet](#112-manualentrybottomsheet)
12. [Dialogs](#12-dialogs)
    - 12.1 [AddCustomerDialogFragment](#121-addcustomerdialogfragment)
    - 12.2 [MonthSummaryDialog](#122-monthsummarydialog)
    - 12.3 [PaymentDialog](#123-paymentdialog)
    - 12.4 [SetMilkPriceDialog](#124-setmilkpricedialog)
13. [Managers](#13-managers)
    - 13.1 [NavigationManager](#131-navigationmanager)
14. [Utilities](#14-utilities)
    - 14.1 [PDFGenerator](#141-pdfgenerator)
    - 14.2 [GoogleDriveHelper](#142-googledrivehelper)
    - 14.3 [CustomerClipboard](#143-customerclipboard)
    - 14.4 [DialogUtils](#144-dialogutils)
    - 14.5 [SimpleTextWatcher](#145-simpletextwatcher)
15. [Custom Views](#15-custom-views)
    - 15.1 [SimpleBarView](#151-simplebarview)
    - 15.2 [SimplePieView](#152-simplepieview)
16. [Database Schema Reference](#16-database-schema-reference)
17. [Data Flow Diagrams](#17-data-flow-diagrams)

---

## 1. Project Layout

```
MilkManager2/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/ignishers/milkmanager2/
│           ├── activities/
│           │   ├── DashboardActivity.java
│           │   ├── PointOfSaleActivity.java
│           │   ├── BillActivity.java
│           │   ├── SalesReportActivity.java
│           │   └── SettingsActivity.java
│           ├── adapters/
│           │   ├── NavigationAdapter.java
│           │   └── DailyTransactionAdapter.java
│           ├── bottomsheets/
│           │   ├── MainMenuBottomSheet.java
│           │   └── ManualEntryBottomSheet.java
│           ├── database/
│           │   ├── DBHelper.java
│           │   ├── CustomerDAO.java
│           │   ├── DailyTransactionDAO.java
│           │   ├── MilkRateDAO.java
│           │   ├── RouteGroupDAO.java
│           │   └── AppSettingsDAO.java
│           ├── dialogs/
│           │   ├── AddCustomerDialogFragment.java
│           │   ├── MonthSummaryDialog.java
│           │   ├── PaymentDialog.java
│           │   └── SetMilkPriceDialog.java
│           ├── fragments/
│           │   └── MilkEntryFragment.java
│           ├── managers/
│           │   └── NavigationManager.java
│           ├── models/
│           │   ├── Customer.java
│           │   ├── DailyTransaction.java
│           │   ├── RouteGroup.java
│           │   └── NavItem.java
│           ├── utils/
│           │   ├── PDFGenerator.java
│           │   ├── GoogleDriveHelper.java
│           │   ├── CustomerClipboard.java
│           │   ├── DialogUtils.java
│           │   └── SimpleTextWatcher.java
│           ├── viewmodels/
│           │   └── MilkEntryViewModel.java
│           ├── views/
│           │   ├── SimpleBarView.java
│           │   └── SimplePieView.java
│           ├── AutoEntryReceiver.java
│           ├── AutoEntryScheduler.java
│           └── BootReceiver.java
└── documentation/
    ├── SDLC.md
    ├── SRS.md
    ├── SystemArchitecture_and_DFD.md
    └── TechnicalReference.md
```

---

## 2. Build Configuration

**File:** `app/build.gradle.kts`

| Property | Value |
|---|---|
| `namespace` | `com.ignishers.milkmanager2` |
| `compileSdk` | 36 |
| `minSdk` | 30 |
| `targetSdk` | 36 |
| `versionCode` | 1 |
| `versionName` | `"2.0"` |
| `archivesBaseName` | `MilkManager2` |
| `sourceCompatibility` | Java 11 |
| `viewBinding` | enabled |
| `buildConfig` | enabled |

### Key Dependencies

| Library | Purpose |
|---|---|
| `appcompat`, `material`, `activity`, `constraintlayout` | Core Android UI |
| `recyclerview` | List/grid displays |
| `lifecycle-livedata-ktx`, `lifecycle-viewmodel-ktx` | ViewModel + LiveData |
| `fragment` | Fragment support |
| `mpandroidchart` | Sales report charts |
| `google.play.auth` | Google Sign-In / OAuth 2.0 |
| `google.drive` | Google Drive REST API |
| `google.api.client.android`, `google.http.client.gson` | HTTP transport & JSON |

---

## 3. AndroidManifest

**File:** `app/src/main/AndroidManifest.xml`

### Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Google Drive API calls |
| `ACCESS_NETWORK_STATE` | Drive connectivity checks |
| `WRITE_EXTERNAL_STORAGE` | PDF export to Downloads |
| `READ_EXTERNAL_STORAGE` | Legacy file reading |
| `RECEIVE_BOOT_COMPLETED` | Allow `BootReceiver` to reschedule alarms after reboot |
| `SCHEDULE_EXACT_ALARM` | Android 12+ exact alarm scheduling |
| `USE_EXACT_ALARM` | Alternative exact alarm permission |

### Declared Components

| Type | Component | Notes |
|---|---|---|
| Activity | `DashboardActivity` | `exported=true`, LAUNCHER entry point |
| Activity | `PointOfSaleActivity` | `exported=false` |
| Activity | `BillActivity` | `exported=false` |
| Activity | `SalesReportActivity` | `exported=false` |
| Activity | `SettingsActivity` | `exported=false` |
| Provider | `androidx.core.content.FileProvider` | authority = `${applicationId}.provider`; used for PDF sharing |
| Receiver | `AutoEntryReceiver` | Handles `AUTO_ENTRY_MORNING` and `AUTO_ENTRY_EVENING` intents |
| Receiver | `BootReceiver` | Handles `BOOT_COMPLETED` to re-register alarms |

---

## 4. Database Layer

### 4.1 DBHelper

**File:** `database/DBHelper.java`
**Extends:** `SQLiteOpenHelper`
**DB Name:** `milky_mist_db`
**Current Version:** `6`

Central database gateway. Owns schema creation, migration, and all table/column constant definitions.

#### Table Constants (static final Strings)

| Constant Name | Table Name |
|---|---|
| `ROUTE_GROUP_TABLE` | `route_group` |
| `CUSTOMER_TABLE` | `customer` |
| `MILK_PRICE_TABLE` | `global_milk_price` |
| `MILK_TRANSACTION_TABLE` | `milk_transaction` |
| `PAYMENT_TABLE` | `payment` |
| `SETTINGS_TABLE` | `app_settings` |

#### Important Column Constants (selected)

| Constant | Column |
|---|---|
| `COL_CUSTOMER_ID` | `customer_id` |
| `COL_CUSTOMER_NAME` | `customer_name` |
| `COL_CUSTOMER_MOBILE` | `mobile_number` |
| `COL_CUSTOMER_CURRENT_DUE` | `current_due` |
| `COL_CUSTOMER_AUTO_ENTRY` | `auto_entry_enabled` |
| `COL_TRANS_ID` | `transaction_id` |
| `COL_TRANS_DATE` | `transaction_date` |
| `COL_TRANS_SESSION` | `session` |
| `COL_TRANS_QUANTITY` | `quantity` |
| `COL_TRANS_AMOUNT` | `amount` |
| `COL_TRANS_PAYMENT_MODE` | `payment_mode` |
| `COL_TRANS_MILK_TYPE` | `milk_type` |
| `COL_GLOBAL_PRICE_PER_LITRE` | `price_per_litre` |
| `COL_EFFECTIVE_DATE` | `effective_date` |
| `SETTING_AUTO_ENTRY_ENABLED` | key: `"auto_entry_enabled"` |

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `DBHelper(Context context)` | Calls `super` with DB name and version 6. |
| `onCreate` | `void onCreate(SQLiteDatabase db)` | Creates all 6 tables. Enables foreign keys. Inserts root route group (ID=0, name=`MAIN_DEPOT`) and default milk price (60 Rs/L). |
| `onUpgrade` | `void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)` | Incremental migration switch: v2->v3 adds `default_qty_morning/evening/sort_order` to `customer`; v3->v4 adds `sort_order` to `route_group`; v4->v5 adds `payment_mode` to `milk_transaction`; v5->v6 adds `milk_type` to `milk_transaction` and `auto_entry_enabled` to `customer`. |

---

### 4.2 CustomerDAO

**File:** `database/CustomerDAO.java`

Handles all CRUD operations for the `customer` table.

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `CustomerDAO(Context context)` | Opens writable database via `DBHelper`. |
| `insertCustomer` | `long insertCustomer(String name, String mobile, Long routeGroupId, double morningQtyL, double eveningQtyL, double currentDue)` | Inserts a new customer row. Sets `sort_order` = count of existing customers in that group. Returns new row ID or -1 on failure. |
| `getAllCustomers` | `List<Customer> getAllCustomers()` | Returns all customers ordered by `sort_order ASC`. |
| `getCustomersByGroup` | `List<Customer> getCustomersByGroup(long groupId)` | Returns customers belonging to a specific route group, ordered by `sort_order ASC`. |
| `getCustomer` | `Customer getCustomer(String customerId)` | Fetches a single customer by primary key. Returns `null` if not found. Populates all fields including `morningQtySet`, `eveningQtySet`, and `autoEntryEnabled`. |
| `updateCustomerDue` | `void updateCustomerDue(long customerId, double change)` | Adds `change` to `current_due` via SQL `UPDATE customer SET current_due = current_due + ?`. Positive = charge, negative = payment. |
| `updateCustomerDefaultQty` | `void updateCustomerDefaultQty(long customerId, double morningQty, double eveningQty)` | Updates morning and evening default quantity columns. |
| `moveCustomerToGroup` | `void moveCustomerToGroup(long customerId, long newGroupId)` | Updates `route_group_id` to migrate a customer to a new folder. |
| `updateAutoEntry` | `void updateAutoEntry(long customerId, boolean enabled)` | Flips the per-customer `auto_entry_enabled` flag. |
| `deleteCustomer` | `boolean deleteCustomer(long customerId)` | Deletes a customer row. Returns `true` if a row was affected. Callers must also call `DailyTransactionDAO.deleteAllForCustomer`. |
| `swapSortOrder` | `void swapSortOrder(long custId1, int order1, long custId2, int order2)` | Swaps `sort_order` values between two customers (used by up/down reorder buttons). |

---

### 4.3 DailyTransactionDAO

**File:** `database/DailyTransactionDAO.java`

Handles all CRUD and reporting queries on the `milk_transaction` table.

#### Nested Helper Classes

| Class | Fields | Purpose |
|---|---|---|
| `ReportSummary` | `totalMilk`, `totalRevenue`, `totalCollected`, `totalDue`, `totalLegacyDue` | Holds lifetime aggregate report data |
| `MonthlyReportItem` | `month` (1-12), `milk`, `amount`, `collected` | One row in a monthly breakdown |
| `CustomerSalesItem` | `customerName`, `totalMilk`, `totalSpent` | One row in customer sales ranking |
| `YearlyReportItem` | `year`, `totalMilk`, `totalRevenue` | One row in yearly revenue trend |

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `DailyTransactionDAO(Context context)` | Opens writable DB. |
| `insert` | `long insert(DailyTransaction transaction)` | Inserts all transaction fields. Returns the new row ID. |
| `getTransactionsByMonth` | `List<DailyTransaction> getTransactionsByMonth(String customerId, String month, String year)` | Queries using `strftime('%m')` and `strftime('%Y')`. Month is zero-padded (`"01"`…`"12"`). |
| `getTransactionsByDate` | `List<DailyTransaction> getTransactionsByDate(long customerId, String date)` | Returns all transactions for a customer on a specific `yyyy-MM-dd` date, newest first. |
| `getTransactionsByDateRange` | `List<DailyTransaction> getTransactionsByDateRange(long customerId, String startDate, String endDate)` | Returns transactions between two dates (inclusive) ordered by date ASC then ID ASC. Used by `BillActivity`. |
| `delete` | `void delete(int transactionId)` | Hard-deletes a single transaction by ID. |
| `hasTodayEntry` | `boolean hasTodayEntry(long customerId, String session)` | Returns `true` if a transaction exists today for the given customer/session. Used to enable delete buttons in `MilkEntryFragment`. |
| `deleteTodaySession` | `double deleteTodaySession(long customerId, String session)` | Finds and deletes the most recent transaction for today's session. Returns the deleted `amount` to reverse customer due. |
| `deleteAllForCustomer` | `void deleteAllForCustomer(long customerId)` | Bulk-deletes all transactions for a customer. Called during customer deletion. |
| `getLifetimeSummary` | `ReportSummary getLifetimeSummary()` | Aggregates lifetime totals. Revenue includes legacy customer dues. |
| `getMonthlyBreakdown` | `List<MonthlyReportItem> getMonthlyBreakdown(int year)` | Month-by-month milk, sales, and collection for a given year. Runs two SQL aggregation queries and merges them. |
| `getCustomerSalesRanking` | `List<CustomerSalesItem> getCustomerSalesRanking(int limit)` | JOINs with `customer` table to return the top-N customers by total spend (excludes payment entries). |
| `getYearlyRevenueTrend` | `List<YearlyReportItem> getYearlyRevenueTrend(int numberOfYears)` | Returns milk and revenue totals per year for the last N years. |

---

### 4.4 MilkRateDAO

**File:** `database/MilkRateDAO.java`

Manages the `global_milk_price` table — a version-controlled history of milk prices.

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `MilkRateDAO(Context context)` | Opens writable DB. |
| `insertRate` | `void insertRate(double rate, String effectiveDate)` | Inserts a new price record. Old prices are retained for historical accuracy. |
| `getRateForDate` | `double getRateForDate(String date)` | Returns the most recent rate whose `effective_date <= target date`. Fall-back: `60.0`. |
| `getCurrentRate` | `double getCurrentRate()` | Calls `getRateForDate(LocalDate.now().toString())`. |

> **Design Note:** Prices are never overwritten. Historical amounts are calculated using the price active on the transaction date.

---

### 4.5 RouteGroupDAO

**File:** `database/RouteGroupDAO.java`

Manages the `route_group` table which implements a recursive folder hierarchy.

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `RouteGroupDAO(Context context)` | Opens writable DB. |
| `getRootGroups` | `List<RouteGroup> getRootGroups()` | Calls `getChildGroups(0)`. Returns top-level folders. |
| `getChildGroups` | `List<RouteGroup> getChildGroups(long parentId)` | Returns groups with `parent_group_id = parentId`, ordered by `sort_order ASC, group_id ASC`. Excludes root (ID=0). |
| `getGroupById` | `RouteGroup getGroupById(long id)` | Fetches a single group by primary key. Returns `null` if not found. |
| `insertGroup` | `boolean insertGroup(String name, Long parentId)` | Creates a new folder. Auto-assigns `sort_order` = sibling count. |
| `deleteGroup` | `boolean deleteGroup(long groupId)` | Deletes a folder by ID. Customers inside must be moved first. |
| `swapSortOrder` | `void swapSortOrder(long groupId1, int order1, long groupId2, int order2)` | Swaps `sort_order` values between two groups. |
| `getAllGroups` | `List<RouteGroup> getAllGroups()` | Returns all groups with `group_id > 0`. Used in "Move Customer" selection dialog. |
| `ensureRootRouteExists` | `void ensureRootRouteExists()` | Inserts `{group_id=0, group_name="MAIN_DEPOT"}` if missing. Called during DB upgrades. |
| `getGroupCountUnder` *(private)* | `int getGroupCountUnder(long parentId)` | Counts sibling groups for sort_order assignment. |

---

### 4.6 AppSettingsDAO

**File:** `database/AppSettingsDAO.java`

Generic key-value settings store backed by the `app_settings` table.

#### Methods

| Method | Signature | Description |
|---|---|---|
| Constructor | `AppSettingsDAO(Context context)` | Opens writable DB. |
| `isAutoEntryEnabled` | `boolean isAutoEntryEnabled()` | Returns `true` if `SETTING_AUTO_ENTRY_ENABLED` value equals `"true"`. |
| `setAutoEntryEnabled` | `void setAutoEntryEnabled(boolean enabled)` | Writes `"true"` or `"false"` using `INSERT OR REPLACE`. |
| `getSetting` *(private)* | `String getSetting(String key)` | Generic key lookup. Returns `null` if key absent. |
| `putSetting` *(private)* | `void putSetting(String key, String value)` | Generic upsert using `CONFLICT_REPLACE`. |

---

## 5. Models

### 5.1 Customer

**File:** `models/Customer.java`

Represents a milk buyer. Maps 1:1 to a row in the `customer` table.

#### Fields

| Field | Type | Description |
|---|---|---|
| `id` | `long` | Primary key |
| `name` | `String` | Customer display name |
| `mobile` | `String` | 10-digit mobile number |
| `routeGroupId` | `long` | FK to `route_group.group_id` |
| `address` | `String` | Optional address |
| `routeGroupName` | `String` | Denormalized name (joined at query time) |
| `defaultQuantity` | `double` | Legacy single default quantity (L) |
| `defaultQtyMorning` | `double` | Morning default quantity (L) — v5 |
| `defaultQtyEvening` | `double` | Evening default quantity (L) — v5 |
| `currentDue` | `double` | Outstanding balance in Rs |
| `sortOrder` | `int` | Dashboard ordering index — v5 |
| `morningQtySet` | `boolean` | True if morning column was explicitly written (prevents NULL=0 confusion) |
| `eveningQtySet` | `boolean` | Same sentinel for evening column |
| `autoEntryEnabled` | `boolean` | Per-customer auto-entry toggle; default `true` — v6 |
| `isVisited` | `boolean` | Runtime-only flag; set to `true` in POS after navigating to this customer |

#### Constructors

| Constructor | Parameters | Use Case |
|---|---|---|
| Full POS | `(long id, String name, String mobile, long routeGroupId)` | Dashboard navigation loading |
| Billing | `(long id, String name, String mobile, double defaultQuantity, double currentDue)` | Bill queries |
| Minimal | `(long id)` | Lightweight delete/update flows |

---

### 5.2 DailyTransaction

**File:** `models/DailyTransaction.java`

Records one milk delivery or payment event. Maps to `milk_transaction` table.

#### Fields

| Field | Type | Description |
|---|---|---|
| `transactionId` | `int` | Auto-increment primary key |
| `customerId` | `long` | FK to `customer.customer_id` |
| `date` | `String` | ISO date `yyyy-MM-dd` |
| `session` | `String` | `"Morning"`, `"Evening"`, or `"Payment"` |
| `quantity` | `double` | Litres dispensed (0 for payment entries) |
| `amount` | `double` | Rupee amount |
| `timestamp` | `String` | Time `HH:mm:ss` (legacy may contain full ISO datetime) |
| `paymentMode` | `String` | `"Cash"`, `"UPI"`, `"Card"`, or null |
| `milkType` | `String` | `"Regular"` (auto/default entries) or `"Extra"` (manual entries) |

All getters follow standard JavaBean pattern. No setters (fields set via constructors only).

#### Key Constructor Variants (7 total)

| Style | Use Case |
|---|---|
| Full with ID + type | DB read (full) |
| Full without type | DB read — defaults `milkType` to `"Regular"` |
| Full without ID | New insert |
| Without payment mode | Legacy inserts |

---

### 5.3 RouteGroup

**File:** `models/RouteGroup.java`

Represents a folder/colony in the navigation hierarchy. Maps to `route_group` table.

#### Fields

| Field | Type | Description |
|---|---|---|
| `id` | `long` | Primary key; `0` = virtual root |
| `parentId` | `Long` | `null` for root; FK to parent group |
| `name` | `String` | Display name |
| `sortOrder` | `int` | Position within its parent — v4 |

Constructor: `RouteGroup(long id, Long parentId, String name)` — `sortOrder` defaults to 0.

---

### 5.4 NavItem

**File:** `models/NavItem.java`

Union type for `NavigationAdapter`. Wraps either a `RouteGroup` (folder) or a `Customer`.

| Constant | Value | Meaning |
|---|---|---|
| `TYPE_GROUP` | `0` | Folder |
| `TYPE_CUSTOMER` | `1` | Customer |

#### Static Factory Methods

| Method | Description |
|---|---|
| `fromGroup(RouteGroup g)` | Creates a `TYPE_GROUP` NavItem |
| `fromCustomer(Customer c)` | Creates a `TYPE_CUSTOMER` NavItem |

---

## 6. Activities

### 6.1 DashboardActivity

**File:** `activities/DashboardActivity.java`
**Role:** Main launcher screen; manages the hierarchical folder/customer navigation tree.

#### Responsibilities
- Renders route groups and customers using `NavigationAdapter` in a `RecyclerView`.
- Manages navigation stack via `NavigationManager`.
- Controls the master auto-entry toggle switch (reads/writes via `AppSettingsDAO`; calls `AutoEntryScheduler`).
- Opens `MainMenuBottomSheet` for navigation to sub-screens.
- Opens `AddCustomerDialogFragment` to add new customers.
- Handles long-press context menus: rename, delete, move, paste for folders and customers.
- Handles up/down arrow presses for reordering items via DAO swap methods.
- Implements `AddCustomerDialogFragment.OnCustomerCreatedListener`.

#### Key Methods

| Method | Description |
|---|---|
| `onCreate` | Initializes DAOs, RecyclerView, NavigationAdapter, back-press callback, toolbar. Calls `loadCurrentLevel()`. |
| `loadCurrentLevel` | Queries `RouteGroupDAO.getChildGroups` and `CustomerDAO.getCustomersByGroup`; submits to adapter. |
| `showFolderOptions(RouteGroup)` | AlertDialog with: Rename, Delete, Add Customer here, Add Sub-folder. |
| `showCustomerOptions(Customer)` | AlertDialog with: Open in POS, Edit Due, Delete, Move, Paste. |
| `navigateToCustomer(long)` | Starts `PointOfSaleActivity` with customer ID as Intent extra. |
| `pasteCustomer` | Reads `CustomerClipboard`, calls `CustomerDAO.moveCustomerToGroup`, clears clipboard. |
| `onAutoEntryToggleChanged` | Calls `AppSettingsDAO.setAutoEntryEnabled` then `AutoEntryScheduler.scheduleAll` or `cancelAll`. |

---

### 6.2 PointOfSaleActivity

**File:** `activities/PointOfSaleActivity.java`
**Role:** Rapid daily milk delivery entry screen for a single customer.

#### Responsibilities
- Loads `Customer` from the intent's `CUSTOMER_ID` extra.
- Displays today's transactions via `DailyTransactionAdapter`.
- Shows outstanding due and default quantities.
- Provides the "Default Entry" button (one-tap morning/evening entry).
- Hosts `MilkEntryFragment` (delete entries + quick extras + manual entry).
- Opens `PaymentDialog` and `MonthSummaryDialog` from the toolbar.
- Allows navigation between customers in the same route group (prev/next).
- Manages per-customer `auto_entry_enabled` toggle.
- Marks `isVisited = true` on customer entry for visual feedback in dashboard.

#### Key Methods

| Method | Description |
|---|---|
| `onCreate` | Loads customer, sets up RecyclerView, observes `MilkEntryViewModel.getEntryAdded()` LiveData. |
| `loadTodaysEntries` | Calls `DailyTransactionDAO.getTransactionsByDate` for today. |
| `refreshDue` | Re-fetches customer from DB and updates the due TextView. |
| `addDefaultEntry` | Calls `MilkEntryViewModel.addDefaultEntry(customerId)`. |
| `onTransactionClick(DailyTransaction)` | Shows delete confirmation; on confirm calls `DailyTransactionDAO.delete` and reverses due. |
| `navigateToPrev / navigateToNext` | Gets sibling list from `CustomerDAO.getCustomersByGroup`, moves index, restarts activity. |
| `showPaymentDialog` | Launches `PaymentDialog.newInstance(customerId)`. |
| `showMonthSummary` | Launches `MonthSummaryDialog.newInstance(...)`. |
| `toggleAutoEntry` | Saves per-customer flag via `CustomerDAO.updateAutoEntry`. |

---

### 6.3 BillActivity

**File:** `activities/BillActivity.java`
**Role:** Generates and exports customer bill PDFs for a selected date range.

#### Architecture Components
- **Input:** `CUSTOMER_ID` Intent extra.
- **DAOs used:** `DailyTransactionDAO`, `CustomerDAO`.
- **PDF:** `PDFGenerator` utility.
- **Sharing:** `FileProvider` URI for share intent.

#### Key Methods

| Method | Description |
|---|---|
| `onCreate` | Loads customer, sets up date-range pickers (default: first of current month to today). |
| `generateBill` | Calls `DailyTransactionDAO.getTransactionsByDateRange`, constructs `PDFGenerator`, calls `generate()`, launches share intent. |
| `showDateRangePicker` | Opens `DatePickerDialog` for start and end dates. |
| `calculateOpeningBalance` | Computes the customer's balance before the start date for the opening balance line on the bill. |

---

### 6.4 SalesReportActivity

**File:** `activities/SalesReportActivity.java`
**Role:** Visual analytics dashboard: lifetime totals, monthly breakdown, customer ranking, yearly revenue.

#### Displayed Data
1. **Lifetime Summary** — total milk, revenue, collected, outstanding due.
2. **Monthly Breakdown** — bar chart per month for selected year; via `getMonthlyBreakdown`.
3. **Customer Ranking** — top-10 customers by spend; via `getCustomerSalesRanking`.
4. **Yearly Trend** — last 5 years; via `getYearlyRevenueTrend`.

#### Key Methods

| Method | Description |
|---|---|
| `loadLifetimeSummary` | Binds `ReportSummary` fields to summary TextViews. |
| `loadMonthlyBreakdown` | Feeds `MonthlyReportItem` list to `SimpleBarView`. |
| `loadCustomerRanking` | Populates a list of `CustomerSalesItem` objects. |
| `loadYearlyTrend` | Feeds `YearlyReportItem` list to a chart view. |
| `exportReportPdf` | Generates a summary PDF and shares via `FileProvider`. |

---

### 6.5 SettingsActivity

**File:** `activities/SettingsActivity.java`
**Role:** Application settings — currently manages Google Drive backup/restore.

#### Flow
1. User taps Backup or Restore.
2. `SettingsActivity` triggers Google Sign-In via `GoogleSignIn`.
3. `onActivityResult` captures `GoogleSignInAccount`.
4. Builds `Drive` service via `GoogleDriveHelper.buildDriveService`.
5. Calls `GoogleDriveHelper.backup` or `restore` on a background thread.
6. Displays result as Toast or AlertDialog.

#### Key Methods

| Method | Description |
|---|---|
| `onCreate` | Binds UI buttons; checks existing sign-in state. |
| `signInAndBackup / signInAndRestore` | Starts `startActivityForResult` with Google Sign-In intent. |
| `onActivityResult` | Resolves account on `RC_SIGN_IN`; dispatches to Drive operation. |
| `handleBackup / handleRestore` | Wraps Drive calls with progress UI. |

---

## 7. Receivers

### 7.1 AutoEntryReceiver

**File:** `AutoEntryReceiver.java`
**Extends:** `BroadcastReceiver`

Fired by `AlarmManager` at 5:00 AM (morning) and 6:00 PM (evening). Inserts automatic milk entries for all eligible customers.

#### Logic Flow (`onReceive`)
1. Check `AppSettingsDAO.isAutoEntryEnabled()` — abort if OFF.
2. Query all customers via `CustomerDAO.getAllCustomers()`.
3. For each customer where `autoEntryEnabled == true`:
   - Get session-specific default qty.
   - Look up today's milk rate via `MilkRateDAO.getRateForDate`.
   - Calculate `amount = quantity x rate`.
   - Call `DailyTransactionDAO.insert(...)` with `milkType = "Regular"`.
   - Call `CustomerDAO.updateCustomerDue(customerId, amount)`.

#### Constants

| Constant | Value |
|---|---|
| `ACTION_MORNING` | `"com.ignishers.milkmanager2.AUTO_ENTRY_MORNING"` |
| `ACTION_EVENING` | `"com.ignishers.milkmanager2.AUTO_ENTRY_EVENING"` |

---

### 7.2 AutoEntryScheduler

**File:** `AutoEntryScheduler.java`

Static utility for scheduling/cancelling the two daily `AlarmManager` alarms.

#### Methods

| Method | Signature | Description |
|---|---|---|
| `scheduleAll` | `static void scheduleAll(Context context)` | Schedules morning (5:00 AM) and evening (6:00 PM) repeating alarms. Uses exact alarms on Android 12+ if permission granted. |
| `cancelAll` | `static void cancelAll(Context context)` | Cancels both pending intents. |
| `scheduleMorning` *(private)* | Schedules/cancels the 5 AM alarm. |
| `scheduleEvening` *(private)* | Schedules/cancels the 6 PM alarm. |

#### Design Notes
- Uses `PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`.
- `Calendar.getInstance()` sets `HOUR_OF_DAY`; if time has passed today `setRepeating` fires next day.

---

### 7.3 BootReceiver

**File:** `BootReceiver.java`
**Extends:** `BroadcastReceiver`

Listens for `BOOT_COMPLETED`. Restores `AlarmManager` alarms after reboot.

#### Logic (`onReceive`)
1. Check `AppSettingsDAO.isAutoEntryEnabled()`.
2. If `true`, call `AutoEntryScheduler.scheduleAll(context)`.

---

## 8. ViewModels

### 8.1 MilkEntryViewModel

**File:** `viewmodels/MilkEntryViewModel.java`
**Extends:** `AndroidViewModel`

Bridges Activities/Fragments with the DAO layer. All write operations run on a dedicated single-thread `Executor`.

#### Fields

| Field | Type | Description |
|---|---|---|
| `entryAdded` | `MutableLiveData<Boolean>` | Emits `true` after any successful write; triggers UI refresh |
| `dao` | `DailyTransactionDAO` | Transaction persistence |
| `customerDao` | `CustomerDAO` | Customer due updates |
| `rateDao` | `MilkRateDAO` | Rate lookups |
| `executor` | `Executor` | Single-thread background executor |

#### Methods

| Method | Description |
|---|---|
| `getEntryAdded()` | Returns observable `LiveData<Boolean>`. |
| `getRateForDate(String date)` | Synchronous read for current rate. |
| `addDefaultEntry(long customerId)` | Reads customer default qty, determines session by current hour (2 AM–3 PM = Morning, else Evening), inserts `"Regular"` transaction, updates due, posts `entryAdded = true`. |
| `addManualEntry(long customerId, String dateStr, double qty, double amount)` | Inserts an `"Extra"` typed transaction for any date. |
| `addNightEntry(long customerId)` | Forces `"Evening"` session regardless of hour. |
| `addEntry(long customerId, String sessionType, double qty, double amount)` | Generic entry for a known session; type = `"Extra"`. |
| `addPayment(long customerId, double amount, String method, String dateStr)` | Inserts a `"Payment"` session transaction (qty=0); decreases due by amount. |
| `addQuickEntry(long customerId, String value)` | Parses `"250ml"`, `"1l"`, or `"30rs"` strings; inserts as `"Extra"`. |
| `deleteTodaySession(long customerId, String session)` | Calls `dao.deleteTodaySession`; if amount > 0, reverses customer due. Posts `entryAdded = true`. |
| `hasTodayEntrySync(long customerId, String session)` | Synchronous DB check. Must NOT be called on main thread. |

#### Session Boundary Rule
Hours 2–14 (2 AM to 2:59 PM) = **Morning**; hours 15–1 = **Evening**. Late night entries count as previous evening.

---

## 9. Adapters

### 9.1 NavigationAdapter

**File:** `adapters/NavigationAdapter.java`
**Extends:** `RecyclerView.Adapter<RecyclerView.ViewHolder>`

Two-view-type adapter for the dashboard list. Renders folders (`FolderVH`) and customers (`CustomerVH`).

#### Interface: `OnItemClickListener`

| Callback | Description |
|---|---|
| `onFolderClick(long groupId)` | Navigate into folder |
| `onCustomerClick(long customerId)` | Open POS |
| `onFolderLongClick(RouteGroup)` | Show folder options |
| `onCustomerLongClick(Customer)` | Show customer options |
| `onFolderMoveUp(int pos, RouteGroup)` | Up arrow pressed on folder |
| `onFolderMoveDown(int pos, RouteGroup)` | Down arrow pressed on folder |
| `onCustomerMoveUp(int pos, Customer)` | Up arrow pressed on customer |
| `onCustomerMoveDown(int pos, Customer)` | Down arrow pressed on customer |

#### Methods

| Method | Description |
|---|---|
| `submit(List<RouteGroup>, List<Customer>)` | Rebuilds items list (groups first, then customers). |
| `getItemViewType(int position)` | Returns `TYPE_GROUP` or `TYPE_CUSTOMER`. |
| `onCreateViewHolder` | Inflates `item_folder` or `item_customer` layout. |
| `onBindViewHolder` | Dispatches to `FolderVH.bind` or `CustomerVH.bind`. |

#### Inner ViewHolder: `FolderVH`
- Displays folder name.
- Up/down arrow alpha and enabled state based on position.
- Click/long-click wired to listener callbacks.

#### Inner ViewHolder: `CustomerVH`
- Customer name shown **bold** (unvisited) or **grayed 50%** (visited).
- Up/down arrows bounded to first/last customer positions.

---

### 9.2 DailyTransactionAdapter

**File:** `adapters/DailyTransactionAdapter.java`
**Extends:** `RecyclerView.Adapter<DailyTransactionAdapter.TransactionViewHolder>`

Renders the transaction list in `PointOfSaleActivity`.

#### Interface: `OnTransactionClickListener`
`void onTransactionClick(DailyTransaction transaction)` — called on row click and "−" delete button.

#### Methods

| Method | Description |
|---|---|
| `submitList(List<DailyTransaction>)` | Replaces data and calls `notifyDataSetChanged()`. |
| `getItemCount()` | Returns size of internal list. |

#### Inner ViewHolder: `TransactionViewHolder`

| Method | Description |
|---|---|
| `bind(DailyTransaction)` | Sets session/time/quantity/amount. Payment rows shown in green. Timestamps formatted to `hh:mm a` with ISO legacy support. |
| `animateHighlight()` | On new entries within 10s: cyan background fade-out (8 s) + subtle scale bounce. |
| `isRecent(String dateStr, String timeStr)` | Returns `true` if the transaction was created within the last 10 seconds. |

---

## 10. Fragments

### 10.1 MilkEntryFragment

**File:** `fragments/MilkEntryFragment.java`
**Extends:** `Fragment`
**Hosted in:** `PointOfSaleActivity`

Provides delete-today-entry controls, quick-extras spinner, and manual entry button.

#### Interface: `OnEntryListener`
`void onEntryAdded()` — implemented by `PointOfSaleActivity` to refresh due and list.

#### Factory Method
`static MilkEntryFragment newInstance(long customerId)`

#### Key Methods

| Method | Description |
|---|---|
| `onCreateView` | Inflates `fragment_milk_entry`; binds `MilkEntryViewModel`; sets up delete buttons, spinner, manual entry button; calls `updateButtonStates()`. |
| `updateButtonStates()` | Background thread checks `hasTodayEntrySync` for both sessions. UI thread enables/disables buttons with alpha + text feedback. |
| `onResume` | Calls `updateButtonStates()` to reflect changes after dialogs close. |

#### Spinner Values
`{"Add Extra...", "250ml", "500ml", "750ml", "1l", "10rs", "20rs", "30rs", "50rs"}`
Selection triggers `MilkEntryViewModel.addQuickEntry`.

---

## 11. Bottom Sheets

### 11.1 MainMenuBottomSheet

**File:** `bottomsheets/MainMenuBottomSheet.java`
**Extends:** `BottomSheetDialogFragment`

Slide-up menu from `DashboardActivity` toolbar. Three options: Sales Record, Milk Price, Settings.

#### Interface: `OnMenuItemClickListener`
Callbacks: `onSalesRecordClick()`, `onMilkPriceClick()`, `onSettingsClick()`.

#### Methods

| Method | Description |
|---|---|
| `setListener(OnMenuItemClickListener)` | Injects callback. |
| `setDismissAction(Runnable)` | Optional action on sheet dismiss. |
| `onViewCreated` | Binds buttons; calls `dismiss()` after routing event. |
| `animateItem(View, long delay)` | Staggered slide-up + fade-in per item (100ms, 200ms, 300ms). |

---

### 11.2 ManualEntryBottomSheet

**File:** `bottomsheets/ManualEntryBottomSheet.java`
**Extends:** `BottomSheetDialogFragment`

Form for manually entering milk quantity for any date, in any unit (L / ml / Rs).

#### Factory Method
`static ManualEntryBottomSheet newInstance(long customerId)`

#### Key Methods

| Method | Description |
|---|---|
| `setupDate()` | Defaults to today; opens `DatePickerDialog` on click; refreshes rate/preview on date change. |
| `updateDateDisplay()` | Formats `selectedDate`; fetches `currentRate` via `viewModel.getRateForDate`. |
| `setupChips()` | Updates `tilValue` hint/suffix on chip selection; calls `updatePreview()`. |
| `updatePreview()` | Converts input to qty/amount by selected mode; displays live preview. |
| `saveEntry()` | Validates and calls `viewModel.addManualEntry`; dismisses. |

---

## 12. Dialogs

### 12.1 AddCustomerDialogFragment

**File:** `dialogs/AddCustomerDialogFragment.java`
**Extends:** `BottomSheetDialogFragment`

Form for creating a new customer. Opens in `STATE_EXPANDED` mode.

#### Factory Method
`static AddCustomerDialogFragment newInstance(long routeGroupId)`

#### Interface: `OnCustomerCreatedListener`
`void onCustomerCreated()` — implemented by `DashboardActivity`.

#### Input Controls
Name, Mobile (10 digits), Morning Qty, Evening Qty, Current Due (optional), Unit (L or Rs).

#### Key Methods

| Method | Description |
|---|---|
| `validateForm()` | Enables Create button only when all required fields pass. |
| `createCustomer()` | Converts Rupee input to litres if needed; calls `CustomerDAO.insertCustomer`; notifies listener; dismisses. |
| `onStart` | Forces `STATE_EXPANDED` with `skipCollapsed=true`. |

---

### 12.2 MonthSummaryDialog

**File:** `dialogs/MonthSummaryDialog.java`
**Extends:** `DialogFragment`

Full-width dialog with a day-by-day table of milk entries. Month/year selectable via spinners.

#### Factory Method
`static MonthSummaryDialog newInstance(long customerId, String month, String year)`

#### Key Methods

| Method | Description |
|---|---|
| `setupSpinners(TableLayout)` | Populates month (Jan–Dec) and year (2020–2049) spinners. Changes trigger `loadTableData`. |
| `loadTableData(TableLayout)` | Fetches transactions for the selected month; groups by day into `DayData` map; builds `TableRow` views programmatically. Alternates row background. |
| `createTextView(String, float)` | Helper for weighted `TextView` in `TableRow`. |

#### Inner Class: `DayData`
Accumulates `morQty`, `morAmt`, `eveQty`, `eveAmt`, `extraQty`, `extraAmt`, `paymentAmt` per day. Session is classified using timestamp time-boundary logic.

---

### 12.3 PaymentDialog

**File:** `dialogs/PaymentDialog.java`
**Extends:** `DialogFragment`

Modal for recording a payment (Cash/UPI/Card) against a customer's outstanding due.

#### Factory Method
`static PaymentDialog newInstance(long customerId)`

#### Input Controls
Date picker, Amount field, Payment method chips (Cash default).

#### Key Methods

| Method | Description |
|---|---|
| `showDatePicker()` | Opens `DatePickerDialog`. |
| `updateDateDisplay()` | Formats and sets date text. |
| `submitPayment()` | Validates amount; calls `MilkEntryViewModel.addPayment`; dismisses. |

---

### 12.4 SetMilkPriceDialog

**File:** `dialogs/SetMilkPriceDialog.java`
**Extends:** `DialogFragment`

Dialog for setting a new milk price effective on a user-selected date.

#### Input Controls
Price field (pre-filled with current rate), Effective date picker.

#### Key Methods

| Method | Description |
|---|---|
| `showDatePicker()` | Opens `DatePickerDialog`. |
| `savePrice()` | Calls `MilkRateDAO.insertRate(rate, date)`. Old prices not deleted — retained for historical accuracy. |

---

## 13. Managers

### 13.1 NavigationManager

**File:** `managers/NavigationManager.java`

Maintains the user's position in the folder hierarchy using a `Stack<Long>` of group IDs.

#### Methods

| Method | Signature | Description |
|---|---|---|
| `enterGroup` | `void enterGroup(long groupId)` | Pushes group ID onto the stack. |
| `goBack` | `Long goBack()` | Pops top ID; returns new current group or `null` if at root. |
| `getCurrentGroup` | `Long getCurrentGroup()` | Peeks top of stack; `null` if at root. |
| `isAtRoot` | `boolean isAtRoot()` | Returns `true` if stack is empty. |
| `getPathStack` | `Stack<Long> getPathStack()` | Returns a defensive copy (for breadcrumb). |
| `clear` | `void clear()` | Resets to root. |

---

## 14. Utilities

### 14.1 PDFGenerator

**File:** `utils/PDFGenerator.java`

Generates a multi-page A4 PDF bill using the Android `PdfDocument` API. Saves to public Downloads.

#### Constructor
`PDFGenerator(Context, Customer, List<DailyTransaction>, double initialOpeningBalance, double headerTotalDue)`

#### Page Layout Constants

| Constant | Value |
|---|---|
| `PAGE_WIDTH` | 595 pts (A4) |
| `PAGE_HEIGHT` | 842 pts (A4) |
| `MARGIN` | 30 pts |
| `COL_DATE` | `MARGIN` |
| `COL_MORN` | `MARGIN + 60` |
| `COL_EVE` | `MARGIN + 160` |
| `COL_EXTRA` | `MARGIN + 260` |
| `COL_PAY` | `MARGIN + 360` |

#### Methods

| Method | Description |
|---|---|
| `initPaints()` | Creates 7 `Paint` objects for title, header, subHeader, text, amounts, lines, and watermark. |
| `generate(String fileName)` | Main method. Pre-calculates opening balances per month. Groups by month then day. Renders pages with headers, table rows, month totals, page breaks. Saves to Downloads with duplicate-file counters. Returns `File` or `null`. |
| `drawMainHeader(Canvas, int y)` | Draws "Milk Manager 2" title, customer info, total due. Returns new `y` offset. |
| `drawTableHeader(Canvas, int y)` | Draws Date / Morning / Evening / Extra / Payment column headers. |
| `drawWatermark(Canvas)` | Draws "IGNISHERS" watermark at page bottom. |
| `formatMilk(double qty)` | Returns `"X.X L"` if >=1 litre, else `"XXX ml"`. |

---

### 14.2 GoogleDriveHelper

**File:** `utils/GoogleDriveHelper.java`

Static utility for Google Drive backup/restore of `milky_mist_db`.

#### Constants
`BACKUP_FOLDER_NAME = "MilkManager2_Backups"`, `BACKUP_FILE_NAME = "milky_mist_db.backup"`, `DB_NAME = "milky_mist_db"`.

#### Interface: `DriveCallback`
`onSuccess(String message)`, `onFailure(String error)`.

#### Methods

| Method | Description |
|---|---|
| `buildDriveService(Context, GoogleSignInAccount)` | Creates authenticated `Drive` using `GoogleAccountCredential` with `DRIVE_FILE` scope and `NetHttpTransport`. |
| `backup(Context, Drive, DriveCallback)` | Background thread: locates/creates backup folder, deletes old backup, uploads DB as `milky_mist_db.backup`. |
| `restore(Context, Drive, DriveCallback)` | Background thread: downloads backup to temp file, overwrites live DB file. App must restart. |
| `getOrCreateFolder(Drive)` *(private)* | Finds or creates `MilkManager2_Backups` folder in Drive. Returns folder ID. |
| `findFile(Drive, String, String)` *(private)* | Finds file by name within a parent folder. Returns file ID or null. |

> **Warning:** `restore` replaces the live DB directly. All open DB connections should be closed. App must restart.

---

### 14.3 CustomerClipboard

**File:** `utils/CustomerClipboard.java`

Static in-memory clipboard for the "Move Customer" cut/paste operation.

#### Methods

| Method | Description |
|---|---|
| `copy(long id, String name)` | Stores a customer for moving. |
| `getCustomerId()` | Returns stored ID or null. |
| `getCustomerName()` | Returns stored name or null. |
| `clear()` | Clears the clipboard. |
| `hasClip()` | Returns `true` if a customer is pending move. |

> **Scope:** Process-level only; not persisted across app restarts.

---

### 14.4 DialogUtils

**File:** `utils/DialogUtils.java`

Factory for a custom-styled warning/confirmation dialog.

#### Interface: `OnPositiveClickListener`
`void onPositiveClick()` — called when user confirms.

#### Methods

| Method | Description |
|---|---|
| `showWarningDialog(Context, title, message, positiveAction, listener)` | Inflates `dialog_warning` CardView layout; shows with transparent window background; parameterized positive button text. |

---

### 14.5 SimpleTextWatcher

**File:** `utils/SimpleTextWatcher.java`
**Implements:** `TextWatcher`

Convenience wrapper — only `onTextChanged` is active.

Constructor: `SimpleTextWatcher(Runnable onTextChanged)`

`beforeTextChanged` and `afterTextChanged` are no-ops. `onTextChanged` calls `onTextChanged.run()`.

---

## 15. Custom Views

### 15.1 SimpleBarView

**File:** `views/SimpleBarView.java`
**Extends:** `View`

Custom canvas-drawn bar chart used in `SalesReportActivity`.

#### Inner Class: `BarItem`
Fields: `String label`, `int value`. Constructor: `BarItem(String l, int v)`.

#### Methods

| Method | Description |
|---|---|
| `setData(List<BarItem>)` | Sets data, recalculates `maxVal`, calls `invalidate()`. |
| `onDraw(Canvas)` | Renders bars (60% of slot width, 40% spacing) proportional to `maxVal`. Label below bar, value above. Bar color: `#4F46E5` (Indigo). |

---

### 15.2 SimplePieView

**File:** `views/SimplePieView.java`
**Extends:** `View`

Custom canvas-drawn pie chart for revenue breakdowns.

#### Inner Class: `PieItem`
Fields: `String label`, `float value`. Constructor: `PieItem(String l, float v)`.

#### Color Palette (cycled)
`#4F46E5` (Indigo), `#14B8A6` (Teal), `#F59E0B` (Amber), `#EF4444` (Red), `#8B5CF6` (Violet).

#### Methods

| Method | Description |
|---|---|
| `setData(List<PieItem>)` | Sets data, calls `invalidate()`. |
| `onDraw(Canvas)` | Computes total, draws arc segments. Radius = 40% of min dimension. |

---

## 16. Database Schema Reference

### Table: `route_group`
```sql
CREATE TABLE route_group (
    group_id        INTEGER PRIMARY KEY,
    parent_group_id INTEGER REFERENCES route_group(group_id),
    group_name      TEXT NOT NULL,
    sort_order      INTEGER DEFAULT 0
);
-- Seed: INSERT INTO route_group VALUES (0, NULL, 'MAIN_DEPOT', 0)
```

### Table: `customer`
```sql
CREATE TABLE customer (
    customer_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name        TEXT NOT NULL,
    mobile_number        TEXT,
    route_group_id       INTEGER REFERENCES route_group(group_id),
    address              TEXT,
    default_quantity     REAL DEFAULT 1.0,
    default_qty_morning  REAL,         -- v5
    default_qty_evening  REAL,         -- v5
    current_due          REAL DEFAULT 0.0,
    sort_order           INTEGER DEFAULT 0,  -- v5
    auto_entry_enabled   INTEGER DEFAULT 1   -- v6 (1=true, 0=false)
);
```

### Table: `global_milk_price`
```sql
CREATE TABLE global_milk_price (
    price_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    price_per_litre REAL NOT NULL,
    effective_date  TEXT NOT NULL   -- yyyy-MM-dd
);
-- Seed: INSERT INTO global_milk_price VALUES (NULL, 60, date('now'))
```

### Table: `milk_transaction`
```sql
CREATE TABLE milk_transaction (
    transaction_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id_fk   INTEGER NOT NULL REFERENCES customer(customer_id),
    transaction_date TEXT NOT NULL,             -- yyyy-MM-dd
    session          TEXT,                       -- 'Morning', 'Evening', 'Payment'
    quantity         REAL DEFAULT 0.0,
    amount           REAL DEFAULT 0.0,
    timestamp        TEXT,                       -- HH:mm:ss
    payment_mode     TEXT,                       -- v5: 'Cash', 'UPI', 'Card'
    milk_type        TEXT DEFAULT 'Regular'      -- v6: 'Regular' or 'Extra'
);
```

### Table: `payment` *(reserved, currently unused)*
```sql
CREATE TABLE payment (
    payment_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id   INTEGER REFERENCES customer(customer_id),
    payment_date  TEXT,
    amount        REAL
);
```
> Payment records are stored in `milk_transaction` (session = "Payment"). This table is declared but not used.

### Table: `app_settings`
```sql
CREATE TABLE app_settings (
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT
);
-- Currently only key: 'auto_entry_enabled'
```

### Schema Migration History

| Version | Changes |
|---|---|
| 1..2 | Initial schema |
| 2 -> 3 | Added `default_qty_morning`, `default_qty_evening`, `sort_order` to `customer` |
| 3 -> 4 | Added `sort_order` to `route_group` |
| 4 -> 5 | Added `payment_mode` to `milk_transaction` |
| 5 -> 6 | Added `milk_type` to `milk_transaction`; added `auto_entry_enabled` to `customer`; created `app_settings` table; ensured root route group exists |

---

## 17. Data Flow Diagrams

### Auto-Entry Flow
```
Device Clock
    |
    | 5:00 AM / 6:00 PM
    v
AlarmManager ──fires──> AutoEntryReceiver.onReceive()
                              |
                              |-- AppSettingsDAO.isAutoEntryEnabled() --> [false] --> ABORT
                              |
                              |-- CustomerDAO.getAllCustomers()
                              |       |
                              |       +-- for each customer where autoEntryEnabled:
                              |               |
                              |               |-- MilkRateDAO.getRateForDate(today)
                              |               |
                              |               |-- DailyTransactionDAO.insert(transaction)
                              |               |
                              |               +-- CustomerDAO.updateCustomerDue(+amount)
                              |
                              +-- Done
```

### POS Transaction Flow
```
PointOfSaleActivity
    |
    |-- "Default Entry" button
    |       +-> MilkEntryViewModel.addDefaultEntry()
    |                   +-> [background executor]
    |                           |-- CustomerDAO.getCustomer()
    |                           |-- MilkRateDAO.getRateForDate()
    |                           |-- DailyTransactionDAO.insert()
    |                           |-- CustomerDAO.updateCustomerDue(+amount)
    |                           +-- entryAdded.postValue(true)
    |                                   +-> [Main Thread] Activity refreshes UI
    |
    |-- ManualEntryBottomSheet -> MilkEntryViewModel.addManualEntry()
    |-- PaymentDialog          -> MilkEntryViewModel.addPayment()
    +-- MilkEntryFragment      -> MilkEntryViewModel.addQuickEntry()
```

### Bill Generation Flow
```
BillActivity
    |
    |-- User selects date range
    |
    |-- DailyTransactionDAO.getTransactionsByDateRange(customerId, start, end)
    |
    |-- PDFGenerator(context, customer, transactions, openingBalance, totalDue)
    |       +-> generate(fileName)
    |                   |-- Pre-calculate opening balances per month
    |                   |-- Group by month then day
    |                   |-- Render pages (header, table, watermark)
    |                   +-- Save to Downloads/
    |
    +-- FileProvider URI -> Share Intent -> PDF viewer / WhatsApp etc.
```

### Google Drive Backup Flow
```
SettingsActivity
    |
    |-- GoogleSignIn.getClient().signIn()
    |       +-> onActivityResult -> GoogleSignInAccount
    |
    |-- GoogleDriveHelper.buildDriveService(context, account)
    |
    +-- GoogleDriveHelper.backup(context, driveService, callback)
            +-> [Background Thread]
                    |-- context.getDatabasePath("milky_mist_db")
                    |-- getOrCreateFolder() -> folderId
                    |-- findFile() -> delete old backup if exists
                    |-- driveService.files().create(metadata, content).execute()
                    +-- callback.onSuccess(message)
```

---

*Document generated: 2026-04-22 | MilkManager2 v2.0 | Schema v6*
