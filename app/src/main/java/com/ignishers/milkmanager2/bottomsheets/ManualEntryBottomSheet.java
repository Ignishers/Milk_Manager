package com.ignishers.milkmanager2.bottomsheets;

import com.ignishers.milkmanager2.R;

import com.ignishers.milkmanager2.viewmodels.MilkEntryViewModel;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ignishers.milkmanager2.viewmodels.MilkEntryViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Default documentation for ManualEntryBottomSheet.
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
public class ManualEntryBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CUSTOMER_ID = "customer_id";
    private long customerId;
    private MilkEntryViewModel viewModel;
    
    // UI Components
    private com.google.android.material.textfield.TextInputEditText etDate;
    private com.google.android.material.textfield.TextInputEditText etValue;
    private com.google.android.material.textfield.TextInputLayout tilValue;
    private com.google.android.material.chip.ChipGroup chipGroup;
    private android.widget.TextView tvPreview;
    
    private LocalDate selectedDate;
    private int selectedMode = 0; // 0=Litre, 1=ML, 2=Rupees
    private double currentRate = 60.0; 

    /**
    * Executes the {@code newInstance} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @return the resulting {@code ManualEntryBottomSheet} payload.
    */
    public static ManualEntryBottomSheet newInstance(long customerId) {
        ManualEntryBottomSheet fragment = new ManualEntryBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_manual_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            customerId = getArguments().getLong(ARG_CUSTOMER_ID);
        }
        
        viewModel = new ViewModelProvider(requireActivity()).get(MilkEntryViewModel.class); // Use activity scope to share VM if needed, or just new instance

        initViews(view);
        setupDate();
        setupChips();
        setupInputListener();
        
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveEntry());
    }

    /**
    * Executes the {@code initViews} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param v standard parameter provided by caller layer.
    */
    private void initViews(View v) {
        etDate = v.findViewById(R.id.etDate);
        etValue = v.findViewById(R.id.etValue);
        tilValue = v.findViewById(R.id.tilValue);
        chipGroup = v.findViewById(R.id.chipGroupUnit);
        tvPreview = v.findViewById(R.id.tvPreview);
    }

    /**
    * Mutates the state of {@code upDate}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupDate() {
        selectedDate = LocalDate.now();
        updateDateDisplay();
        
        etDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                updateDateDisplay();
            }, selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
            
            // Optional: Restrict future dates if needed
            // dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
            dpd.show();
        });
    }

    /**
    * Modifies the existing {@code DateDisplay} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    */
    private void updateDateDisplay() {
        etDate.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())));
        // Fetch rate for the selected date
        currentRate = viewModel.getRateForDate(selectedDate.toString());
        // Refresh preview if value exists
        updatePreview();
    }

    /**
    * Mutates the state of {@code upChips}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupChips() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            
            if (id == R.id.chipLitre) {
                selectedMode = 0;
                tilValue.setSuffixText("L");
                tilValue.setHint("Enter Litres");
            } else if (id == R.id.chipMl) {
                selectedMode = 1;
                tilValue.setSuffixText("ml");
                tilValue.setHint("Enter Millilitres");
            } else if (id == R.id.chipRupees) {
                selectedMode = 2;
                tilValue.setSuffixText("₹");
                tilValue.setHint("Enter Amount");
            }
            updatePreview();
        });
    }

    /**
    * Mutates the state of {@code upInputListener}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupInputListener() {
        etValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
    * Modifies the existing {@code Preview} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    */
    private void updatePreview() {
        String input = etValue.getText().toString();
        if (input.isEmpty()) {
            tvPreview.setText("");
            return;
        }

        try {
            double val = Double.parseDouble(input);
            double quantity = 0;
            double amount = 0;

            if (selectedMode == 0) { // Litres
                quantity = val;
                amount = (quantity * currentRate);
            } else if (selectedMode == 1) { // ML
                quantity = val / 1000.0;
                amount = (quantity * currentRate);
            } else { // Rupees
                amount = val;
                quantity = amount / currentRate;
            }
            
            tvPreview.setText(String.format(Locale.getDefault(), "Preview: %.3f L = ₹ %.2f", quantity, amount));
        } catch (NumberFormatException e) {
            tvPreview.setText("Invalid Input");
        }
    }

    /**
    * Executes the {@code saveEntry} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void saveEntry() {
        String input = etValue.getText().toString();
        if (input.isEmpty()) {
            tilValue.setError("Required");
            return;
        }

        try {
            double val = Double.parseDouble(input);
            double quantity;
            double amount;

            if (selectedMode == 0) { // Litres
                quantity = val;
                amount = (quantity * currentRate);
            } else if (selectedMode == 1) { // ML
                quantity = val / 1000.0;
                amount = (quantity * currentRate);
            } else { // Rupees
                amount = val;
                quantity = amount / currentRate;
            }

            viewModel.addManualEntry(customerId, selectedDate.toString(), quantity, amount);
            dismiss();
            
        } catch (NumberFormatException e) {
            tilValue.setError("Invalid number");
        }
    }
}