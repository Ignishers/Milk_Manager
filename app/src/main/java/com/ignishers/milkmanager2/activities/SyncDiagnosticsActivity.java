package com.ignishers.milkmanager2.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.database.DBHelper;
import com.ignishers.milkmanager2.managers.SessionManager;
import com.ignishers.milkmanager2.network.SyncWorker;

public class SyncDiagnosticsActivity extends AppCompatActivity {

    private TextView tvStatus, tvStatRoutes, tvStatCustomers, tvStatPrices, tvStatTransactions, tvStatPayments, tvLogs;
    private Button btnSyncNow, btnRestoreFromCloud, btnRefreshStats, btnClearLogs;
    private DBHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_diagnostics);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStatus = findViewById(R.id.tvStatus);
        tvStatRoutes = findViewById(R.id.tvStatRoutes);
        tvStatCustomers = findViewById(R.id.tvStatCustomers);
        tvStatPrices = findViewById(R.id.tvStatPrices);
        tvStatTransactions = findViewById(R.id.tvStatTransactions);
        tvStatPayments = findViewById(R.id.tvStatPayments);
        tvLogs = findViewById(R.id.tvLogs);
        
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnRestoreFromCloud = findViewById(R.id.btnRestoreFromCloud);
        btnRefreshStats = findViewById(R.id.btnRefreshStats);
        btnClearLogs = findViewById(R.id.btnClearLogs);

        dbHelper = new DBHelper(this);
        sessionManager = new SessionManager(this);

        btnRefreshStats.setOnClickListener(v -> loadStats());
        btnClearLogs.setOnClickListener(v -> {
            getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE).edit().putString("last_error", "").apply();
            loadLogs();
        });
        
        btnSyncNow.setOnClickListener(v -> {
            tvStatus.setText("Syncing...");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
            WorkManager.getInstance(this).enqueueUniqueWork("ManualSync", ExistingWorkPolicy.REPLACE, syncRequest);
            Toast.makeText(this, "Sync Triggered!", Toast.LENGTH_SHORT).show();
        });

        btnRestoreFromCloud.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Restore Data")
                .setMessage("This will download all your data from the cloud and merge it with your local device. Continue?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    tvStatus.setText("Restoring...");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    OneTimeWorkRequest restoreRequest = new OneTimeWorkRequest.Builder(com.ignishers.milkmanager2.network.RestoreWorker.class).build();
                    WorkManager.getInstance(this).enqueueUniqueWork("ManualRestore", ExistingWorkPolicy.REPLACE, restoreRequest);
                    Toast.makeText(this, "Restore Triggered!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        observeSyncStatus();
        observeRestoreStatus();
        loadStats();
        loadLogs();
    }

    private void observeSyncStatus() {
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("ManualSync")
                .observe(this, workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {
                        WorkInfo info = workInfos.get(0);
                        if (info.getState() == WorkInfo.State.ENQUEUED || info.getState() == WorkInfo.State.RUNNING) {
                            tvStatus.setText("Syncing...");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        } else if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            tvStatus.setText("Idle (Last Sync Successful)");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            loadStats();
                            loadLogs();
                        } else if (info.getState() == WorkInfo.State.FAILED) {
                            tvStatus.setText("Idle (Last Sync Failed)");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            loadStats();
                            loadLogs();
                        }
                    }
                });
    }

    private void observeRestoreStatus() {
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("ManualRestore")
                .observe(this, workInfos -> {
                    if (workInfos != null && !workInfos.isEmpty()) {
                        WorkInfo info = workInfos.get(0);
                        if (info.getState() == WorkInfo.State.ENQUEUED || info.getState() == WorkInfo.State.RUNNING) {
                            tvStatus.setText("Restoring...");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                        } else if (info.getState() == WorkInfo.State.SUCCEEDED) {
                            tvStatus.setText("Idle (Last Restore Successful)");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            loadStats();
                            loadLogs();
                        } else if (info.getState() == WorkInfo.State.FAILED) {
                            tvStatus.setText("Idle (Last Restore Failed)");
                            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            loadStats();
                            loadLogs();
                        }
                    }
                });
    }

    private void loadLogs() {
        SharedPreferences prefs = getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE);
        String err = prefs.getString("last_error", "");
        
        SharedPreferences restorePrefs = getSharedPreferences("RestorePrefs", Context.MODE_PRIVATE);
        String restoreErr = restorePrefs.getString("last_error", "");
        
        if (err.isEmpty() && restoreErr.isEmpty()) {
            tvLogs.setText("No errors. Cloud connection is healthy.");
            tvLogs.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            String combined = "";
            if (!err.isEmpty()) combined += "Sync Error:\n" + err + "\n\n";
            if (!restoreErr.isEmpty()) combined += "Restore Error:\n" + restoreErr;
            tvLogs.setText(combined.trim());
            tvLogs.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void loadStats() {
        if (!sessionManager.isLoggedIn()) {
            tvStatus.setText("Offline (Not Logged In)");
            return;
        }
        String sellerId = sessionManager.getSellerId();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        tvStatRoutes.setText("Routes: " + getSyncText(db, DBHelper.ROUTE_TABLE, sellerId));
        tvStatCustomers.setText("Customers: " + getSyncText(db, DBHelper.CUSTOMER_TABLE, sellerId));
        tvStatPrices.setText("Prices: " + getSyncText(db, DBHelper.MILK_PRICE_TABLE, sellerId));
        tvStatTransactions.setText("Transactions: " + getSyncText(db, DBHelper.MILK_TRANSACTION_TABLE, sellerId));
        tvStatPayments.setText("Payments: " + getSyncText(db, DBHelper.PAYMENT_TABLE, sellerId));
    }

    private String getSyncText(SQLiteDatabase db, String tableName, String sellerId) {
        int total = 0;
        int synced = 0;

        Cursor c1 = null;
        Cursor c2 = null;
        try {
            String queryTable = tableName.equals(DBHelper.PAYMENT_TABLE) ? DBHelper.MILK_TRANSACTION_TABLE : tableName;
            String extraCondition = "";
            if (tableName.equals(DBHelper.PAYMENT_TABLE)) {
                extraCondition = " AND " + DBHelper.COL_TRANS_SESSION + " LIKE 'Payment%'";
            } else if (tableName.equals(DBHelper.MILK_TRANSACTION_TABLE)) {
                extraCondition = " AND " + DBHelper.COL_TRANS_SESSION + " NOT LIKE 'Payment%'";
            }

            c1 = db.rawQuery("SELECT COUNT(*) FROM " + queryTable + " WHERE seller_id = ?" + extraCondition, new String[]{sellerId});
            if (c1.moveToFirst()) total = c1.getInt(0);

            c2 = db.rawQuery("SELECT COUNT(*) FROM " + queryTable + " WHERE is_synced = 1 AND seller_id = ?" + extraCondition, new String[]{sellerId});
            if (c2.moveToFirst()) synced = c2.getInt(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c1 != null) c1.close();
            if (c2 != null) c2.close();
        }

        return synced + " / " + total + " Synced";
    }
}
