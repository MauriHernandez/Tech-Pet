    package com.example.techpet;
    
    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;
    
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.fragment.app.Fragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    
    import com.google.android.material.card.MaterialCardView;
    import com.google.android.material.textfield.TextInputEditText;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    
    import java.io.Serializable;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import android.util.Log;
    
    
    public class PetDetailFragment extends Fragment {
    
        private static final String ARG_PET = "pet";
    
        // Views
        private TextView tvPetName, tvRecommendations, tvCurrentWeight, tvCurrentHeight;
        private RecyclerView rvMetrics;
        private Button btnAddMetric, btnSaveMetric;
        private MaterialCardView cardForm;
        private TextInputEditText etWeight, etHeight;
    
        // Datos
        private Mascota currentPet;
        private MetricAdapter metricAdapter;
        private List<PetMetric> metricList = new ArrayList<>();
    
        public static PetDetailFragment newInstance(Mascota pet) {
            PetDetailFragment fragment = new PetDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_PET, pet);
            fragment.setArguments(args);
            return fragment;
        }
    
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pet_detail, container, false);
        }
    
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
    
            // Obtener mascota de los argumentos
            if (getArguments() != null) {
                currentPet = (Mascota) getArguments().getSerializable(ARG_PET);
            }
    
            initViews(view);
            setupRecyclerView();
            loadMetrics();
    
            // Listeners
            btnAddMetric.setOnClickListener(v -> toggleForm());
            btnSaveMetric.setOnClickListener(v -> saveMetric());
        }
    
        private void initViews(View view) {
            tvPetName = view.findViewById(R.id.tvPetName);
            tvRecommendations = view.findViewById(R.id.tvRecommendations);
            tvCurrentWeight = view.findViewById(R.id.tvCurrentWeight);
            tvCurrentHeight = view.findViewById(R.id.tvCurrentHeight);
            rvMetrics = view.findViewById(R.id.rvMetrics);
            btnAddMetric = view.findViewById(R.id.btnAddMetric);
            cardForm = view.findViewById(R.id.cardForm);
            etWeight = view.findViewById(R.id.etWeight);
            etHeight = view.findViewById(R.id.etHeight);
            btnSaveMetric = view.findViewById(R.id.btnSaveMetric);
    
            // Mostrar nombre de la mascota
            tvPetName.setText(currentPet.getNombre());
        }
    
        private void setupRecyclerView() {
            metricAdapter = new MetricAdapter(metricList);
            rvMetrics.setLayoutManager(new LinearLayoutManager(getContext()));
            rvMetrics.setAdapter(metricAdapter);
        }
    
        private void toggleForm() {
            if (cardForm.getVisibility() == View.VISIBLE) {
                cardForm.setVisibility(View.GONE);
                btnAddMetric.setText("Añadir Registro");
            } else {
                cardForm.setVisibility(View.VISIBLE);
                btnAddMetric.setText("Cancelar");
                // Limpiar campos al mostrar el formulario
                etWeight.setText("");
                etHeight.setText("");
            }
        }
    
        private void loadMetrics() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || currentPet == null || currentPet.getId() == null) {
                Log.e("PetDetail", "Usuario o mascota no válidos");
                Toast.makeText(getContext(), "No se puede cargar métricas", Toast.LENGTH_SHORT).show();
                return;
            }
    
            String userId = user.getUid();
            DatabaseReference metricsRef = FirebaseDatabase.getInstance().getReference()
                    .child("usuarios")
                    .child(userId)
                    .child("mascotas")
                    .child(currentPet.getId())
                    .child("medidas")
                    .child("historico");
    
            metricsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    metricList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        PetMetric metric = dataSnapshot.getValue(PetMetric.class);
                        if (metric != null) {
                            metricList.add(metric);
                        }
                    }
    
                    metricList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    metricAdapter.notifyDataSetChanged();
                    updateCurrentMetrics();
                    updateRecommendations();
                }
    
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error al cargar métricas", Toast.LENGTH_SHORT).show();
                }
            });
        }
    
    
        private void updateCurrentMetrics() {
            if (!metricList.isEmpty()) {
                PetMetric latest = metricList.get(0); // El más reciente
                tvCurrentWeight.setText(String.format(Locale.getDefault(), "Peso: %.1f kg", latest.getPeso()));
                tvCurrentHeight.setText(String.format(Locale.getDefault(), "Altura: %.1f cm", latest.getAltura()));
            } else {
                tvCurrentWeight.setText("Peso: -- kg");
                tvCurrentHeight.setText("Altura: -- cm");
            }
        }

        private void saveMetric() {
            String weightStr = etWeight.getText().toString().trim();
            String heightStr = etHeight.getText().toString().trim();

            if (weightStr.isEmpty() || heightStr.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double peso = Double.parseDouble(weightStr);
                double altura = Double.parseDouble(heightStr);

                // Crear mapa con estructura correcta
                Map<String, Object> nuevaMetrica = new HashMap<>();
                nuevaMetrica.put("timestamp", System.currentTimeMillis());
                nuevaMetrica.put("peso", peso); // Nota: Usamos "peso" y "altura" para que coincida con la BD
                nuevaMetrica.put("altura", altura);

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference petRef = FirebaseDatabase.getInstance().getReference()
                        .child("usuarios")
                        .child(userId)
                        .child("mascotas")
                        .child(currentPet.getId());

                // Actualización atómica con estructura correcta
                Map<String, Object> updates = new HashMap<>();
                // Generar una clave única para la nueva entrada en el historial
                String key = petRef.child("medidas").child("historico").push().getKey();
                updates.put("medidas/historico/" + key, nuevaMetrica);

                // **SE ELIMINAN ESTAS LÍNEAS para depender solo del historial**
                // updates.put("medidas/peso", peso);
                // updates.put("medidas/altura", altura);

                petRef.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Registro guardado", Toast.LENGTH_SHORT).show();
                            toggleForm();
                            // El updateCurrentMetrics se llamará desde el listener de carga (loadMetrics)
                            // cuando los datos se actualicen en Firebase.
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PetDetail", "Error al guardar métrica", e);
                            Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Ingresa valores numéricos válidos", Toast.LENGTH_SHORT).show();
            }
        }
    
        private void updateRecommendations() {
            if (currentPet.getTipo().equalsIgnoreCase("perro")) {
                if (!metricList.isEmpty()) {
                    PetMetric lastMetric = metricList.get(0);
                    String recommendation = generateDogRecommendation(lastMetric.getPeso(), currentPet.getRaza());
                    tvRecommendations.setText(recommendation);
                } else {
                    tvRecommendations.setText("Añade registros para obtener recomendaciones");
                }
            } else {
                tvRecommendations.setText("Recomendaciones específicas para " + currentPet.getTipo());
            }
        }
    
        private String generateDogRecommendation(double weight, String breed) {
            // Lógica mejorada por raza
            breed = breed.toLowerCase(Locale.ROOT);
    
            if (breed.contains("chihuahua")) {
                if (weight < 1) return "🚨 Muy bajo peso para un Chihuahua";
                if (weight > 3) return "⚠️ Sobrepeso para un Chihuahua";
                return "✅ Peso ideal para un Chihuahua";
            } else if (breed.contains("labrador") || breed.contains("golden")) {
                if (weight < 20) return "🚨 Muy bajo peso para esta raza";
                if (weight > 35) return "⚠️ Sobrepeso, necesita más ejercicio";
                return "✅ Peso saludable para un " + breed;
            } else if (breed.contains("pitbull")) {
                if (weight < 15) return "🚨 Peso muy bajo para un Pitbull";
                if (weight > 30) return "⚠️ Sobrepeso, ajusta su dieta";
                return "✅ Peso ideal para un Pitbull";
            } else {
                if (weight < 2) return "🚨 Peso muy bajo. Consulta al veterinario";
                if (weight > 30) return "⚠️ Sobrepeso. Más ejercicio y dieta";
                return "✅ Peso saludable. ¡Buen trabajo!";
            }
        }
    
    
        // Clase interna para el adaptador
        private static class MetricAdapter extends RecyclerView.Adapter<MetricAdapter.MetricViewHolder> {
    
            private final List<PetMetric> metricList;
    
            public MetricAdapter(List<PetMetric> metricList) {
                this.metricList = metricList;
            }
    
            @NonNull
            @Override
            public MetricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_metric, parent, false);
                return new MetricViewHolder(view);
            }
    
            @Override
            public void onBindViewHolder(@NonNull MetricViewHolder holder, int position) {
                PetMetric metric = metricList.get(position);
    
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                holder.tvDate.setText(sdf.format(new Date(metric.getTimestamp())));
                holder.tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", metric.getPeso()));
                holder.tvHeight.setText(String.format(Locale.getDefault(), "%.1f cm", metric.getAltura()));
            }
    
            @Override
            public int getItemCount() {
                return metricList.size();
            }
    
            static class MetricViewHolder extends RecyclerView.ViewHolder {
                TextView tvDate, tvWeight, tvHeight;
    
                MetricViewHolder(@NonNull View itemView) {
                    super(itemView);
                    tvDate = itemView.findViewById(R.id.tvDate);
                    tvWeight = itemView.findViewById(R.id.tvWeight);
                    tvHeight = itemView.findViewById(R.id.tvHeight);
                }
            }
        }
    
        // Modelo para las métricas
        public static class PetMetric implements Serializable {
            private long timestamp;
            private double peso;
            private double altura;
    
            public PetMetric() {
                // Constructor vacío necesario para Firebase
            }
    
            public PetMetric(long timestamp, double peso, double altura) {
                this.timestamp = timestamp;
                this.peso = peso;
                this.altura = altura;
            }
    
            public long getTimestamp() {
                return timestamp;
            }
    
            public double getPeso() {
                return peso;
            }
    
            public double getAltura() {
                return altura;
            }
        }
    }