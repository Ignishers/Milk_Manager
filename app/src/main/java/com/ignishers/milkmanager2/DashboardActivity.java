package com.ignishers.milkmanager2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Stack;

import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ignishers.milkmanager2.DAO.CustomerDAO;
import com.ignishers.milkmanager2.DAO.DBHelper;
import com.ignishers.milkmanager2.DAO.RouteGroupDAO;
import com.ignishers.milkmanager2.adapter.NavigationAdapter;
import com.ignishers.milkmanager2.model.Customer;
import com.ignishers.milkmanager2.model.NavigationManager;
import com.ignishers.milkmanager2.model.RouteGroup;

import java.util.List;



/*
*   This is the dashboard activity, formerly MainActivity
 */
public class DashboardActivity extends AppCompatActivity implements AddCustomerDialogFragment.OnCustomerCreatedListener {


    DBHelper dbHelper;
    RouteGroupDAO groupDAO;
    CustomerDAO customerDAO;
    NavigationManager navManager;
    RecyclerView recyclerView;
    NavigationAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DBHelper(this);
        groupDAO = new RouteGroupDAO(this);
        groupDAO.ensureRootRouteExists();
        customerDAO = new CustomerDAO(this);
        navManager = new NavigationManager();


        recyclerView = findViewById(R.id.recyclerViewMain);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Dynamic Animation
        int resId = R.anim.layout_animation_fall_down;
        android.view.animation.LayoutAnimationController animation = android.view.animation.AnimationUtils.loadLayoutAnimation(this, resId);
        recyclerView.setLayoutAnimation(animation);
        adapter = new NavigationAdapter(new NavigationAdapter.OnItemClickListener() {
            @Override
            public void onFolderClick(long groupId) {
                onFolderClicked(groupId);
            }

            @Override
            public void onCustomerClick(long customerId) {
                onCustomerClicked(customerId);
            }
        });
        recyclerView.setAdapter(adapter);












///-----------------------------------------------------------------------------
///-----------------------------------------------------------------------------
        /// ---------------------------------------------------------
        /// NEW: Floating Action Button Logic for creating customers
        /// ---------------------------------------------------------
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Long currentGroupId = navManager.getCurrentGroup();
            long groupId = currentGroupId == null ? 0 : currentGroupId;
            
            AddCustomerDialogFragment.newInstance(groupId)
                    .show(getSupportFragmentManager(), "CreateCustomer");
        });

