package com.ignishers.milkmanager2.dialogs;

import com.ignishers.milkmanager2.R;

import com.ignishers.milkmanager2.viewmodels.MilkEntryViewModel;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.ignishers.milkmanager2.viewmodels.MilkEntryViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Default documentation for PaymentDialog.
 * <p>
 * This class is a part of the dialogs component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class PaymentDialog extends DialogFragment {

    private static final String ARG_CUSTOMER_ID = "arg_cust_id";
    private long customerId;
    private MilkEntryViewModel viewModel;

    private TextInputEditText etDate, etAmount;
    private ChipGroup chipGroupMethod;
    private LocalDate selectedDate;
    private String selectedMethod = "Cash";

    /**
    * Executes the {@code newInstance} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    * @return the resulting {@code PaymentDialog} payload.
    */
    public static PaymentDialog newInstance(long customerId) {
        PaymentDialog fragment = new PaymentDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            customerId = getArguments().getLong(ARG_CUSTOMER_ID);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(MilkEntryViewModel.class);

        etDate = view.findViewById(R.id.etPaymentDate);
        etAmount = view.findViewById(R.id.etAmount);
        chipGroupMethod = view.findViewById(R.id.chipGroupMethod);

        selectedDate = LocalDate.now();
        updateDateDisplay();

        etDate.setOnClickListener(v -> showDatePicker());

        chipGroupMethod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipCash) selectedMethod = "Cash";
            else if (id == R.id.chipUpi) selectedMethod = "UPI";
            else if (id == R.id.chipCard) selectedMethod = "Card";
        });
        
        // Default selection
        chipGroupMethod.check(R.id.chipCash);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSubmit).setOnClickListener(v -> submitPayment());
    }

    /**
    * Executes the {@code showDatePicker} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateDateDisplay();
        }, selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        dpd.show();
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
    }

    /**
    * Executes the {@code submitPayment} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void submitPayment() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Enter Amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            viewModel.addPayment(customerId, amount, selectedMethod, selectedDate.toString());
            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid Amount", Toast.LENGTH_SHORT).show();
        }
    }
}