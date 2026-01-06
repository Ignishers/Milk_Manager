package com.ignishers.milkmanager2.DAO;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "milky_mist_db";
    private static final int DB_VERSION = 3; // Increment version if you already deployed v1

    // ==========================================
    // 1. ROUTE GROUP TABLE (Hierarchy) - LOOKS GOOD
    // ==========================================
    public static final String ROUTE_TABLE = "route_group";
    public static final String COL_ROUTE_ID = "group_id";  // PK
    public static final String COL_PARENT_ROUTE_ID = "parent_group_id"; // FK to itself
    public static final String COL_ROUTE_NAME = "group_name";

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
    // <<< NEW: Default daily amount to speed up data entry (e.g., 1.0, 1.5)
    public static final String COL_DEFAULT_QUANTITY = "default_quantity";
    // Keep current due, but recalculate it frequently based on transactions vs payments
    public static final String COL_CUSTOMER_CURRENT_DUE = "customer_due_balance";

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
                COL_ROUTE_NAME + " TEXT" + ");";
        db.execSQL(createRouteTable);

        // Creating CUSTOMER TABLE (Added FK to Route)
        String createCustomerTable = "CREATE TABLE " + CUSTOMER_TABLE + " (" +
                COL_CUSTOMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CUST_ROUTE_ID_FK + " INTEGER, " + // The link column
                COL_CUSTOMER_NAME + " TEXT, " +
                COL_CUSTOMER_MOBILE + " TEXT, " +
                COL_CUSTOMER_ADDRESS_DETAIL + " TEXT, " +
                COL_DEFAULT_QUANTITY + " REAL, " +
                COL_CUSTOMER_CURRENT_DUE + " REAL, " +
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
                COL_TRANS_PAYMENT_MODE + " TEXT, " + // NEW COLUMN
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

        // ==========================================
        // NEW: INSERT ROOT NODE (ID 0) AUTOMATIZED
        // ==========================================
        // This ensures you have a "Base" for your hierarchy
        android.content.ContentValues rootValues = new android.content.ContentValues();
        rootValues.put(COL_ROUTE_ID, 0);
        rootValues.put(COL_ROUTE_NAME, "MAIN_DEPOT"); // Or "All Routes"
        // Parent is NULL for the absolute root
        db.insert(ROUTE_TABLE, null, rootValues);

        // ==========================================
        // NEW: INSERT DEFAULT MILK PRICE
        // ==========================================
        android.content.ContentValues priceValues = new android.content.ContentValues();
        priceValues.put(COL_GLOBAL_PRICE_PER_LITRE, 60.0);
        priceValues.put(COL_EFFECTIVE_DATE, "2020-01-01");
        db.insert(MILK_PRICE_TABLE, null, priceValues);
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
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

}

