package com.ignishers.milkmanager2.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ignishers.milkmanager2.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private final SQLiteDatabase db;

    public CustomerDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    // Customers under a folder
    // Pass null for Root level
    public List<Customer> getCustomersByGroup(Long groupId) {
        List<Customer> list = new ArrayList<>();
        String selection;
        String[] args;

        if (groupId == null || groupId == 0) { // Treating 0 as null for backward compat or if passed 0
            // For Root: Fetch where route_id is NULL (preferred) OR 0 (legacy/forced)
            selection = "route_id_fk IS NULL OR route_id_fk = 0";
            args = null;
        } else {
            selection = "route_id_fk = ?";
            args = new String[]{String.valueOf(groupId)};
        }

        Cursor c = db.query(
                "customer",
                new String[]{"customer_id", "customer_name", "customer_mobile", "route_id_fk"},
                selection,
                args,
                null, null, null
        );

        while (c.moveToNext()) {
            list.add(new Customer(
                    c.getLong(0),
                    c.getString(1),
                    c.getString(2),
                    c.getLong(3)
            ));
        }
        c.close();
        return list;
    }

    // Create customer at ANY hierarchy level
    public long insertCustomer(String name, String mobile, Long routeGroupId, double defaultQuantity, double currentDue) {
        ContentValues cv = new ContentValues();
        cv.put("customer_name", name);
        cv.put("customer_mobile", mobile);
        cv.put("default_quantity", defaultQuantity);
        cv.put("customer_due_balance", currentDue);
        // If routeGroupId is null or 0, we can store NULL to satisfy FK constraint (since ID 0 might not exist)
        if (routeGroupId == null || routeGroupId == 0) {
            cv.putNull("route_id_fk");
        } else {
            cv.put("route_id_fk", routeGroupId);
        }
        return db.insert("customer", null, cv);
    }

    public  Customer getCustomer(String customer_id){
        Customer customer = null;
        Cursor c = db.query(
                "customer",
                new String[]{"customer_id", "customer_name", "customer_mobile", "default_quantity", "customer_due_balance", "route_id_fk"},
                "customer_id = ?",
                new String[]{customer_id},
                null, null, null
        );
        if (c.moveToFirst()) {
            customer = new Customer(
                    c.getLong(0),
                    c.getString(1),
                    c.getString(2),
                    c.getDouble(3),
                    c.getDouble(4)
            );
            // route_id_fk can be null, handled by getLong returning 0 if null usually, but let's be safe or just use getLong
            // cursor.getLong returns 0 if column is null which matches our "0 is null/root" logic often used
            customer.routeGroupId = c.getLong(5);
        }
        c.close();

        return customer;
    }
    public void updateCustomerDue(long customerId, double amountToAdd) {
        String sql = "UPDATE customer SET customer_due_balance = customer_due_balance + ? WHERE customer_id = ?";
        db.execSQL(sql, new Object[]{amountToAdd, customerId});
    }
}