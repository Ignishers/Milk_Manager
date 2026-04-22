package com.ignishers.milkmanager2.models;

import com.ignishers.milkmanager2.models.Customer;
import com.ignishers.milkmanager2.models.RouteGroup;

/**
 * Default documentation for NavItem.
 * <p>
 * This class is a part of the models component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class NavItem {
    public static final int TYPE_GROUP = 0;
    public static final int TYPE_CUSTOMER = 1;

    public int type;
    public RouteGroup group;
    public Customer customer;

    /**
    * Executes the {@code fromGroup} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param g standard parameter provided by caller layer.
    * @return the resulting {@code NavItem} payload.
    */
    public static NavItem fromGroup(RouteGroup g) {
        NavItem item = new NavItem();
        item.type = TYPE_GROUP;
        item.group = g;
        return item;
    }

    /**
    * Executes the {@code fromCustomer} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param c standard parameter provided by caller layer.
    * @return the resulting {@code NavItem} payload.
    */
    public static NavItem fromCustomer(Customer c) {
        NavItem item = new NavItem();
        item.type = TYPE_CUSTOMER;
        item.customer = c;
        return item;
    }
}