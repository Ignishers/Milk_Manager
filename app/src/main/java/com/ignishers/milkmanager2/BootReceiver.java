package com.ignishers.milkmanager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ignishers.milkmanager2.database.AppSettingsDAO;

/**
 * Reschedules auto-entry alarms after device reboot.
 * <p>
 * AlarmManager alarms are lost when the phone is powered off. This receiver fires
 * on {@code BOOT_COMPLETED} and re-schedules the morning and evening alarms if the
 * master auto-entry toggle is currently ON.
 * </p>
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppSettingsDAO settings = new AppSettingsDAO(context);
            if (settings.isAutoEntryEnabled()) {
                AutoEntryScheduler.scheduleAll(context);
                Log.d("BootReceiver", "Alarms rescheduled after boot.");
            }
        }
    }
}
