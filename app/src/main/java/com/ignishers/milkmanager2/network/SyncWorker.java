package com.ignishers.milkmanager2.network;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ignishers.milkmanager2.database.DBHelper;
import com.ignishers.milkmanager2.managers.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final Context context;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isLoggedIn() || sessionManager.getSellerId() == null) {
            Log.d(TAG, "Not logged in. Skipping sync.");
            return Result.success();
        }

        String sellerId = sessionManager.getSellerId();

        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL)
                .client(client)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
                .build();

        SupabaseSyncService service = retrofit.create(SupabaseSyncService.class);
        String authHeader = "Bearer " + SupabaseConfig.SUPABASE_KEY;
        String prefer = "resolution=merge-duplicates"; // Upsert mode

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Fix orphaned offline data (including MAIN_DEPOT created at install)
        String[] tables = {DBHelper.ROUTE_TABLE, DBHelper.CUSTOMER_TABLE, DBHelper.MILK_PRICE_TABLE, DBHelper.MILK_TRANSACTION_TABLE, DBHelper.PAYMENT_TABLE};
        for (String table : tables) {
            db.execSQL("UPDATE " + table + " SET " + DBHelper.COL_SYNC_SELLER_ID + " = ? WHERE " + DBHelper.COL_SYNC_SELLER_ID + " IS NULL", new String[]{sellerId});
        }

        boolean allSuccess = true;

        try {
            // 1. Sync Route Groups (Parent Dependency)
            if (!syncTable(db, service, DBHelper.ROUTE_TABLE, sellerId, authHeader, prefer)) {
                return abortSync(context, db, "Failed to sync Route Groups.");
            }
            // 2. Sync Customers (Depends on Route Groups)
            if (!syncTable(db, service, DBHelper.CUSTOMER_TABLE, sellerId, authHeader, prefer)) {
                return abortSync(context, db, "Failed to sync Customers.");
            }
            // 3. Sync Milk Prices
            if (!syncTable(db, service, DBHelper.MILK_PRICE_TABLE, sellerId, authHeader, prefer)) {
                return abortSync(context, db, "Failed to sync Milk Prices.");
            }
            // 4. Sync Transactions (Depends on Customers)
            if (!syncTable(db, service, DBHelper.MILK_TRANSACTION_TABLE, sellerId, authHeader, prefer)) {
                return abortSync(context, db, "Failed to sync Transactions.");
            }
            // 5. Sync Payments (Depends on Customers)
            if (!syncTable(db, service, DBHelper.PAYMENT_TABLE, sellerId, authHeader, prefer)) {
                return abortSync(context, db, "Failed to sync Payments.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Sync failed: " + e.getMessage());
            android.content.SharedPreferences prefs = context.getSharedPreferences("SyncPrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("last_error", "Exception: " + e.getMessage()).apply();
            return Result.retry();
        } finally {
            db.close();
        }

        if (allSuccess) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SyncPrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("last_error", "").apply();
        }

        return allSuccess ? Result.success() : Result.retry();
    }

    private Result abortSync(Context context, SQLiteDatabase db, String msg) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("SyncPrefs", android.content.Context.MODE_PRIVATE);
        // If a specific error was not already set, set a generic one
        if (prefs.getString("last_error", "").isEmpty()) {
            prefs.edit().putString("last_error", msg).apply();
        }
        db.close();
        return Result.retry();
    }

    private boolean syncTable(SQLiteDatabase db, SupabaseSyncService service, String tableName, 
                              String sellerId, String authHeader, String prefer) throws IOException {
        
        String primaryKeyColumn = getPrimaryKeyColumn(tableName);
        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences("SyncPrefs", android.content.Context.MODE_PRIVATE);

        while (true) {
            List<Object> recordsToSync = new ArrayList<>();
            List<Long> idsToMarkSynced = new ArrayList<>();

            // Build query based on table priority
            String orderBy = "";
            if (tableName.equals(DBHelper.ROUTE_TABLE)) {
                orderBy = " ORDER BY parent_group_id IS NOT NULL ASC, group_id ASC";
            } else if (tableName.equals(DBHelper.CUSTOMER_TABLE)) {
                orderBy = " ORDER BY route_id_fk ASC";
            }
            
            // Fetch unsynced records in batches of 50
            Cursor cursor;
            if (tableName.equals(DBHelper.MILK_TRANSACTION_TABLE)) {
                cursor = db.rawQuery("SELECT * FROM " + tableName + " WHERE is_synced = 0 AND seller_id = ? AND " + DBHelper.COL_TRANS_SESSION + " NOT LIKE 'Payment%' LIMIT 50", new String[]{sellerId});
            } else if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                cursor = db.rawQuery("SELECT * FROM " + DBHelper.MILK_TRANSACTION_TABLE + " WHERE is_synced = 0 AND seller_id = ? AND " + DBHelper.COL_TRANS_SESSION + " LIKE 'Payment%' LIMIT 50", new String[]{sellerId});
            } else {
                cursor = db.rawQuery("SELECT * FROM " + tableName + " WHERE is_synced = 0 AND seller_id = ?" + orderBy + " LIMIT 50", new String[]{sellerId});
            }
            
            if (cursor.getCount() == 0) {
                cursor.close();
                return true; // Nothing left to sync
            }

            String[] columnNames = cursor.getColumnNames();

            while (cursor.moveToNext()) {
                Map<String, Object> record = new HashMap<>();
                long id = -1;
                for (int i = 0; i < columnNames.length; i++) {
                    String col = columnNames[i];
                    if (col.equals("is_synced") || col.equals("default_quantity")) continue; // Skip local-only columns
                    
                    String outCol = col;
                    if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                        if (col.equals(DBHelper.COL_TRANS_ID)) outCol = DBHelper.COL_PAYMENT_ID;
                        else if (col.equals(DBHelper.COL_TRANS_DATE)) outCol = DBHelper.COL_PAYMENT_DATE;
                        else if (col.equals(DBHelper.COL_TRANS_AMOUNT)) outCol = DBHelper.COL_PAYMENT_AMOUNT;
                        
                        if (col.equals(DBHelper.COL_TRANS_SESSION) || 
                            col.equals(DBHelper.COL_TRANS_QUANTITY) || 
                            col.equals(DBHelper.COL_TRANS_TIMESTAMP) || 
                            col.equals(DBHelper.COL_TRANS_MILK_TYPE) || 
                            col.equals(DBHelper.COL_TRANS_PAYMENT_MODE)) {
                            continue;
                        }
                    }

                    if (col.equals("updated_at")) {
                        long timestamp = cursor.getLong(i);
                        if (timestamp > 0) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                            record.put(outCol, sdf.format(new java.util.Date(timestamp)));
                        }
                        continue;
                    }

                    String checkCol = tableName.equals(DBHelper.PAYMENT_TABLE) ? outCol : col;

                    int type = cursor.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        record.put(outCol, cursor.getLong(i));
                        if (checkCol.equals(primaryKeyColumn)) id = cursor.getLong(i);
                    } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                        record.put(outCol, cursor.getDouble(i));
                    } else if (type == Cursor.FIELD_TYPE_STRING) {
                        record.put(outCol, cursor.getString(i));
                    } else if (type == Cursor.FIELD_TYPE_NULL) {
                        record.put(outCol, null);
                    }
                }
                recordsToSync.add(record);
                if (id != -1) idsToMarkSynced.add(id);
            }
            cursor.close();

            // Push batch to Supabase
            Response<Void> response = null;
            if (tableName.equals(DBHelper.ROUTE_TABLE)) {
                response = service.upsertRoutes(SupabaseConfig.SUPABASE_KEY, authHeader, prefer, recordsToSync).execute();
            } else if (tableName.equals(DBHelper.CUSTOMER_TABLE)) {
                response = service.upsertCustomers(SupabaseConfig.SUPABASE_KEY, authHeader, prefer, recordsToSync).execute();
            } else if (tableName.equals(DBHelper.MILK_PRICE_TABLE)) {
                response = service.upsertPrices(SupabaseConfig.SUPABASE_KEY, authHeader, prefer, recordsToSync).execute();
            } else if (tableName.equals(DBHelper.MILK_TRANSACTION_TABLE)) {
                response = service.upsertTransactions(SupabaseConfig.SUPABASE_KEY, authHeader, prefer, recordsToSync).execute();
            } else if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                response = service.upsertPayments(SupabaseConfig.SUPABASE_KEY, authHeader, prefer, recordsToSync).execute();
            }

            // Mark batch as synced locally if successful
            if (response != null && response.isSuccessful()) {
                db.beginTransaction();
                try {
                    String localTable = tableName.equals(DBHelper.PAYMENT_TABLE) ? DBHelper.MILK_TRANSACTION_TABLE : tableName;
                    String localPK = tableName.equals(DBHelper.PAYMENT_TABLE) ? DBHelper.COL_TRANS_ID : primaryKeyColumn;

                    for (Long id : idsToMarkSynced) {
                        db.execSQL("UPDATE " + localTable + " SET is_synced = 1 WHERE " + localPK + " = " + id);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                Log.d(TAG, "Synced batch of " + recordsToSync.size() + " records for " + tableName);
            } else {
                String err = response != null ? response.errorBody().string() : "Unknown network error";
                Log.e(TAG, "Failed to sync " + tableName + " batch: " + err);
                prefs.edit().putString("last_error", "Failed on table " + tableName + " (Response " + response.code() + "): " + err).apply();
                return false; // Stop syncing this table on failure
            }
        }
    }

    private String getPrimaryKeyColumn(String tableName) {
        if (tableName.equals(DBHelper.ROUTE_TABLE)) return DBHelper.COL_ROUTE_ID;
        if (tableName.equals(DBHelper.CUSTOMER_TABLE)) return DBHelper.COL_CUSTOMER_ID;
        if (tableName.equals(DBHelper.MILK_PRICE_TABLE)) return DBHelper.COL_PRICE_ID;
        if (tableName.equals(DBHelper.MILK_TRANSACTION_TABLE)) return DBHelper.COL_TRANS_ID;
        if (tableName.equals(DBHelper.PAYMENT_TABLE)) return DBHelper.COL_PAYMENT_ID;
        return "id";
    }
}
