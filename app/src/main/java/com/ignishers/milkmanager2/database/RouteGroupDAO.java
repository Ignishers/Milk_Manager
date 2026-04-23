package com.ignishers.milkmanager2.database;

import com.ignishers.milkmanager2.database.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ignishers.milkmanager2.models.RouteGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Default documentation for RouteGroupDAO.
 * <p>
 * This class is a part of the database component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class RouteGroupDAO {

    private SQLiteDatabase db;    /**
     * Constructs a new {@code RouteGroupDAO} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public RouteGroupDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    // Root folders (Colonies)
    public List<RouteGroup> getRootGroups() {

        return getChildGroups(0);
    }

    // Sub-folders — ordered by sort_order
    public List<RouteGroup> getChildGroups(long parentId) {
        List<RouteGroup> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT group_id, parent_group_id, group_name, sort_order FROM route_group" +
                " WHERE parent_group_id = ? AND group_id != 0" +
                " ORDER BY sort_order ASC, group_id ASC",
                new String[]{String.valueOf(parentId)}
        );
        while (c.moveToNext()) {
            RouteGroup g = new RouteGroup(c.getLong(0), parentId, c.getString(2));
            g.sortOrder = c.isNull(3) ? 0 : c.getInt(3);
            list.add(g);
        }
        c.close();
        return list;
    }

    /**
    * Retrieves the {@code GroupById} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    *
    * @param id standard parameter provided by caller layer.
    * @return the resulting {@code RouteGroup} payload.
    */
    public RouteGroup getGroupById(long id) {
        Cursor c = db.rawQuery("SELECT * FROM route_group WHERE group_id = ?", new String[]{String.valueOf(id)});
        RouteGroup group = null;
        if (c.moveToFirst()) {
            group = new RouteGroup(c.getLong(0), c.getLong(1), c.getString(2));
        }
        c.close();
        return group;
    }

    // Create folder at ANY level — auto-assigns sort_order at end of list
    public boolean insertGroup(String name, Long parentId) {
        ContentValues cv = new ContentValues();
        cv.put("group_name", name);
        if (parentId != null) cv.put("parent_group_id", parentId);
        // Assign sort_order = count of siblings
        int count = getGroupCountUnder(parentId == null ? 0 : parentId);
        cv.put("sort_order", count);
        long result = db.insert("route_group", null, cv);
        return result != -1;
    }

    /** Count of child groups under a parent (for sort_order assignment). */
    private int getGroupCountUnder(long parentId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM route_group WHERE parent_group_id = ? AND group_id != 0",
                new String[]{String.valueOf(parentId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** Swap sort_order of two groups (used for move up/down). */
    public void swapSortOrder(long groupId1, int order1, long groupId2, int order2) {
        ContentValues cv1 = new ContentValues();
        cv1.put("sort_order", order2);
        db.update("route_group", cv1, "group_id = ?", new String[]{String.valueOf(groupId1)});

        ContentValues cv2 = new ContentValues();
        cv2.put("sort_order", order1);
        db.update("route_group", cv2, "group_id = ?", new String[]{String.valueOf(groupId2)});
    }

    /**
    * Removes the {@code Group} entity permanently.
    * <p>
    * Executes a destructive SQL DELETE operation. Caution: This might cascade through
    * related tables if foreign key constraints are enabled.
    * </p>
    *
    * @param groupId standard parameter provided by caller layer.
    * @return the resulting {@code boolean} payload.
    */
    public boolean deleteGroup(long groupId) {
        return db.delete("route_group", "group_id = ?", new String[]{String.valueOf(groupId)}) > 0;
    }

    // List All Groups (for Selection Dialog)
    public List<RouteGroup> getAllGroups() {
        List<RouteGroup> list = new ArrayList<>();
        // Exclude Root (0) if you don't want them directly in Root, but here we probably want all valid groups
        Cursor c = db.rawQuery("SELECT * FROM route_group WHERE group_id > 0", null);
        while (c.moveToNext()) {
            list.add(new RouteGroup(c.getLong(0), c.getLong(1), c.getString(2)));
        }
        c.close();
        return list;
    }
    
    // Ensure Root Route (ID 0) exists for Foreign Key constraints
    public void ensureRootRouteExists() {
        Cursor c = db.rawQuery("SELECT 1 FROM route_group WHERE group_id = 0", null);
        boolean exists = c.moveToFirst();
        c.close();

        if (!exists) {
            ContentValues cv = new ContentValues();
            cv.put("group_id", 0);
            cv.put("group_name", "MAIN_DEPOT");
            // Parent is null for root
            db.insert("route_group", null, cv);
        }
    }
}