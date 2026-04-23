package com.ignishers.milkmanager2.viewmodels;

import com.ignishers.milkmanager2.models.DailyTransaction;
import com.ignishers.milkmanager2.database.MilkRateDAO;
import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.models.Customer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.ignishers.milkmanager2.database.DailyTransactionDAO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Default documentation for MilkEntryViewModel.
 * <p>
 * This class is a part of the viewmodels component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class MilkEntryViewModel extends AndroidViewModel {
    
    private final MutableLiveData<Boolean> entryAdded = new MutableLiveData<>();
public LiveData<Boolean> getEntryAdded() { return entryAdded; }
    // TODO: Implement the ViewModel

    private final DailyTransactionDAO dao;
    private final com.ignishers.milkmanager2.database.CustomerDAO customerDao;
    private final com.ignishers.milkmanager2.database.MilkRateDAO rateDao;
    private final Executor executor = Executors.newSingleThreadExecutor();    /**
     * Constructs a new {@code MilkEntryViewModel} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public MilkEntryViewModel(@NonNull Application app) {
        super(app);
        dao = new DailyTransactionDAO(app);
        customerDao = new com.ignishers.milkmanager2.database.CustomerDAO(app);
        rateDao = new com.ignishers.milkmanager2.database.MilkRateDAO(app);
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
        return rateDao.getRateForDate(date);
    }

    /**
    * Executes the {@code addDefaultEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    */
    public void addDefaultEntry(long customerId) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.models.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
            if (customer != null) {
                double quantity = customer.defaultQuantity > 0 ? customer.defaultQuantity : 1.0;
                double rate = rateDao.getRateForDate(LocalDate.now().toString());
                double amount = (quantity * rate);

                String session;
                int currentHour = LocalTime.now().getHour();
                if (currentHour >= 2 && currentHour < 15) {
                    session = "Morning";
                } else {
                    session = "Evening";
                }

                DailyTransaction entry = new DailyTransaction(
                        customerId,
                        LocalDate.now().toString(),
                        session,
                        quantity,
                        amount,
                        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        null,
                        "Regular" // Default Button = Regular
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }

    /**
    * Executes the {@code addManualEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param dateStr standard parameter provided by caller layer.
    * @param quantity standard parameter provided by caller layer.
    * @param amount standard parameter provided by caller layer.
    */
    public void addManualEntry(long customerId, String dateStr, double quantity, double amount) {
        executor.execute(() -> {
            String session;
            int currentHour = LocalTime.now().getHour();
            if (currentHour >= 2 && currentHour < 15) {
                // 2 AM to 3 PM -> Morning
                session = "Morning";
            } else {
                // 3 PM to 2 AM -> Evening
                session = "Evening";
            }
            
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    dateStr,
                    session,
                    quantity,
                    amount,
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    null,
                    "Extra" // Manual Entry = Extra
            );
            dao.insert(entry);
            customerDao.updateCustomerDue(customerId, amount);
            entryAdded.postValue(true);
        });
    }

    /**
    * Executes the {@code addNightEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    */
    public void addNightEntry(long customerId) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.models.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
            if (customer != null) {
                double quantity = customer.defaultQuantity > 0 ? customer.defaultQuantity : 1.0;
                double rate = rateDao.getRateForDate(LocalDate.now().toString());
                double amount = (quantity * rate); 

                DailyTransaction entry = new DailyTransaction(
                        customerId,
                        LocalDate.now().toString(),
                        "Evening",
                        quantity,
                        amount,
                        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        null,
                        "Regular" // Night Entry (Scheduled) = Regular
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }

    /**
    * Executes the {@code addEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param sessionType standard parameter provided by caller layer.
    * @param quantity standard parameter provided by caller layer.
    * @param amount standard parameter provided by caller layer.
    */
    public void addEntry(long customerId, String sessionType, double quantity, double amount) {
        executor.execute(() -> {
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    LocalDate.now().toString(),
                    sessionType,
                    quantity,
                    amount,
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    null,
                    "Extra" // Generic Add Entry = Extra (usually from manual flows)
            );

            dao.insert(entry);
            customerDao.updateCustomerDue(customerId, amount);
            entryAdded.postValue(true);

        });
    }
    /**
    * Executes the {@code addPayment} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param amount standard parameter provided by caller layer.
    * @param method standard parameter provided by caller layer.
    * @param dateStr standard parameter provided by caller layer.
    */
    public void addPayment(long customerId, double amount, String method, String dateStr) {
        executor.execute(() -> {
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    dateStr,
                    "Payment", // Session
                    0.0,
                    amount,
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    method // Payment Mode
            );
            dao.insert(entry);
            // DECREASE due by amount
            customerDao.updateCustomerDue(customerId, -amount);
            entryAdded.postValue(true);
        });
    }

    /**
    * Executes the {@code addQuickEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param value standard parameter provided by caller layer.
    */
    public void addQuickEntry(long customerId, String value) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.models.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
            if (customer != null) {
                double quantity = 0;
                double amount = 0;
                double rate = rateDao.getRateForDate(LocalDate.now().toString());

                // Parse value
                String normalizedValue = value.toLowerCase().trim();
                boolean valid = false;

                if (normalizedValue.endsWith("ml")) {
                     String qtyStr = normalizedValue.replace("ml", "");
                     try {
                         double ml = Double.parseDouble(qtyStr);
                         quantity = ml / 1000.0;
                         amount = quantity * rate;
                         valid = true;
                     } catch (NumberFormatException e) {
                         e.printStackTrace();
                     }
                } else if (normalizedValue.endsWith("l")) {
                    String qtyStr = normalizedValue.replace("l", "");
                    try {
                        quantity = Double.parseDouble(qtyStr);
                        amount = quantity * rate;
                        valid = true;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else if (normalizedValue.endsWith("rs")) {
                    String amtStr = normalizedValue.replace("rs", "");
                    try {
                        amount = Double.parseDouble(amtStr);
                        if (rate > 0) {
                            quantity = amount / rate;
                        } else {
                            quantity = 0;
                        }
                        valid = true;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                if (!valid) return;

                // Determine session
                String session;
                int currentHour = LocalTime.now().getHour();
                if (currentHour >= 2 && currentHour < 15) {
                    session = "Morning";
                } else {
                    session = "Evening";
                }

                DailyTransaction entry = new DailyTransaction(
                        customerId,
                        LocalDate.now().toString(),
                        session,
                        quantity,
                        amount,
                        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        null,
                        "Extra" // Quick Entry (Dropdown) = Extra
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }

    /**
     * Deletes today's session entry (Morning or Evening) for a customer.
     * Used by the POS fragment delete buttons.
     * Reverses the customer due by the deleted amount.
     */
    public void deleteTodaySession(long customerId, String session) {
        executor.execute(() -> {
            double deletedAmount = dao.deleteTodaySession(customerId, session);
            if (deletedAmount > 0) {
                // Reverse the due
                customerDao.updateCustomerDue(customerId, -deletedAmount);
            }
            entryAdded.postValue(true); // Trigger UI refresh
        });
    }

    /**
     * Checks (on background thread) whether today already has an entry for a session.
     * Returns result via a MutableLiveData so the caller can update button state.
     */
    public boolean hasTodayEntrySync(long customerId, String session) {
        return dao.hasTodayEntry(customerId, session);
    }
}