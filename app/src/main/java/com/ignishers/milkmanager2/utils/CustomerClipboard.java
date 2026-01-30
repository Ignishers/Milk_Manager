package com.ignishers.milkmanager2.utils;

public class CustomerClipboard {
    private static Long customerIdToMove = null;
    private static String customerName = null;

    public static void copy(long id, String name) {
        customerIdToMove = id;
        customerName = name;
    }

    public static Long getCustomerId() {
        return customerIdToMove;
    }
    
    public static String getCustomerName() {
        return customerName;
    }

    public static void clear() {
        customerIdToMove = null;
        customerName = null;
    }

    public static boolean hasClip() {
        return customerIdToMove != null;
    }
}
