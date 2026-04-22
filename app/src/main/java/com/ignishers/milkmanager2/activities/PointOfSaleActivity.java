package com.ignishers.milkmanager2.activities;

import com.ignishers.milkmanager2.R;

import com.ignishers.milkmanager2.activities.BillActivity;
import com.ignishers.milkmanager2.fragments.MilkEntryFragment;
import com.ignishers.milkmanager2.dialogs.PaymentDialog;
import com.ignishers.milkmanager2.dialogs.MonthSummaryDialog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.database.DailyTransactionDAO;
import com.ignishers.milkmanager2.adapters.DailyTransactionAdapter;
import com.ignishers.milkmanager2.models.Customer;
import com.ignishers.milkmanager2.models.DailyTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facilitates the rapid entry of daily milk deliveries.
 * <p>
 * The {@code PointOfSaleActivity} retrieves a list of active {@link com.ignishers.milkmanager2.models.Customer}s
 * based on selected routes. It presents them in a fast-scrolling {@link androidx.recyclerview.widget.RecyclerView}.
 * Users can log morning/evening milk quantities with one tap.
 * </p>
 * <p>
 * <b>Outbound Data Flow:</b> The inputs captured here are transformed into {@link com.ignishers.milkmanager2.models.DailyTransaction}
 * instances and written to the database via corresponding DAOs.
 * </p>
 */
public class PointOfSaleActivity extends AppCompatActivity implements MilkEntryFragment.OnEntryListener {

    private DailyTransactionDAO dailyTransactionDAO;
    private CustomerDAO customerDAO;
    private Customer customer;

    private TextView tvName, tvNumber, tvDues, tvMonthDues;
    private RecyclerView rvTodayEntries;
    private DailyTransactionAdapter adapter;
    private android.view.View mainContentView;