///-----------------------------------------------------------------------------
///-----------------------------------------------------------------------------
/// ---------------------------------------------------------
/// NEW: Floating Action Button Logic for creating Group
/// ---------------------------------------------------------
        FloatingActionButton fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Create New Route Group");

            final EditText input = new EditText(this);
            input.setHint("Enter Group Name (e.g. Street 5)");
            builder.setView(input);

            builder.setPositiveButton("Create", (dialog, which) -> {
                String name = input.getText().toString().trim();

                if (!name.isEmpty()) {
                    // 1. Get current location (Parent ID)
                    // If we are at root, parent is 0 (from our new logic)
                    Long currentParent = navManager.getCurrentGroup();
                    if(currentParent == null) currentParent = 0L;

                    /// Change method to take object argument of RouteGroup
                    // 2. Insert into DB
                    boolean success = groupDAO.insertGroup(name, currentParent);

                    if (success) {
                        Toast.makeText(DashboardActivity.this, "Route Created!", Toast.LENGTH_SHORT).show();
                        // 3. Refresh the list immediately
                        loadCurrentLevel();
                    } else {
                        Toast.makeText(DashboardActivity.this, "Error creating route", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
///        Floating Action Button Logic Ended


///---------------------------------------------------------------------------------
///----------------------------------------------------------------------------------
///        Setting up the toolbar, the dropdown menu and the navigation drawer
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNavigationDrawer(v);
            }
        });

        // Navigate to root on toolbar click
        toolbar.setOnClickListener(v -> {
            if (!navManager.isAtRoot()) {
                navManager.clear();
                loadCurrentLevel();
                Toast.makeText(DashboardActivity.this, "Returned to Dashboard", Toast.LENGTH_SHORT).show();
            }
        });
///        Ending of toolbar setup

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        if (!navManager.isAtRoot()) {
                            navManager.goBack();
                            loadCurrentLevel();
                        } else {
                            // Disable callback to avoid infinite loop
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                }
        );


        loadCurrentLevel();
/// --------------------------------------------------------------------------------
///---------------------Ending of OnCreate--------------------------------------------------
///-----------------------------------------------------------------------------------------
    }




    private void onFolderClicked(long groupId) {
        navManager.enterGroup(groupId);
        loadCurrentLevel();
    }

    private void onCustomerClicked(long customerId) {
        Intent intent = new Intent(this, PointOfSaleActivity.class);
        intent.putExtra("customerId", customerId);
        startActivity(intent);
    }




    private void loadCurrentLevel() {
        Long currentGroupId = navManager.getCurrentGroup();

        List<RouteGroup> groups;
        List<Customer> customers;

        if (currentGroupId == null) {
            groups = groupDAO.getRootGroups();
            customers = customerDAO.getCustomersByGroup(null); // Fetch root (null) customers
        } else {
            groups = groupDAO.getChildGroups(currentGroupId);
            customers = customerDAO.getCustomersByGroup(currentGroupId);
        }


                adapter.submit(groups, customers);
                // DEBUG: Remove after verification
                // Toast.makeText(this, "Folders: " + groups.size() + " Cust: " + customers.size(), Toast.LENGTH_SHORT).show();

        updateBreadcrumbs();
    }

    private void updateBreadcrumbs() {
        StringBuilder pathBuilder = new StringBuilder("root");
        String title = "Dashboard";

        Stack<Long> stack = navManager.getPathStack();
        for (Long groupId : stack) {
            RouteGroup group = groupDAO.getGroupById(groupId);
            if (group != null) {
                pathBuilder.append(" > ").append(group.name);
                title = group.name;
            }
        }

        TextView pathView = findViewById(R.id.textPath);
        if (pathView != null) {
            pathView.setText(pathBuilder.toString());
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
            getSupportActionBar().setSubtitle(date);
        }











    }

    ///  Toolbar Important Functions
    private void showNavigationDrawer(View anchorView) {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        
        // 1. Play Dots -> Lines
        toolbar.setNavigationIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.avd_dots_to_lines));
        android.graphics.drawable.Drawable icon = toolbar.getNavigationIcon();
        if (icon instanceof android.graphics.drawable.Animatable) {
            ((android.graphics.drawable.Animatable) icon).start();
        }
        
        // 2. Snap to Lines (Hamburger) after animation ensures it stays open
        // (Wait for ~400ms cover animation)
        toolbar.postDelayed(() -> {
             toolbar.setNavigationIcon(R.drawable.vec_menu_lines);
        }, 450);

        MainMenuBottomSheet bottomSheet = new MainMenuBottomSheet();
        bottomSheet.setDismissAction(() -> {
            // 3. Play Lines -> Dots
            toolbar.setNavigationIcon(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.avd_lines_to_dots));
            android.graphics.drawable.Drawable closeIcon = toolbar.getNavigationIcon();
            if (closeIcon instanceof android.graphics.drawable.Animatable) {
                ((android.graphics.drawable.Animatable) closeIcon).start();
            }
            // 4. Snap back to Dots
            toolbar.postDelayed(() -> {
                 toolbar.setNavigationIcon(R.drawable.vec_menu_dots);
            }, 450);
        });
        
        bottomSheet.setListener(new MainMenuBottomSheet.OnMenuItemClickListener() {
            @Override
            public void onSalesRecordClick() {
                // Handle Sales Record Click
                Intent intent = new Intent(DashboardActivity.this, SalesReportActivity.class);
                startActivity(intent);
            }

            @Override
            public void onMilkPriceClick() {
                new SetMilkPriceDialog().show(getSupportFragmentManager(), "SetPrice");
            }

            @Override
            public void onSettingsClick() {
                // Handle Settings Click
                Toast.makeText(DashboardActivity.this, "Settings Comming Soon", Toast.LENGTH_SHORT).show();
            }
        });
        
        bottomSheet.show(getSupportFragmentManager(), "MainMenu");
    }
///    Toolbar Function Ended


    /// ---------------------------------------------------------
    /// NEW: Lifecycle method to fetch data when app is visible
    /// ---------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        refresh();/// Ensures updated dues appear immediately
        checkClipboard();
    }
    
    // Check if we need to show Paste Button
    private void checkClipboard() {
        if (com.ignishers.milkmanager2.utils.CustomerClipboard.hasClip()) {
             FloatingActionButton fabPaste = findViewById(R.id.fabPaste);
             fabPaste.setVisibility(View.VISIBLE);
             
             String custName = com.ignishers.milkmanager2.utils.CustomerClipboard.getCustomerName();
             fabPaste.setOnClickListener(v -> showPasteDialog(custName));
        } else {
             FloatingActionButton fabPaste = findViewById(R.id.fabPaste);
             if (fabPaste != null) fabPaste.setVisibility(View.GONE);
        }
    }
    
    private void showPasteDialog(String custName) {
         new AlertDialog.Builder(this)
             .setTitle("Move Customer Here?")
             .setMessage("Move '" + custName + "' to current folder?")
             .setPositiveButton("Paste", (dialog, which) -> {
                 Long currentGroupId = navManager.getCurrentGroup();
                 // If null, we are at root (0 or null in DB, depending on impl)
                 // CustomerDAO treats 0 or NULL strictly. Let's send 0 if root.
                 long targetRouteId = (currentGroupId == null) ? 0 : currentGroupId;
                 
                 customerDAO.updateCustomerRoute(com.ignishers.milkmanager2.utils.CustomerClipboard.getCustomerId(), targetRouteId);
                 
                 com.ignishers.milkmanager2.utils.CustomerClipboard.clear();
                 checkClipboard(); // Hide FAB
                 loadCurrentLevel(); // Refresh List
                 
                 Toast.makeText(this, "Customer Moved Successfully", Toast.LENGTH_SHORT).show();
             })
             .setNegativeButton("Cancel", null)
             .show();
    }

    private void refresh() {
        loadCurrentLevel();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCustomerCreated() {
        loadCurrentLevel();
    }
}
