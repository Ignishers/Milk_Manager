package com.ignishers.milkmanager2.activities;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.BuildConfig;


import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.models.Customer;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Default documentation for BillActivity.
 * <p>
 * This class is a part of the activities component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class BillActivity extends AppCompatActivity {

    private TextView tvName, tvId, tvDues;
    private RadioGroup rgMain;
    private RadioButton rbLastMonth, rbCustom;
    private LinearLayout layoutCustomOptions;
    
    // Custom Inputs
    private Spinner spReportType, spYear, spMonth, spQuarter;
    private LinearLayout layoutYear, layoutMonth, layoutQuarter, layoutDateRange;
    private Button btnStartDate, btnEndDate, btnDownload, btnView, btnShare;

    private CustomerDAO customerDAO;
    private com.ignishers.milkmanager2.database.DailyTransactionDAO dailyTransactionDAO;
    private Customer customer;
    private LocalDate startDate, endDate;
    
    // Data for Spinners
    private List<String> quarterListFull = new ArrayList<>();
    private ArrayAdapter<String> quarterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        // Setup Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Init Views
        tvName = findViewById(R.id.tvCustomerName);
        tvId = findViewById(R.id.tvCustomerId);
        tvDues = findViewById(R.id.tvTotalDues);
        
        rgMain = findViewById(R.id.rgMainSelection);
        rbLastMonth = findViewById(R.id.rbLastMonth);
        rbCustom = findViewById(R.id.rbCustom);
        
        layoutCustomOptions = findViewById(R.id.layoutCustomOptions);
        
        spReportType = findViewById(R.id.spReportType);
        spYear = findViewById(R.id.spYear);
        spMonth = findViewById(R.id.spMonth);
        spQuarter = findViewById(R.id.spQuarter);
        
        layoutYear = findViewById(R.id.layoutYear);
        layoutMonth = findViewById(R.id.layoutMonth);
        layoutQuarter = findViewById(R.id.layoutQuarter);
        layoutDateRange = findViewById(R.id.layoutDateRange);
        
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        
        btnDownload = findViewById(R.id.btnDownload);
        btnView = findViewById(R.id.btnView);
        btnShare = findViewById(R.id.btnShare);

        // Load Customer
        customerDAO = new CustomerDAO(this);
        dailyTransactionDAO = new com.ignishers.milkmanager2.database.DailyTransactionDAO(this);
        long customerId = getIntent().getLongExtra("customerId", -1);
        if (customerId != -1) {
            customer = customerDAO.getCustomer(String.valueOf(customerId));
            if (customer != null) {
                tvName.setText(customer.name);
                tvId.setText("ID: " + customer.id);
                tvDues.setText(String.format("Total Due: %.2f", customer.currentDue));
            }
        }

        setupMainSelection();
        setupSpinners();
        setupDatePickers();
        
        btnDownload.setOnClickListener(v -> handleBillAction(ActionType.DOWNLOAD));
        btnView.setOnClickListener(v -> handleBillAction(ActionType.VIEW));
        btnShare.setOnClickListener(v -> handleBillAction(ActionType.SHARE));
    }

    private enum ActionType {
        DOWNLOAD, VIEW, SHARE
    }

    /**
    * Mutates the state of {@code upMainSelection}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupMainSelection() {
        rgMain.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCustom) {
                layoutCustomOptions.setVisibility(View.VISIBLE);
            } else {
                layoutCustomOptions.setVisibility(View.GONE);
            }
        });
    }

    /**
    * Mutates the state of {@code upSpinners}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupSpinners() {
        // Report Types
        String[] types = {"By Month", "By Quarter", "By Year", "Lifetime", "Custom Date Range"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReportType.setAdapter(typeAdapter);

        // Years (Current year - 9)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            years.add(String.valueOf(currentYear - i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);

        // Months
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(monthAdapter);
        // Set current month as default
        spMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        // Quarters
        quarterListFull.add("Q1 (Jan-Mar)");
        quarterListFull.add("Q2 (Apr-Jun)");
        quarterListFull.add("Q3 (Jul-Sep)");
        quarterListFull.add("Q4 (Oct-Dec)");
        
        quarterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(quarterListFull));
        quarterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spQuarter.setAdapter(quarterAdapter);

        // Listeners
        spReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVisibility(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateQuarterList();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Initial update
        updateQuarterList();
    }
    
    /**
    * Modifies the existing {@code QuarterList} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    */
    private void updateQuarterList() {
        String selectedYearStr = (String) spYear.getSelectedItem();
        if (selectedYearStr == null) return;
        
        int selectedYear = Integer.parseInt(selectedYearStr);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        quarterAdapter.clear();
        
        if (selectedYear == currentYear) {
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH); // 0-11
            int currentQuarterIndex = currentMonth / 3; // 0-3
            
            for (int i = 0; i <= currentQuarterIndex; i++) {
                if (i < quarterListFull.size()) {
                    quarterAdapter.add(quarterListFull.get(i));
                }
            }
        } else {
            quarterAdapter.addAll(quarterListFull);
        }
        quarterAdapter.notifyDataSetChanged();
    }

    /**
    * Modifies the existing {@code Visibility} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    *
    * @param typeIndex standard parameter provided by caller layer.
    */
    private void updateVisibility(int typeIndex) {
        // Types: 0:Month, 1:Quarter, 2:Year, 3:Lifetime, 4:Range
        
        // Reset all
        layoutYear.setVisibility(View.GONE);
        layoutMonth.setVisibility(View.GONE);
        layoutQuarter.setVisibility(View.GONE);
        layoutDateRange.setVisibility(View.GONE);

        switch (typeIndex) {
            case 0: // Month
                layoutYear.setVisibility(View.VISIBLE);
                layoutMonth.setVisibility(View.VISIBLE);
                break;
            case 1: // Quarter
                layoutYear.setVisibility(View.VISIBLE);
                layoutQuarter.setVisibility(View.VISIBLE);
                break;
            case 2: // Year
                layoutYear.setVisibility(View.VISIBLE);
                break;
            case 3: // Lifetime
                // No extra inputs
                break;
            case 4: // Date Range
                layoutDateRange.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
    * Mutates the state of {@code upDatePickers}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupDatePickers() {
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    /**
    * Executes the {@code showDatePicker} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param isStart standard parameter provided by caller layer.
    */
    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
            if (isStart) {
                startDate = date;
                btnStartDate.setText(date.toString());
            } else {
                endDate = date;
                btnEndDate.setText(date.toString());
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    /**
    * Executes the {@code handleBillAction} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param action standard parameter provided by caller layer.
    */
    private void handleBillAction(ActionType action) {
        if (customer == null) return;

        LocalDate start = null;
        LocalDate end = null;
        String fileName = "Bill_" + customer.name.replaceAll("\\s+", "_") + "_";
        boolean isLastMonthMode = rbLastMonth.isChecked();

        if (isLastMonthMode) {
            // Last Month
            LocalDate now = LocalDate.now();
            LocalDate firstDayCurrent = now.withDayOfMonth(1);
            end = firstDayCurrent.minusDays(1); // Last day of prev month
            start = end.withDayOfMonth(1); // First day of prev month
            fileName += start.getMonth().toString() + "_" + start.getYear() + ".pdf";
        } else {
            // Custom
            int type = spReportType.getSelectedItemPosition();
            int year = Integer.parseInt((String) spYear.getSelectedItem());
            
            switch (type) {
                case 0: // By Month
                    int monthIdx = spMonth.getSelectedItemPosition() + 1;
                    start = LocalDate.of(year, monthIdx, 1);
                    end = start.plusMonths(1).minusDays(1);
                    fileName += start.getMonth().toString() + "_" + year + ".pdf";
                    break;
                    
                case 1: // By Quarter
                    String qStr = (String) spQuarter.getSelectedItem(); 
                    int qNum = Integer.parseInt(qStr.substring(1, 2));
                    int startMonth = (qNum - 1) * 3 + 1;
                    start = LocalDate.of(year, startMonth, 1);
                    end = start.plusMonths(3).minusDays(1);
                    fileName += "Q" + qNum + "_" + year + ".pdf";
                    break;
                    
                case 2: // By Year
                    start = LocalDate.of(year, 1, 1);
                    end = LocalDate.of(year, 12, 31);
                    fileName += year + ".pdf";
                    break;
                    
                case 3: // Lifetime
                    start = LocalDate.of(2000, 1, 1); // Way back
                    end = LocalDate.now();
                    fileName += "Lifetime.pdf";
                    break;
                    
                case 4: // Range
                    if (startDate == null || endDate == null) {
                        Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    start = startDate;
                    end = endDate;
                    fileName += "CustomRange.pdf";
                    break;
            }
        }
        
        if (start != null && end != null) {
            List<com.ignishers.milkmanager2.models.DailyTransaction> transactions = 
                dailyTransactionDAO.getTransactionsByDateRange(customer.id, start.toString(), end.toString());
            
            if (transactions.isEmpty()) {
                Toast.makeText(this, "No transactions found for this period", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- Calculation Logic ---
            double billAmount = 0;
            double billPaid = 0;
            for (com.ignishers.milkmanager2.models.DailyTransaction t : transactions) {
                if (t.getSession().startsWith("Payment")) billPaid += t.getAmount();
                else billAmount += t.getAmount();
            }
            double netChangeList = billAmount - billPaid;

            LocalDate postStart = end.plusDays(1);
            LocalDate postEnd = LocalDate.now();
            double netChangePost = 0;
            double postPayments = 0;
            
            if (!postStart.isAfter(postEnd)) {
                List<com.ignishers.milkmanager2.models.DailyTransaction> postTransactions = 
                    dailyTransactionDAO.getTransactionsByDateRange(customer.id, postStart.toString(), postEnd.toString());
                
                for (com.ignishers.milkmanager2.models.DailyTransaction t : postTransactions) {
                    if (t.getSession().startsWith("Payment")) {
                        postPayments += t.getAmount();
                        netChangePost -= t.getAmount();
                    } else {
                        netChangePost += t.getAmount();
                    }
                }
            }

            double openingBalance = customer.currentDue - netChangeList - netChangePost;
            double closingBalanceOfBill = openingBalance + netChangeList;
            double headerTotalDue;
            
            if (isLastMonthMode) {
                headerTotalDue = closingBalanceOfBill - postPayments;
            } else {
                headerTotalDue = closingBalanceOfBill;
            }

            // Sort Order
            if (!isLastMonthMode) {
                transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
            } else {
                transactions.sort((t1, t2) -> t1.getDate().compareTo(t2.getDate()));
            }
            
            File pdfFile = new com.ignishers.milkmanager2.utils.PDFGenerator(this, customer, transactions, openingBalance, headerTotalDue).generate(fileName);
            
            if (pdfFile != null && pdfFile.exists()) {
                if (action == ActionType.DOWNLOAD) {
                    Toast.makeText(this, "Saved to: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    shareOrViewPdf(pdfFile, action == ActionType.VIEW);
                }
            } else {
                Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
    * Executes the {@code shareOrViewPdf} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param file standard parameter provided by caller layer.
    * @param isView standard parameter provided by caller layer.
    */
    private void shareOrViewPdf(File file, boolean isView) {
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        
        Intent intent = new Intent(isView ? Intent.ACTION_VIEW : Intent.ACTION_SEND);
        if (isView) {
            intent.setDataAndType(uri, "application/pdf");
        } else {
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(Intent.createChooser(intent, isView ? "Open Bill" : "Share Bill"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to handle PDF", Toast.LENGTH_SHORT).show();
        }
    }
}