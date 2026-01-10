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
            // Reset state to avoid recycling artifacts
            itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            itemView.setScaleX(1.0f);
            itemView.setScaleY(1.0f);

            // Check if transaction is recent (within last 10 seconds)
            // Use date + timestamp to be precise
            if (isRecent(transaction.getDate(), transaction.getTimestamp())) {
                animateHighlight();
            }

            String session = transaction.getSession();
            tvSession.setText(session);
            
            // Format timestamp (Display logic)
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
                
                if (transaction.getPaymentMode() != null && !transaction.getPaymentMode().isEmpty()) {
                    display += " - " + transaction.getPaymentMode();
                } 
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

        private void animateHighlight() {
            // Bg Color Animation (Cyan -> Transparent)
            int colorFrom = android.graphics.Color.parseColor("#E0F7FA");
            int colorTo = android.graphics.Color.TRANSPARENT;
            
            android.animation.ValueAnimator colorAnimation = android.animation.ValueAnimator.ofObject(
                    new android.animation.ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(8000); // 8 Seconds fade out
            colorAnimation.addUpdateListener(animator -> itemView.setBackgroundColor((int) animator.getAnimatedValue()));
            colorAnimation.start();

            // Scale Animation (1.05 -> 1.0)
            itemView.setScaleX(1.05f);
            itemView.setScaleY(1.05f);
            itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(8000)
                    .start();
        }

        private boolean isRecent(String dateStr, String timeStr) {
            try {
                // Parse Transaction DateTime
                java.time.LocalDate date = java.time.LocalDate.parse(dateStr); // yyyy-MM-dd
                java.time.LocalTime time = java.time.LocalTime.parse(timeStr); // HH:mm:ss.SSSSSS (default ISO)
                java.time.LocalDateTime transDateTime = java.time.LocalDateTime.of(date, time);

                // Current Time
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                // Diff in seconds
                long seconds = java.time.Duration.between(transDateTime, now).getSeconds();
                
                // Return true if within last 10 seconds
                return seconds >= 0 && seconds < 10;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
