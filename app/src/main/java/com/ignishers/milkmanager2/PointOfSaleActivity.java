package com.ignishers.milkmanager2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ignishers.milkmanager2.DAO.CustomerDAO;
import com.ignishers.milkmanager2.DAO.DailyTransactionDAO;
import com.ignishers.milkmanager2.adapter.DailyTransactionAdapter;
import com.ignishers.milkmanager2.model.Customer;
import com.ignishers.milkmanager2.model.DailyTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PointOfSaleActivity extends AppCompatActivity implements MilkEntryFragment.OnEntryListener {

    private DailyTransactionDAO dailyTransactionDAO;
    private CustomerDAO customerDAO;
    private Customer customer;

    private TextView tvName, tvNumber, tvDues, tvMonthDues;
    private RecyclerView rvTodayEntries;
    private DailyTransactionAdapter adapter;

    // Swipe Navigation
    private androidx.core.view.GestureDetectorCompat gestureDetector;
    private List<Customer> groupCustomers;
    private int currentCustomerIndex = -1;

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
        rvTodayEntries = findViewById(R.id.rvTodayEntries);

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
        
        // Setup Gesture Detector
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
        } else if (item.getItemId() == R.id.action_move_customer) {
            initiateMoveCustomer();
            return true;
        } else if (item.getItemId() == R.id.action_bill) {
            android.widget.Toast.makeText(this, "Bill feature coming soon!", android.widget.Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEditDefaultQtyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Default Quantity");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(customer.defaultQuantity));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String qtyStr = input.getText().toString();
            try {
                double newQty = Double.parseDouble(qtyStr);
                customerDAO.updateCustomerDefaultQty(customer.id, newQty);
                customer.defaultQuantity = newQty; // Update local obj
                android.widget.Toast.makeText(this, "Default Quantity Updated", android.widget.Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(this, "Invalid Quantity", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void initiateMoveCustomer() {
        com.ignishers.milkmanager2.utils.CustomerClipboard.copy(customer.id, customer.name);
        android.widget.Toast.makeText(this, "Customer Cut! Navigate to new folder and Click Paste.", android.widget.Toast.LENGTH_LONG).show();
        finish(); // Return to dashboard to navigate
    }
    
    private void setupSwipeNavigation() {
        gestureDetector = new androidx.core.view.GestureDetectorCompat(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(@androidx.annotation.NonNull android.view.MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(@androidx.annotation.Nullable android.view.MotionEvent e1, @androidx.annotation.NonNull android.view.MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) return false;
                
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
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
    
    private void loadCustomerByIndex(int index) {
        currentCustomerIndex = index;
        customer = groupCustomers.get(index);
        
        // Fetch full details again to ensure fresh Data (like updated Dues)
        customer = customerDAO.getCustomer(String.valueOf(customer.id));
        
        refreshUI();
    }

    private void refreshUI() {
        tvName.setText("Name: " + customer.name);
        tvNumber.setText("Mobile: " + customer.mobile);
        tvDues.setText(String.format("Dues: %.2f", customer.currentDue));

        updateMonthTotal();
        loadTodayEntries();

        // Reload Fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainerView,
                        MilkEntryFragment.newInstance(customer.id)
                )
                .commit();
    }

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
        
        // Logic:
        // User wants "Last Month Due" to be the remainder of the total debt after subtracting the current month's bill.
        // If a payment is made, it reduces the Total Due, and thus naturally reduces this "Last Month Due" figure.
        // Formula: Last Month Due = Total Due - Current Month Bill
        
        double lastMonthDue = customer.currentDue - currentMonthDue;
        
        // Update UI
        // 1. Month Dues TextView now shows "Last Month Due"
        tvMonthDues.setText(String.format("Last Month Due: %.2f", lastMonthDue));
        
        // 2. New TextView for Current Month Bill
        TextView tvCurrentMonth = findViewById(R.id.tvCurrentMonthTotal);
        if (tvCurrentMonth != null) { 
            tvCurrentMonth.setText(String.format("Current Month Bill: %.2f", currentMonthDue));
        }
    }

    private void loadTodayEntries() {
        if (customer == null) return;
        String today = LocalDate.now().toString();
        List<DailyTransaction> list = dailyTransactionDAO.getTransactionsByDate(customer.id, today);
        adapter.submitList(list);
    }

    private void showDeleteDialog(DailyTransaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (transaction.getSession().startsWith("Payment")) {
                        // If deleting a payment, we must ASC (Add) the amount back to dues
                         customerDAO.updateCustomerDue(customer.id, transaction.getAmount());
                    } else {
                        // Reduce the amount from customer dues (normal entry)
                        customerDAO.updateCustomerDue(customer.id, -transaction.getAmount());
                    }
                    
                    dailyTransactionDAO.delete(transaction.getTransactionId());
                    loadTodayEntries();
                    updateMonthTotal();
                    // Refresh Dues UI
                    customer = customerDAO.getCustomer(String.valueOf(customer.id));
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
            updateMonthTotal();
            // Refresh customer data to get updated dues
            customer = customerDAO.getCustomer(String.valueOf(customer.id));
            if (customer != null) {
                tvDues.setText(String.format("Dues: %.2f", customer.currentDue));
            }
        });
    }


    private void showMonthSummary() {
         if (customer == null) return;
         LocalDateTime now = LocalDateTime.now();
         String month = String.format("%02d", now.getMonthValue());
         String year = String.valueOf(now.getYear());
        
         MonthSummaryDialog.newInstance(customer.id, month, year)
                .show(getSupportFragmentManager(), "MonthSummary");
    }
}
