package com.ignishers.milkmanager2.network;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    public static final String SYNC_WORK_NAME = "MilkManagerDailySync";

    public static void setupPeriodicSync(Context context) {
        // We want the sync to run at 2:00 AM every day
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();

        // Set Execution around 02:00:00 AM
        dueDate.set(Calendar.HOUR_OF_DAY, 2);
        dueDate.set(Calendar.MINUTE, 0);
        dueDate.set(Calendar.SECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
