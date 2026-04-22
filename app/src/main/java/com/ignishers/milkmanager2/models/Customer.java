package com.ignishers.milkmanager2.models;

/**
 * Represents a Customer entity within the Milk Manager 2 domain space.
 * <p>
 * A {@code Customer} object defines the state and blueprint of a buyer. It encapsulates
 * details such as the customer's unique identifier, name, mobile number, address, 
 * default milk quantity required daily, and the current outstanding due amount.
 * </p>
 * <p>
 * <b>Data Source:</b> Populated from the {@code customer} table in SQLite via {@code CustomerDAO}.
 * <b>Data Destination:</b> Displayed in {@code PointOfSaleActivity} and utilized as foreign key
 * references in {@link DailyTransaction}.
 * </p>
 *
 * @author MilkManager Core Team
 * @see com.ignishers.milkmanager2.database.CustomerDAO
 */
public class Customer {
    public long id;
    public String name;
    public String mobile;
    public long routeGroupId;
    public String address;
    public String routeGroupName;
    public double defaultQuantity;
    public double defaultQtyMorning;  // v5: morning session default
    public double defaultQtyEvening;  // v5: evening session default
    public double currentDue;
    public int sortOrder = 0;         // v5: for dashboard reordering
    /** true = column has been explicitly written (even if value is 0); false = column was NULL (never set). */
    public boolean morningQtySet = false;
    public boolean eveningQtySet = false;
    /** Per-customer auto-entry toggle. true = entries fire at 5 AM/4 PM; false = paused for this customer. */
    public boolean autoEntryEnabled = true;
    public boolean isVisited = false;
/**
     * Constructs a new {@code Customer} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public Customer(long id, String name, String mobile, long routeGroupId) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.routeGroupId = routeGroupId;
    }    /**
     * Constructs a new {@code Customer} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public Customer(long id, String name, String mobile, double defaultQuantity, double currentDue) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.defaultQuantity = defaultQuantity;
        this.currentDue = currentDue;
    }    /**
     * Constructs a new {@code Customer} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public Customer(long id) {
        this.id=id;
    }
}