package com.ignishers.milkmanager2.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "MilkManagerSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_SELLER_ID = "sellerId";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String sellerId, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_SELLER_ID, sellerId);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getSellerId() {
        return pref.getString(KEY_SELLER_ID, null);
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
