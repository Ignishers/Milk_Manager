package com.ignishers.milkmanager2.model;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.ignishers.milkmanager2.DAO.DailyTransactionDAO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MilkEntryViewModel extends AndroidViewModel {
    
    private final MutableLiveData<Boolean> entryAdded = new MutableLiveData<>();
public LiveData<Boolean> getEntryAdded() { return entryAdded; }
    // TODO: Implement the ViewModel

    private final DailyTransactionDAO dao;
    private final com.ignishers.milkmanager2.DAO.CustomerDAO customerDao;
    private final com.ignishers.milkmanager2.DAO.MilkRateDAO rateDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public MilkEntryViewModel(@NonNull Application app) {
        super(app);
        dao = new DailyTransactionDAO(app);
        customerDao = new com.ignishers.milkmanager2.DAO.CustomerDAO(app);
        rateDao = new com.ignishers.milkmanager2.DAO.MilkRateDAO(app);
    }

    public double getRateForDate(String date) {
        return rateDao.getRateForDate(date);
    }

    public void addDefaultEntry(long customerId) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.model.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
            if (customer != null) {
                double quantity = customer.defaultQuantity > 0 ? customer.defaultQuantity : 1.0;
                double rate = rateDao.getRateForDate(LocalDate.now().toString());
                double amount = (quantity * rate);

                String session;
                if (LocalTime.now().getHour() < 17) {
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
                        LocalTime.now().toString()
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }

    public void addManualEntry(long customerId, String dateStr, double quantity, double amount) {
        executor.execute(() -> {
            String session;
            if (LocalTime.now().getHour() < 17) {
                // Before 5 PM -> Morning
                session = "Morning";
            } else {
                // After 5 PM -> Evening
                session = "Evening";
            }
            
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    dateStr,
                    session,
                    quantity,
                    amount,
                    LocalTime.now().toString()
            );
            dao.insert(entry);
            customerDao.updateCustomerDue(customerId, amount);
            entryAdded.postValue(true);
        });
    }

    public void addNightEntry(long customerId) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.model.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
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
                        LocalTime.now().toString()
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }

    public void addEntry(long customerId, String sessionType, double quantity, double amount) {
        executor.execute(() -> {
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    LocalDate.now().toString(),
                    sessionType,
                    quantity,
                    amount,
                    LocalTime.now().toString()
            );

            dao.insert(entry);
            customerDao.updateCustomerDue(customerId, amount);
            entryAdded.postValue(true);

        });
    }
    public void addPayment(long customerId, double amount, String method, String dateStr) {
        executor.execute(() -> {
            DailyTransaction entry = new DailyTransaction(
                    customerId,
                    dateStr,
                    "Payment", // Session
                    0.0,
                    amount,
                    LocalTime.now().toString(),
                    method // Payment Mode
            );
            dao.insert(entry);
            // DECREASE due by amount
            customerDao.updateCustomerDue(customerId, -amount);
            entryAdded.postValue(true);
        });
    }

    public void addQuickEntry(long customerId, String value) {
        executor.execute(() -> {
            com.ignishers.milkmanager2.model.Customer customer = customerDao.getCustomer(String.valueOf(customerId));
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
                if (LocalTime.now().getHour() < 17) {
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
                        LocalTime.now().toString()
                );
                dao.insert(entry);
                customerDao.updateCustomerDue(customerId, amount);
                entryAdded.postValue(true);
            }
        });
    }
}