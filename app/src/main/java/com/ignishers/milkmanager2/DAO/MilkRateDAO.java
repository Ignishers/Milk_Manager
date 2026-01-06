package com.ignishers.milkmanager2.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static com.ignishers.milkmanager2.DAO.DBHelper.*;

import java.time.LocalDate;

public class MilkRateDAO {

    private final SQLiteDatabase db;

    public MilkRateDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    public void insertRate(double rate, String effectiveDate) {
        ContentValues cv = new ContentValues();
        cv.put(COL_GLOBAL_PRICE_PER_LITRE, rate);
        cv.put(COL_EFFECTIVE_DATE, effectiveDate);
        db.insert(MILK_PRICE_TABLE, null, cv);
    }

    public double getRateForDate(String date) {
        double rate = 60.0; // Fail-safe default
        
        // Find the latest rate that started ON or BEFORE the target date
        String query = "SELECT " + COL_GLOBAL_PRICE_PER_LITRE + 
                       " FROM " + MILK_PRICE_TABLE + 
                       " WHERE " + COL_EFFECTIVE_DATE + " <= ?" + 
                       " ORDER BY " + COL_EFFECTIVE_DATE + " DESC, " + COL_PRICE_ID + " DESC LIMIT 1";
                       
        Cursor cursor = db.rawQuery(query, new String[]{date});
        
        if (cursor.moveToFirst()) {
            rate = cursor.getDouble(0);
        }
        cursor.close();
        return rate;
    }

    public double getCurrentRate() {
        return getRateForDate(LocalDate.now().toString());
    }
}
