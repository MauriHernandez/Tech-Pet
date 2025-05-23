package com.example.techpet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DispenserAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "DispenserAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String deviceId = intent.getStringExtra("deviceId");
        String userId = intent.getStringExtra("userId");
        String rationStr = intent.getStringExtra("ration");

        if (deviceId == null || userId == null || rationStr == null) {
            Log.e(TAG, "Datos faltantes en el Intent");
            return;
        }

        int ration;
        try {
            ration = Integer.parseInt(rationStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Ración inválida: " + rationStr);
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String timestampKey = "timestamp_" + System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("usuarios/" + userId + "/dispensadores/" + deviceId + "/estado", "dispensando");
        updates.put("usuarios/" + userId + "/dispensadores/" + deviceId + "/configuracion/ultima_actualizacion", ServerValue.TIMESTAMP);

        Map<String, Object> historial = new HashMap<>();
        historial.put("tipo", "dispensacion_programada");
        historial.put("hora", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        historial.put("racion", ration);
        historial.put("completado", true);

        updates.put("usuarios/" + userId + "/dispensadores/" + deviceId + "/historial_acciones/" + timestampKey, historial);

        dbRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dbRef.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                            .child("estado").setValue("conectado");
                }, 10000);
            } else {
                Log.e(TAG, "Error al actualizar estado", task.getException());
            }
        });
    }
}
