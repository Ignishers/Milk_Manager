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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseSyncService service = retrofit.create(SupabaseSyncService.class);
        String authHeader = "Bearer " + SupabaseConfig.SUPABASE_KEY;
        String prefer = "resolution=merge-duplicates"; // Upsert mode

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        boolean allSuccess = true;

        try {
            // 1. Sync Route Groups
            if (!syncTable(db, service, DBHelper.ROUTE_TABLE, sellerId, authHeader, prefer)) allSuccess = false;
            // 2. Sync Customers
            if (!syncTable(db, service, DBHelper.CUSTOMER_TABLE, sellerId, authHeader, prefer)) allSuccess = false;
            // 3. Sync Milk Prices
            if (!syncTable(db, service, DBHelper.MILK_PRICE_TABLE, sellerId, authHeader, prefer)) allSuccess = false;
            // 4. Sync Transactions
            if (!syncTable(db, service, DBHelper.MILK_TRANSACTION_TABLE, sellerId, authHeader, prefer)) allSuccess = false;
            // 5. Sync Payments
            if (!syncTable(db, service, DBHelper.PAYMENT_TABLE, sellerId, authHeader, prefer)) allSuccess = false;

        } catch (Exception e) {
            Log.e(TAG, "Sync failed: " + e.getMessage());
            return Result.retry();
        } finally {
            db.close();
        }

        return allSuccess ? Result.success() : Result.retry();
    }

    private boolean syncTable(SQLiteDatabase db, SupabaseSyncService service, String tableName, 
                              String sellerId, String authHeader, String prefer) throws IOException {
        
        List<Object> recordsToSync = new ArrayList<>();
        List<Long> idsToMarkSynced = new ArrayList<>();
        String primaryKeyColumn = getPrimaryKeyColumn(tableName);

        // Fetch unsynced records for this seller
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName + " WHERE is_synced = 0 AND seller_id = ?", new String[]{sellerId});
        
        if (cursor.getCount() == 0) {
            cursor.close();
            return true; // Nothing to sync
        }

        String[] columnNames = cursor.getColumnNames();

        while (cursor.moveToNext()) {
            Map<String, Object> record = new HashMap<>();
            long id = -1;
            for (int i = 0; i < columnNames.length; i++) {
                String col = columnNames[i];
                if (col.equals("is_synced") || col.equals("default_quantity")) continue; // Skip local-only columns
                
                int type = cursor.getType(i);
                if (type == Cursor.FIELD_TYPE_INTEGER) {
                    record.put(col, cursor.getLong(i));
                    if (col.equals(primaryKeyColumn)) id = cursor.getLong(i);
                } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                    record.put(col, cursor.getDouble(i));
                } else if (type == Cursor.FIELD_TYPE_STRING) {
                    record.put(col, cursor.getString(i));
                } else if (type == Cursor.FIELD_TYPE_NULL) {
                    record.put(col, null);
                }
            }
            recordsToSync.add(record);
            if (id != -1) idsToMarkSynced.add(id);
        }
        cursor.close();

        // Push to Supabase
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

        // Mark as synced locally if successful
        if (response != null && response.isSuccessful()) {
            for (Long id : idsToMarkSynced) {
                db.execSQL("UPDATE " + tableName + " SET is_synced = 1 WHERE " + primaryKeyColumn + " = " + id);
            }
            Log.d(TAG, "Synced " + recordsToSync.size() + " records for " + tableName);
            return true;
        } else {
            String err = response != null ? response.errorBody().string() : "Unknown error";
            Log.e(TAG, "Failed to sync " + tableName + ": " + err);
            return false;
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
