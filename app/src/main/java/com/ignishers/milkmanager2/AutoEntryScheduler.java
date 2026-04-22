package com.ignishers.milkmanager2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * Schedules and cancels the two daily alarms for automatic milk entry:
 * <ul>
 *   <li>Morning alarm: 5:00 AM every day</li>
 *   <li>Evening alarm: 6:00 PM every day</li>
 * </ul>
 * <p>
 * Call {@link #scheduleAll(Context)} when the toggle is turned ON,
 * and {@link #cancelAll(Context)} when it is turned OFF.
 * Also call {@link #scheduleAll(Context)} from BootReceiver after device restart.
 * </p>
 */
public class AutoEntryScheduler {

    private static final String TAG = "AutoEntryScheduler";
    private static final int MORNING_REQUEST_CODE = 1001;
    private static final int EVENING_REQUEST_CODE = 1002;

    /** Schedule both morning (5 AM) and evening (4 PM) alarms. */
    public static void scheduleAll(Context context) {
        scheduleMorning(context);
        scheduleEvening(context);
        Log.d(TAG, "Auto-entry alarms scheduled.");
    }

    /** Cancel both alarms. */
    public static void cancelAll(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getMorningIntent(context));
        am.cancel(getEveningIntent(context));
        Log.d(TAG, "Auto-entry alarms cancelled.");
    }

    private static void scheduleMorning(Context context) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 5);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // If already past 5 AM today, schedule for tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        setRepeatingAlarm(context, getMorningIntent(context), cal.getTimeInMillis());
        Log.d(TAG, "Morning alarm set for: " + cal.getTime());
    }

    private static void scheduleEvening(Context context) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 16); // 4:00 PM
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        setRepeatingAlarm(context, getEveningIntent(context), cal.getTimeInMillis());
        Log.d(TAG, "Evening alarm set for: " + cal.getTime());
    }

    private static void setRepeatingAlarm(Context context, PendingIntent pi, long triggerAtMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
        // Note: For API 31+ (Android 12), exact alarms require SCHEDULE_EXACT_ALARM permission.
        // We reschedule from the receiver itself to maintain daily recurrence.
    }

    public static PendingIntent getMorningIntent(Context context) {
        Intent intent = new Intent(context, AutoEntryReceiver.class);
        intent.setAction(AutoEntryReceiver.ACTION_MORNING);
        return PendingIntent.getBroadcast(context, MORNING_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent getEveningIntent(Context context) {
        Intent intent = new Intent(context, AutoEntryReceiver.class);
        intent.setAction(AutoEntryReceiver.ACTION_EVENING);
        return PendingIntent.getBroadcast(context, EVENING_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
