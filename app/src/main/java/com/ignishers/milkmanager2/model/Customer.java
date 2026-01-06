package com.ignishers.milkmanager2.model;

public class Customer {
    public long id;
    public String name;
    public String mobile;
    public long routeGroupId;
    public String address;
    public String routeGroupName;
    public double defaultQuantity;
    public double currentDue;


    public Customer(long id, String name, String mobile, long routeGroupId) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.routeGroupId = routeGroupId;
    }

    public Customer(long id, String name, String mobile, double defaultQuantity, double currentDue) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.defaultQuantity = defaultQuantity;
        this.currentDue = currentDue;
    }

    public Customer(long id) {
        this.id=id;
    }
}