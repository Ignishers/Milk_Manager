package com.ignishers.milkmanager2.models;

/**
 * Default documentation for RouteGroup.
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
public class RouteGroup {
    public long id;
    public Long parentId; // null = root
    public String name;
    public int sortOrder = 0; // v5: for dashboard reordering

    /**
     * Constructs a new {@code RouteGroup} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */

    public RouteGroup(long id, Long parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }
}