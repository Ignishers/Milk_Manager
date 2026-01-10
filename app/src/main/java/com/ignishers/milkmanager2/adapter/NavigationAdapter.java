package com.ignishers.milkmanager2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.model.Customer;
import com.ignishers.milkmanager2.model.NavItem;
import com.ignishers.milkmanager2.model.RouteGroup;

import java.util.ArrayList;
import java.util.List;

public class NavigationAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener {
        void onFolderClick(long groupId);
        void onCustomerClick(long customerId);
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
            ((FolderVH) holder).bind(item.group);
        } else {
            ((CustomerVH) holder).bind(item.customer);
        }
    }

    // ---------- VIEW HOLDER CLASSES ----------

    class FolderVH extends RecyclerView.ViewHolder {
        TextView name;

        FolderVH(View v) {
            super(v);
            name = v.findViewById(R.id.folderName);
        }

        void bind(RouteGroup g) {
            name.setText(g.name);
            itemView.setOnClickListener(v ->
                    listener.onFolderClick(g.id));
        }
    }

    class CustomerVH extends RecyclerView.ViewHolder {
        TextView name;

        CustomerVH(View v) {
            super(v);
            name = v.findViewById(R.id.customerName);
        }

        void bind(Customer c) {
            if (c.isVisited) {
                name.setAlpha(0.5f);
                name.setTypeface(null, android.graphics.Typeface.NORMAL);
            } else {
                name.setAlpha(1.0f);
                name.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            name.setText(c.name);
            itemView.setOnClickListener(v ->
                    listener.onCustomerClick(c.id));
        }
    }
}