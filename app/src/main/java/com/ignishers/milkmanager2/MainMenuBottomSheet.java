package com.ignishers.milkmanager2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MainMenuBottomSheet extends BottomSheetDialogFragment {

    public interface OnMenuItemClickListener {
        void onSalesRecordClick();
        void onMilkPriceClick();
        void onSettingsClick();
    }

    private OnMenuItemClickListener listener;

    public void setListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }

    private Runnable dismissAction;
    public void setDismissAction(Runnable action) {
        this.dismissAction = action;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissAction != null) {
            dismissAction.run();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_main_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnSales = view.findViewById(R.id.btnSalesRecord);
        View btnPrice = view.findViewById(R.id.btnMilkPrice);
        View btnSettings = view.findViewById(R.id.btnSettings);

        btnSales.setOnClickListener(v -> {
            if (listener != null) listener.onSalesRecordClick();
            dismiss();
        });

        btnPrice.setOnClickListener(v -> {
            if (listener != null) listener.onMilkPriceClick();
            dismiss();
        });

        btnSettings.setOnClickListener(v -> {
            if (listener != null) listener.onSettingsClick();
            dismiss();
        });

        // Staggered Animation
        animateItem(btnSales, 100);
        animateItem(btnPrice, 200);
        animateItem(btnSettings, 300);
    }

    private void animateItem(View view, long delay) {
        view.setTranslationY(100f);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(delay)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    // Optional: make height wrap content nicely? Usually default behavior is fine.
    @Override
    public int getTheme() {
        return com.google.android.material.R.style.Theme_Design_BottomSheetDialog; // Standard clean sheet
    }
}
