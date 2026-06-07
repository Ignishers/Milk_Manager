package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.database.DBHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ignishers.milkmanager2.models.Customer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    private static final String TABLE_CUSTOMER = "customer";
    private static final String COL_ROUTE_ID = "route_id_fk";
    private static final String COL_DEFAULT_QTY = "default_quantity";
    private static final String COL_DEFAULT_QTY_MORN = "default_qty_morning";
    private static final String COL_DEFAULT_QTY_EVE = "default_qty_evening";
    private static final String COL_DUE_BALANCE = "customer_due_balance";
    private static final String WHERE_CUSTOMER_ID_EQ = "customer_id = ?";

    private final SQLiteDatabase db;
    private final Context context;

    public CustomerDAO(Context context) {
        this.context = context;
        db = new DBHelper(context).getWritableDatabase();
    }

    private BigDecimal getBigDecimal(Cursor c, int index) {
        if (c.isNull(index)) return BigDecimal.ZERO;
        String val = c.getString(index);
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public List<Customer> getCustomersByGroup(Long groupId) {
        List<Customer> list = new ArrayList<>();
        String today = java.time.LocalDate.now().toString(); 

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
                    getBigDecimal(c, 4), getBigDecimal(c, 5));
            customer.routeGroupId      = c.getLong(3);
            customer.defaultQtyMorning = getBigDecimal(c, 6);
            customer.defaultQtyEvening = getBigDecimal(c, 7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.isVisited         = (c.getInt(9) == 1);
            list.add(customer);
        }
        c.close();
        return list;
    }

    public long insertCustomer(String name, String mobile, Long routeGroupId,
                               BigDecimal morningQty, BigDecimal eveningQty, BigDecimal currentDue) {
        ContentValues cv = new ContentValues();
        cv.put("customer_name", name);
        cv.put("customer_mobile", mobile);
        
        BigDecimal legacyQty = morningQty.add(eveningQty).divide(new BigDecimal("2.0"), java.math.RoundingMode.HALF_UP);
        cv.put(COL_DEFAULT_QTY, legacyQty.toPlainString());
        cv.put(COL_DEFAULT_QTY_MORN, morningQty.toPlainString());
        cv.put(COL_DEFAULT_QTY_EVE, eveningQty.toPlainString());
        cv.put(COL_DUE_BALANCE, currentDue.toPlainString());
        
        int sortOrder = countCustomersInGroup(routeGroupId);
        cv.put("sort_order", sortOrder);
        if (routeGroupId == null || routeGroupId == 0) {
            cv.putNull(COL_ROUTE_ID);
        } else {
            cv.put(COL_ROUTE_ID, routeGroupId);
        }

        com.ignishers.milkmanager2.managers.SessionManager session = new com.ignishers.milkmanager2.managers.SessionManager(context);
        if (session.isLoggedIn()) {
            cv.put(DBHelper.COL_SYNC_SELLER_ID, session.getSellerId());
        }
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());

        return db.insert(TABLE_CUSTOMER, null, cv);
    }

    private int countCustomersInGroup(Long routeGroupId) {
        String where = (routeGroupId == null || routeGroupId == 0)
                ? "(" + COL_ROUTE_ID + " IS NULL OR " + COL_ROUTE_ID + " = 0)"
                : COL_ROUTE_ID + " = " + routeGroupId;
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CUSTOMER + " WHERE " + where, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public long insertCustomer(String name, String mobile, Long routeGroupId,
                               BigDecimal defaultQuantity, BigDecimal currentDue) {
        return insertCustomer(name, mobile, routeGroupId, defaultQuantity, defaultQuantity, currentDue);
    }

    public Customer getCustomer(String customer_id) {
        Customer customer = null;
        Cursor c = db.rawQuery(
            "SELECT customer_id, customer_name, customer_mobile, " + COL_DEFAULT_QTY + "," +
            " " + COL_DUE_BALANCE + ", " + COL_ROUTE_ID + ", " + COL_DEFAULT_QTY_MORN + ", " + COL_DEFAULT_QTY_EVE + "," +
            " sort_order, COALESCE(auto_entry_enabled, 1) as auto_entry_enabled" +
            " FROM " + TABLE_CUSTOMER + " WHERE " + WHERE_CUSTOMER_ID_EQ,
            new String[]{customer_id});
        if (c.moveToFirst()) {
            customer = new Customer(
                    c.getLong(0), c.getString(1), c.getString(2),
                    getBigDecimal(c, 3), getBigDecimal(c, 4));
            customer.routeGroupId      = c.getLong(5);
            customer.defaultQtyMorning = getBigDecimal(c, 6);
            customer.defaultQtyEvening = getBigDecimal(c, 7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.morningQtySet     = !c.isNull(6);
            customer.eveningQtySet     = !c.isNull(7);
            customer.autoEntryEnabled  = c.isNull(9) || c.getInt(9) == 1;
        }
        c.close();
        return customer;
    }

    public void updateCustomerDetails(long customerId, String name, String mobile, BigDecimal defaultQty, BigDecimal currentDue) {
        ContentValues cv = new ContentValues();
        cv.put("customer_name", name);
        cv.put("customer_mobile", mobile);
        cv.put(COL_DEFAULT_QTY, defaultQty.toPlainString());
        cv.put(COL_DUE_BALANCE, currentDue.toPlainString());
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update(TABLE_CUSTOMER, cv, WHERE_CUSTOMER_ID_EQ, new String[]{String.valueOf(customerId)});
    }

    public void updateCustomerDue(long customerId, BigDecimal amountToAdd) {
        // Must fetch first, then update because sqlite can't do string math easily
        Customer c = getCustomer(String.valueOf(customerId));
        if (c != null) {
            BigDecimal newDue = c.currentDue.add(amountToAdd);
            ContentValues cv = new ContentValues();
            cv.put(COL_DUE_BALANCE, newDue.toPlainString());
            cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
            cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
            db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
        }
    }

    public void updateCustomerDefaultQty(long customerId, BigDecimal newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_quantity", newQty.toPlainString());
        cv.put("default_qty_morning", newQty.toPlainString());
        cv.put("default_qty_evening", newQty.toPlainString());
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public void updateCustomerMorningQty(long customerId, BigDecimal newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_qty_morning", newQty.toPlainString());
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public void updateCustomerEveningQty(long customerId, BigDecimal newQty) {
        ContentValues cv = new ContentValues();
        cv.put("default_qty_evening", newQty.toPlainString());
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public void updateCustomerSortOrder(long customerId, int sortOrder) {
        ContentValues cv = new ContentValues();
        cv.put("sort_order", sortOrder);
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        Cursor c = db.rawQuery(
            "SELECT customer_id, customer_name, customer_mobile, default_quantity, customer_due_balance," +
            " route_id_fk, default_qty_morning, default_qty_evening, sort_order," +
            " COALESCE(auto_entry_enabled, 1) as auto_entry_enabled" +
            " FROM customer WHERE COALESCE(auto_entry_enabled, 1) = 1", null);
        while (c.moveToNext()) {
            Customer customer = new Customer(
                    c.getLong(0), c.getString(1), c.getString(2), getBigDecimal(c, 3), getBigDecimal(c, 4));
            customer.routeGroupId      = c.getLong(5);
            customer.defaultQtyMorning = getBigDecimal(c, 6);
            customer.defaultQtyEvening = getBigDecimal(c, 7);
            customer.sortOrder         = c.isNull(8) ? 0 : c.getInt(8);
            customer.autoEntryEnabled  = true;
            list.add(customer);
        }
        c.close();
        return list;
    }

    public void updateCustomerRoute(long customerId, long newRouteId) {
        ContentValues cv = new ContentValues();
        cv.put("route_id_fk", newRouteId);
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public void deleteCustomer(long customerId) {
        db.delete("customer", "customer_id = ?", new String[]{String.valueOf(customerId)});
    }

    public void updateCustomerAutoEntry(long customerId, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("auto_entry_enabled", enabled ? 1 : 0);
        cv.put(DBHelper.COL_SYNC_IS_SYNCED, 0);
        cv.put(DBHelper.COL_SYNC_UPDATED_AT, System.currentTimeMillis());
        db.update("customer", cv, "customer_id = ?", new String[]{String.valueOf(customerId)});
    }
}