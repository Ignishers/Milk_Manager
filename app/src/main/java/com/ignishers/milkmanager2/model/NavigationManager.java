package com.ignishers.milkmanager2.model;

import java.util.Stack;

public class NavigationManager {

    private Stack<Long> navigationStack = new Stack<>();

    public void enterGroup(long groupId) {
        navigationStack.push(groupId);
    }

    public Long goBack() {
        if (!navigationStack.isEmpty()) {
            navigationStack.pop();
        }
        return getCurrentGroup();
    }

    public Long getCurrentGroup() {
        return navigationStack.isEmpty() ? null : navigationStack.peek();
    }

    public boolean isAtRoot() {
        return navigationStack.isEmpty();
    }

    public Stack<Long> getPathStack() {
        Stack<Long> copy = new Stack<>();
        copy.addAll(navigationStack);
        return copy;
    }

    public void clear() {
        navigationStack.clear();
    }
}
