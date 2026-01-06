package com.ignishers.milkmanager2.DAO;

import static com.ignishers.milkmanager2.DAO.DBHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ignishers.milkmanager2.model.DailyTransaction;

import java.util.ArrayList;
import java.util.List;

public class DailyTransactionDAO {
    private final SQLiteDatabase db;

    ///    Constructor
    public DailyTransactionDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    ///    Insert transaction
    public long insert(DailyTransaction transaction) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TRANS_CUSTOMER_ID_FK, transaction.getCustomerId());
        cv.put(COL_TRANS_DATE, transaction.getDate());
        cv.put(COL_TRANS_SESSION, transaction.getSession());
        cv.put(COL_TRANS_QUANTITY, transaction.getQuantity());
        cv.put(COL_TRANS_AMOUNT, transaction.getAmount());
        cv.put(COL_TRANS_TIMESTAMP, transaction.getTimestamp());
        cv.put(COL_TRANS_PAYMENT_MODE, transaction.getPaymentMode()); // Save Payment Mode
        return db.insert(MILK_TRANSACTION_TABLE, null, cv);
    }


    ///    Get Transactions by Month
    public List<DailyTransaction> getTransactionsByMonth(String customerId, String month, String year) {
        List<DailyTransaction> transactionList = new ArrayList<>();


        // Query: Select rows where month and year match parameters
        // month should be "01" through "12"
        String selectQuery = "SELECT * FROM " + MILK_TRANSACTION_TABLE +
                " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
                " AND strftime('%m', " + COL_TRANS_DATE + ") = ?" +
                " AND strftime('%Y', " + COL_TRANS_DATE + ") = ?";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{customerId, month, year});

        if (cursor.moveToFirst()) {
            // Check column index safely
            int modeIndex = cursor.getColumnIndex(COL_TRANS_PAYMENT_MODE);

            do {
                String mode = (modeIndex != -1) ? cursor.getString(modeIndex) : null;

                DailyTransaction transaction = new DailyTransaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_CUSTOMER_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_SESSION)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANS_QUANTITY)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP)),
                        mode // Pass retrieved mode
                );
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactionList;
    }

    public List<DailyTransaction> getTransactionsByDate(long customerId, String date) {
        List<DailyTransaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + MILK_TRANSACTION_TABLE +
                " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
                " AND " + COL_TRANS_DATE + " = ?" +
                " ORDER BY " + COL_TRANS_ID + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(customerId), date});

        if (cursor.moveToFirst()) {
            int modeIndex = cursor.getColumnIndex(COL_TRANS_PAYMENT_MODE);

            do {
                String mode = (modeIndex != -1) ? cursor.getString(modeIndex) : null;

                DailyTransaction transaction = new DailyTransaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_CUSTOMER_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_SESSION)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANS_QUANTITY)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP)),
                        mode // Pass retrieved mode
                );
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactionList;
    }

    public void delete(int transactionId) {
        db.delete(MILK_TRANSACTION_TABLE, COL_TRANS_ID + " = ?", new String[]{String.valueOf(transactionId)});
    }

    // --------------------------------------------------------------------------------
    // REPORTING METHODS
    // --------------------------------------------------------------------------------

    // 1. Lifetime Summary
    public ReportSummary getLifetimeSummary() {
        ReportSummary summary = new ReportSummary();
        
        // Total Milk & Revenue (Exclude Payment entries)
        String sqlSales = "SELECT SUM(" + COL_TRANS_QUANTITY + "), SUM(" + COL_TRANS_AMOUNT + ") FROM " + MILK_TRANSACTION_TABLE + 
                          " WHERE " + COL_TRANS_SESSION + " NOT LIKE 'Payment%'";
        Cursor c1 = db.rawQuery(sqlSales, null);
        if (c1.moveToFirst()) {
            summary.totalMilk = c1.getDouble(0);
            summary.totalRevenue = c1.getDouble(1);
        }
        c1.close();
        
        // Total Collected (Only Payment entries)
        String sqlCollected = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + MILK_TRANSACTION_TABLE + 
                              " WHERE " + COL_TRANS_SESSION + " LIKE 'Payment%'";
        Cursor c2 = db.rawQuery(sqlCollected, null);
        if (c2.moveToFirst()) {
            summary.totalCollected = c2.getDouble(0);
        }
        c2.close();
        
        // Total Legacy Due (From Customer Table)
        String sqlLegacy = "SELECT SUM(" + COL_CUSTOMER_CURRENT_DUE + ") FROM " + CUSTOMER_TABLE;
        Cursor c3 = db.rawQuery(sqlLegacy, null);
        if (c3.moveToFirst()) {
            summary.totalLegacyDue = c3.getDouble(0);
        }
        c3.close();
        
        // Update Revenue to include Legacy Due (as per user request)
        summary.totalRevenue += summary.totalLegacyDue;
        
        // Total Due = Revenue (now includes Legacy) - Collected
        summary.totalDue = summary.totalRevenue - summary.totalCollected;
        
        return summary;
    }

    // 2. Monthly Breakdown for a specific year
    public List<MonthlyReportItem> getMonthlyBreakdown(int year) {
        List<MonthlyReportItem> list = new ArrayList<>();
        String yearStr = String.valueOf(year);
        
        // Arrays to hold data for 12 months (index 1-12)
        double[] milk = new double[13];
        double[] sales = new double[13];
        double[] collected = new double[13];

        // 1. Get Sales
        String sqlSales = "SELECT strftime('%m', " + COL_TRANS_DATE + ") as month, " +
                     "SUM(" + COL_TRANS_QUANTITY + "), " +
                     "SUM(" + COL_TRANS_AMOUNT + ") " +
                     "FROM " + MILK_TRANSACTION_TABLE + " " +
                     "WHERE strftime('%Y', " + COL_TRANS_DATE + ") = ? " +
                     "AND " + COL_TRANS_SESSION + " NOT LIKE 'Payment%' " +
                     "GROUP BY month";
        Cursor c1 = db.rawQuery(sqlSales, new String[]{yearStr});
        while (c1.moveToNext()) {
            String mStr = c1.getString(0);
            if (mStr != null) {
                try {
                    int m = Integer.parseInt(mStr);
                    if (m >= 1 && m <= 12) {
                        milk[m] = c1.getDouble(1);
                        sales[m] = c1.getDouble(2);
                    }
                } catch (NumberFormatException e) { }
            }
        }
        c1.close();
        
        // 2. Get Collections
        String sqlCollected = "SELECT strftime('%m', " + COL_TRANS_DATE + ") as month, " +
                              "SUM(" + COL_TRANS_AMOUNT + ") " +
                              "FROM " + MILK_TRANSACTION_TABLE + " " +
                              "WHERE strftime('%Y', " + COL_TRANS_DATE + ") = ? " +
                              "AND " + COL_TRANS_SESSION + " LIKE 'Payment%' " +
                              "GROUP BY month";
        Cursor c2 = db.rawQuery(sqlCollected, new String[]{yearStr});
        while (c2.moveToNext()) {
            String mStr = c2.getString(0);
            if (mStr != null) {
                try {
                    int m = Integer.parseInt(mStr);
                    if (m >= 1 && m <= 12) {
                        collected[m] = c2.getDouble(1);
                    }
                } catch (NumberFormatException e) { }
            }
        }
        c2.close();
        
        // Merge
        for (int i=1; i<=12; i++) {
            if (milk[i] > 0 || sales[i] > 0 || collected[i] > 0) {
                 list.add(new MonthlyReportItem(i, milk[i], sales[i], collected[i]));
            }
        }
        
        return list;
    }

    // Helper classes for Reporting
    public static class ReportSummary {
        public double totalMilk;
        public double totalRevenue;
        public double totalCollected;
        public double totalDue;
        public double totalLegacyDue;
    }

    public static class MonthlyReportItem {
        public int month; // 1-12
        public double milk;
        public double amount;
        public double collected;

        public MonthlyReportItem(int m, double mi, double a, double c) {
            month = m;
            milk = mi;
            amount = a;
            collected = c;
        }
    }

    // 3. Customer Sales Ranking (Top N)
    public List<CustomerSalesItem> getCustomerSalesRanking(int limit) {
        List<CustomerSalesItem> list = new ArrayList<>();
        // Join with Customer table to get names
        // Sum quantity and amount for each customer
        String sql = "SELECT c." + COL_CUSTOMER_NAME + ", SUM(t." + COL_TRANS_QUANTITY + "), SUM(t." + COL_TRANS_AMOUNT + ") " +
                     "FROM " + MILK_TRANSACTION_TABLE + " t " +
                     "JOIN " + CUSTOMER_TABLE + " c ON t." + COL_TRANS_CUSTOMER_ID_FK + " = c." + COL_CUSTOMER_ID + " " +
                     "WHERE t." + COL_TRANS_SESSION + " NOT LIKE 'Payment%' " +
                     "GROUP BY t." + COL_TRANS_CUSTOMER_ID_FK + " " +
                     "ORDER BY SUM(t." + COL_TRANS_AMOUNT + ") DESC " +
                     "LIMIT ?";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            list.add(new CustomerSalesItem(c.getString(0), c.getDouble(1), c.getDouble(2)));
        }
        c.close();
        return list;
    }

    // 4. Yearly Revenue Trend (Last N Years)
    public List<YearlyReportItem> getYearlyRevenueTrend(int numberOfYears) {
        List<YearlyReportItem> list = new ArrayList<>();
        // Get current year
        int currentYear = java.time.LocalDate.now().getYear();
        int startYear = currentYear - numberOfYears + 1;

        String sql = "SELECT strftime('%Y', " + COL_TRANS_DATE + ") as year, " +
                "SUM(" + COL_TRANS_QUANTITY + "), " +
                "SUM(" + COL_TRANS_AMOUNT + ") " +
                "FROM " + MILK_TRANSACTION_TABLE + " " +
                "WHERE " + COL_TRANS_SESSION + " NOT LIKE 'Payment%' " +
                "AND strftime('%Y', " + COL_TRANS_DATE + ") >= ? " +
                "GROUP BY year ORDER BY year";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(startYear)});
        while (c.moveToNext()) {
            String yearStr = c.getString(0);
            if (yearStr != null) {
                try {
                    list.add(new YearlyReportItem(Integer.parseInt(yearStr), c.getDouble(1), c.getDouble(2)));
                } catch (NumberFormatException e) { }
            }
        }
        c.close();
        return list;
    }

    public static class CustomerSalesItem {
        public String customerName;
        public double totalMilk;
        public double totalSpent;

        public CustomerSalesItem(String n, double m, double s) {
            customerName = n;
            totalMilk = m;
            totalSpent = s;
        }
    }

    public static class YearlyReportItem {
        public int year;
        public double totalMilk;
        public double totalRevenue;

        public YearlyReportItem(int y, double m, double r) {
            year = y;
            totalMilk = m;
            totalRevenue = r;
        }
    }
}