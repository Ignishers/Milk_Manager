package com.ignishers.milkmanager2.utils;

/**
 * Default documentation for CustomerClipboard.
 * <p>
 * This class is a part of the utils component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class CustomerClipboard {
    private static Long customerIdToMove = null;
    private static String customerName = null;

    /**
    * Executes the {@code copy} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param id standard parameter provided by caller layer.
    * @param name standard parameter provided by caller layer.
    */
    public static void copy(long id, String name) {
        customerIdToMove = id;
        customerName = name;
    }

    /**
    * Retrieves the {@code CustomerId} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code Long} payload.
    */
    public static Long getCustomerId() {
        return customerIdToMove;
    }
    
    /**
    * Retrieves the {@code CustomerName} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code String} payload.
    */
    public static String getCustomerName() {
        return customerName;
    }

    /**
    * Executes the {@code clear} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    public static void clear() {
        customerIdToMove = null;
        customerName = null;
    }

    /**
    * Executes the {@code hasClip} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    * @return the resulting {@code boolean} payload.
    */
    public static boolean hasClip() {
        return customerIdToMove != null;
    }
}