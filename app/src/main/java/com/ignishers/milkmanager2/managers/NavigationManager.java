package com.ignishers.milkmanager2.managers;

import java.util.Stack;

/**
 * Default documentation for NavigationManager.
 * <p>
 * This class is a part of the managers component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class NavigationManager {

    private Stack<Long> navigationStack = new Stack<>();

    /**
    * Executes the {@code enterGroup} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param groupId standard parameter provided by caller layer.
    */
    public void enterGroup(long groupId) {
        navigationStack.push(groupId);
    }

    /**
    * Executes the {@code goBack} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    * @return the resulting {@code Long} payload.
    */
    public Long goBack() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop();
        }
        return getCurrentGroup();
    }

    /**
    * Retrieves the {@code CurrentGroup} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code Long} payload.
    */
    public Long getCurrentGroup() {
        return navigationStack.isEmpty() ? null : navigationStack.peek();
    }

    /**
    * Executes the {@code isAtRoot} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    * @return the resulting {@code boolean} payload.
    */
    public boolean isAtRoot() {
        return navigationStack.isEmpty();
    }

    /**
    * Retrieves the {@code PathStack} data.
    * <p>
    * This method acts as an accessor. If interacting with DAOs, it fetches the state
    * from the SQLite database via a {@code Cursor} and maps it to the respective model objects.
    * </p>
    * @return the resulting {@code Stack<Long>} payload.
    */
    public Stack<Long> getPathStack() {
        Stack<Long> copy = new Stack<>();
        copy.addAll(navigationStack);
        return copy;
    }

    /**
    * Executes the {@code clear} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    public void clear() {
        navigationStack.clear();
    }
}