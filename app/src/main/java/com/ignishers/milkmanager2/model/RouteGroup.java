package com.ignishers.milkmanager2.model;

public class RouteGroup {
    public long id;
    public Long parentId; // null = root
    public String name;

    public RouteGroup(long id, Long parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }
}