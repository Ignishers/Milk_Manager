package com.ignishers.milkmanager2.model;

public class DailyTransaction {
    private int transactionId;
    private long customerId;
    private String date;
    private String session;
    private double quantity;
    private double amount;
    private String timestamp;
    private String paymentMode;

    public DailyTransaction(int transactionId, long customerId, String date, double quantity, double amount){
        this.transactionId = transactionId;
        this.customerId = customerId;

        this.date = date;
        this.quantity = quantity;
        this.amount = amount;
    }

    public DailyTransaction(int transactionId, long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode){
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.date = date;
        this.session = session;
        this.quantity = quantity;
        this.amount = amount;
        this.timestamp = timestamp;
        this.paymentMode = paymentMode;
    }

    // Legacy with ID
    public DailyTransaction(int transactionId, long customerId, String date,String session, double quantity, double amount, String timestamp){
        this(transactionId, customerId, date, session, quantity, amount, timestamp, null);
    }

    public DailyTransaction(long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode){
         this.customerId = customerId;
         this.date = date;
         this.session = session;
         this.quantity = quantity;
         this.amount = amount;
         this.timestamp = timestamp;
         this.paymentMode = paymentMode;
    }

    // Legacy no ID
    public DailyTransaction(long customerId, String date,String session, double quantity, double amount, String timestamp){
        this(customerId, date, session, quantity, amount, timestamp, null);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getDate() {
        return date;
    }

    public String getSession() {
        return session;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getAmount() {
        return amount;
    }

    public String getTimestamp() {
        return timestamp;
    }
    
    public String getPaymentMode() {
        return paymentMode;
    }
}
