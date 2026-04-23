package com.ignishers.milkmanager2.dialogs;

import com.ignishers.milkmanager2.R;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ignishers.milkmanager2.utils.SimpleTextWatcher;
import com.ignishers.milkmanager2.models.Customer;
import com.ignishers.milkmanager2.database.CustomerDAO;
import com.ignishers.milkmanager2.database.MilkRateDAO;

/**
 * BottomSheet dialog for editing an existing customer's details.
 * <p>
 * Reuses {@code fragment_add_customer_dialog_list_dialog} layout.
 * Allows editing name, mobile, morning qty, evening qty, and current due.
 * Quantities can be entered in Liters or Rupees (auto-converted using current milk rate).
 * </p>
 */
public class EditCustomerDialogFragment extends BottomSheetDialogFragment {

    private EditText etName, etMobile, etMorningQty, etEveningQty, etCurrentDue;
    private RadioGroup rgUnit;
    private RadioButton rbLiters, rbRupees;
    private TextInputLayout tilMorningQty, tilEveningQty;
    private Button btnSave;
    private CustomerDAO customerDao;
    private MilkRateDAO rateDAO;
    private double milkRate = 0;
    private long customerId;

    public interface OnCustomerUpdatedListener {
        void onCustomerUpdated();
    }

    private OnCustomerUpdatedListener listener;

    public static EditCustomerDialogFragment newInstance(long customerId) {
        EditCustomerDialogFragment fragment = new EditCustomerDialogFragment();
        Bundle args = new Bundle();
        args.putLong("CUSTOMER_ID", customerId);
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            milkRate = rateDAO.getRateForDate(java.time.LocalDate.now().toString());
        }

        if (getArguments() != null) {
            customerId = getArguments().getLong("CUSTOMER_ID", 0);
        }

        // Bind views
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        etName        = view.findViewById(R.id.etName);
        etMobile      = view.findViewById(R.id.etMobile);
        etMorningQty  = view.findViewById(R.id.etMorningQty);
        etEveningQty  = view.findViewById(R.id.etEveningQty);
        etCurrentDue  = view.findViewById(R.id.etCurrentDue);
        rgUnit        = view.findViewById(R.id.rgUnit);
        rbLiters      = view.findViewById(R.id.rbLiters);
        rbRupees      = view.findViewById(R.id.rbRupees);
        tilMorningQty = view.findViewById(R.id.tilMorningQty);
        tilEveningQty = view.findViewById(R.id.tilEveningQty);
        btnSave       = view.findViewById(R.id.btnCreate);

        if (tvTitle != null) tvTitle.setText("Edit Customer");
        btnSave.setText("Save Changes");

        // Rate hint
        TextView tvRateHint = view.findViewById(R.id.tvRateHint);
        if (tvRateHint != null) {
            tvRateHint.setText(String.format("Rate: ₹%.0f/L  |  Enter ₹ → auto-converts to L", milkRate));
        }

        // Unit toggle: clear fields and update hints on switch
        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRupees = (checkedId == R.id.rbRupees);
            tilMorningQty.setHint(isRupees ? "☀ Morning Quantity (₹)" : "☀ Morning Quantity (L)");
            tilEveningQty.setHint(isRupees ? "🌙 Evening Quantity (₹)" : "🌙 Evening Quantity (L)");
            etMorningQty.setText("");
            etEveningQty.setText("");
            validateForm();
        });

        // Pre-fill existing data in Liters (default unit = Liters)
        loadCustomerData();

        TextWatcher watcher = new SimpleTextWatcher(this::validateForm);
        etName.addTextChangedListener(watcher);
        etMobile.addTextChangedListener(watcher);
        etMorningQty.addTextChangedListener(watcher);
        etEveningQty.addTextChangedListener(watcher);

        btnSave.setOnClickListener(v -> saveCustomer());

        return view;
    }

    private void loadCustomerData() {
        if (customerId > 0) {
            Customer c = customerDao.getCustomer(String.valueOf(customerId));
            if (c != null) {
                etName.setText(c.name);
                etMobile.setText(c.mobile);
                // Pre-fill in Liters — use session qty if ever explicitly set, else fall back to legacy
                etMorningQty.setText(String.format("%.2f",
                        c.morningQtySet ? c.defaultQtyMorning : c.defaultQuantity));
                etEveningQty.setText(String.format("%.2f",
                        c.eveningQtySet ? c.defaultQtyEvening : c.defaultQuantity));

                String dueStr = String.valueOf(c.currentDue);
                if (dueStr.endsWith(".0")) dueStr = dueStr.replace(".0", "");
                etCurrentDue.setText(dueStr);
            }
        }
    }

    private void validateForm() {
        if (getView() == null) return;
        boolean nameOk    = !etName.getText().toString().trim().isEmpty();
        boolean mobileOk  = etMobile.getText().toString().trim().length() == 10;
        boolean morningOk = !etMorningQty.getText().toString().trim().isEmpty();
        boolean eveningOk = !etEveningQty.getText().toString().trim().isEmpty();

        TextInputLayout tilName   = getView().findViewById(R.id.tilName);
        TextInputLayout tilMobile = getView().findViewById(R.id.tilMobile);
        if (etName.hasFocus())       tilName.setError(nameOk ? null : "Name required");
        if (etMobile.hasFocus())     tilMobile.setError(mobileOk ? null : "Must be 10 digits");
        if (etMorningQty.hasFocus()) tilMorningQty.setError(morningOk ? null : "Required");
        if (etEveningQty.hasFocus()) tilEveningQty.setError(eveningOk ? null : "Required");

        btnSave.setEnabled(nameOk && mobileOk && morningOk && eveningOk);
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnCustomerUpdatedListener) {
            listener = (OnCustomerUpdatedListener) context;
        } else if (getParentFragment() instanceof OnCustomerUpdatedListener) {
            listener = (OnCustomerUpdatedListener) getParentFragment();
        }
    }

    private void saveCustomer() {
        boolean isRupees = rbRupees.isChecked();
        try {
            double rawMorning = Double.parseDouble(etMorningQty.getText().toString().trim());
            double rawEvening = Double.parseDouble(etEveningQty.getText().toString().trim());

            double morningL, eveningL;
            if (isRupees) {
                if (milkRate <= 0) {
                    Toast.makeText(requireContext(),
                            "Milk rate is ₹0 — set milk rate first.", Toast.LENGTH_LONG).show();
                    return;
                }
                morningL = rawMorning / milkRate;
                eveningL = rawEvening / milkRate;
            } else {
                morningL = rawMorning;
                eveningL = rawEvening;
            }

            double due = etCurrentDue.getText().toString().trim().isEmpty()
                    ? 0
                    : Double.parseDouble(etCurrentDue.getText().toString().trim());

            String name   = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();

            // Update name, mobile, legacy defaultQty (avg), and current due
            double avgQty = (morningL + eveningL) / 2.0;
            customerDao.updateCustomerDetails(customerId, name, mobile, avgQty, due);
            // Update morning + evening separately
            customerDao.updateCustomerMorningQty(customerId, morningL);
            customerDao.updateCustomerEveningQty(customerId, eveningL);

            Toast.makeText(requireContext(),
                    String.format("Updated! Morning: %.2fL | Evening: %.2fL", morningL, eveningL),
                    Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onCustomerUpdated();
            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid value entered", Toast.LENGTH_SHORT).show();
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