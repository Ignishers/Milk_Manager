package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.database.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ignishers.milkmanager2.models.Customer;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the {@link com.ignishers.milkmanager2.models.Customer} model.
 * <p>
 * The {@code CustomerDAO} abstracts the underlying SQLite database operations. It provides
 * an API for CRUD (Create, Read, Update, Delete) operations specifically customized for customers.
 * It handles the translation of {@code Cursor} objects into Java {@code Customer} entities.
 * </p>
 * <p>
 * <b>Call Flow:</b> ViewModel -> DAO -> DBHelper -> SQLiteDatabase.
 * </p>
 */
public class CustomerDAO {

    private final SQLiteDatabase db;    /**
     * Constructs a new {@code CustomerDAO} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public CustomerDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    // Customers under a folder — Pass null for Root level
    public List<Customer> getCustomersByGroup(Long groupId) {
        List<Customer> list = new ArrayList<>();
        String today = java.time.LocalDate.now().toString(); // API 26+

        // COALESCE on v5+ columns guarantees no crash if migration was partial
        String sql = "SELECT c.customer_id, c.customer_name, c.customer_mobile, c.route_id_fk," +
                     " c.default_quantity, c.customer_due_balance," +
                     " COALESCE(c.default_qty_morning, c.default_quantity) as default_qty_morning," +
                     " COALESCE(c.default_qty_evening, c.default_quantity) as default_qty_evening," +
                     " COALESCE(c.sort_order, 0) as sort_order," +
                     " (CASE WHEN t.transaction_id IS NOT NULL THEN 1 ELSE 0 END) as is_visited" +
                     " FROM " + DBHelper.CUSTOMER_TABLE + " c" +
                     " LEFT JOIN " + DBHelper.MILK_TRANSACTION_TABLE + " t" +
                     " ON c." + DBHelper.COL_CUSTOMER_ID + " = t." + DBHelper.COL_TRANS_CUSTOMER_ID_FK +
                     " AND t." + DBHelper.COL_TRANS_DATE + " = ? ";

        String[] args;
        if (groupId == null || groupId == 0) {
            sql += "WHERE (c.route_id_fk IS NULL OR c.route_id_fk = 0)";
            args = new String[]{today};
        } else {
            sql += "WHERE c.route_id_fk = ?";
            args = new String[]{today, String.valueOf(groupId)};
        }
        sql += " GROUP BY c.customer_id ORDER BY COALESCE(c.sort_order, 0) ASC, c.customer_id ASC";

        Cursor c = db.rawQuery(sql, args);
        while (c.moveToNext()) {
            Customer customer = new Customer(c.getLong(0), c.getString(1), c.getString(2),
                    c.getDouble(4), c.getDouble(5));
            customer.routeGroupId      = c.getLong(3);
            customer.defaultQtyMorning = c.isNull(6) ? customer.defaultQuantity : c.getDouble(6);
            customer.defaultQtyEvening = c.isNull(7) ? customer.defaultQuantity : c.getDouble(7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.isVisited         = (c.getInt(9) == 1);
            list.add(customer);
        }
        c.close();
        return list;

    }

    /** Create customer with separate morning and evening default quantities. */
    public long insertCustomer(String name, String mobile, Long routeGroupId,
                               double morningQty, double eveningQty, double currentDue) {
        ContentValues cv = new ContentValues();
        cv.put("customer_name", name);
        cv.put("customer_mobile", mobile);
        // Store both session-specific and legacy column (use average for compat)
        double legacyQty = (morningQty + eveningQty) / 2.0;
        cv.put("default_quantity", legacyQty);
        cv.put("default_qty_morning", morningQty);
        cv.put("default_qty_evening", eveningQty);
        cv.put("customer_due_balance", currentDue);
        // Auto-assign sort_order = count of existing customers in same group (append at bottom)
        int sortOrder = countCustomersInGroup(routeGroupId);
        cv.put("sort_order", sortOrder);
        if (routeGroupId == null || routeGroupId == 0) {
            cv.putNull("route_id_fk");
        } else {
            cv.put("route_id_fk", routeGroupId);
        }
        return db.insert("customer", null, cv);
    }

