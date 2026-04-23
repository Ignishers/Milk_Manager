package com.ignishers.milkmanager2.bottomsheets;

import com.ignishers.milkmanager2.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Default documentation for MainMenuBottomSheet.
 * <p>
 * This class is a part of the bottomsheets component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class MainMenuBottomSheet extends BottomSheetDialogFragment {

    public interface OnMenuItemClickListener {
        void onSalesRecordClick();
        void onMilkPriceClick();
        void onSettingsClick();
    }

    private OnMenuItemClickListener listener;

    /**
    * Mutates the state of {@code Listener}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    *
    * @param listener standard parameter provided by caller layer.
    */
    public void setListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }

    private Runnable dismissAction;
    /**
    * Mutates the state of {@code DismissAction}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    *
    * @param action standard parameter provided by caller layer.
    */
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

    /**
    * Executes the {@code animateItem} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param view standard parameter provided by caller layer.
    * @param delay standard parameter provided by caller layer.
    */
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