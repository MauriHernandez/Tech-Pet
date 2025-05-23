package com.example.techpet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomBarChart extends View {
    private Paint barPaint, textPaint;
    private List<Float> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private List<Integer> colors = new ArrayList<>();
    private float maxValue = 0;

    public CustomBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Float> values, List<String> labels) {
        this.values = values;
        this.labels = labels;
        this.colors = new ArrayList<>(); // se asignarán colores por defecto
        for (int i = 0; i < values.size(); i++) {
            colors.add(i % 2 == 0 ? Color.parseColor("#2196F3") : Color.parseColor("#FF9800"));
        }
        this.maxValue = Collections.max(values);
        invalidate();
    }

    public void setDataWithColors(List<Float> values, List<String> labels, List<Integer> colors) {
        this.values = values;
        this.labels = labels;
        this.colors = colors;
        this.maxValue = Collections.max(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float barWidth = width / (values.size() * 2f);
        float startX = barWidth / 2;

        for (int i = 0; i < values.size(); i++) {
            float barHeight = (values.get(i) / maxValue) * (height * 0.7f);
            float left = startX + (i * barWidth * 1.8f);
            float top = height - barHeight - 50;
            float right = left + barWidth;
            float bottom = height - 50;

            int color = (colors != null && i < colors.size()) ? colors.get(i) : Color.GRAY;
            barPaint.setColor(color);

            canvas.drawRect(left, top, right, bottom, barPaint);
            canvas.drawText(String.format("%.0f%%", values.get(i)), left + (barWidth / 2), top - 10, textPaint);
            canvas.drawText(labels.get(i), left + (barWidth / 2), height - 20, textPaint);
        }
    }
}
