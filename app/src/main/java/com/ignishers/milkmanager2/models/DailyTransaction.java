package com.ignishers.milkmanager2.models;

/**
 * Models a single milk transaction.
 * <p>
 * A {@code DailyTransaction} records the event of selling milk to a {@link Customer}. It tracks
 * the transaction ID, customer ID, date, shift (Morning/Evening), quantity dispensed, total amount,
 * and the mode of payment. 
 * </p>
 * <p>
 * <b>Lifecycle:</b> Instantiated from user input in {@code PointOfSaleActivity} -> 
 * Validated by ViewModels -> Persisted into {@code milk_transaction} table by {@code DailyTransactionDAO}.
 * </p>
 *
 * @since 1.0
 */
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
    }    /**
     * Constructs a new {@code DailyTransaction} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
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

    /**
    * Retrieves the {@code TransactionId} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code int} payload.
    */
    public int getTransactionId() {
        return transactionId;
    }

    /**
    * Retrieves the {@code CustomerId} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code long} payload.
    */
    public long getCustomerId() {
        return customerId;
    }

    /**
    * Retrieves the {@code Date} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public String getDate() {
        return date;
    }

    /**
    * Retrieves the {@code Session} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public String getSession() {
        return session;
    }

    /**
    * Retrieves the {@code Quantity} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code double} payload.
    */
    public double getQuantity() {
        return quantity;
    }

    /**
    * Retrieves the {@code Amount} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code double} payload.
    */
    public double getAmount() {
        return amount;
    }

    /**
    * Retrieves the {@code Timestamp} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public String getTimestamp() {
        return timestamp;
    }
    
    /**
    * Retrieves the {@code PaymentMode} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public String getPaymentMode() {
        return paymentMode;
    }
    
    /**
    * Retrieves the {@code MilkType} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public String getMilkType() {
        return milkType;
    }
}