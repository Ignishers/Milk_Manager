package com.ignishers.milkmanager2;

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

import com.google.android.material.textfield.TextInputEditText;
import com.ignishers.milkmanager2.DAO.MilkRateDAO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SetMilkPriceDialog extends DialogFragment {

    private TextInputEditText etPrice, etEffectiveDate;
    private LocalDate selectedDate;
    private MilkRateDAO rateDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_set_milk_price, container, false);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rateDAO = new MilkRateDAO(requireContext());

        etPrice = view.findViewById(R.id.etPrice);
        etEffectiveDate = view.findViewById(R.id.etEffectiveDate);

        // Default to today
        selectedDate = LocalDate.now();
        updateDateDisplay();
        
        // Load current rate
        double currentRate = rateDAO.getCurrentRate();
        etPrice.setText(String.valueOf(currentRate));
        etPrice.selectAll(); // Convenience

        etEffectiveDate.setOnClickListener(v -> showDatePicker());
        
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> savePrice());
    }

    private void showDatePicker() {
        DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            updateDateDisplay();
        }, selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
        dpd.show();
    }

    private void updateDateDisplay() {
        etEffectiveDate.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())));
    }

    private void savePrice() {
        String priceStr = etPrice.getText().toString();
        if (priceStr.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a valid price", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double rate = Double.parseDouble(priceStr);
            rateDAO.insertRate(rate, selectedDate.toString()); // yyyy-MM-dd
            Toast.makeText(requireContext(), "Price Updated", Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
        }
    }
}
