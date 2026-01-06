package com.ignishers.milkmanager2.model;

public class NavItem {
    public static final int TYPE_GROUP = 0;
    public static final int TYPE_CUSTOMER = 1;

    public int type;
    public RouteGroup group;
    public Customer customer;

    public static NavItem fromGroup(RouteGroup g) {
        NavItem item = new NavItem();
        item.type = TYPE_GROUP;
        item.group = g;
        return item;
    }

    public static NavItem fromCustomer(Customer c) {
        NavItem item = new NavItem();
        item.type = TYPE_CUSTOMER;
        item.customer = c;
        return item;
    }
}