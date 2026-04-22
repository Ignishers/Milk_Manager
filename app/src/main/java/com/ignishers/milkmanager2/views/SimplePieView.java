package com.ignishers.milkmanager2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;
import java.util.Random;

/**
 * Default documentation for SimplePieView.
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
public class SimplePieView extends View {

    private List<PieItem> data;
    private Paint paint;
    private RectF rectF = new RectF();
    private int[] colors = {
            Color.parseColor("#4F46E5"), // Indigo
            Color.parseColor("#14B8A6"), // Teal
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#EF4444"), // Red
            Color.parseColor("#8B5CF6")  // Violet
    };

    public static class PieItem {
        public String label;
        public float value;
        public PieItem(String l, float v) { label = l; value = v; }
    }    /**
     * Constructs a new {@code SimplePieView} instance.
     * <p>
     * Initializes the object's state and prepares it for use within the application context.
     * Data dependencies required for the entity are injected here.
     * </p>
     */
    public SimplePieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    public void setData(List<PieItem> data) {
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float minDim = Math.min(width, height);
        float radius = minDim * 0.4f;
        float cx = width / 2;
        float cy = height / 2;

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius);

        float total = 0;
        for (PieItem item : data) total += item.value;

        float startAngle = 0;
        int colorIdx = 0;

        for (PieItem item : data) {
            float sweepAngle = (item.value / total) * 360f;
            
            paint.setColor(colors[colorIdx % colors.length]);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            
            // Draw Legend or Label? Text might clutter, let's keep it simple for now or draw simple legend below
            // For now, simpler: user clicks for detail or we draw text if slice is big enough
            
            startAngle += sweepAngle;
            colorIdx++;
        }
    }
}