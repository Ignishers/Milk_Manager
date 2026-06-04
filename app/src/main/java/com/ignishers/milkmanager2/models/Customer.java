package com.ignishers.milkmanager2.models;

import java.math.BigDecimal;

/**
 * Represents a Customer entity within the Milk Manager 2 domain space.
 * Updated to use BigDecimal for exact financial precision.
 */
public class Customer {
    public long id;
    public String name;
    public String mobile;
    public long routeGroupId;
    public String address;
    public String routeGroupName;
    
    public BigDecimal defaultQuantity;
    public BigDecimal defaultQtyMorning;
    public BigDecimal defaultQtyEvening;
    public BigDecimal currentDue;
    
    public int sortOrder = 0;
    public boolean morningQtySet = false;
    public boolean eveningQtySet = false;
    public boolean autoEntryEnabled = true;
    public boolean isVisited = false;

    public Customer(long id, String name, String mobile, long routeGroupId) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.routeGroupId = routeGroupId;
        this.defaultQuantity = BigDecimal.ZERO;
        this.defaultQtyMorning = BigDecimal.ZERO;
        this.defaultQtyEvening = BigDecimal.ZERO;
        this.currentDue = BigDecimal.ZERO;
    }

    public Customer(long id, String name, String mobile, BigDecimal defaultQuantity, BigDecimal currentDue) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.defaultQuantity = defaultQuantity;
        this.currentDue = currentDue;
        this.defaultQtyMorning = defaultQuantity;
        this.defaultQtyEvening = defaultQuantity;
    }

    public Customer(long id) {
        this.id = id;
        this.defaultQuantity = BigDecimal.ZERO;
        this.defaultQtyMorning = BigDecimal.ZERO;
        this.defaultQtyEvening = BigDecimal.ZERO;
        this.currentDue = BigDecimal.ZERO;
    }
}