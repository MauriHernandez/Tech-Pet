package com.example.techpet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DispensadorAdapter extends RecyclerView.Adapter<DispensadorAdapter.DispensadorViewHolder> {
    private List<Dispensador> dispensadores;

    public DispensadorAdapter(List<Dispensador> dispensadores) {
        this.dispensadores = dispensadores;
    }

    @NonNull
    @Override
    public DispensadorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dispensador, parent, false);
        return new DispensadorViewHolder(view);
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

    static class DispensadorViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private ImageView deviceImage;
        private ProgressBar waterProgress, foodProgress;
        private Button connectionButton;

        public DispensadorViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceImage = itemView.findViewById(R.id.device_image);
            waterProgress = itemView.findViewById(R.id.water_progress);
            foodProgress = itemView.findViewById(R.id.food_progress);
            connectionButton = itemView.findViewById(R.id.connection_button);
        }

        public void bind(Dispensador disp) {
            deviceName.setText(disp.getModelo());
            connectionButton.setText(disp.isStatus() ? "Conectado" : "Desconectado");
            connectionButton.setBackgroundTintList(ColorStateList.valueOf(
                    disp.isStatus() ? Color.GREEN : Color.RED));

        }
    }
}