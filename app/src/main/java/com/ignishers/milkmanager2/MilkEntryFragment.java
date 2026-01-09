package com.ignishers.milkmanager2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ignishers.milkmanager2.model.MilkEntryViewModel;

public class MilkEntryFragment extends Fragment {

    private static final String ARG_CUSTOMER_ID = "customer_id";

    private MilkEntryViewModel viewModel;
    private long customerId;

    public interface OnEntryListener {
        void onEntryAdded();
    }

    private OnEntryListener listener;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnEntryListener) {
            listener = (OnEntryListener) context;
        }
    }

    public static MilkEntryFragment newInstance(long customerId) {
        MilkEntryFragment fragment = new MilkEntryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_milk_entry, container, false);

        if (getArguments() == null) {
            throw new IllegalStateException("MilkEntryFragment requires customerId");
        }

        customerId = getArguments().getLong(ARG_CUSTOMER_ID);

        viewModel = new ViewModelProvider(requireActivity())
                .get(MilkEntryViewModel.class);

        viewModel.getEntryAdded().observe(getViewLifecycleOwner(), added -> {
            if (Boolean.TRUE.equals(added)) {
                if (listener != null) listener.onEntryAdded();
            }
        });

        // UI Initialization
        TextView tvDate = view.findViewById(R.id.tvDate);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            tvDate.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        } else {
             tvDate.setText("Today");
        }

        android.widget.Spinner spinner = view.findViewById(R.id.spinnerQuantity);
        com.google.android.material.button.MaterialButton addBtn = view.findViewById(R.id.btnAdd);
        com.google.android.material.button.MaterialButton manualBtn = view.findViewById(R.id.btnManualEntry);

        // Setup Spinner
        String[] quantities = new String[]{"Add Extra...", "100ml", "250ml", "500ml", "1l", "2l", "10rs", "20rs", "50rs"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                quantities
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                 if (position == 0) return; // Ignore placeholder

                 String selected = quantities[position];
                 viewModel.addQuickEntry(customerId, selected);
                 android.widget.Toast.makeText(requireContext(), "Added: " + selected, android.widget.Toast.LENGTH_SHORT).show();
                 
                 // Reset spinner to 0
                 spinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        addBtn.setOnClickListener(v -> {
            viewModel.addDefaultEntry(customerId);
            android.widget.Toast.makeText(requireContext(), "Added Default Entry", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        manualBtn.setOnClickListener(v -> {
             ManualEntryBottomSheet bottomSheet = ManualEntryBottomSheet.newInstance(customerId);
             bottomSheet.show(getParentFragmentManager(), "ManualEntry");
        });

        return view;
    }
}
