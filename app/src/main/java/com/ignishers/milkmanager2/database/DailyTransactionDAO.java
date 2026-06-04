package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.database.DBHelper;
import com.ignishers.milkmanager2.models.Customer;
import static com.ignishers.milkmanager2.database.DBHelper.*;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ignishers.milkmanager2.models.DailyTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DailyTransactionDAO {
    private final SQLiteDatabase db;

    public DailyTransactionDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    private BigDecimal getBigDecimal(Cursor c, int index) {
        if (c.isNull(index)) return BigDecimal.ZERO;
        String val = c.getString(index);
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public long insert(DailyTransaction transaction) {
        ContentValues cv = new ContentValues();
        cv.put(COL_TRANS_CUSTOMER_ID_FK, transaction.getCustomerId());
        cv.put(COL_TRANS_DATE, transaction.getDate());
        cv.put(COL_TRANS_SESSION, transaction.getSession());
        cv.put(COL_TRANS_QUANTITY, transaction.getQuantity().toPlainString());
        cv.put(COL_TRANS_AMOUNT, transaction.getAmount().toPlainString());
        cv.put(COL_TRANS_TIMESTAMP, transaction.getTimestamp());
        cv.put(COL_TRANS_PAYMENT_MODE, transaction.getPaymentMode()); 
        cv.put(COL_TRANS_MILK_TYPE, transaction.getMilkType()); 
        return db.insert(MILK_TRANSACTION_TABLE, null, cv);
    }

    public List<DailyTransaction> getTransactionsByMonth(String customerId, String month, String year) {
        List<DailyTransaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + MILK_TRANSACTION_TABLE +
                " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
                " AND strftime('%m', " + COL_TRANS_DATE + ") = ?" +
                " AND strftime('%Y', " + COL_TRANS_DATE + ") = ?";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{customerId, month, year});

        if (cursor.moveToFirst()) {
            int modeIndex = cursor.getColumnIndex(COL_TRANS_PAYMENT_MODE);
            int typeIndex = cursor.getColumnIndex(COL_TRANS_MILK_TYPE);

            do {
                String mode = (modeIndex != -1) ? cursor.getString(modeIndex) : null;
                String type = (typeIndex != -1) ? cursor.getString(typeIndex) : "Regular"; 

                DailyTransaction transaction = new DailyTransaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_CUSTOMER_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_SESSION)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_QUANTITY)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP)),
                        mode,
                        type
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
            int typeIndex = cursor.getColumnIndex(COL_TRANS_MILK_TYPE);

            do {
                String mode = (modeIndex != -1) ? cursor.getString(modeIndex) : null;
                String type = (typeIndex != -1) ? cursor.getString(typeIndex) : "Regular";

                DailyTransaction transaction = new DailyTransaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_CUSTOMER_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_SESSION)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_QUANTITY)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP)),
                        mode,
                        type
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

    public boolean hasTodayEntry(long customerId, String session) {
        String today = java.time.LocalDate.now().toString();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM " + MILK_TRANSACTION_TABLE +
            " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
            " AND " + COL_TRANS_DATE + " = ?" +
            " AND " + COL_TRANS_SESSION + " = ?",
            new String[]{String.valueOf(customerId), today, session});
        boolean exists = false;
        if (c.moveToFirst()) exists = c.getInt(0) > 0;
        c.close();
        return exists;
    }

    public BigDecimal deleteTodaySession(long customerId, String session) {
        String today = java.time.LocalDate.now().toString();
        BigDecimal amount = BigDecimal.ZERO;
        Cursor c = db.rawQuery(
            "SELECT " + COL_TRANS_ID + ", " + COL_TRANS_AMOUNT + " FROM " + MILK_TRANSACTION_TABLE +
            " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
            " AND " + COL_TRANS_DATE + " = ?" +
            " AND " + COL_TRANS_SESSION + " = ?" +
            " ORDER BY " + COL_TRANS_ID + " DESC LIMIT 1",
            new String[]{String.valueOf(customerId), today, session});
        if (c.moveToFirst()) {
            amount = getBigDecimal(c, 1);
            int transId = c.getInt(0);
            db.delete(MILK_TRANSACTION_TABLE, COL_TRANS_ID + " = ?", new String[]{String.valueOf(transId)});
        }
        c.close();
        return amount;
    }

    public void deleteAllForCustomer(long customerId) {
        db.delete(MILK_TRANSACTION_TABLE, COL_TRANS_CUSTOMER_ID_FK + " = ?", new String[]{String.valueOf(customerId)});
    }

    public List<DailyTransaction> getTransactionsByDateRange(long customerId, String startDate, String endDate) {
        List<DailyTransaction> transactionList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + MILK_TRANSACTION_TABLE +
                " WHERE " + COL_TRANS_CUSTOMER_ID_FK + " = ?" +
                " AND " + COL_TRANS_DATE + " BETWEEN ? AND ?" +
                " ORDER BY " + COL_TRANS_DATE + " ASC, " + COL_TRANS_ID + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(customerId), startDate, endDate});

        if (cursor.moveToFirst()) {
            int modeIndex = cursor.getColumnIndex(COL_TRANS_PAYMENT_MODE);
            int typeIndex = cursor.getColumnIndex(COL_TRANS_MILK_TYPE);

            do {
                String mode = (modeIndex != -1) ? cursor.getString(modeIndex) : null;
                String type = (typeIndex != -1) ? cursor.getString(typeIndex) : "Regular";

                DailyTransaction transaction = new DailyTransaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_CUSTOMER_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_SESSION)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_QUANTITY)),
                        getBigDecimal(cursor, cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP)),
                        mode,
                        type
                );
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactionList;
    }

    public ReportSummary getLifetimeSummary() {
        ReportSummary summary = new ReportSummary();
        
        String sqlSales = "SELECT SUM(CAST(" + COL_TRANS_QUANTITY + " as REAL)), SUM(CAST(" + COL_TRANS_AMOUNT + " as REAL)) FROM " + MILK_TRANSACTION_TABLE + 
                          " WHERE " + COL_TRANS_SESSION + " NOT LIKE 'Payment%'";
        Cursor c1 = db.rawQuery(sqlSales, null);
        if (c1.moveToFirst()) {
            summary.totalMilk = getBigDecimal(c1, 0);
            summary.totalRevenue = getBigDecimal(c1, 1);
        }
        c1.close();
        
        String sqlCollected = "SELECT SUM(CAST(" + COL_TRANS_AMOUNT + " as REAL)) FROM " + MILK_TRANSACTION_TABLE + 
                              " WHERE " + COL_TRANS_SESSION + " LIKE 'Payment%'";
        Cursor c2 = db.rawQuery(sqlCollected, null);
        if (c2.moveToFirst()) {
            summary.totalCollected = getBigDecimal(c2, 0);
        }
        c2.close();
        
        String sqlLegacy = "SELECT SUM(CAST(" + COL_CUSTOMER_CURRENT_DUE + " as REAL)) FROM " + CUSTOMER_TABLE;
        Cursor c3 = db.rawQuery(sqlLegacy, null);
        if (c3.moveToFirst()) {
            summary.totalLegacyDue = getBigDecimal(c3, 0);
        }
        c3.close();
        
        summary.totalRevenue = summary.totalRevenue.add(summary.totalLegacyDue);
        summary.totalDue = summary.totalRevenue.subtract(summary.totalCollected);
        
        return summary;
    }

    public List<MonthlyReportItem> getMonthlyBreakdown(int year) {
        List<MonthlyReportItem> list = new ArrayList<>();
        String yearStr = String.valueOf(year);
        
        BigDecimal[] milk = new BigDecimal[13];
        BigDecimal[] sales = new BigDecimal[13];
        BigDecimal[] collected = new BigDecimal[13];
        for(int i=0; i<13; i++) { milk[i] = BigDecimal.ZERO; sales[i] = BigDecimal.ZERO; collected[i] = BigDecimal.ZERO; }

        String sqlSales = "SELECT strftime('%m', " + COL_TRANS_DATE + ") as month, " +
                     "SUM(CAST(" + COL_TRANS_QUANTITY + " as REAL)), " +
                     "SUM(CAST(" + COL_TRANS_AMOUNT + " as REAL)) " +
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
                        milk[m] = getBigDecimal(c1, 1);
                        sales[m] = getBigDecimal(c1, 2);
                    }
                } catch (NumberFormatException e) { }
            }
        }
        c1.close();
        
        String sqlCollected = "SELECT strftime('%m', " + COL_TRANS_DATE + ") as month, " +
                              "SUM(CAST(" + COL_TRANS_AMOUNT + " as REAL)) " +
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
                        collected[m] = getBigDecimal(c2, 1);
                    }
                } catch (NumberFormatException e) { }
            }
        }
        c2.close();
        
        for (int i=1; i<=12; i++) {
            if (milk[i].compareTo(BigDecimal.ZERO) > 0 || sales[i].compareTo(BigDecimal.ZERO) > 0 || collected[i].compareTo(BigDecimal.ZERO) > 0) {
                 list.add(new MonthlyReportItem(i, milk[i], sales[i], collected[i]));
            }
        }
        
        return list;
    }

    public static class ReportSummary {
        public BigDecimal totalMilk = BigDecimal.ZERO;
        public BigDecimal totalRevenue = BigDecimal.ZERO;
        public BigDecimal totalCollected = BigDecimal.ZERO;
        public BigDecimal totalDue = BigDecimal.ZERO;
        public BigDecimal totalLegacyDue = BigDecimal.ZERO;
    }

    public static class MonthlyReportItem {
        public int month; // 1-12
        public BigDecimal milk;
        public BigDecimal amount;
        public BigDecimal collected;

        public MonthlyReportItem(int m, BigDecimal mi, BigDecimal a, BigDecimal c) {
            month = m;
            milk = mi;
            amount = a;
            collected = c;
        }
    }

    public List<CustomerSalesItem> getCustomerSalesRanking(int limit) {
        List<CustomerSalesItem> list = new ArrayList<>();
        String sql = "SELECT c." + COL_CUSTOMER_NAME + ", SUM(CAST(t." + COL_TRANS_QUANTITY + " as REAL)), SUM(CAST(t." + COL_TRANS_AMOUNT + " as REAL)) " +
                     "FROM " + MILK_TRANSACTION_TABLE + " t " +
                     "JOIN " + CUSTOMER_TABLE + " c ON t." + COL_TRANS_CUSTOMER_ID_FK + " = c." + COL_CUSTOMER_ID + " " +
                     "WHERE t." + COL_TRANS_SESSION + " NOT LIKE 'Payment%' " +
                     "GROUP BY t." + COL_TRANS_CUSTOMER_ID_FK + " " +
                     "ORDER BY SUM(CAST(t." + COL_TRANS_AMOUNT + " as REAL)) DESC " +
                     "LIMIT ?";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(limit)});
        while (c.moveToNext()) {
            list.add(new CustomerSalesItem(c.getString(0), getBigDecimal(c, 1), getBigDecimal(c, 2)));
        }
        c.close();
        return list;
    }

    public List<YearlyReportItem> getYearlyRevenueTrend(int numberOfYears) {
        List<YearlyReportItem> list = new ArrayList<>();
        int currentYear = java.time.LocalDate.now().getYear();
        int startYear = currentYear - numberOfYears + 1;

        String sql = "SELECT strftime('%Y', " + COL_TRANS_DATE + ") as year, " +
                "SUM(CAST(" + COL_TRANS_QUANTITY + " as REAL)), " +
                "SUM(CAST(" + COL_TRANS_AMOUNT + " as REAL)) " +
                "FROM " + MILK_TRANSACTION_TABLE + " " +
                "WHERE " + COL_TRANS_SESSION + " NOT LIKE 'Payment%' " +
                "AND strftime('%Y', " + COL_TRANS_DATE + ") >= ? " +
                "GROUP BY year ORDER BY year";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(startYear)});
        while (c.moveToNext()) {
            String yearStr = c.getString(0);
            if (yearStr != null) {
                try {
                    list.add(new YearlyReportItem(Integer.parseInt(yearStr), getBigDecimal(c, 1), getBigDecimal(c, 2)));
                } catch (NumberFormatException e) { }
            }
        }
        c.close();
        return list;
    }

    public static class CustomerSalesItem {
        public String customerName;
        public BigDecimal totalMilk;
        public BigDecimal totalSpent;

        public CustomerSalesItem(String n, BigDecimal m, BigDecimal s) {
            customerName = n;
            totalMilk = m;
            totalSpent = s;
        }
    }

    public static class YearlyReportItem {
        public int year;
        public BigDecimal totalMilk;
        public BigDecimal totalRevenue;

        public YearlyReportItem(int y, BigDecimal m, BigDecimal r) {
            year = y;
            totalMilk = m;
            totalRevenue = r;
        }
    }
}