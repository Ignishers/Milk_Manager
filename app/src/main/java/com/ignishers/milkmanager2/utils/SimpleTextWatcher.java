package com.ignishers.milkmanager2.utils;

import android.text.Editable;
import android.text.TextWatcher;

public class SimpleTextWatcher implements TextWatcher {

    private final Runnable onTextChanged;

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
