package com.ignishers.milkmanager2.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ignishers.milkmanager2.model.RouteGroup;

import java.util.ArrayList;
import java.util.List;

public class RouteGroupDAO {

    private SQLiteDatabase db;

    public RouteGroupDAO(Context context) {
        db = new DBHelper(context).getWritableDatabase();
    }

    // Root folders (Colonies)
    public List<RouteGroup> getRootGroups() {

        return getChildGroups(0);
    }

    // Sub-folders
    public List<RouteGroup> getChildGroups(long parentId) {
        List<RouteGroup> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM route_group WHERE parent_group_id = ?",
                new String[]{String.valueOf(parentId)}
        );

        while (c.moveToNext()) {
            list.add(new RouteGroup(
                    c.getLong(0),
                    parentId,
                    c.getString(2)
            ));
        }
        c.close();
        return list;
    }

    public RouteGroup getGroupById(long id) {
        Cursor c = db.rawQuery("SELECT * FROM route_group WHERE group_id = ?", new String[]{String.valueOf(id)});
        RouteGroup group = null;
        if (c.moveToFirst()) {
            group = new RouteGroup(c.getLong(0), c.getLong(1), c.getString(2));
        }
        c.close();
        return group;
    }

    // Create folder at ANY level
    public boolean insertGroup(String name, Long parentId) {
        ContentValues cv = new ContentValues();
        cv.put("group_name", name);
        if (parentId != null) cv.put("parent_group_id", parentId);
        long result = db.insert("route_group", null, cv);
        return result != -1;
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