    // Swipe Navigation
    private android.view.GestureDetector gestureDetector;
    private List<Customer> groupCustomers;
    private int currentCustomerIndex = -1;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_point_of_sale);

        Intent intent = getIntent();
        long customerId = intent.getLongExtra("customerId", -1);

        if (customerId == -1) {
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        tvNumber = findViewById(R.id.tvNumber);
        tvDues = findViewById(R.id.tvDues);
        tvMonthDues = findViewById(R.id.tvMonthDues);
        tvMonthDues = findViewById(R.id.tvMonthDues);
        rvTodayEntries = findViewById(R.id.rvTodayEntries);
        mainContentView = findViewById(R.id.mainContent);

        customerDAO = new CustomerDAO(this);
        dailyTransactionDAO = new DailyTransactionDAO(this);

        // Setup RecyclerView
        rvTodayEntries.setLayoutManager(new LinearLayoutManager(this));
        
        // Dynamic Animation
        int resId = R.anim.layout_animation_fall_down;
        android.view.animation.LayoutAnimationController animation = android.view.animation.AnimationUtils.loadLayoutAnimation(this, resId);
        rvTodayEntries.setLayoutAnimation(animation);
        
        adapter = new DailyTransactionAdapter(this::showDeleteDialog);
        rvTodayEntries.setAdapter(adapter);

        // Initial Load
        loadInitialCustomer(customerId);
        
        // Setup Gesture Detector (No Animation)
        setupSwipeNavigation();
        
        // Setup Month Summary Click
        findViewById(R.id.iconAction).setOnClickListener(v -> showMonthSummary());
        
        // Setup Payment Click
        findViewById(R.id.btnPayment).setOnClickListener(v -> {
            PaymentDialog.newInstance(customer.id).show(getSupportFragmentManager(), "PaymentDialog");
        });

        // Setup Toolbar Menu
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Per-customer auto-entry toggle
        setupAutoEntryToggle();
    }

    /** Reads customer.autoEntryEnabled and wires the switch to persist changes. */
    private void setupAutoEntryToggle() {
        androidx.appcompat.widget.SwitchCompat toggle = findViewById(R.id.switchCustomerAutoEntry);
        if (toggle == null || customer == null) return;

        // Set initial state without triggering listener
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(customer.autoEntryEnabled);

        toggle.setOnCheckedChangeListener((btn, isChecked) -> {
            customer.autoEntryEnabled = isChecked;
            customerDAO.updateCustomerAutoEntry(customer.id, isChecked);
            String msg = isChecked ? "Auto entry ON for " + customer.name : "Auto entry PAUSED for " + customer.name;
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
            updateAutoEntryLabel(isChecked);
        });

        updateAutoEntryLabel(customer.autoEntryEnabled);
    }

    private void updateAutoEntryLabel(boolean enabled) {
        TextView label = findViewById(R.id.tvAutoEntryLabel);
        if (label != null) {
            label.setText(enabled ? "Auto Milk Entry  ✓" : "Auto Milk Entry  ✗ (paused)");
        }
    }



    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_point_of_sale, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_edit_default_qty) {
            showEditDefaultQtyDialog();
            return true;
        } else if (item.getItemId() == R.id.action_bill) {
            Intent intent = new Intent(this, BillActivity.class);
            intent.putExtra("customerId", customer.id);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
    * Executes the {@code showEditDefaultQtyDialog} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void showEditDefaultQtyDialog() {
        // Get current milk rate (needed if user enters ₹)
        com.ignishers.milkmanager2.database.MilkRateDAO rateDAO =
                new com.ignishers.milkmanager2.database.MilkRateDAO(this);
        final double milkRate = rateDAO.getRateForDate(
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                        ? java.time.LocalDate.now().toString() : "2020-01-01");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Default Quantities");

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        int padSm = (int) (8 * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);

        // --- Unit Toggle: Liters / ₹ Rupees ---
        android.widget.RadioGroup rgUnit = new android.widget.RadioGroup(this);
        rgUnit.setOrientation(android.widget.RadioGroup.HORIZONTAL);

        android.widget.RadioButton rbLiters = new android.widget.RadioButton(this);
        rbLiters.setText("Liters (L)");
        rbLiters.setId(android.view.View.generateViewId());

        android.widget.RadioButton rbRupees = new android.widget.RadioButton(this);
        rbRupees.setText("Rupees (₹)");
        rbRupees.setId(android.view.View.generateViewId());

        rgUnit.addView(rbLiters);
        rgUnit.addView(rbRupees);
        rbLiters.setChecked(true); // Default = Liters
        root.addView(rgUnit);

        // Rate info hint
        android.widget.TextView tvRateHint = new android.widget.TextView(this);
        tvRateHint.setTextSize(11);
        tvRateHint.setTextColor(0xFF9E9E9E);
        tvRateHint.setText("Current rate: ₹" + String.format("%.0f", milkRate) + "/L  |  Enter ₹ → auto-converts to L");
        android.widget.LinearLayout.LayoutParams lpHint = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHint.topMargin = padSm / 2;
        tvRateHint.setLayoutParams(lpHint);
        root.addView(tvRateHint);

        // --- Morning field ---
        android.widget.TextView lblMorning = new android.widget.TextView(this);
        lblMorning.setText("☀ Morning");
        lblMorning.setTextSize(13);
        android.widget.LinearLayout.LayoutParams lpLbl = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLbl.topMargin = pad;
        lblMorning.setLayoutParams(lpLbl);
        root.addView(lblMorning);

        final android.widget.EditText inputMorning = new android.widget.EditText(this);
        inputMorning.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputMorning.setHint("e.g. 1.5");
        // Show current value — use session qty if explicitly set (even if 0), else legacy qty
        inputMorning.setText(String.format("%.2f",
                customer.morningQtySet ? customer.defaultQtyMorning : customer.defaultQuantity));
        root.addView(inputMorning);

        // --- Evening field ---
        android.widget.TextView lblEvening = new android.widget.TextView(this);
        lblEvening.setText("🌙 Evening");
        lblEvening.setTextSize(13);
        android.widget.LinearLayout.LayoutParams lpLbl2 = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLbl2.topMargin = padSm;
        lblEvening.setLayoutParams(lpLbl2);
        root.addView(lblEvening);

        final android.widget.EditText inputEvening = new android.widget.EditText(this);
        inputEvening.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputEvening.setHint("e.g. 1.0");
        inputEvening.setText(String.format("%.2f",
                customer.eveningQtySet ? customer.defaultQtyEvening : customer.defaultQuantity));
        root.addView(inputEvening);

        // When unit changes → update labels and clear fields so user re-enters
        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRupees = (checkedId == rbRupees.getId());
            String morningHint = isRupees ? "e.g. 90 (₹)" : "e.g. 1.5 (L)";
            String eveningHint = isRupees ? "e.g. 60 (₹)" : "e.g. 1.0 (L)";
            lblMorning.setText(isRupees ? "☀ Morning (₹ amount)" : "☀ Morning (Liters)");
            lblEvening.setText(isRupees ? "🌙 Evening (₹ amount)" : "🌙 Evening (Liters)");
            inputMorning.setHint(morningHint);
            inputEvening.setHint(eveningHint);
            // Reset so user enters fresh value in chosen unit
            inputMorning.setText("");
            inputEvening.setText("");
        });

        builder.setView(root);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String morningStr = inputMorning.getText().toString().trim();
            String eveningStr = inputEvening.getText().toString().trim();
            boolean isRupees = rbRupees.isChecked();

            try {
                double rawMorning = morningStr.isEmpty() ? -1 : Double.parseDouble(morningStr);
                double rawEvening = eveningStr.isEmpty() ? -1 : Double.parseDouble(eveningStr);

                // Convert ₹ → Liters if needed
                double newMorning, newEvening;
                if (isRupees) {
                    if (milkRate <= 0) {
                        android.widget.Toast.makeText(this,
                                "Milk rate is 0 — cannot convert ₹ to L. Set milk rate first.",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    newMorning = (rawMorning < 0) ? customer.defaultQtyMorning : rawMorning / milkRate;
                    newEvening = (rawEvening < 0) ? customer.defaultQtyEvening : rawEvening / milkRate;
                } else {
                    newMorning = (rawMorning < 0) ? customer.defaultQtyMorning : rawMorning;
                    newEvening = (rawEvening < 0) ? customer.defaultQtyEvening : rawEvening;
                }

                customerDAO.updateCustomerMorningQty(customer.id, newMorning);
                customerDAO.updateCustomerEveningQty(customer.id, newEvening);
                customer.defaultQtyMorning = newMorning;
                customer.defaultQtyEvening = newEvening;

                String morningDisplay = String.format("%.2fL", newMorning);
                String eveningDisplay = String.format("%.2fL", newEvening);
                android.widget.Toast.makeText(this,
                        "Updated! Morning: " + morningDisplay + " | Evening: " + eveningDisplay,
                        android.widget.Toast.LENGTH_LONG).show();
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(this, "Invalid value entered", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    
    /**
    * Mutates the state of {@code upSwipeNavigation}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    */
    private void setupSwipeNavigation() {
        gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(@androidx.annotation.NonNull android.view.MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(@androidx.annotation.Nullable android.view.MotionEvent e1, @androidx.annotation.NonNull android.view.MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (!isNavigating) { // Prevent rapid swipes
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void onSwipeLeft() { // Next Customer
        if (groupCustomers != null && currentCustomerIndex < groupCustomers.size() - 1) {
            loadCustomerByIndex(currentCustomerIndex + 1);
        } else {
            android.widget.Toast.makeText(this, "No more customers", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void onSwipeRight() { // Previous Customer
        if (groupCustomers != null && currentCustomerIndex > 0) {
            loadCustomerByIndex(currentCustomerIndex - 1);
        } else {
            android.widget.Toast.makeText(this, "First customer", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
    * Executes the {@code loadInitialCustomer} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param customerId standard parameter provided by caller layer.
    */
    private void loadInitialCustomer(long customerId) {
        // Fetch specific initial customer to ensure we get it right
        customer = customerDAO.getCustomer(String.valueOf(customerId));
        if (customer == null) {
            finish();
            return;
        }
        
        // Fetch Siblings
        groupCustomers = customerDAO.getCustomersByGroup(customer.routeGroupId);
        
        // Find index
        for (int i = 0; i < groupCustomers.size(); i++) {
            if (groupCustomers.get(i).id == customerId) {
                currentCustomerIndex = i;
                break;
            }
        }
        
        refreshUI();
    }
    
    /**
    * Executes the {@code loadCustomerByIndex} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param index standard parameter provided by caller layer.
    */
    private void loadCustomerByIndex(int index) {
        currentCustomerIndex = index;
        customer = groupCustomers.get(index);
        
        // Fetch full details again to ensure fresh Data (like updated Dues)
        customer = customerDAO.getCustomer(String.valueOf(customer.id));
        
        refreshUI();
    }

    /**
    * Executes the {@code refreshUI} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void refreshUI() {
        tvName.setText("Name: " + customer.name);
        tvNumber.setText("Mobile: " + customer.mobile);
        tvDues.setText(String.format("Dues: %.2f", customer.currentDue));

        updateMonthTotal();
        loadTodayEntries();

        // Update per-customer auto-entry toggle for this customer
        setupAutoEntryToggle();

        // Reload Fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainerView,
                        MilkEntryFragment.newInstance(customer.id)
                )
                .commit();
    }


    /**
    * Modifies the existing {@code MonthTotal} record.
    * <p>
    * Executes an SQL UPDATE operation on the target table. Ensures that any dependent UI
    * views should be invalidated or notified after this returns.
    * </p>
    */
    private void updateMonthTotal() {
        LocalDateTime now = LocalDateTime.now();
        double currentMonthDue = 0;
        String month = String.format("%02d", now.getMonthValue());
        
        List<DailyTransaction> transactions =
                dailyTransactionDAO.getTransactionsByMonth(
                        String.valueOf(customer.id),
                        month,
                        String.valueOf(now.getYear())
                );

        if (transactions != null) {
            for (DailyTransaction t : transactions) {
                if (t.getSession().startsWith("Payment")) continue;
                currentMonthDue += t.getAmount();
            }
        }
        
        double lastMonthDue = customer.currentDue - currentMonthDue;
        
        // Update UI
        tvMonthDues.setText(String.format("Last Month Due: %.2f", lastMonthDue));
        
        TextView tvCurrentMonth = findViewById(R.id.tvCurrentMonthTotal);
        if (tvCurrentMonth != null) { 
            tvCurrentMonth.setText(String.format("Current Month Bill: %.2f", currentMonthDue));
        }
    }

    /**
    * Executes the {@code loadTodayEntries} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void loadTodayEntries() {
        if (customer == null) return;
        String today = LocalDate.now().toString();
        List<DailyTransaction> list = dailyTransactionDAO.getTransactionsByDate(customer.id, today);
        adapter.submitList(list);
        rvTodayEntries.scheduleLayoutAnimation();
    }

    /**
    * Executes the {@code showDeleteDialog} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param transaction standard parameter provided by caller layer.
    */
    private void showDeleteDialog(DailyTransaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (transaction.getSession().startsWith("Payment")) {
                         customerDAO.updateCustomerDue(customer.id, transaction.getAmount());
                    } else {
                        customerDAO.updateCustomerDue(customer.id, -transaction.getAmount());
                    }
                    
                    dailyTransactionDAO.delete(transaction.getTransactionId());
                    loadTodayEntries();
                    customer = customerDAO.getCustomer(String.valueOf(customer.id));
                    updateMonthTotal();
                    if (customer != null) {
                        tvDues.setText(String.format("Dues: %.2f", customer.currentDue));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEntryAdded() {
        runOnUiThread(() -> {
            loadTodayEntries();
            customer = customerDAO.getCustomer(String.valueOf(customer.id));
            updateMonthTotal();
            if (customer != null) {
                tvDues.setText(String.format("Dues: %.2f", customer.currentDue));
            }
        });
    }

    /**
    * Executes the {@code showMonthSummary} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    */
    private void showMonthSummary() {
         if (customer == null) return;
         LocalDateTime now = LocalDateTime.now();
         String month = String.format("%02d", now.getMonthValue());
         String year = String.valueOf(now.getYear());
        
         MonthSummaryDialog.newInstance(customer.id, month, year)
                .show(getSupportFragmentManager(), "MonthSummary");
    }
}