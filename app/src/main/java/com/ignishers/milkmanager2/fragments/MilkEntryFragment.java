package com.ignishers.milkmanager2.fragments;

import com.ignishers.milkmanager2.R;

import com.ignishers.milkmanager2.bottomsheets.ManualEntryBottomSheet;
import com.ignishers.milkmanager2.viewmodels.MilkEntryViewModel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

/**
 * Fragment in PointOfSaleActivity that provides:
 * <ul>
 *   <li>Delete morning auto-entry button (☀ − Morning)</li>
 *   <li>Delete evening auto-entry button (🌙 − Evening)</li>
 *   <li>Spinner for quick extra quantities</li>
 *   <li>Manual entry button</li>
 * </ul>
 */
public class MilkEntryFragment extends Fragment {

    private static final String ARG_CUSTOMER_ID = "customer_id";

    private MilkEntryViewModel viewModel;
    private long customerId;

    private MaterialButton btnDeleteMorning;
    private MaterialButton btnDeleteEvening;

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

        View view = inflater.inflate(R.layout.fragment_milk_entry, container, false);

        if (getArguments() == null) {
            throw new IllegalStateException("MilkEntryFragment requires customerId");
        }

        customerId = getArguments().getLong(ARG_CUSTOMER_ID);

        viewModel = new ViewModelProvider(requireActivity()).get(MilkEntryViewModel.class);

        viewModel.getEntryAdded().observe(getViewLifecycleOwner(), added -> {
            if (Boolean.TRUE.equals(added)) {
                if (listener != null) listener.onEntryAdded();
                updateButtonStates(); // Refresh enabled/disabled state after any change
            }
        });

        // Date label
        TextView tvDate = view.findViewById(R.id.tvDate);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            tvDate.setText(java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy")));
        } else {
            tvDate.setText("Today");
        }

        btnDeleteMorning = view.findViewById(R.id.btnDeleteMorning);
        btnDeleteEvening = view.findViewById(R.id.btnDeleteEvening);

        // --- Delete Morning ---
        btnDeleteMorning.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Aaj ki Morning Entry Hatayein?")
                    .setMessage("Morning ki automatic milk entry delete ho jayegi.")
                    .setPositiveButton("− Delete", (dialog, which) -> {
                        viewModel.deleteTodaySession(customerId, "Morning");
                        android.widget.Toast.makeText(requireContext(),
                                "Morning entry deleted", android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- Delete Evening ---
        btnDeleteEvening.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Aaj ki Evening Entry Hatayein?")
                    .setMessage("Evening ki automatic milk entry delete ho jayegi.")
                    .setPositiveButton("− Delete", (dialog, which) -> {
                        viewModel.deleteTodaySession(customerId, "Evening");
                        android.widget.Toast.makeText(requireContext(),
                                "Evening entry deleted", android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --- Spinner for quick extras ---
        android.widget.Spinner spinner = view.findViewById(R.id.spinnerQuantity);
        String[] quantities = new String[]{"Add Extra...", "250ml", "500ml", "750ml", "1l", "10rs", "20rs", "30rs", "50rs"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, quantities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;
                String selected = quantities[position];
                viewModel.addQuickEntry(customerId, selected);
                android.widget.Toast.makeText(requireContext(), "Added: " + selected, android.widget.Toast.LENGTH_SHORT).show();
                spinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // --- Manual Entry Button ---
        MaterialButton manualBtn = view.findViewById(R.id.btnManualEntry);
        manualBtn.setOnClickListener(v -> {
            ManualEntryBottomSheet bottomSheet = ManualEntryBottomSheet.newInstance(customerId);
            bottomSheet.show(getParentFragmentManager(), "ManualEntry");
        });

        // Initial state update (check if morning/evening entries already exist today)
        updateButtonStates();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonStates();
    }

    /**
     * Runs on background thread to check DB; then updates button enabled/alpha state on UI thread.
     */
    private void updateButtonStates() {
        if (!isAdded() || btnDeleteMorning == null) return;
        new Thread(() -> {
            boolean hasMorning = viewModel.hasTodayEntrySync(customerId, "Morning");
            boolean hasEvening = viewModel.hasTodayEntrySync(customerId, "Evening");
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                // Morning button: enabled (red) if entry exists, grey if not
                btnDeleteMorning.setEnabled(hasMorning);
                btnDeleteMorning.setAlpha(hasMorning ? 1.0f : 0.45f);
                btnDeleteMorning.setText(hasMorning ? "☀ − Morning (Delete)" : "☀ Morning (No Entry)");

                // Evening button
                btnDeleteEvening.setEnabled(hasEvening);
                btnDeleteEvening.setAlpha(hasEvening ? 1.0f : 0.45f);
                btnDeleteEvening.setText(hasEvening ? "🌙 − Evening (Delete)" : "🌙 Evening (No Entry)");
            });
        }).start();
    }
}