    /** Count customers in a group — used to assign sort_order on insert. */
    private int countCustomersInGroup(Long routeGroupId) {
        String where = (routeGroupId == null || routeGroupId == 0)
                ? "(route_id_fk IS NULL OR route_id_fk = 0)"
                : "route_id_fk = " + routeGroupId;
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM customer WHERE " + where, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }


    /**
     * Legacy overload — kept so old callers compile.
     * Sets both morning and evening to the same defaultQuantity.
     */
    public long insertCustomer(String name, String mobile, Long routeGroupId,
                               double defaultQuantity, double currentDue) {
        return insertCustomer(name, mobile, routeGroupId, defaultQuantity, defaultQuantity, currentDue);
    }


    /**
    * Retrieves the {@code Customer} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    *
    * @param customer_id standard parameter provided by caller layer.
    * @return the resulting {@code Customer} payload.
    */
    public Customer getCustomer(String customer_id) {
        Customer customer = null;
        // Use COALESCE so auto_entry_enabled missing column (before v6 upgrade) returns 1 safely
        Cursor c = db.rawQuery(
            "SELECT customer_id, customer_name, customer_mobile, default_quantity," +
            " customer_due_balance, route_id_fk, default_qty_morning, default_qty_evening," +
            " sort_order, COALESCE(auto_entry_enabled, 1) as auto_entry_enabled" +
            " FROM customer WHERE customer_id = ?",
            new String[]{customer_id});
        if (c.moveToFirst()) {
            customer = new Customer(
                    c.getLong(0), c.getString(1), c.getString(2),
                    c.getDouble(3), c.getDouble(4));
            customer.routeGroupId      = c.getLong(5);
            customer.defaultQtyMorning = c.isNull(6) ? customer.defaultQuantity : c.getDouble(6);
            customer.defaultQtyEvening = c.isNull(7) ? customer.defaultQuantity : c.getDouble(7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.morningQtySet     = !c.isNull(6);
            customer.eveningQtySet     = !c.isNull(7);
            // auto_entry_enabled: 1 = ON (default), 0 = OFF
            customer.autoEntryEnabled  = c.isNull(9) || c.getInt(9) == 1;
        }
        c.close();
        return customer;
    }

    /**
    * Modifies the existing {@code CustomerDetails} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param name standard parameter provided by caller layer.
    * @param mobile standard parameter provided by caller layer.
    * @param defaultQty standard parameter provided by caller layer.
    * @param currentDue standard parameter provided by caller layer.
    */
    public void updateCustomerDetails(long customerId, String name, String mobile, double defaultQty, double currentDue) {
        ContentValues cv = new ContentValues();
        cv.put("customer_name", name);
        cv.put("customer_mobile", mobile);
        cv.put("default_quantity", defaultQty);
        cv.put("customer_due_balance", currentDue);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /**
    * Modifies the existing {@code CustomerDue} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param amountToAdd standard parameter provided by caller layer.
    */
    public void updateCustomerDue(long customerId, double amountToAdd) {
        String sql = "UPDATE customer SET customer_due_balance = customer_due_balance + ? WHERE customer_id = ?";
        db.execSQL(sql, new Object[]{amountToAdd, customerId});
    }

    /**
    * Modifies the existing {@code CustomerDefaultQty} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param newQty standard parameter provided by caller layer.
    */
    public void updateCustomerDefaultQty(long customerId, double newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_quantity", newQty);
        cv.put("default_qty_morning", newQty);
        cv.put("default_qty_evening", newQty);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /** Update only the morning default quantity. */
    public void updateCustomerMorningQty(long customerId, double newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_qty_morning", newQty);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /** Update only the evening default quantity. */
    public void updateCustomerEveningQty(long customerId, double newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_qty_evening", newQty);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /** Update the sort_order of a customer row. */
    public void updateCustomerSortOrder(long customerId, int sortOrder) {
        ContentValues cv = new ContentValues();
        cv.put("sort_order", sortOrder);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /**
     * Get all customers where auto_entry_enabled = 1 (used by auto-entry scheduler).
     * Customers with the toggle OFF are excluded so they don't get auto entries.
     * Uses COALESCE so devices where v6 migration hasn't run yet default to ON.
     */
    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT customer_id, customer_name, customer_mobile, default_quantity, customer_due_balance," +
            " route_id_fk, default_qty_morning, default_qty_evening, sort_order," +
            " COALESCE(auto_entry_enabled, 1) as auto_entry_enabled" +
            " FROM customer WHERE COALESCE(auto_entry_enabled, 1) = 1", null);
        while (c.moveToNext()) {
            Customer customer = new Customer(
                    c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4));
            customer.routeGroupId      = c.getLong(5);
            customer.defaultQtyMorning = c.isNull(6) ? customer.defaultQuantity : c.getDouble(6);
            customer.defaultQtyEvening = c.isNull(7) ? customer.defaultQuantity : c.getDouble(7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.autoEntryEnabled  = true; // filtered above
            list.add(customer);
        }
        c.close();
        return list;
    }


    /**
    * Modifies the existing {@code CustomerRoute} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @param newRouteId standard parameter provided by caller layer.
    */
    public void updateCustomerRoute(long customerId, long newRouteId) {
        ContentValues cv = new ContentValues();
        cv.put("route_id_fk", newRouteId);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }
    
    /**
    * Removes the {@code Customer} entity permanently.
    * <p>
    * Executes a destructive SQL DELETE operation. Caution: This might cascade through
    * related tables if foreign key constraints are enabled.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    */
    /** Remove the customer permanently. */
    public void deleteCustomer(long customerId) {
        db.delete("customer", "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    /** Toggle auto-entry on/off for a specific customer. */
    public void updateCustomerAutoEntry(long customerId, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("auto_entry_enabled", enabled ? 1 : 0);
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }
}