package com.ignishers.milkmanager2.models;

import java.math.BigDecimal;

/**
 * Models a single milk transaction.
 * Updated to use BigDecimal for exact financial precision.
 */
public class DailyTransaction {
    private int transactionId;
    private long customerId;
    private String date;
    private String session;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String timestamp;
    private String paymentMode;
    private String milkType; 

    public DailyTransaction(int transactionId, long customerId, String date, BigDecimal quantity, BigDecimal amount){
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.date = date;
        this.quantity = quantity;
        this.amount = amount;
    }

    public DailyTransaction(int transactionId, long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp, String paymentMode, String milkType){
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
    
    public DailyTransaction(int transactionId, long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp, String paymentMode) {
        this(transactionId, customerId, date, session, quantity, amount, timestamp, paymentMode, "Regular");
    }

    public DailyTransaction(int transactionId, long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp){
        this(transactionId, customerId, date, session, quantity, amount, timestamp, null, "Regular");
    }

    public DailyTransaction(long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp, String paymentMode, String milkType){
         this.customerId = customerId;
         this.date = date;
         this.session = session;
         this.quantity = quantity;
         this.amount = amount;
         this.timestamp = timestamp;
         this.paymentMode = paymentMode;
         this.milkType = milkType;
    }
    
    public DailyTransaction(long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp, String paymentMode){
         this(customerId, date, session, quantity, amount, timestamp, paymentMode, "Regular");
    }

    public DailyTransaction(long customerId, String date, String session, BigDecimal quantity, BigDecimal amount, String timestamp){
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
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