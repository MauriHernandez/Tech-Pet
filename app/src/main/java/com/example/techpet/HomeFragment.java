package com.example.techpet;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.database.annotations.NotNull;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView cardNoDevices, titleDispositivos, noDevicesText;
    private ImageView plus;
    private RecyclerView devicesRecyclerView;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    private List<Dispensador> listaActualDeDispensadores = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        cardNoDevices = view.findViewById(R.id.card_no_devices);
        titleDispositivos = view.findViewById(R.id.title_dispositivos);
        noDevicesText = view.findViewById(R.id.no_devices_text);
        plus = view.findViewById(R.id.plusImage);  
        devicesRecyclerView = view.findViewById(R.id.resource_recycler_view);

        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        plus.setOnClickListener(v -> showAddDeviceDialog());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserName(userId);
            loadUserDevices(userId);

            mDatabase.child("usuarios").child(userId).child("dispensadores")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                                checkExistingAlarms(deviceSnapshot.getKey());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Error al cargar dispositivos", error.toException());
                        }
                    });
        }

        return view;
    }


    // ***** AÑADE ESTE MÉTODO COMPLETO *****
    public void showMainDeviceOptionsDialog(String deviceId, String deviceName) {
        if (getContext() == null || deviceId == null) {
            Log.e("HomeFragment", "Contexto o deviceId nulo en showMainDeviceOptionsDialog");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Asegúrate que R.layout.dialog_main_dispenser_options es tu XML con las opciones generales
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_device_options, null);
        builder.setView(dialogView);
        builder.setTitle("Opciones para " + (deviceName != null ? deviceName : deviceId));

        AlertDialog mainOptionsDialog = builder.create();

        Button btnAssociatePet = dialogView.findViewById(R.id.btn_associate_pet);
        Button btnSetSchedule = dialogView.findViewById(R.id.btn_set_schedule);
        Button btnViewReports = dialogView.findViewById(R.id.btn_view_reports);
        Button btnDispenserControl = dialogView.findViewById(R.id.btn_dispenser_control);
        Button btnCancelMain = dialogView.findViewById(R.id.btn_cancel); // ID del botón cancelar en ESTE diálogo

        btnAssociatePet.setOnClickListener(v -> {
            loadUserPetsForAssociation(deviceId); // Asumo que ya tienes este método o lo implementarás
            mainOptionsDialog.dismiss();
        });

        btnSetSchedule.setOnClickListener(v -> {
            showScheduleDialog(deviceId); // Asumo que ya tienes este método
            mainOptionsDialog.dismiss();
        });

        btnViewReports.setOnClickListener(v -> {
            // Aquí va tu lógica para navegar a ReportFragment
            // Ejemplo:
            // NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            // navController.navigate(R.id.nav_reportes); // Ajusta la acción/destino
            Toast.makeText(getContext(), "Ir a Reportes...", Toast.LENGTH_SHORT).show();
            mainOptionsDialog.dismiss();
        });

        btnDispenserControl.setOnClickListener(v -> {
            // Llama al método (renombrado) para el control manual
            showManualDispenserControlDialog(deviceId);
            mainOptionsDialog.dismiss();
        });

        btnCancelMain.setOnClickListener(v -> mainOptionsDialog.dismiss());

        mainOptionsDialog.show();
    }
    private void loadUserDevices(String userId) {
        mDatabase.child("usuarios").child(userId).child("dispensadores")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Dispensador> dispensadores = new ArrayList<>();

                        if (!snapshot.exists()) {
                            updateUIForNoDevices();
                            return;
                        }

                        for (DataSnapshot dispSnapshot : snapshot.getChildren()) {
                            Dispensador disp = parseDispensadorFromSnapshot(dispSnapshot);
                            if (disp != null) {
                                dispensadores.add(disp);
                            }
                        }
                        updateUIBasedOnDevices(dispensadores);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al cargar dispositivos", error.toException());
                        Toast.makeText(getContext(), "Error al cargar dispositivos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Dispensador parseDispensadorFromSnapshot(DataSnapshot dispSnapshot) {
        String deviceId = dispSnapshot.getKey();
        Dispensador disp = new Dispensador();
        disp.setId(deviceId);

        if (dispSnapshot.child("configuracion").exists()) {
            Map<String, Object> config = new HashMap<>();
            DataSnapshot configSnapshot = dispSnapshot.child("configuracion");

            if (configSnapshot.child("modo").exists()) {
                config.put("modo", configSnapshot.child("modo").getValue(String.class));
            }
            if (configSnapshot.child("ultima_actualizacion").exists()) {
                config.put("ultima_actualizacion", configSnapshot.child("ultima_actualizacion").getValue(Long.class));
            }
            disp.setConfiguracion(config);
        }

        if (dispSnapshot.child("sensores").exists()) {
            Map<String, Object> sensores = new HashMap<>();
            DataSnapshot sensoresSnapshot = dispSnapshot.child("sensores");

            if (sensoresSnapshot.child("ambiente").exists()) {
                // parseSensorData() es perfecto para esto, ya que convierte DataSnapshot a Map<String, Object>
                sensores.put("ambiente", parseSensorData(sensoresSnapshot.child("ambiente")));
                Log.d("HomeFragment", "Nodo 'ambiente' procesado para Dispensador " + deviceId);
            } else {
                Log.w("HomeFragment", "Nodo 'ambiente' no encontrado en el snapshot de sensores para " + deviceId);
            }

            if (sensoresSnapshot.child("recipientes").exists()) {
                Map<String, Object> recipientes = new HashMap<>();
                DataSnapshot recipientesSnapshot = sensoresSnapshot.child("recipientes");

                if (recipientesSnapshot.child("agua").exists()) {
                    recipientes.put("agua", parseSensorData(recipientesSnapshot.child("agua")));
                }
                if (recipientesSnapshot.child("comida").exists()) {
                    recipientes.put("comida", parseSensorData(recipientesSnapshot.child("comida")));
                }
                sensores.put("recipientes", recipientes);
            }
            disp.setSensores(sensores);
        }

        if (dispSnapshot.child("asignado_a").exists()) {
            String petId = dispSnapshot.child("asignado_a").getValue(String.class);
            disp.setAsignadoA(petId);
        }

        return disp;
    }

    private Map<String, Object> parseSensorData(DataSnapshot sensorSnapshot) {
        Map<String, Object> sensorData = new HashMap<>();
        for (DataSnapshot data : sensorSnapshot.getChildren()) {
            sensorData.put(data.getKey(), data.getValue());
        }
        return sensorData;
    }

    private void updateUIForNoDevices() {
        noDevicesText.setVisibility(View.VISIBLE);
        devicesRecyclerView.setVisibility(View.GONE);
        cardNoDevices.setVisibility(View.VISIBLE);
    }

    private void updateUIBasedOnDevices(List<Dispensador> dispensadores) {
        if (dispensadores.isEmpty()) {
            updateUIForNoDevices();
        } else {
            noDevicesText.setVisibility(View.GONE);
            devicesRecyclerView.setVisibility(View.VISIBLE);
            cardNoDevices.setVisibility(View.GONE);
            setupRecyclerView(dispensadores);
        }
    }

    // ***** ESTE ES EL ÚNICO MÉTODO QUE NECESITAS MODIFICAR *****
    private void setupRecyclerView(List<Dispensador> dispensadores) {
        // Es buena idea actualizar la lista de referencia del fragmento aquí también,
        // si 'dispensadores' es la lista más actual que se le pasará al adapter.
        // Si ya la actualizaste en updateUIBasedOnDevices, este paso podría ser redundante
        // pero no hace daño asegurar que 'listaActualDeDispensadores' esté al día.
        if (this.listaActualDeDispensadores != dispensadores) { // Solo si no es la misma instancia
            this.listaActualDeDispensadores.clear();
            this.listaActualDeDispensadores.addAll(dispensadores);
        }

        DispensadorAdapter adapter = new DispensadorAdapter(dispensadores, new DispensadorAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(String deviceId) { // Recibes solo el deviceId como String
                Dispensador dispensadorSeleccionado = null;
                String nombreDispensador = "Dispensador"; // Nombre por defecto

                // Buscar el dispensador en la lista que tiene el HomeFragment
                // usando 'HomeFragment.this.listaActualDeDispensadores'
                if (HomeFragment.this.listaActualDeDispensadores != null) {
                    for (Dispensador disp : HomeFragment.this.listaActualDeDispensadores) {
                        if (disp.getId() != null && disp.getId().equals(deviceId)) {
                            dispensadorSeleccionado = disp;
                            break;
                        }
                    }
                }

                if (dispensadorSeleccionado != null) {
                    // Obtener el nombre del objeto Dispensador encontrado
                    nombreDispensador = dispensadorSeleccionado.getId();
                    // Llamar con el deviceId (que ya tienes) y el nombre encontrado
                    showMainDeviceOptionsDialog(deviceId, nombreDispensador);
                } else {
                    // Fallback si no se encuentra el dispensador (debería ser raro)
                    Log.w("HomeFragment", "Dispensador con ID: " + deviceId + " no encontrado en listaActualDeDispensadores.");
                    // Puedes decidir si mostrar el diálogo solo con el ID o manejar el error
                    showMainDeviceOptionsDialog(deviceId, "ID: " + deviceId);
                }
            }

            @Override
            public void onConfigClick(String deviceId) {
                loadUserPetsForAssociation(deviceId);
            }
        });

        if (devicesRecyclerView != null) { // Siempre es bueno verificar
            devicesRecyclerView.setAdapter(adapter);
        }
    }

    // ***** RENOMBRA tu método showDeviceOptions a esto y verifica el contenido *****
    private void showManualDispenserControlDialog(String deviceId) {
        if (getContext() == null || deviceId == null) {
            Log.e("HomeFragment", "Contexto o deviceId nulo en showManualDispenserControlDialog");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // Este es tu layout para los botones de abrir/cerrar comida/agua
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_dispenser_control, null);
        builder.setView(dialogView);
        builder.setTitle("Control Manual: " + deviceId);
        AlertDialog dialog = builder.create();

        Button btnOpenFood = dialogView.findViewById(R.id.btn_open_food);
        Button btnCloseFood = dialogView.findViewById(R.id.btn_close_food);
        Button btnOpenWater = dialogView.findViewById(R.id.btn_open_water);
        Button btnCloseWater = dialogView.findViewById(R.id.btn_close_water);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel); // ID del botón cancelar en ESTE diálogo

        // Comandos actualizados a "dispensar" y "detener"
        btnOpenFood.setOnClickListener(v -> controlDispenser(deviceId, "comida", "abrir"));
        btnCloseFood.setOnClickListener(v -> controlDispenser(deviceId, "comida", "cerrar"));
        btnOpenWater.setOnClickListener(v -> controlDispenser(deviceId, "agua", "abrir"));
        btnCloseWater.setOnClickListener(v -> controlDispenser(deviceId, "agua", "cerrar"));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void controlDispenser(String deviceId, String tipo, String accion) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("usuarios")
                .child(user.getUid())
                .child("dispensadores")
                .child(deviceId)
                .child("comandos")
                .child("ultimo_comando");

        Map<String, Object> comando = new HashMap<>();
        comando.put("accion_" + tipo, accion);

        ref.updateChildren(comando)
                .addOnSuccessListener(aVoid -> {
                    String mensaje = "";
                    switch (accion) {
                        case "abrir":
                            mensaje = "Abriendo dispensador de " + tipo;
                            break;
                        case "cerrar":
                            mensaje = "Cerrando dispensador de " + tipo;
                            break;
                        case "ninguno":
                            mensaje = "Deteniendo dispensador de " + tipo;
                            break;
                    }
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error al enviar comando", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDispenserControlDialog(String deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_dispenser_control, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnOpenFood = dialogView.findViewById(R.id.btn_open_food);
        Button btnCloseFood = dialogView.findViewById(R.id.btn_close_food);
        Button btnOpenWater = dialogView.findViewById(R.id.btn_open_water);
        Button btnCloseWater = dialogView.findViewById(R.id.btn_close_water);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnOpenFood.setOnClickListener(v -> {
            controlDispenser(deviceId, "comida", "abrir");
            dialog.dismiss();
        });

        btnCloseFood.setOnClickListener(v -> {
            controlDispenser(deviceId, "comida", "cerrar");
            dialog.dismiss();
        });



        btnOpenWater.setOnClickListener(v -> {
            controlDispenser(deviceId, "agua", "abrir");
            dialog.dismiss();
        });

        btnCloseWater.setOnClickListener(v -> {
            controlDispenser(deviceId, "agua", "cerrar");
            dialog.dismiss();
        });



        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    private void showScheduleDialog(String deviceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_schedule, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Cambiar a MaterialButton para los horarios
        MaterialButton btn_time_1 = dialogView.findViewById(R.id.btn_time_1);
        MaterialButton btn_time_2 = dialogView.findViewById(R.id.btn_time_2);
        MaterialButton btn_time_3 = dialogView.findViewById(R.id.btn_time_3);

        EditText editPortion1 = dialogView.findViewById(R.id.edit_portion_1);
        EditText editPortion2 = dialogView.findViewById(R.id.edit_portion_2);
        EditText editPortion3 = dialogView.findViewById(R.id.edit_portion_3);
        Button btnSave = dialogView.findViewById(R.id.btn_save_schedule);

        // Configurar listeners para los botones de hora
        btn_time_1.setOnClickListener(v -> showTimePickerDialog(btn_time_1));
        btn_time_2.setOnClickListener(v -> showTimePickerDialog(btn_time_2));
        btn_time_3.setOnClickListener(v -> showTimePickerDialog(btn_time_3));

        // Cargar horarios existentes (necesitarás adaptar este método)
        loadExistingSchedules(deviceId, btn_time_1, editPortion1, btn_time_2, editPortion2, btn_time_3, editPortion3);

        btnSave.setOnClickListener(v -> {
            if (validateScheduleInputs(btn_time_1, editPortion1, btn_time_2, editPortion2, btn_time_3, editPortion3)) {
                saveSchedulesToDatabase(deviceId,
                        btn_time_1.getText().toString(),  // Esto obtendrá "21:55" si el usuario lo seleccionó
                        editPortion1.getText().toString(),
                        btn_time_2.getText().toString(),
                        editPortion2.getText().toString(),
                        btn_time_3.getText().toString(),
                        editPortion3.getText().toString());
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    private void showTimePickerDialog(MaterialButton timeButton) {
        String currentTime = timeButton.getText().toString();
        int hour = 12; // Hora por defecto
        int minute = 0;

        try {
            if(!currentTime.equals("Seleccionar hora") && currentTime.contains(":")) {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, selectedMinute);
                    timeButton.setText(time);
                },
                hour, minute, true); // true para formato 24h

        timePickerDialog.show();
    }
    private boolean validateScheduleInputs(MaterialButton btn_time_1, EditText portion1,
                                           MaterialButton btn_time_2, EditText portion2,
                                           MaterialButton btn_time_3, EditText portion3) {
        // Validar horario 1
        if (btn_time_1.getText().toString().equals("Seleccionar hora") && !portion1.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Selecciona una hora para el horario 1", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar porción 1
        if (!btn_time_1.getText().toString().equals("Seleccionar hora") && portion1.getText().toString().isEmpty()) {
            portion1.setError("Ingresa la ración");
            return false;
        }

        // Validar formato de hora 1
        if (!btn_time_1.getText().toString().equals("Seleccionar hora") &&
                !btn_time_1.getText().toString().matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) {
            Toast.makeText(getContext(), "Formato de hora inválido en horario 1 (use HH:MM)", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar valor de porción 1
        if (!portion1.getText().toString().isEmpty()) {
            try {
                double portionValue = Double.parseDouble(portion1.getText().toString());
                if (portionValue <= 0) {
                    portion1.setError("La ración debe ser mayor a 0");
                    return false;
                }
            } catch (NumberFormatException e) {
                portion1.setError("Valor numérico inválido");
                return false;
            }
        }

        return true;
    }


    private void loadExistingSchedules(String deviceId,
                                       MaterialButton btn_time_1, EditText portion1,
                                       MaterialButton btn_time_2, EditText portion2,
                                       MaterialButton btn_time_3, EditText portion3) {
        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .child("asignado_a").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String petId = snapshot.getValue(String.class);
                            if (petId != null && !petId.isEmpty()) {
                                loadPetSchedules(petId, btn_time_1, portion1, btn_time_2, portion2, btn_time_3, portion3);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al cargar mascota asignada", error.toException());
                    }
                });
    }
    private void saveSchedulesToDatabase(String deviceId, String... schedules) {
        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .child("asignado_a").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            String petId = snapshot.getValue(String.class);
                            if (!petId.isEmpty()) {
                                updatePetSchedules(petId, schedules);
                                setupAlarmManager(deviceId, petId, schedules);
                            } else {
                                Toast.makeText(getContext(), "Asocia una mascota primero", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "El dispensador no tiene mascota asociada", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al obtener mascota asignada", error.toException());
                    }
                });



    }

    private void checkExistingAlarms(String deviceId) {
        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .child("asignado_a").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String petId = snapshot.getValue(String.class);
                            mDatabase.child("usuarios").child(userId).child("mascotas").child(petId)
                                    .child("alimentacion").child("configuracion_actual")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                List<String> horarios = new ArrayList<>();
                                                List<String> raciones = new ArrayList<>();

                                                if (snapshot.child("horarios").exists()) {
                                                    for (DataSnapshot hora : snapshot.child("horarios").getChildren()) {
                                                        horarios.add(hora.getValue(String.class));
                                                    }
                                                }

                                                if (snapshot.child("raciones").exists()) {
                                                    for (DataSnapshot racion : snapshot.child("raciones").getChildren()) {
                                                        raciones.add(String.valueOf(racion.getValue(Long.class)));
                                                    }
                                                }

                                                // Reconstruir el array de schedules para setupAlarmManager
                                                String[] schedules = new String[6];
                                                for (int i = 0; i < 3; i++) {
                                                    if (i < horarios.size()) {
                                                        schedules[i*2] = horarios.get(i);
                                                        if (i < raciones.size()) {
                                                            schedules[i*2+1] = raciones.get(i);
                                                        }
                                                    }
                                                }

                                                setupAlarmManager(deviceId, petId, schedules);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("Firebase", "Error al verificar horarios", error.toException());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al verificar mascota asignada", error.toException());
                    }
                });
    }
    private void setupAlarmManager(String deviceId, String petId, String... schedules) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        // Cancelar todas las alarmas anteriores para este dispositivo
        for (int i = 0; i < 3; i++) {
            Intent cancelIntent = new Intent(requireContext(), com.example.techpet.DispenserAlarmReceiver.class);
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    i,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(cancelPendingIntent);
        }

        // Programar nuevas alarmas
        for (int i = 0; i < 3; i++) {
            String hora = schedules[i*2];
            String racion = schedules[i*2+1];

            if (!hora.isEmpty() && !racion.isEmpty()) {
                String[] timeParts = hora.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                // Si la hora ya pasó hoy, programar para mañana
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(requireContext(), com.example.techpet.DispenserAlarmReceiver.class);
                intent.putExtra("deviceId", deviceId);
                intent.putExtra("userId", userId);
                intent.putExtra("ration", racion);

                PendingIntent pi = PendingIntent.getBroadcast(
                        requireContext(),
                        i, // Código de solicitud único para cada alarma
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Usar setExactAndAllowWhileIdle para mayor precisión en Android 6+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pi
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pi
                    );
                }

                // Configurar repetición diaria
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pi
                );
            }
        }
    }

    private void loadPetSchedules(String petId,
                                  MaterialButton time1, EditText portion1,
                                  MaterialButton time2, EditText portion2,
                                  MaterialButton time3, EditText portion3) {
        mDatabase.child("usuarios").child(userId).child("mascotas").child(petId)
                .child("alimentacion").child("configuracion_actual")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Limpiar campos primero
                            time1.setText("Seleccionar hora");
                            time2.setText("Seleccionar hora");
                            time3.setText("Seleccionar hora");
                            portion1.setText("");
                            portion2.setText("");
                            portion3.setText("");

                            // Cargar horarios
                            if (snapshot.child("horarios").exists()) {
                                int index = 0;
                                for (DataSnapshot horaSnapshot : snapshot.child("horarios").getChildren()) {
                                    String hora = horaSnapshot.getValue(String.class);
                                    if (hora != null) {
                                        switch (index) {
                                            case 0: time1.setText(hora); break;
                                            case 1: time2.setText(hora); break;
                                            case 2: time3.setText(hora); break;
                                        }
                                    }
                                    index++;
                                }
                            }

                            // Cargar raciones
                            if (snapshot.child("raciones").exists()) {
                                int index = 0;
                                for (DataSnapshot racionSnapshot : snapshot.child("raciones").getChildren()) {
                                    Long racion = racionSnapshot.getValue(Long.class);
                                    if (racion != null) {
                                        switch (index) {
                                            case 0: portion1.setText(String.valueOf(racion)); break;
                                            case 1: portion2.setText(String.valueOf(racion)); break;
                                            case 2: portion3.setText(String.valueOf(racion)); break;
                                        }
                                    }
                                    index++;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al cargar horarios de mascota", error.toException());
                    }
                });
    }
    private void updatePetSchedules(String petId, String... schedules) {
        // Primero limpiar los datos existentes
        Map<String, Object> updates = new HashMap<>();
        updates.put("usuarios/"+userId+"/mascotas/"+petId+"/alimentacion/configuracion_actual/horarios", null);
        updates.put("usuarios/"+userId+"/mascotas/"+petId+"/alimentacion/configuracion_actual/raciones", null);

        // Luego preparar los nuevos datos
        List<String> horarios = new ArrayList<>();
        List<Long> raciones = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            String hora = schedules[i*2];
            String racion = schedules[i*2+1];

            if (!hora.isEmpty() && !racion.isEmpty()) {
                horarios.add(hora);
                try {
                    raciones.add(Long.parseLong(racion));
                } catch (NumberFormatException e) {
                    raciones.add(0L);
                    Log.e("Schedule", "Error en ración: " + racion, e);
                }
            }
        }

        // Agregar los nuevos valores
        updates.put("usuarios/"+userId+"/mascotas/"+petId+"/alimentacion/configuracion_actual/horarios", horarios);
        updates.put("usuarios/"+userId+"/mascotas/"+petId+"/alimentacion/configuracion_actual/raciones", raciones);

        // Ejecutar la actualización atómica
        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Horarios actualizados correctamente");
                    Toast.makeText(getContext(), "Horarios guardados", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Error al actualizar horarios", e);
                    Toast.makeText(getContext(), "Error al guardar horarios", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadUserPetsForAssociation(final String deviceId) {
        FirebaseUser user = mAuth.getCurrentUser(); // Verificar usuario autenticado AHORA
        if (user == null) {
            Log.e("LoadPets", "Usuario no autenticado al intentar cargar mascotas para asociación.");
            Toast.makeText(getContext(), "Error: Debes iniciar sesión para asociar mascotas.", Toast.LENGTH_LONG).show();
            return; // Salir si no hay usuario
        }
        final String currentUserId = user.getUid(); // Usar el ID del usuario actual

        Log.d("LoadPets", "Cargando mascotas para asociar al dispositivo: " + deviceId + " para Usuario: " + currentUserId);

        mDatabase.child("usuarios").child(currentUserId).child("mascotas") // Ruta corregida según reglas
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Mascota> mascotas = new ArrayList<>();
                        if (!snapshot.exists()){
                            Log.w("LoadPets", "El usuario " + currentUserId + " no tiene mascotas registradas en /usuarios/" + currentUserId + "/mascotas");
                            // El diálogo mostrará el mensaje de "No tienes mascotas..."
                        } else {
                            Log.d("LoadPets", "Procesando " + snapshot.getChildrenCount() + " mascotas encontradas.");
                            for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                                Mascota mascota = petSnapshot.getValue(Mascota.class);
                                String petKey = petSnapshot.getKey(); // <-- OBTENER LA CLAVE (ID)

                                if (mascota != null && petKey != null) {
                                    // --- ¡IMPORTANTE! Asignar la clave al campo ID ---
                                    try {
                                        mascota.setId(petKey); // <<<--- LLAMAR AL SETTER
                                        mascotas.add(mascota);
                                        // Asumiendo que Mascota tiene un método getNombre() para el log
                                        Log.d("LoadPets", "Mascota cargada: " + (mascota.getNombre() != null ? mascota.getNombre() : "NombreDesconocido") + " (ID: " + petKey + ")");
                                    } catch (Exception e) {
                                        Log.e("LoadPets", "Error al llamar a setId en Mascota. ¿Existe el método setId(String id)? Clave: " + petKey, e);
                                        // Puedes decidir si continuar o mostrar un error más genérico
                                    }

                                } else {
                                    Log.w("LoadPets", "Error al deserializar mascota o clave nula para el nodo con clave: " + petKey);
                                }
                            }
                        }
                        // Pasar la lista (puede estar vacía) y el deviceId al diálogo
                        showPetSelectionDialog(mascotas, deviceId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Mostrar error más detallado
                        Log.e("LoadPets", "Error de Firebase al cargar mascotas para usuario " + currentUserId, error.toException());
                        if (getContext() != null) { // Verificar contexto antes de mostrar Toast
                            Toast.makeText(getContext(), "Error al cargar mascotas: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showPetSelectionDialog(List<Mascota> mascotas, final String deviceId) {
        if (getContext() == null) { // Evitar crash si el fragmento se destruye
            Log.w("PetSelectDialog", "Contexto nulo, no se puede mostrar el diálogo.");
            return;
        }

        if (mascotas.isEmpty()) {
            // Mensaje más útil
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sin Mascotas")
                    .setMessage("No tienes mascotas registradas. Ve a la sección de Mascotas para añadir una.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Asociar a mascota");

        // Inflar el layout del diálogo (asegúrate que R.layout.dialog_select_pet existe y tiene R.id.pets_recycler_view)
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_pet, null);
        RecyclerView petsRecyclerView = dialogView.findViewById(R.id.pets_recycler_view);
        petsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Capturar el diálogo para poder cerrarlo desde la lambda del adapter
        final AlertDialog selectionDialog = builder.setView(dialogView)
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss()) // Botón cancelar estándar
                .create(); // Crear el diálogo aquí

        // --- Adapter ---
        // Asegúrate que tu PetSelectionAdapter esté implementado correctamente
        // y que su listener (el segundo argumento) se llame al hacer clic en un item,
        // pasando el objeto Mascota correspondiente a esa posición.
        PetSelectionAdapter adapter = new PetSelectionAdapter(mascotas, mascotaSeleccionada -> {
            // Esta lambda se ejecuta cuando se hace clic en una mascota en el adapter
            if (mascotaSeleccionada != null && mascotaSeleccionada.getId() != null && !mascotaSeleccionada.getId().isEmpty()) {
                String selectedPetId = mascotaSeleccionada.getId();
                Log.d("PetSelectDialog", "Mascota seleccionada: " + (mascotaSeleccionada.getNombre() != null ? mascotaSeleccionada.getNombre() : "NombreDesconocido") + " (ID: " + selectedPetId + "), para asociar a DeviceID: " + deviceId);
                // Llamar a la función de asociación
                associatePetWithDevice(selectedPetId, deviceId);
                selectionDialog.dismiss(); // <<< CERRAR EL DIÁLOGO DESPUÉS DE SELECCIONAR
            } else {
                Log.e("PetSelectDialog", "Error: Mascota seleccionada es nula o no tiene ID válido.");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error al seleccionar mascota.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        petsRecyclerView.setAdapter(adapter);

        selectionDialog.show(); // Mostrar el diálogo
    }

    private void associatePetWithDevice(final String petId, final String deviceId) {
        // 1. Validar IDs de entrada
        if (petId == null) {
            Log.e("AssociatePet", "Error crítico: petId es nulo.");
            if (getContext() != null)
                Toast.makeText(getContext(), "Error interno [petId null].", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e("AssociatePet", "Error crítico: deviceId es nulo o vacío.");
            if (getContext() != null)
                Toast.makeText(getContext(), "Error interno [deviceId null].", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Verificar autenticación
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("AssociatePet", "Fallo de asociación: Usuario no autenticado.");
            if (getContext() != null)
                Toast.makeText(getContext(), "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }

        final String currentUserId = user.getUid();

        // 3. Referencia
        final DatabaseReference assocRef = mDatabase.child("usuarios")
                .child(currentUserId)
                .child("dispensadores")
                .child(deviceId)
                .child("asignado_a");

        // 4. Ejecutar escritura
        assocRef.setValue(petId)
                .addOnSuccessListener(aVoid -> {
                    Log.i("AssociatePet", "¡Asociación exitosa!");
                    if (getContext() != null) {
                        String msg = petId.isEmpty() ?
                                "Mascota desasociada del dispensador." :
                                "Mascota asociada correctamente.";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }


                    loadUserDevices(currentUserId);


                })
                .addOnFailureListener(e -> {
                    Log.e("AssociatePet", "¡Fallo al asociar! Error: " + e.getMessage(), e);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error al asociar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }



    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Vincular nuevo dispensador");
        builder.setMessage("Ingresa el ID del dispositivo (Ej: DISP_001)");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("DISP_XXX");
        builder.setView(input);

        builder.setPositiveButton("Vincular", null);
        builder.setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String deviceId = input.getText().toString().trim();
                if (!isValidDeviceId(deviceId)) {
                    input.setError("Formato inválido. Use DISP_XXX");
                    return;
                }
                checkAndLinkDevice(deviceId);
                dialog.dismiss();
            });
        });
        dialog.show();
    }
    private boolean checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return false;
            }
        }
        return true;
    }
    private boolean isValidDeviceId(String deviceId) {
        return deviceId.matches("DISP_\\d{3}");
    }

    private void checkAndLinkDevice(String deviceId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        mDatabase.child("dispositivos").child(deviceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            verifyAndLinkDevice(user.getUid(), deviceId, snapshot);
                        } else {
                            Toast.makeText(getContext(), "Dispositivo no registrado", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifyAndLinkDevice(String userId, String deviceId, DataSnapshot deviceData) {
        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            linkDeviceToUser(userId, deviceId, deviceData);
                        } else {
                            Toast.makeText(getContext(), "Dispositivo ya vinculado", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error al verificar", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void linkDeviceToUser(String userId, String deviceId, DataSnapshot deviceData) {
        Map<String, Object> dispData = new HashMap<>();
        Map<String, Object> config = new HashMap<>();

        config.put("modo", "auto");
        config.put("ultima_actualizacion", ServerValue.TIMESTAMP);

        dispData.put("configuracion", config);
        dispData.put("estado", "desconectado");
        dispData.put("asignado_a", "");

        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .setValue(dispData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "¡Dispensador vinculado!", Toast.LENGTH_SHORT).show();
                    loadUserDevices(userId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al vincular: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firebase", "Error al vincular dispositivo", e);
                });
    }

    private void loadUserName(String userId) {
        mDatabase.child("usuarios").child(userId).child("informacion").child("nombre")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String nombre = snapshot.getValue(String.class);
                            titleDispositivos.setText("Dispositivos de " + capitalizarNombre(nombre));
                        } else {
                            titleDispositivos.setText("Dispositivos");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error al obtener nombre", error.toException());
                        titleDispositivos.setText("Dispositivos");
                    }
                });
    }

    private String capitalizarNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) return "";

        String[] palabras = nombre.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                sb.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}