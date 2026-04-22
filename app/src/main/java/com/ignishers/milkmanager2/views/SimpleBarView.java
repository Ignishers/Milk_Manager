package com.ignishers.milkmanager2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * Default documentation for SimpleBarView.
 * <p>
 * This class is a part of the views component in the Milk Manager 2 architecture.
 * It operates within the standard Android application lifecycle and interacts
 * with its associated modules to fulfill business logic requirements.
 * Data usually flows from the local SQLite layer through DAOs, into ViewModels, 
 * and finally binding to Android Views.
 * </p>
 *
 * @since 1.0
 */
public class SimpleBarView extends View {

    private List<BarItem> data;
    private Paint paintBar;
    private Paint paintText;
    private int maxVal = 0;

    public static class BarItem {
        String label;
        int value;
        public BarItem(String l, int v) { label = l; value = v; }
    }    /**
     * Constructs a new {@code SimpleBarView} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public SimpleBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBar.setColor(Color.parseColor("#4F46E5")); // Primary

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.BLACK);
        paintText.setTextSize(30f);
        paintText.setTextAlign(Paint.Align.CENTER);
    }

    /**
    * Mutates the state of {@code Data}.
    * <p>
    * Assigns the provided value to the underlying property. This may trigger UI updates
    * or database writes depending on the architecture layer.
    * </p>
    *
    * @param data standard parameter provided by caller layer.
    */
    public void setData(List<BarItem> data) {
        this.data = data;
        maxVal = 0;
        for (BarItem item : data) {
            if (item.value > maxVal) maxVal = item.value;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float barWidth = (width / data.size()) * 0.6f;
        float spacing = (width / data.size()) * 0.4f;
        
        // Dynamic scaling
        float chartHeight = height - 60f; // space for text at bottom
        float scaleFactor = maxVal > 0 ? chartHeight / maxVal : 0;

        float x = spacing / 2;

        for (BarItem item : data) {
            float barHeight = item.value * scaleFactor;
            
            // Draw Bar
            canvas.drawRect(x, chartHeight - barHeight, x + barWidth, chartHeight, paintBar);
            
            // Draw Text
            if (item.label != null) {
                canvas.drawText(item.label, x + barWidth / 2, height - 20, paintText);
            }
            
            // Value on top
            canvas.drawText(String.valueOf(item.value), x + barWidth / 2, chartHeight - barHeight - 10, paintText);

            x += barWidth + spacing;
        }
    }
}