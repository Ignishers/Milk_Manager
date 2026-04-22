package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.database.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static com.ignishers.milkmanager2.database.DBHelper.*;

import java.time.LocalDate;

/**
 * Default documentation for MilkRateDAO.
 * <p>
 * This class is a part of the database component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class MilkRateDAO {

    private final SQLiteDatabase db;    /**
     * Constructs a new {@code MilkRateDAO} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public MilkRateDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    /**
    * Persists a new {@code Rate} entity to the underlying data store.
    * <p>
    * Wraps the provided parameters into {@code ContentValues} and executes an SQL INSERT.
    * Data flows from the ViewModel/Activity down into the {@code SQLiteOpenHelper}.
    * </p>
    *
    * @param rate standard parameter provided by caller layer.
    * @param effectiveDate standard parameter provided by caller layer.
    */
    public void insertRate(double rate, String effectiveDate) {
        ContentValues cv = new ContentValues();
        cv.put(COL_GLOBAL_PRICE_PER_LITRE, rate);
        cv.put(COL_EFFECTIVE_DATE, effectiveDate);
        db.insert(MILK_PRICE_TABLE, null, cv);
    }

    /**
    * Retrieves the {@code RateForDate} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    *
    * @param date standard parameter provided by caller layer.
    * @return the resulting {@code double} payload.
    */
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

    /**
    * Retrieves the {@code CurrentRate} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code double} payload.
    */
    public double getCurrentRate() {
        return getRateForDate(LocalDate.now().toString());
    }
}