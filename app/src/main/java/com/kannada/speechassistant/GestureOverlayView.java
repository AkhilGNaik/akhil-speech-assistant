package com.kannada.speechassistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom View that draws hand tracking landmark points and connecting lines (skeleton)
 * on top of the CameraX PreviewView.
 */
public class GestureOverlayView extends View {

    private final Paint jointPaint;
    private final Paint linePaint;
    private final List<PointF> landmarks = new ArrayList<>();

    // Hand landmark connections in MediaPipe (21 landmarks)
    // Thumb: 0-1-2-3-4
    // Index: 0-5-6-7-8, 5-9
    // Middle: 9-10-11-12, 9-13
    // Ring: 13-14-15-16, 13-17
    // Pinky: 0-17-18-19-20
    private static final int[][] CONNECTIONS = {
        {0, 1}, {1, 2}, {2, 3}, {3, 4}, // Thumb
        {0, 5}, {5, 6}, {6, 7}, {7, 8}, // Index
        {5, 9}, {9, 10}, {10, 11}, {11, 12}, // Middle
        {9, 13}, {13, 14}, {14, 15}, {15, 16}, // Ring
        {0, 17}, {13, 17}, {17, 18}, {18, 19}, {19, 20} // Pinky
    };

    public GestureOverlayView(Context context) {
        this(context, null);
    }

    public GestureOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Joint (circles) paint
        jointPaint = new Paint();
        jointPaint.setColor(0xFFF59E0B); // Amber
        jointPaint.setStyle(Paint.Style.FILL);
        jointPaint.setAntiAlias(true);

        // Skeleton line paint
        linePaint = new Paint();
        linePaint.setColor(0xFF10B981); // Emerald Green
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        jointPaint.setAntiAlias(true);
    }

    /**
     * Updates the landmarks to draw.
     * Coordinate point coordinates should be normalized (0.0 to 1.0).
     */
    public void updateLandmarks(List<PointF> normalizedLandmarks) {
        landmarks.clear();
        if (normalizedLandmarks != null) {
            landmarks.addAll(normalizedLandmarks);
        }
        invalidate(); // Redraw view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (landmarks.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Convert normalized coordinates to absolute pixels
        List<PointF> pixelPoints = new ArrayList<>();
        for (PointF normPt : landmarks) {
            // MediaPipe frame is typically mirrored or oriented. We scale coordinates.
            float px = normPt.x * width;
            float py = normPt.y * height;
            pixelPoints.add(new PointF(px, py));
        }

        // 1. Draw connecting skeleton lines
        for (int[] connection : CONNECTIONS) {
            int startIndex = connection[0];
            int endIndex = connection[1];

            if (startIndex < pixelPoints.size() && endIndex < pixelPoints.size()) {
                PointF startPt = pixelPoints.get(startIndex);
                PointF endPt = pixelPoints.get(endIndex);
                canvas.drawLine(startPt.x, startPt.y, endPt.x, endPt.y, linePaint);
            }
        }

        // 2. Draw joint landmark circles
        for (PointF joint : pixelPoints) {
            canvas.drawCircle(joint.x, joint.y, 10f, jointPaint);
        }
    }
}
