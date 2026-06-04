package com.ignishers.milkmanager2.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import com.ignishers.milkmanager2.database.DBHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CsvExportManager {

    public interface ExportCallback {
        void onSuccess(ArrayList<Uri> fileUris);
        void onFailure(String error);
    }

    public static void exportDatabaseToCsv(Context context, ExportCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                DBHelper dbHelper = new DBHelper(context);
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                File cacheDir = new File(context.getCacheDir(), "exports");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

                ArrayList<Uri> uris = new ArrayList<>();
                uris.add(exportTable(context, db, DBHelper.CUSTOMER_TABLE, new File(cacheDir, "Customers_" + timestamp + ".csv")));
                uris.add(exportTable(context, db, DBHelper.MILK_TRANSACTION_TABLE, new File(cacheDir, "Transactions_" + timestamp + ".csv")));
                uris.add(exportTable(context, db, DBHelper.PAYMENT_TABLE, new File(cacheDir, "Payments_" + timestamp + ".csv")));

                db.close();

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(uris));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e.getMessage()));
            }
        });
    }

    private static Uri exportTable(Context context, SQLiteDatabase db, String tableName, File outFile) throws IOException {
        Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, null);
        FileWriter fw = new FileWriter(outFile);

        // Write headers
        String[] columnNames = cursor.getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            fw.append(columnNames[i]);
            if (i < columnNames.length - 1) fw.append(",");
        }
        fw.append("\n");

        // Write rows
        while (cursor.moveToNext()) {
            for (int i = 0; i < columnNames.length; i++) {
                String value = cursor.getString(i);
                if (value != null) {
                    value = value.replace("\"", "\"\"");
                    if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
                        value = "\"" + value + "\"";
                    }
                    fw.append(value);
                }
                if (i < columnNames.length - 1) fw.append(",");
            }
            fw.append("\n");
        }

        fw.flush();
        fw.close();
        cursor.close();

        return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", outFile);
    }
}
