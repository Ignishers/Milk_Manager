package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.models.Customer;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Manages database creation, schema design, and version management.
 * <p>
 * Inheriting from {@link android.database.sqlite.SQLiteOpenHelper}, this class ensures
 * that the internal SQLite database {@code milky_mist_db} is properly initialized.
 * It defines the Data Definition Language (DDL) for core tables including:
 * <ul>
 *   <li>Route Group Table (Hierarchy support)</li>
 *   <li>Customer Table</li>
 *   <li>Milk Price Table</li>
 *   <li>Transaction Table</li>
 *   <li>Payment Table</li>
 * </ul>
 * </p>
 * <p>
 * <b>Data Flow:</b> Whenever an application context requests a readable/writable database,
 * this class provides the connection instance. All DAO classes rely on this helper.
 * </p>
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "milky_mist_db";
    private static final int DB_VERSION = 6; // v6: per-customer auto_entry_enabled toggle

    // ==========================================
    // 1. ROUTE GROUP TABLE (Hierarchy) - LOOKS GOOD
    // ==========================================
    public static final String ROUTE_TABLE = "route_group";
    public static final String COL_ROUTE_ID = "group_id";  // PK
    public static final String COL_PARENT_ROUTE_ID = "parent_group_id"; // FK to itself
    public static final String COL_ROUTE_NAME = "group_name";
    public static final String COL_ROUTE_SORT_ORDER = "sort_order";

    // ==========================================
    // 2. CUSTOMER TABLE (Improved for real world)
    // ==========================================
    public static final String CUSTOMER_TABLE = "customer";
    public static final String COL_CUSTOMER_ID = "customer_id";  // PK
    // <<< NEW: Link customer to a route/street
    public static final String COL_CUST_ROUTE_ID_FK = "route_id_fk";
    public static final String COL_CUSTOMER_NAME = "customer_name";
    public static final String COL_CUSTOMER_MOBILE = "customer_mobile";
    // <<< NEW: Specific house number/address details within the street
    public static final String COL_CUSTOMER_ADDRESS_DETAIL = "address_detail";
    // Default daily quantities — split into Morning and Evening sessions
    public static final String COL_DEFAULT_QUANTITY = "default_quantity"; // legacy, kept for compat
    public static final String COL_DEFAULT_QTY_MORNING = "default_qty_morning";
    public static final String COL_DEFAULT_QTY_EVENING = "default_qty_evening";
    // Sort order for dashboard reordering
    public static final String COL_CUSTOMER_SORT_ORDER = "sort_order";
    // Per-customer auto-entry toggle (1 = ON, 0 = OFF)
    public static final String COL_CUSTOMER_AUTO_ENTRY = "auto_entry_enabled";
    // Keep current due, but recalculate it frequently based on transactions vs payments
    public static final String COL_CUSTOMER_CURRENT_DUE = "customer_due_balance";

    // ==========================================
    // 6. APP SETTINGS TABLE (Key-Value store)
    // ==========================================
    public static final String SETTINGS_TABLE = "app_settings";
    public static final String COL_SETTING_KEY = "setting_key";
    public static final String COL_SETTING_VALUE = "setting_value";
    public static final String SETTING_AUTO_ENTRY_ENABLED = "auto_entry_enabled";

    // ==========================================
    // 3. GLOBAL MILK PRICE TABLE (Keep for default pricing)
    // ==========================================
    public static final String MILK_PRICE_TABLE = "global_milk_price";
    public static final String COL_PRICE_ID = "price_id";  // PK
    public static final String COL_GLOBAL_PRICE_PER_LITRE = "price_per_litre";
    public static final String COL_EFFECTIVE_DATE = "effective_date";

    // ==========================================
    // 4. DAILY TRANSACTION TABLE (The daily entry)
    // ==========================================
    public static final String MILK_TRANSACTION_TABLE = "milk_transaction";

    public static final String COL_TRANS_ID = "transaction_id";  // PK
    public static final String COL_TRANS_CUSTOMER_ID_FK = "customer_id_fk";
    public static final String COL_TRANS_DATE = "transaction_date";   // yyyy-MM-dd
    public static final String COL_TRANS_SESSION = "transaction_session"; // DAY / NIGHT / MANUAL
    public static final String COL_TRANS_QUANTITY = "transaction_quantity"; // double
    public static final String COL_TRANS_AMOUNT = "transaction_amount";     // double
    public static final String COL_TRANS_TIMESTAMP = "transaction_timestamp"; // millis
    public static final String COL_TRANS_PAYMENT_MODE = "transaction_payment_mode"; // Cash / UPI / Card
    public static final String COL_TRANS_MILK_TYPE = "transaction_milk_type"; // NEW: Regular / Extra

    // ==========================================
    // 5. PAYMENT TABLE (Cash received)
    // ==========================================
    public static final String PAYMENT_TABLE = "payment";
    public static final String COL_PAYMENT_ID = "payment_id";  // PK
    // <<< CRITICAL NEW: Who paid?
    public static final String COL_PAYMENT_CUSTOMER_ID_FK = "customer_id_fk";
    public static final String COL_PAYMENT_DATE = "payment_date";
    public static final String COL_PAYMENT_AMOUNT = "payment_amount";

    // creating a constructor for the database helper class
    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Enable Foreign Key support (must be done every time db is opened, but good practice to note here)
        db.execSQL("PRAGMA foreign_keys=ON;");

        // Creating ROUTE GROUP TABLE (No FK needed here)
        String createRouteTable = "CREATE TABLE " + ROUTE_TABLE + " (" +
                COL_ROUTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PARENT_ROUTE_ID + " INTEGER, " +
                COL_ROUTE_NAME + " TEXT, " +
                COL_ROUTE_SORT_ORDER + " INTEGER DEFAULT 0" + ");";
        db.execSQL(createRouteTable);

        // Creating CUSTOMER TABLE (Added FK to Route)
        String createCustomerTable = "CREATE TABLE " + CUSTOMER_TABLE + " (" +
                COL_CUSTOMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CUST_ROUTE_ID_FK + " INTEGER, " + // The link column
                COL_CUSTOMER_NAME + " TEXT, " +
                COL_CUSTOMER_MOBILE + " TEXT, " +
                COL_CUSTOMER_ADDRESS_DETAIL + " TEXT, " +
                COL_DEFAULT_QUANTITY + " REAL DEFAULT 1.0, " +
                COL_DEFAULT_QTY_MORNING + " REAL DEFAULT 1.0, " +
                COL_DEFAULT_QTY_EVENING + " REAL DEFAULT 1.0, " +
                COL_CUSTOMER_SORT_ORDER + " INTEGER DEFAULT 0, " +
                COL_CUSTOMER_AUTO_ENTRY + " INTEGER NOT NULL DEFAULT 1, " +
                COL_CUSTOMER_CURRENT_DUE + " REAL DEFAULT 0, " +
                // FOREIGN KEY CONSTRAINT:
                "FOREIGN KEY(" + COL_CUST_ROUTE_ID_FK + ") REFERENCES " + ROUTE_TABLE + "(" + COL_ROUTE_ID + ")" +
                ");";
        db.execSQL(createCustomerTable);

        // Creating GLOBAL MILK PRICE TABLE (No FK needed)
        String createMilkPriceTable = "CREATE TABLE " + MILK_PRICE_TABLE + " (" +
                COL_PRICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_GLOBAL_PRICE_PER_LITRE + " REAL, " +
                COL_EFFECTIVE_DATE + " TEXT" + ");";
        db.execSQL(createMilkPriceTable);

        // Creating DAILY TRANSACTION TABLE (Added FK to Customer)
        String createMilkTransactionTable = "CREATE TABLE " + MILK_TRANSACTION_TABLE + " (" +
                COL_TRANS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TRANS_CUSTOMER_ID_FK + " INTEGER, " + // The link column
                COL_TRANS_DATE + " TEXT, " +
                COL_TRANS_SESSION + " TEXT, " +
                COL_TRANS_QUANTITY + " REAL, " +
                COL_TRANS_AMOUNT + " REAL, " +
                COL_TRANS_TIMESTAMP + " TEXT, " +
                COL_TRANS_PAYMENT_MODE + " TEXT, " +
                COL_TRANS_MILK_TYPE + " TEXT, " + // NEW COLUMN
                // FOREIGN KEY CONSTRAINT: Ensure customer exists before adding transaction
                "FOREIGN KEY(" + COL_TRANS_CUSTOMER_ID_FK + ") REFERENCES " + CUSTOMER_TABLE + "(" + COL_CUSTOMER_ID + ") ON DELETE CASCADE" +
                ");";
        db.execSQL(createMilkTransactionTable);

        // Creating PAYMENT TABLE (Added FK to Customer and FIXED YOUR BUG)
        String createPaymentTable = "CREATE TABLE " + PAYMENT_TABLE + " (" +
                COL_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PAYMENT_CUSTOMER_ID_FK + " INTEGER, " + // The link column
                COL_PAYMENT_DATE + " TEXT, " +
                COL_PAYMENT_AMOUNT + " REAL, " +
                // FOREIGN KEY CONSTRAINT:
                "FOREIGN KEY(" + COL_PAYMENT_CUSTOMER_ID_FK + ") REFERENCES " + CUSTOMER_TABLE + "(" + COL_CUSTOMER_ID + ") ON DELETE CASCADE" +
                ");";
        // BUG FIXED IN LINE BELOW:
        db.execSQL(createPaymentTable);

        // Creating APP SETTINGS TABLE
        String createSettingsTable = "CREATE TABLE " + SETTINGS_TABLE + " (" +
                COL_SETTING_KEY + " TEXT PRIMARY KEY, " +
                COL_SETTING_VALUE + " TEXT" + ");"; 
        db.execSQL(createSettingsTable);

        // ==========================================
        // NEW: INSERT ROOT NODE (ID 0) AUTOMATIZED
        // ==========================================
        android.content.ContentValues rootValues = new android.content.ContentValues();
        rootValues.put(COL_ROUTE_ID, 0);
        rootValues.put(COL_ROUTE_NAME, "MAIN_DEPOT");
        rootValues.put(COL_ROUTE_SORT_ORDER, 0);
        db.insert(ROUTE_TABLE, null, rootValues);

        // ==========================================
        // NEW: INSERT DEFAULT MILK PRICE
        // ==========================================
        android.content.ContentValues priceValues = new android.content.ContentValues();
        priceValues.put(COL_GLOBAL_PRICE_PER_LITRE, 60.0);
        priceValues.put(COL_EFFECTIVE_DATE, "2020-01-01");
        db.insert(MILK_PRICE_TABLE, null, priceValues);

        // INSERT DEFAULT SETTINGS
        android.content.ContentValues settingValues = new android.content.ContentValues();
        settingValues.put(COL_SETTING_KEY, SETTING_AUTO_ENTRY_ENABLED);
        settingValues.put(COL_SETTING_VALUE, "false");
        db.insert(SETTINGS_TABLE, null, settingValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Intentionally left blank for Version 1.
        // Future schema changes for V2, V3, etc., will be handled here incrementally.

        if (oldVersion < 2) {
            // USER REQUEST: Do not delete old data. 
            // Commenting out destructive updates for now.
            // db.execSQL("DROP TABLE IF EXISTS " + PAYMENT_TABLE);
            // db.execSQL("DROP TABLE IF EXISTS " + MILK_TRANSACTION_TABLE);
            // db.execSQL("DROP TABLE IF EXISTS " + MILK_PRICE_TABLE);
            // db.execSQL("DROP TABLE IF EXISTS " + CUSTOMER_TABLE);
            // db.execSQL("DROP TABLE IF EXISTS " + ROUTE_TABLE);
            // onCreate(db);
        }
        
        if (oldVersion < 3) {
            try {
                // Add Payment Mode column to existing table
                db.execSQL("ALTER TABLE " + MILK_TRANSACTION_TABLE + " ADD COLUMN " + COL_TRANS_PAYMENT_MODE + " TEXT;");
            } catch (Exception e) {
                 Log.e("DBHelper", "Error upgrading to v3: " + e.getMessage());
            }
        }
        
        if (oldVersion < 4) {
             try {
                 db.execSQL("ALTER TABLE " + MILK_TRANSACTION_TABLE + " ADD COLUMN " + COL_TRANS_MILK_TYPE + " TEXT;");
             } catch (Exception e) {
                 Log.e("DBHelper", "Error upgrading to v4: " + e.getMessage());
             }
        }

        if (oldVersion < 5) {
            // Each statement has its own try-catch so one failure doesn't skip the rest
            safeExec(db, "ALTER TABLE " + CUSTOMER_TABLE + " ADD COLUMN " + COL_DEFAULT_QTY_MORNING + " REAL DEFAULT 1.0;", "v5-morning");
            safeExec(db, "ALTER TABLE " + CUSTOMER_TABLE + " ADD COLUMN " + COL_DEFAULT_QTY_EVENING + " REAL DEFAULT 1.0;", "v5-evening");
            // Copy old default_quantity into both new columns
            safeExec(db, "UPDATE " + CUSTOMER_TABLE + " SET " + COL_DEFAULT_QTY_MORNING + " = " + COL_DEFAULT_QUANTITY
                    + ", " + COL_DEFAULT_QTY_EVENING + " = " + COL_DEFAULT_QUANTITY + ";", "v5-copy");
            safeExec(db, "ALTER TABLE " + CUSTOMER_TABLE + " ADD COLUMN " + COL_CUSTOMER_SORT_ORDER + " INTEGER DEFAULT 0;", "v5-cust-sort");
            safeExec(db, "ALTER TABLE " + ROUTE_TABLE  + " ADD COLUMN " + COL_ROUTE_SORT_ORDER  + " INTEGER DEFAULT 0;", "v5-route-sort");
            safeExec(db, "CREATE TABLE IF NOT EXISTS " + SETTINGS_TABLE + " ("
                    + COL_SETTING_KEY + " TEXT PRIMARY KEY, "
                    + COL_SETTING_VALUE + " TEXT);", "v5-settings");
            safeExec(db, "INSERT OR IGNORE INTO " + SETTINGS_TABLE
                    + " VALUES ('" + SETTING_AUTO_ENTRY_ENABLED + "', 'false');", "v5-settings-val");
        }

        if (oldVersion < 6) {
            safeExec(db, "ALTER TABLE " + CUSTOMER_TABLE
                    + " ADD COLUMN " + COL_CUSTOMER_AUTO_ENTRY + " INTEGER DEFAULT 1;", "v6-auto-entry");
        }
    }

    /** Run a single DDL/DML statement and log but never crash on failure. */
    private void safeExec(SQLiteDatabase db, String sql, String tag) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            Log.w("DBHelper", "Migration [" + tag + "] skipped (likely already applied): " + e.getMessage());
        }
    }


    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

}