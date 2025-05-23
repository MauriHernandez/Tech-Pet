package com.example.techpet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ConsumptionChart extends View {
    private List<Float> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private Paint barPaint, textPaint, axisPaint;
    private float maxValue = 100f;
    private float barWidth = 50f;
    private float barSpacing = 30f;

    public ConsumptionChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConsumptionChart(Context context) {
        super(context);
        init(); // asegúrate de llamar a init aquí también
    }

    private void init() {
        // Pintura para las barras
        barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#3F51B5"));
        barPaint.setStyle(Paint.Style.FILL);

        // Pintura para el texto
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Pintura para los ejes
        axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(3f);
    }

    public void setData(List<Float> values, List<String> labels) {
        if (values == null || labels == null) {
            throw new IllegalArgumentException("values y labels no pueden ser null");
        }

        if (values.size() != labels.size()) {
            throw new IllegalArgumentException("values y labels deben tener el mismo tamaño");
        }

        this.values = new ArrayList<>(values);
        this.labels = new ArrayList<>(labels);

        // Calcular el valor máximo para escalar las barras
        this.maxValue = 0f;
        for (float value : values) {
            if (value > maxValue) maxValue = value;
        }
        if (maxValue == 0f) maxValue = 100f;

        invalidate(); // Forzar redibujado
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values.isEmpty() || labels.isEmpty()) return;

        float width = getWidth();
        float height = getHeight() - 50f; // Espacio para etiquetas

        // Ejes
        canvas.drawLine(50f, height, width - 50f, height, axisPaint); // Eje X
        canvas.drawLine(50f, 50f, 50f, height, axisPaint); // Eje Y

        float xPos = 100f; // Inicio en X
        int count = Math.min(values.size(), labels.size()); // Protección

        for (int i = 0; i < count; i++) {
            float value = values.get(i);
            float barHeight = (value / maxValue) * (height - 100f);

            // Barra
            RectF rect = new RectF(
                    xPos,
                    height - barHeight,
                    xPos + barWidth,
                    height
            );
            canvas.drawRect(rect, barPaint);

            // Valor sobre la barra
            canvas.drawText(
                    String.format("%.1f", value),
                    xPos + (barWidth / 2),
                    height - barHeight - 10f,
                    textPaint
            );

            // Etiqueta X debajo
            canvas.drawText(
                    labels.get(i),
                    xPos + (barWidth / 2),
                    height + 30f,
                    textPaint
            );

            xPos += barWidth + barSpacing;
        }
    }
}
