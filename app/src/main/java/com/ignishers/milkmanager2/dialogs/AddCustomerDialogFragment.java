package com.ignishers.milkmanager2.dialogs;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.database.MilkRateDAO;

import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.ignishers.milkmanager2.utils.SimpleTextWatcher;

/**
 * BottomSheet dialog for creating a new customer.
 * <p>
 * Supports entering morning and evening default quantities in either
 * Liters (L) or Rupees (₹). When ₹ is selected the value is divided
 * by the current milk rate to produce a litre quantity for storage.
 * </p>
 */
public class AddCustomerDialogFragment extends BottomSheetDialogFragment {

    private EditText etName, etMobile, etMorningQty, etEveningQty, etCurrentDue;
    private RadioGroup rgUnit;
    private RadioButton rbLiters, rbRupees;
    private TextView tvRateHint;
    private TextInputLayout tilMorningQty, tilEveningQty;
    private Button btnCreate;

    private CustomerDAO customerDao;
    private MilkRateDAO rateDAO;
    private double milkRate = 0;
    private long routeGroupId;

    public interface OnCustomerCreatedListener {
        void onCustomerCreated();
    }

    private OnCustomerCreatedListener listener;

    public static AddCustomerDialogFragment newInstance(long routeGroupId) {
        AddCustomerDialogFragment fragment = new AddCustomerDialogFragment();
        Bundle args = new Bundle();
        args.putLong("GROUP_ID", routeGroupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_add_customer_dialog_list_dialog, container, false);

        customerDao = new CustomerDAO(requireContext());
        rateDAO = new MilkRateDAO(requireContext());

        // Get current milk rate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            milkRate = rateDAO.getRateForDate(java.time.LocalDate.now().toString());
        }

        if (getArguments() != null) {
            routeGroupId = getArguments().getLong("GROUP_ID", 0);
        }

        // Bind views
        etName        = view.findViewById(R.id.etName);
        etMobile      = view.findViewById(R.id.etMobile);
        etMorningQty  = view.findViewById(R.id.etMorningQty);
        etEveningQty  = view.findViewById(R.id.etEveningQty);
        etCurrentDue  = view.findViewById(R.id.etCurrentDue);
        rgUnit        = view.findViewById(R.id.rgUnit);
        rbLiters      = view.findViewById(R.id.rbLiters);
        rbRupees      = view.findViewById(R.id.rbRupees);
        tvRateHint    = view.findViewById(R.id.tvRateHint);
        tilMorningQty = view.findViewById(R.id.tilMorningQty);
        tilEveningQty = view.findViewById(R.id.tilEveningQty);
        btnCreate     = view.findViewById(R.id.btnCreate);

        // Show current rate
        tvRateHint.setText(String.format("Rate: ₹%.0f/L  |  Enter ₹ → auto-converts to L", milkRate));

        // Unit toggle listener
        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRupees = (checkedId == R.id.rbRupees);
            tilMorningQty.setHint(isRupees ? "☀ Morning Quantity (₹)" : "☀ Morning Quantity (L)");
            tilEveningQty.setHint(isRupees ? "🌙 Evening Quantity (₹)" : "🌙 Evening Quantity (L)");
            // Clear when switching unit so user types fresh values
            etMorningQty.setText("");
            etEveningQty.setText("");
            validateForm();
        });

        // Validation watchers
        TextWatcher watcher = new SimpleTextWatcher(this::validateForm);
        etName.addTextChangedListener(watcher);
        etMobile.addTextChangedListener(watcher);
        etMorningQty.addTextChangedListener(watcher);
        etEveningQty.addTextChangedListener(watcher);

        btnCreate.setOnClickListener(v -> createCustomer());

        return view;
    }

    private void validateForm() {
        if (getView() == null) return;

        boolean nameOk    = !etName.getText().toString().trim().isEmpty();
        boolean mobileOk  = etMobile.getText().toString().trim().length() == 10;
        boolean morningOk = !etMorningQty.getText().toString().trim().isEmpty();
        boolean eveningOk = !etEveningQty.getText().toString().trim().isEmpty();

        TextInputLayout tilName   = getView().findViewById(R.id.tilName);
        TextInputLayout tilMobile = getView().findViewById(R.id.tilMobile);

        if (etName.hasFocus())   tilName.setError(nameOk ? null : "Name required");
        if (etMobile.hasFocus()) tilMobile.setError(mobileOk ? null : "Must be 10 digits");
        if (etMorningQty.hasFocus()) tilMorningQty.setError(morningOk ? null : "Morning qty required");
        if (etEveningQty.hasFocus()) tilEveningQty.setError(eveningOk ? null : "Evening qty required");

        btnCreate.setEnabled(nameOk && mobileOk && morningOk && eveningOk);
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnCustomerCreatedListener) {
            listener = (OnCustomerCreatedListener) context;
        } else if (getParentFragment() instanceof OnCustomerCreatedListener) {
            listener = (OnCustomerCreatedListener) getParentFragment();
        }
    }

    private void createCustomer() {
        boolean isRupees = rbRupees.isChecked();

        try {
            double rawMorning = Double.parseDouble(etMorningQty.getText().toString().trim());
            double rawEvening = Double.parseDouble(etEveningQty.getText().toString().trim());

            double morningQtyL, eveningQtyL;
            if (isRupees) {
                if (milkRate <= 0) {
                    android.widget.Toast.makeText(requireContext(),
                            "Milk rate is ₹0 — set milk rate first, then create customer.",
                            android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                morningQtyL = rawMorning / milkRate;
                eveningQtyL = rawEvening / milkRate;
            } else {
                morningQtyL = rawMorning;
                eveningQtyL = rawEvening;
            }

            double due = etCurrentDue.getText().toString().trim().isEmpty()
                    ? 0
                    : Double.parseDouble(etCurrentDue.getText().toString().trim());

            String name   = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();

            long id = customerDao.insertCustomer(
                    name, mobile,
                    routeGroupId == 0 ? null : routeGroupId,
                    morningQtyL, eveningQtyL, due);

            if (id != -1) {
                String morFmt = String.format("%.2fL", morningQtyL);
                String eveFmt = String.format("%.2fL", eveningQtyL);
                android.widget.Toast.makeText(requireContext(),
                        "Customer created! Morning: " + morFmt + " | Evening: " + eveFmt,
                        android.widget.Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onCustomerCreated();
                dismiss();
            } else {
                android.widget.Toast.makeText(requireContext(),
                        "Failed to create customer", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            android.widget.Toast.makeText(requireContext(),
                    "Invalid quantity entered", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }
}