package com.ignishers.milkmanager2;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ignishers.milkmanager2.DAO.DailyTransactionDAO;
import com.ignishers.milkmanager2.model.DailyTransaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthSummaryDialog extends DialogFragment {

    private static final String ARG_CUSTOMER_ID = "arg_cust_id";
    private static final String ARG_MONTH = "arg_month";
    private static final String ARG_YEAR = "arg_year";

    private DailyTransactionDAO dao;

    public static MonthSummaryDialog newInstance(long customerId, String month, String year) {
        MonthSummaryDialog fragment = new MonthSummaryDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_CUSTOMER_ID, customerId);
        args.putString(ARG_MONTH, month);
        args.putString(ARG_YEAR, year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Transparent for CardView rounded corners
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_month_summary, container, false);
    }

    private android.widget.Spinner spinnerMonth, spinnerYear;
    private long currentCustomerId;
    private int currentMonth;
    private int currentYear;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = new DailyTransactionDAO(requireContext());

        if (getArguments() == null) return;
        currentCustomerId = getArguments().getLong(ARG_CUSTOMER_ID);
        String monthStr = getArguments().getString(ARG_MONTH);
        String yearStr = getArguments().getString(ARG_YEAR);
        
        try {
            currentMonth = Integer.parseInt(monthStr);
            currentYear = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            LocalDate now = LocalDate.now();
            currentMonth = now.getMonthValue();
            currentYear = now.getYear();
        }

        view.findViewById(R.id.btnFooterClose).setOnClickListener(v -> dismiss());

        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        TableLayout tableLayout = view.findViewById(R.id.tableLayout);
        
        setupSpinners(tableLayout);
        loadTableData(tableLayout);
    }
    
    private void setupSpinners(TableLayout table) {
        // Months
        // Let's use a fixed array for safety and consistency
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        android.widget.ArrayAdapter<String> monthAdapter = new android.widget.ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, monthNames);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(currentMonth - 1); // 0-indexed

        // Years 2020 - 2049
        String[] years = new String[30];
        for (int i = 0; i < 30; i++) {
            years[i] = String.valueOf(2020 + i);
        }
        android.widget.ArrayAdapter<String> yearAdapter = new android.widget.ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        
        // Find index for current year
        int yearIndex = 0;
        for(int i=0; i<years.length; i++) {
            if (Integer.parseInt(years[i]) == currentYear) {
                yearIndex = i;
                break;
            }
        }
        spinnerYear.setSelection(yearIndex);

        // Listeners for user selection
        // We need to avoid infinite loops or triggering on initial setup if not desired, 
        // but simple OnItemSelectedListener is usually fine.
        
        android.widget.AdapterView.OnItemSelectedListener listener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                 // Update internal state
                 currentMonth = spinnerMonth.getSelectedItemPosition() + 1;
                 currentYear = Integer.parseInt((String) spinnerYear.getSelectedItem());
                 
                 // Reload Data
                 loadTableData(table);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };

        spinnerMonth.setOnItemSelectedListener(listener);
        spinnerYear.setOnItemSelectedListener(listener);
    }

    private void loadTableData(TableLayout table) {
        table.removeAllViews(); // Clear existing rows
        
        String monthStr = String.format(Locale.getDefault(), "%02d", currentMonth);
        String yearStr = String.valueOf(currentYear);

        // Fetch data
        List<DailyTransaction> transactions = dao.getTransactionsByMonth(String.valueOf(currentCustomerId), monthStr, yearStr);

        // Map: Day -> Data
        Map<Integer, DayData> dataMap = new HashMap<>();

        for (DailyTransaction t : transactions) {
            try {
                LocalDate date = LocalDate.parse(t.getDate()); // yyyy-MM-dd
                int day = date.getDayOfMonth();
                
                if (!dataMap.containsKey(day)) {
                    dataMap.put(day, new DayData());
                }
                DayData d = dataMap.get(day);
                
                // Aggregate
                // Skip Payments from Milk Summary
                if (t.getSession().startsWith("Payment")) continue;
                
                if ("Evening".equalsIgnoreCase(t.getSession())) {
                    d.eveQty += t.getQuantity();
                    d.eveAmt += t.getAmount();
                } else {
                    // Morning OR Manual (Assuming Morning/Manual goes to Day col)
                    d.morQty += t.getQuantity();
                    d.morAmt += t.getAmount();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fetch "Payment" entries from Transactions
        for (DailyTransaction t : transactions) {
            try {
                if (!t.getSession().startsWith("Payment")) continue;
                
                LocalDate date = LocalDate.parse(t.getDate());
                int day = date.getDayOfMonth();
                
                if (!dataMap.containsKey(day)) {
                     dataMap.put(day, new DayData());
                }
                
                // Assuming amount is stored in Transaction Amount field
                dataMap.get(day).paymentAmt += t.getAmount();
                
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }

        // Determine days in month
        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);
        int daysInMonth = yearMonth.lengthOfMonth();

        // Build Rows
        for (int i = 1; i <= daysInMonth; i++) {
            TableRow row = new TableRow(requireContext());
            row.setPadding(0, 16, 0, 16);
            if (i % 2 == 0) row.setBackgroundColor(Color.parseColor("#F9F9F9"));

            // Date Col
            TextView tvDate = createTextView(String.valueOf(i));
            
            // Morning Col
            DayData data = dataMap.get(i);
            String morText = "-";
            String eveText = "-";
            
            if (data != null) {
                if (data.morQty > 0 || data.morAmt > 0) {
                     morText = String.format(Locale.getDefault(), "%.3f L\n₹ %.2f", data.morQty, data.morAmt);
                }
                if (data.eveQty > 0 || data.eveAmt > 0) {
                     eveText = String.format(Locale.getDefault(), "%.3f L\n₹ %.2f", data.eveQty, data.eveAmt);
                }
            }

            TextView tvMor = createTextView(morText);
            TextView tvEve = createTextView(eveText);
            
            // Payment Col
            String payText = "-";
            if (data != null && data.paymentAmt > 0) {
                 payText = String.format(Locale.getDefault(), "₹ %.2f", data.paymentAmt);
            }
            TextView tvPay = createTextView(payText);
            tvPay.setTextColor(Color.parseColor("#4CAF50")); // Green color for payment

            row.addView(tvDate);
            row.addView(tvMor);
            row.addView(tvEve);
            row.addView(tvPay);

            table.addView(row);
        }

        // Display Totals
        double totalMilk = 0;
        double totalAmount = 0;
        double totalPayment = 0;

        for (DayData d : dataMap.values()) {
            totalMilk += d.morQty + d.eveQty;
            totalAmount += d.morAmt + d.eveAmt;
            totalPayment += d.paymentAmt;
        }
        
        TextView tvTotal = getView().findViewById(R.id.tvTotalSummary);
        if (tvTotal != null) {
            String summary = String.format(Locale.getDefault(), 
                "Milk: %.2f L  |  Amt: ₹ %.2f\nPaid: ₹ %.2f", 
                totalMilk, totalAmount, totalPayment);
            tvTotal.setText(summary);
        }
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.BLACK);
        tv.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return tv;
    }

    private static class DayData {
        double morQty, morAmt;
        double eveQty, eveAmt;
        double paymentAmt;
    }
}
