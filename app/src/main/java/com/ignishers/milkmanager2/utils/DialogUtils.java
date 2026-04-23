package com.ignishers.milkmanager2.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.ignishers.milkmanager2.R;

/**
 * Default documentation for DialogUtils.
 * <p>
 * This class is a part of the utils component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class DialogUtils {

    public interface OnPositiveClickListener {
        void onPositiveClick();
    }

    /**
    * Executes the {@code showWarningDialog} operation.
    * <p>
    * Handles specific domain logic pertaining to this component's responsibility. Data input
    * is processed and state mutations or callbacks are executed locally.
    * </p>
    *
    * @param context standard parameter provided by caller layer.
    * @param title standard parameter provided by caller layer.
    * @param message standard parameter provided by caller layer.
    * @param positiveAction standard parameter provided by caller layer.
    * @param listener standard parameter provided by caller layer.
    */
    public static void showWarningDialog(Context context, String title, String message, String positiveAction, OnPositiveClickListener listener) {
        
        // Inflate Custom Layout
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null);
        
        TextView tvTitle = view.findViewById(R.id.dialogTitle);
        TextView tvMessage = view.findViewById(R.id.dialogMessage);
        android.widget.Button btnPositive = view.findViewById(R.id.btnPositive); // Use Button/TextView
        android.widget.Button btnNegative = view.findViewById(R.id.btnNegative);
        
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(positiveAction);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();
                
        // Transparent background to let CardView handle corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onPositiveClick();
        });
        
        dialog.show();
    }
}