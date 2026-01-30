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
    private String milkType; // Regular (Default) or Extra 

    public DailyTransaction(int transactionId, long customerId, String date, double quantity, double amount){
        this.transactionId = transactionId;
        this.customerId = customerId;

        this.date = date;
        this.quantity = quantity;
        this.amount = amount;
    }

    public DailyTransaction(int transactionId, long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode, String milkType){
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.date = date;
        this.session = session;
        this.quantity = quantity;
        this.amount = amount;
        this.timestamp = timestamp;
        this.paymentMode = paymentMode;
        this.milkType = milkType;
    }
    
    // Compatibility Constructor for DB Reading (where ID is present)
    public DailyTransaction(int transactionId, long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode) {
        this(transactionId, customerId, date, session, quantity, amount, timestamp, paymentMode, "Regular");
    }

    // Legacy with ID
    public DailyTransaction(int transactionId, long customerId, String date,String session, double quantity, double amount, String timestamp){
        this(transactionId, customerId, date, session, quantity, amount, timestamp, null, "Regular");
    }

    // Main Constructor for New Entries (No ID)
    public DailyTransaction(long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode, String milkType){
         this.customerId = customerId;
         this.date = date;
         this.session = session;
         this.quantity = quantity;
         this.amount = amount;
         this.timestamp = timestamp;
         this.paymentMode = paymentMode;
         this.milkType = milkType;
    }
    
     // Compatibility for New Entries (No ID, defaults)
    public DailyTransaction(long customerId, String date,String session, double quantity, double amount, String timestamp, String paymentMode){
         this(customerId, date, session, quantity, amount, timestamp, paymentMode, "Regular");
    }

    // Legacy no ID
    public DailyTransaction(long customerId, String date,String session, double quantity, double amount, String timestamp){
        this(customerId, date, session, quantity, amount, timestamp, null, "Regular");
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
    
    public String getMilkType() {
        return milkType;
    }
}
