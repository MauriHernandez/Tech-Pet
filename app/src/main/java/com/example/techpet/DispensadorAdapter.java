package com.example.techpet;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DispensadorAdapter extends RecyclerView.Adapter<DispensadorAdapter.DispensadorViewHolder> {
    private final List<Dispensador> dispensadores;
    private final OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(String deviceId);
        void onConfigClick(String deviceId);

    }

    public DispensadorAdapter(List<Dispensador> dispensadores, OnDeviceClickListener listener) {
        this.dispensadores = new ArrayList<>(dispensadores != null ? dispensadores : Collections.emptyList());
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public DispensadorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dispensador, parent, false);
        return new DispensadorViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DispensadorViewHolder holder, int position) {
        Dispensador disp = dispensadores.get(position);
        holder.bind(disp);
    }

    @Override
    public int getItemCount() {
        return dispensadores.size();
    }

    public void updateList(List<Dispensador> newList) {
        List<Dispensador> filteredList = newList != null ? newList : Collections.emptyList();

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DispensadorDiffCallback(
                this.dispensadores, filteredList));

        this.dispensadores.clear();
        this.dispensadores.addAll(filteredList);
        result.dispatchUpdatesTo(this);
    }

    private static class DispensadorDiffCallback extends DiffUtil.Callback {
        private final List<Dispensador> oldList;
        private final List<Dispensador> newList;

        DispensadorDiffCallback(List<Dispensador> oldList, List<Dispensador> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    static class DispensadorViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "DispensadorViewHolder";
        private static final int DEFAULT_PROGRESS = 0;

        private final TextView deviceName;
        private final TextView tvEstado;
        private final TextView tvTemp;
        private final TextView tvHum;
        private final ProgressBar progressAgua;
        private final ProgressBar progressComida;
        private final ImageButton btnConfig;

        private final TextView tvNivelAgua;
        private final TextView tvNivelComida;
        private final Context context;

        public DispensadorViewHolder(@NonNull View itemView, OnDeviceClickListener listener) {
            super(itemView);
            context = itemView.getContext();

            // Inicializar vistas
            deviceName = itemView.findViewById(R.id.device_name);
            tvEstado = itemView.findViewById(R.id.tvEstadoConexion);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvHum = itemView.findViewById(R.id.tvHum);
            progressAgua = itemView.findViewById(R.id.progressAgua);
            progressComida = itemView.findViewById(R.id.progressComida);
            btnConfig = itemView.findViewById(R.id.btnConfig);
            tvNivelAgua = itemView.findViewById(R.id.tvNivelAgua);
            tvNivelComida = itemView.findViewById(R.id.tvNivelComida);

            // Configurar listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeviceClick(deviceName.getTag().toString());
                }
            });

            btnConfig.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onConfigClick(deviceName.getTag().toString());
                }
            });


        }

        void bind(Dispensador disp) {
            if (disp == null) {
                return;
            }

            // Guardar ID en el tag de deviceName para acceso en los clicks
            deviceName.setTag(disp.getId());

            // Configurar nombre y estado
            setupBasicInfo(disp);

            // Configurar datos de sensores
            setupSensorData(disp);
        }

        private void setupBasicInfo(Dispensador disp) {
            // Nombre del dispositivo
            String displayName = disp.getModelo() != null ? disp.getModelo() : disp.getId();
            deviceName.setText(displayName);

            // Estado de conexión
            String estado = disp.getConexion() != null ? disp.getConexion() : "desconectado";
            boolean estaConectado = estado.equalsIgnoreCase("conectado");

            tvEstado.setText(estaConectado ?
                    context.getString(R.string.estado_conectado) :
                    context.getString(R.string.estado_desconectado));

            tvEstado.setTextColor(ContextCompat.getColor(context,
                    estaConectado ? R.color.green : R.color.red));
        }

        private void setupSensorData(Dispensador disp) {
            try {
                Map<String, Object> sensores = (Map<String, Object>) disp.getSensores();
                if (sensores == null) {
                    Log.w(TAG, "Sensores es null");
                    return;
                }

                Log.d(TAG, "Sensores recibidos: " + sensores.toString());

                // Procesar datos de ambiente
                if (sensores.containsKey("ambiente")) {
                    Object ambienteObj = sensores.get("ambiente");
                    Log.d(TAG, "Contenido de 'ambiente': " + (ambienteObj != null ? ambienteObj.toString() : "null"));
                    if (ambienteObj instanceof Map) {
                        Map<String, Object> ambiente = (Map<String, Object>) ambienteObj;

                        String tempText = "--";
                        String humText = "--";

                        if (ambiente.containsKey("temperatura") && ambiente.get("temperatura") instanceof Number) {
                            float temp = ((Number) ambiente.get("temperatura")).floatValue();
                            tempText = String.format(Locale.getDefault(), context.getString(R.string.temperatura_formato), temp);
                            Log.d(TAG, "Temperatura: " + temp);
                        } else {
                            Log.d(TAG, "Temperatura no disponible");
                        }

                        if (ambiente.containsKey("humedad") && ambiente.get("humedad") instanceof Number) {
                            int hum = ((Number) ambiente.get("humedad")).intValue();
                            humText = String.format(Locale.getDefault(), context.getString(R.string.humedad_formato), hum);
                            Log.d(TAG, "Humedad: " + hum);
                        } else {
                            Log.d(TAG, "Humedad no disponible");
                        }

                        tvTemp.setText(tempText);
                        tvHum.setText(humText);
                    } else {
                        Log.w(TAG, "'ambiente' no es un Map");
                    }
                } else {
                    Log.w(TAG, "No se encontró clave 'ambiente' en sensores");
                }

                // Procesar datos de recipientes
                if (sensores.containsKey("recipientes")) {
                    Object recipientesObj = sensores.get("recipientes");
                    if (recipientesObj instanceof Map) {
                        Map<String, Object> recipientes = (Map<String, Object>) recipientesObj;
                        Log.d(TAG, "Datos de recipientes: " + recipientes.toString());

                        if (recipientes.containsKey("agua")) {
                            Object aguaObj = recipientes.get("agua");
                            if (aguaObj instanceof Map) {
                                Map<String, Object> aguaData = (Map<String, Object>) aguaObj;
                                int porcentajeAgua = calcularPorcentajeSeguro(aguaData);
                                progressAgua.setProgress(porcentajeAgua);
                                tvNivelAgua.setText(context.getString(R.string.nivel_agua_formato, porcentajeAgua));
                                Log.d(TAG, "Nivel de agua: " + porcentajeAgua + "%");
                            }
                        }

                        if (recipientes.containsKey("comida")) {
                            Object comidaObj = recipientes.get("comida");
                            if (comidaObj instanceof Map) {
                                Map<String, Object> comidaData = (Map<String, Object>) comidaObj;
                                int porcentajeComida = calcularPorcentajeSeguro(comidaData);
                                progressComida.setProgress(porcentajeComida);
                                tvNivelComida.setText(context.getString(R.string.nivel_comida_formato, porcentajeComida));
                                Log.d(TAG, "Nivel de comida: " + porcentajeComida + "%");
                            }
                        }
                    } else {
                        Log.w(TAG, "'recipientes' no es un Map");
                    }
                } else {
                    Log.w(TAG, "No se encontró clave 'recipientes' en sensores");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error procesando sensores", e);
            }
        }

        private int calcularPorcentajeSeguro(Map<String, Object> data) {
            try {
                Object nivelObj = data.get("peso_actual");
                Object capacidadObj = data.get("capacidad");

                if (!(nivelObj instanceof Number) || !(capacidadObj instanceof Number)) {
                    return DEFAULT_PROGRESS;
                }

                float nivel = ((Number) nivelObj).floatValue();
                float capacidad = ((Number) capacidadObj).floatValue();

                if (capacidad <= 0) return DEFAULT_PROGRESS;

                return Math.round((nivel / capacidad) * 100);
            } catch (Exception e) {
                Log.e(TAG, "Error calculando porcentaje", e);
                return DEFAULT_PROGRESS;
            }
        }
    }
}
