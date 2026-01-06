package com.ignishers.milkmanager2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ignishers.milkmanager2.R;
import com.ignishers.milkmanager2.model.DailyTransaction;

import java.util.ArrayList;
import java.util.List;

public class DailyTransactionAdapter extends RecyclerView.Adapter<DailyTransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(DailyTransaction transaction);
    }

    private final List<DailyTransaction> transactions = new ArrayList<>();
    private final OnTransactionClickListener listener;

    public DailyTransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DailyTransaction> newTransactions) {
        transactions.clear();
        transactions.addAll(newTransactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        DailyTransaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSession, tvTime, tvQuantity, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSession = itemView.findViewById(R.id.tvSession);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        public void bind(DailyTransaction transaction) {
            String session = transaction.getSession();
            tvSession.setText(session);
            
            // Format timestamp
            String timestamp = transaction.getTimestamp();
            if (timestamp != null && timestamp.contains("T")) {
                 try {
                     String timePart = timestamp.substring(timestamp.indexOf("T") + 1);
                     if (timePart.contains(".")) {
                         timePart = timePart.substring(0, timePart.indexOf("."));
                     }
                     tvTime.setText(timePart);
                 } catch (Exception e) {
                     tvTime.setText(timestamp);
                 }
            } else {
                tvTime.setText(timestamp);
            }

            if (session != null && session.startsWith("Payment")) {
                String display = "Payment";
                
                // 1. Try to get from separate column
                if (transaction.getPaymentMode() != null && !transaction.getPaymentMode().isEmpty()) {
                    display += " - " + transaction.getPaymentMode();
                } 
                // 2. Fallback: Parse from Session name (e.g. "Payment - Cash") if mostly old data
                else if (session.contains("-")) {
                    display = session;
                }
                
                tvSession.setText(display);

                tvQuantity.setText(""); 
                tvAmount.setText(String.format("₹ %.2f", transaction.getAmount()));
                tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Green
                tvSession.setTextColor(android.graphics.Color.parseColor("#4CAF50")); 
            } else {
                tvQuantity.setText(String.format("%.3f L", transaction.getQuantity()));
                tvAmount.setText(String.format("₹ %.2f", transaction.getAmount()));
                tvAmount.setTextColor(android.graphics.Color.BLACK);
                tvSession.setTextColor(android.graphics.Color.BLACK);
            }

            itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
        }
    }
}
