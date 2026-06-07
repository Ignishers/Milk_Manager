package com.ignishers.milkmanager2.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ignishers.milkmanager2.database.DBHelper;
import com.ignishers.milkmanager2.managers.SessionManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestoreWorker extends Worker {

    private static final String TAG = "RestoreWorker";
    private Context context;

    public RestoreWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        SessionManager sessionManager = new SessionManager(context);
        if (!sessionManager.isLoggedIn()) return Result.failure();
        String sellerId = sessionManager.getSellerId();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        SupabaseSyncService service = retrofit.create(SupabaseSyncService.class);
        String authHeader = "Bearer " + SupabaseConfig.SUPABASE_KEY;

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        boolean allSuccess = true;

        try {
            // Restore in dependency order
            if (!restoreTable(db, service, DBHelper.ROUTE_TABLE, sellerId, authHeader)) {
                return abortRestore(context, db, "Failed to restore Route Groups.");
            }
            if (!restoreTable(db, service, DBHelper.CUSTOMER_TABLE, sellerId, authHeader)) {
                return abortRestore(context, db, "Failed to restore Customers.");
            }
            if (!restoreTable(db, service, DBHelper.MILK_PRICE_TABLE, sellerId, authHeader)) {
                return abortRestore(context, db, "Failed to restore Milk Prices.");
            }
            if (!restoreTable(db, service, DBHelper.MILK_TRANSACTION_TABLE, sellerId, authHeader)) {
                return abortRestore(context, db, "Failed to restore Transactions.");
            }
            if (!restoreTable(db, service, DBHelper.PAYMENT_TABLE, sellerId, authHeader)) {
                return abortRestore(context, db, "Failed to restore Payments.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage());
            android.content.SharedPreferences prefs = context.getSharedPreferences("RestorePrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("last_error", "Exception: " + e.getMessage()).apply();
            return Result.retry();
        } finally {
            db.close();
        }

        if (allSuccess) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("RestorePrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("last_error", "").apply();
        }

        return allSuccess ? Result.success() : Result.retry();
    }

    private Result abortRestore(Context context, SQLiteDatabase db, String msg) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("RestorePrefs", Context.MODE_PRIVATE);
        if (prefs.getString("last_error", "").isEmpty()) {
            prefs.edit().putString("last_error", msg).apply();
        }
        db.close();
        return Result.retry();
    }

    private boolean restoreTable(SQLiteDatabase db, SupabaseSyncService service, String tableName,
                                 String sellerId, String authHeader) throws IOException {

        Response<List<Map<String, Object>>> response = null;
        String query = "eq." + sellerId;

        if (tableName.equals(DBHelper.ROUTE_TABLE)) {
            response = service.getRoutes(SupabaseConfig.SUPABASE_KEY, authHeader, query).execute();
        } else if (tableName.equals(DBHelper.CUSTOMER_TABLE)) {
            response = service.getCustomers(SupabaseConfig.SUPABASE_KEY, authHeader, query).execute();
        } else if (tableName.equals(DBHelper.MILK_PRICE_TABLE)) {
            response = service.getPrices(SupabaseConfig.SUPABASE_KEY, authHeader, query).execute();
        } else if (tableName.equals(DBHelper.MILK_TRANSACTION_TABLE)) {
            response = service.getTransactions(SupabaseConfig.SUPABASE_KEY, authHeader, query).execute();
        } else if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
            response = service.getPayments(SupabaseConfig.SUPABASE_KEY, authHeader, query).execute();
        }

        android.content.SharedPreferences prefs = context.getSharedPreferences("RestorePrefs", Context.MODE_PRIVATE);

        if (response != null && response.isSuccessful() && response.body() != null) {
            List<Map<String, Object>> records = response.body();
            db.beginTransaction();
            try {
                for (Map<String, Object> record : records) {
                    ContentValues values = new ContentValues();
                    for (Map.Entry<String, Object> entry : record.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                            if (key.equals(DBHelper.COL_PAYMENT_ID)) key = DBHelper.COL_TRANS_ID;
                            else if (key.equals(DBHelper.COL_PAYMENT_DATE)) key = DBHelper.COL_TRANS_DATE;
                            else if (key.equals(DBHelper.COL_PAYMENT_AMOUNT)) key = DBHelper.COL_TRANS_AMOUNT;
                        }

                        if (key.equals("updated_at")) {
                            continue; // Ignore updated_at from cloud (we'll generate it locally if needed)
                        }

                        if (value == null) {
                            values.putNull(key);
                        } else if (value instanceof Double) {
                            // JSON parses numbers as Double. 
                            // If it's an ID, we might need it as Long, but SQLite handles it dynamically.
                            values.put(key, (Double) value);
                        } else if (value instanceof String) {
                            values.put(key, (String) value);
                        } else if (value instanceof Boolean) {
                            values.put(key, ((Boolean) value) ? 1 : 0);
                        }
                    }
                    
                    if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                        values.put(DBHelper.COL_TRANS_SESSION, "Payment-Cloud");
                        values.put(DBHelper.COL_TRANS_QUANTITY, 0.0);
                        values.put(DBHelper.COL_TRANS_PAYMENT_MODE, "Cash"); // Default
                    }
                    
                    values.put("is_synced", 1); // Mark as synced so we don't push it back!
                    
                    String targetTable = tableName.equals(DBHelper.PAYMENT_TABLE) ? DBHelper.MILK_TRANSACTION_TABLE : tableName;
                    db.insertWithOnConflict(targetTable, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return true;
        } else {
            String err = response != null && response.errorBody() != null ? response.errorBody().string() : "Unknown network error";
            Log.e(TAG, "Failed to fetch " + tableName + ": " + err);
            prefs.edit().putString("last_error", "Failed on table " + tableName + " (Response " + (response != null ? response.code() : 0) + "): " + err).apply();
            return false;
        }
    }
}
