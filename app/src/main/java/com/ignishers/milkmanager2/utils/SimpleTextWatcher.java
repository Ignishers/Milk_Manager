package com.ignishers.milkmanager2.utils;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Default documentation for SimpleTextWatcher.
 * <p>
 * This class is a part of the utils component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class SimpleTextWatcher implements TextWatcher {

    private final Runnable onTextChanged;    /**
     * Constructs a new {@code SimpleTextWatcher} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public SimpleTextWatcher(Runnable onTextChanged) {
        this.onTextChanged = onTextChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // no-op
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (onTextChanged != null) {
            onTextChanged.run();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // no-op
    }
}