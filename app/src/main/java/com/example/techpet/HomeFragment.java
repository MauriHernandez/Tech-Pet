package com.example.techpet;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.annotation.NonNull;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HomeFragment extends Fragment {
    private TextView titleDispositivos;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RecyclerView devicesRecyclerView;
    private TextView noDevicesText;

    private ImageView plus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        titleDispositivos = view.findViewById(R.id.title_dispositivos);
        devicesRecyclerView = view.findViewById(R.id.resource_recycler_view);
        noDevicesText = view.findViewById(R.id.no_devices_text);
        plus = view.findViewById(R.id.plusImage);
        // Firebase, no lo borres
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //  datos del usuario
        loadUserData();

        return view;
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            mDatabase.child("usuarios").child(userId).child("info").child("nombre")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {

                                String nombre = snapshot.getValue(String.class);
                                updateTitle(nombre);
                            } else {
                                String nombre = currentUser.getDisplayName();
                                if (nombre != null && !nombre.isEmpty()) {
                                    saveUserName(userId, nombre);
                                    updateTitle(nombre);
                                } else {
                                    updateTitle("Usuario");
                                }
                            }
                            loadUserDevices(userId);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("Firebase", "Error loading name", error.toException());
                        }
                    });
        }
    }

    private void updateTitle(String nombre) {
        getActivity().runOnUiThread(() -> {
            titleDispositivos.setText("DISPOSITIVOS DE " + nombre.toUpperCase());
        });
    }

    private void saveUserName(String userId, String nombre) {
        mDatabase.child("usuarios").child(userId).child("info").child("nombre")
                .setValue(nombre)
                .addOnFailureListener(e -> Log.e("Firebase", "Error saving name", e));
    }

    private void loadUserDevices(String userId) {
        mDatabase.child("usuarios").child(userId).child("dispensadores")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            noDevicesText.setVisibility(View.GONE);

                            List<Dispensador> dispensadores = new ArrayList<>();
                            for (DataSnapshot dispSnapshot : snapshot.getChildren()) {
                                Dispensador disp = dispSnapshot.getValue(Dispensador.class);
                                disp.setId(dispSnapshot.getKey());
                                dispensadores.add(disp);
                            }
                            setupRecyclerView(dispensadores);
                        } else {
                            noDevicesText.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error loading devices", error.toException());
                    }
                });
    }

    private void setupRecyclerView(List<Dispensador> dispensadores) {
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        DispensadorAdapter adapter = new DispensadorAdapter(dispensadores);
        devicesRecyclerView.setAdapter(adapter);
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Vincular nuevo dispensador");
        builder.setMessage("Ingresa el ID de 6 dígitos del dispositivo (Ej: DISP_001)");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("DISP_XXX");
        builder.setView(input);

        builder.setPositiveButton("Vincular", (dialog, which) -> {
            String deviceId = input.getText().toString().trim();
            if (isValidDeviceId(deviceId)) {
                checkAndLinkDevice(deviceId);
            } else {
                input.setError("Formato inválido. Use DISP_XXX");
            }
        });
        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String deviceId = input.getText().toString().trim();
                if (!isValidDeviceId(deviceId)) {
                    input.setError("Formato: DISP_001 a DISP_999");
                    return;
                }
                positiveButton.setEnabled(false);
                checkAndLinkDevice(deviceId);
                dialog.dismiss();
            });
        });
        dialog.show();
        builder.show();
    }


    private void validateDeviceId(String deviceId) {
        if (deviceId.length() != 6) {
            Toast.makeText(getContext(), "El ID debe tener exactamente 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        mDatabase.child("dispensadores").child(deviceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            linkDeviceToUser(currentUser.getUid(), deviceId, snapshot);
                        } else {
                            Toast.makeText(getContext(), "No se encontró el dispensador", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void linkDeviceToUser(String userId, String deviceId, DataSnapshot deviceData) {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("modelo", deviceData.child("modelo").getValue());
        deviceInfo.put("marca", deviceData.child("marca").getValue());
        deviceInfo.put("status", false);
        mDatabase.child("usuarios").child(userId).child("dispensadores").child(deviceId)
                .setValue(deviceInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "¡Dispensador vinculado!", Toast.LENGTH_SHORT).show();
                    loadUserDevices(userId);
                    addToDeviceHistory(deviceId, "Vinculado a usuario");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al vincular: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addToDeviceHistory(String deviceId, String action) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("fecha", ServerValue.TIMESTAMP);
        historyEntry.put("accion", action);
        historyEntry.put("usuario", user.getUid());

        mDatabase.child("usuarios").child(user.getUid())
                .child("dispensadores").child(deviceId)
                .child("historial").child(timestamp)
                .setValue(historyEntry);
    }

    private boolean isValidDeviceId(String deviceId) {
        return deviceId.matches("DISP_\\d{3}");
    }

    private void checkAndLinkDevice(String deviceId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        mDatabase.child("usuarios").child(user.getUid()).child("dispensadores").child(deviceId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(getContext(), "Ya tienes este dispositivo vinculado", Toast.LENGTH_SHORT).show();
                        } else {
                            checkDeviceAvailability(user.getUid(), deviceId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkDeviceAvailability(String userId, String deviceId) {
        mDatabase.child("usuarios").orderByChild("dispensadores/" + deviceId).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                DataSnapshot deviceSnapshot = userSnapshot.child("dispensadores").child(deviceId);
                                linkDeviceToUser(userId, deviceId, deviceSnapshot);
                            }
                        } else {
                            Toast.makeText(getContext(), "Dispositivo no registrado", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error de búsqueda", Toast.LENGTH_SHORT).show();
                    }
                });


    }
}