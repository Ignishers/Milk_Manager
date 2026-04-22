package com.ignishers.milkmanager2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ignishers.milkmanager2.database.AppSettingsDAO;
import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.database.DailyTransactionDAO;
import com.ignishers.milkmanager2.database.MilkRateDAO;
import com.ignishers.milkmanager2.models.Customer;
import com.ignishers.milkmanager2.models.DailyTransaction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Receives morning (5 AM) and evening (6 PM) alarms and inserts automatic milk entries
 * for all customers using their respective session default quantities.
 * <p>
 * Only fires when the master auto-entry toggle is ON. Each entry is marked as "Regular"
 * milk type so it can be identified and deleted from the POS screen if needed.
 * </p>
 */
public class AutoEntryReceiver extends BroadcastReceiver {

    public static final String ACTION_MORNING = "com.ignishers.milkmanager2.AUTO_ENTRY_MORNING";
    public static final String ACTION_EVENING = "com.ignishers.milkmanager2.AUTO_ENTRY_EVENING";

    private static final String TAG = "AutoEntryReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check master toggle first
        AppSettingsDAO settings = new AppSettingsDAO(context);
        if (!settings.isAutoEntryEnabled()) {
            Log.d(TAG, "Auto-entry is disabled. Skipping.");
            return;
        }

        String action = intent.getAction();
        String session;
        if (ACTION_MORNING.equals(action)) {
            session = "Morning";
        } else if (ACTION_EVENING.equals(action)) {
            session = "Evening";
        } else {
            Log.w(TAG, "Unknown action: " + action);
            return;
        }

        Log.d(TAG, "Auto-entry firing for session: " + session);
        insertAutoEntries(context, session);

        // Reschedule alarm for next day (exact alarms don't auto-repeat)
        if (ACTION_MORNING.equals(action)) {
            android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 5);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AutoEntryScheduler.getMorningIntent(context));
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AutoEntryScheduler.getMorningIntent(context));
            }
        } else {
            android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 16); // 4:00 PM
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AutoEntryScheduler.getEveningIntent(context));
            } else {
                am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AutoEntryScheduler.getEveningIntent(context));
            }
        }
    }

    private void insertAutoEntries(Context context, String session) {
        CustomerDAO customerDAO = new CustomerDAO(context);
        DailyTransactionDAO transactionDAO = new DailyTransactionDAO(context);
        MilkRateDAO rateDAO = new MilkRateDAO(context);

        String today = LocalDate.now().toString();
        double rate = rateDAO.getRateForDate(today);
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        List<Customer> customers = customerDAO.getAllCustomers();
        int count = 0;

        for (Customer customer : customers) {
            // Skip if an entry already exists for this session today (avoid duplicates)
            if (transactionDAO.hasTodayEntry(customer.id, session)) {
                Log.d(TAG, "Skipping " + customer.name + " — entry already exists for " + session);
                continue;
            }

            double quantity = session.equals("Morning")
                    ? (customer.defaultQtyMorning > 0 ? customer.defaultQtyMorning : 0)
                    : (customer.defaultQtyEvening > 0 ? customer.defaultQtyEvening : 0);

            if (quantity <= 0) {
                Log.d(TAG, "Skipping " + customer.name + " — zero quantity for " + session);
                continue;
            }

            double amount = quantity * rate;

            DailyTransaction entry = new DailyTransaction(
                    customer.id,
                    today,
                    session,
                    quantity,
                    amount,
                    timestamp,
                    null,       // paymentMode
                    "Regular"   // milkType: Regular = auto-entry, can be deleted by POS buttons
            );
            transactionDAO.insert(entry);
            customerDAO.updateCustomerDue(customer.id, amount);
            count++;
        }

        Log.d(TAG, "Auto-entries inserted: " + count + " for session: " + session);
    }
}
