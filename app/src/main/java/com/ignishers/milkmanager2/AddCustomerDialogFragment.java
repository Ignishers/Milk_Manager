package com.ignishers.milkmanager2;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.ignishers.milkmanager2.databinding.FragmentAddCustomerDialogListDialogBinding;
import com.ignishers.milkmanager2.utils.SimpleTextWatcher;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * <p>You can show this modal bottom sheet from your activity like this:</p>
 * <pre>
 *     AddCustomerDialogFragment.newInstance(30).show(getSupportFragmentManager(), "dialog");
 * </pre>
 */
public class AddCustomerDialogFragment extends BottomSheetDialogFragment {

    private EditText etName, etMobile, etDefaultQty, etCurrentDue;
    private Button btnCreate;
    private com.ignishers.milkmanager2.DAO.CustomerDAO customerDao;
    private long routeGroupId;

    public interface OnCustomerCreatedListener {
        void onCustomerCreated();
    }
    
    // We'll cast the parent context/activity/fragment to this
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

        View view = inflater.inflate(R.layout.fragment_add_customer_dialog_list_dialog, container, false);
        
        customerDao = new com.ignishers.milkmanager2.DAO.CustomerDAO(requireContext());
        
        if (getArguments() != null) {
            routeGroupId = getArguments().getLong("GROUP_ID", 0);
        }

        etName = view.findViewById(R.id.etName);
        etMobile = view.findViewById(R.id.etMobile);
        etDefaultQty = view.findViewById(R.id.etDefaultQty);
        etCurrentDue = view.findViewById(R.id.etCurrentDue);
        btnCreate = view.findViewById(R.id.btnCreate);

        TextWatcher watcher = new SimpleTextWatcher(this::validateForm);

        etName.addTextChangedListener(watcher);
        etMobile.addTextChangedListener(watcher);
        etDefaultQty.addTextChangedListener(watcher);

        btnCreate.setOnClickListener(v -> createCustomer());

        return view;
    }

    private void validateForm() {
        boolean isNameValid = !etName.getText().toString().trim().isEmpty();
        boolean isMobileValid = etMobile.getText().toString().trim().length() == 10;
        boolean isQtyValid = !etDefaultQty.getText().toString().trim().isEmpty();

        if (!isNameValid && !etName.getText().toString().trim().isEmpty()) { // Only show error if not empty but invalid (not applicable here as just empty check)
             // or show error immediately if touched? Simple approach:
        }
        
        // Better UX: Show errors on the TextInputLayouts
        com.google.android.material.textfield.TextInputLayout tilName = getView().findViewById(R.id.tilName);
        com.google.android.material.textfield.TextInputLayout tilMobile = getView().findViewById(R.id.tilMobile);
        com.google.android.material.textfield.TextInputLayout tilQty = getView().findViewById(R.id.tilDefaultQty);
        
        if (etName.hasFocus()) {
             tilName.setError(isNameValid ? null : "Name required");
        }
        if (etMobile.hasFocus()) {
            tilMobile.setError(isMobileValid ? null : "Must be 10 digits");
        }
        if (etDefaultQty.hasFocus()) {
            tilQty.setError(isQtyValid ? null : "Quantity required");
        }

        btnCreate.setEnabled(isNameValid && isMobileValid && isQtyValid);
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
        // Safe parsing
        double defaultQty = Double.parseDouble(etDefaultQty.getText().toString().trim());
        double due = etCurrentDue.getText().toString().trim().isEmpty()
                ? 0
                : Double.parseDouble(etCurrentDue.getText().toString().trim());

        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        // Persist customer (DAO call)
        long id = customerDao.insertCustomer(name, mobile, routeGroupId == 0 ? null : routeGroupId, defaultQty, due);
        
        if (id != -1) {
            android.widget.Toast.makeText(requireContext(), "Customer Created", android.widget.Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onCustomerCreated();
            }
            dismiss();
        } else {
             android.widget.Toast.makeText(requireContext(), "Failed to create customer", android.widget.Toast.LENGTH_SHORT).show();
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