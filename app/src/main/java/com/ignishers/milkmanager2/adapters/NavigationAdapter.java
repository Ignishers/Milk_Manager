package com.ignishers.milkmanager2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.models.Customer;
import com.ignishers.milkmanager2.models.NavItem;
import com.ignishers.milkmanager2.models.RouteGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays route groups (folders) and customers in the dashboard RecyclerView.
 * Supports long-press for options and up/down arrow buttons for reordering.
 */
public class NavigationAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onFolderClick(long groupId);
        void onCustomerClick(long customerId);

        // Long Click Events
        void onFolderLongClick(RouteGroup group);
        void onCustomerLongClick(Customer customer);

        // Reorder events (position = index in the current visible list)
        void onFolderMoveUp(int position, RouteGroup group);
        void onFolderMoveDown(int position, RouteGroup group);
        void onCustomerMoveUp(int position, Customer customer);
        void onCustomerMoveDown(int position, Customer customer);
    }

    private final List<NavItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public NavigationAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<RouteGroup> groups, List<Customer> customers) {
        items.clear();
        for (RouteGroup g : groups) items.add(NavItem.fromGroup(g));
        for (Customer c : customers) items.add(NavItem.fromCustomer(c));
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------- VIEW HOLDERS ----------

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == NavItem.TYPE_GROUP) {
            View v = inflater.inflate(R.layout.item_folder, parent, false);
            return new FolderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_customer, parent, false);
            return new CustomerVH(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        NavItem item = items.get(position);

        if (holder instanceof FolderVH) {
            ((FolderVH) holder).bind(item.group, position);
        } else {
            ((CustomerVH) holder).bind(item.customer, position);
        }
    }

    // ---------- VIEW HOLDER CLASSES ----------

    class FolderVH extends RecyclerView.ViewHolder {
        TextView name;
        ImageButton btnUp, btnDown;

        FolderVH(View v) {
            super(v);
            name = v.findViewById(R.id.folderName);
            btnUp = v.findViewById(R.id.btnMoveUp);
            btnDown = v.findViewById(R.id.btnMoveDown);
        }

        void bind(RouteGroup g, int position) {
            name.setText(g.name);

            // Disable up arrow if first item, down if last group item
            btnUp.setAlpha(position == 0 ? 0.3f : 1.0f);
            btnUp.setEnabled(position > 0);

            // Last group position = last item with TYPE_GROUP
            int lastGroupPos = -1;
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).type == NavItem.TYPE_GROUP) { lastGroupPos = i; break; }
            }
            btnDown.setAlpha(position == lastGroupPos ? 0.3f : 1.0f);
            btnDown.setEnabled(position < lastGroupPos);

            itemView.setOnClickListener(v -> listener.onFolderClick(g.id));
            itemView.setOnLongClickListener(v -> {
                listener.onFolderLongClick(g);
                return true;
            });
            btnUp.setOnClickListener(v -> listener.onFolderMoveUp(position, g));
            btnDown.setOnClickListener(v -> listener.onFolderMoveDown(position, g));
        }
    }

    class CustomerVH extends RecyclerView.ViewHolder {
        TextView name;
        ImageButton btnUp, btnDown;

        CustomerVH(View v) {
            super(v);
            name = v.findViewById(R.id.customerName);
            btnUp = v.findViewById(R.id.btnMoveUp);
            btnDown = v.findViewById(R.id.btnMoveDown);
        }

        void bind(Customer c, int position) {
            if (c.isVisited) {
                name.setAlpha(0.5f);
                name.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else {
                name.setAlpha(1.0f);
                name.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            name.setText(c.name);

            // First customer item index
            int firstCustPos = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).type == NavItem.TYPE_CUSTOMER) { firstCustPos = i; break; }
            }
            int lastCustPos = -1;
            for (int i = items.size() - 1; i >= 0; i--) {
                if (items.get(i).type == NavItem.TYPE_CUSTOMER) { lastCustPos = i; break; }
            }

            btnUp.setAlpha(position == firstCustPos ? 0.3f : 1.0f);
            btnUp.setEnabled(position > firstCustPos);
            btnDown.setAlpha(position == lastCustPos ? 0.3f : 1.0f);
            btnDown.setEnabled(position < lastCustPos);

            itemView.setOnClickListener(v -> listener.onCustomerClick(c.id));
            itemView.setOnLongClickListener(v -> {
                listener.onCustomerLongClick(c);
                return true;
            });
            btnUp.setOnClickListener(v -> listener.onCustomerMoveUp(position, c));
            btnDown.setOnClickListener(v -> listener.onCustomerMoveDown(position, c));
        }
    }
}