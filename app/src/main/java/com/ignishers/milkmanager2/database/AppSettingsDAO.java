package com.ignishers.milkmanager2.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static com.ignishers.milkmanager2.database.DBHelper.*;

/**
 * DAO for reading and writing application-wide settings stored in the {@code app_settings} table.
 * <p>
 * Currently manages the auto-entry toggle (master on/off switch) which controls whether
 * morning (5 AM) and evening (6 PM) milk entries are automatically created for all customers.
 * </p>
 */
public class AppSettingsDAO {

    private final SQLiteDatabase db;

    public AppSettingsDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    /** Returns true if the master auto-entry toggle is ON. */
    public boolean isAutoEntryEnabled() {
        String value = getSetting(SETTING_AUTO_ENTRY_ENABLED);
        return "true".equalsIgnoreCase(value);
    }

    /** Sets the master auto-entry toggle ON or OFF. */
    public void setAutoEntryEnabled(boolean enabled) {
        putSetting(SETTING_AUTO_ENTRY_ENABLED, enabled ? "true" : "false");
    }

    // ---- Generic key-value helpers ----

    private String getSetting(String key) {
        Cursor c = db.query(SETTINGS_TABLE,
                new String[]{COL_SETTING_VALUE},
                COL_SETTING_KEY + " = ?",
                new String[]{key},
                null, null, null);
        String value = null;
        if (c.moveToFirst()) {
            value = c.getString(0);
        }
        c.close();
        return value;
    }

    private void putSetting(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SETTING_KEY, key);
        cv.put(COL_SETTING_VALUE, value);
        // INSERT OR REPLACE
        db.insertWithOnConflict(SETTINGS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
