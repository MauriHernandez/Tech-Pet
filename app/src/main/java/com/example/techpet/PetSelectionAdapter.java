package com.example.techpet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PetSelectionAdapter extends RecyclerView.Adapter<PetSelectionAdapter.ViewHolder> {

    private List<Mascota> mascotas;
    private OnPetSelectedListener listener;

    public interface OnPetSelectedListener {
        void onPetSelected(Mascota mascota);
    }

    public PetSelectionAdapter(List<Mascota> mascotas, OnPetSelectedListener listener) {
        this.mascotas = mascotas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mascota mascota = mascotas.get(position);

        holder.petName.setText(mascota.getNombre());
        holder.petType.setText(mascota.getTipo());

        // Configurar imagen si es necesario
        // Glide.with(holder.itemView.getContext()).load(mascota.getImagenUrl()).into(holder.petImage);

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPetSelected(mascota);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mascotas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView petName, petType;
        ImageView petImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_pet);
            petName = itemView.findViewById(R.id.pet_name);
            petType = itemView.findViewById(R.id.pet_type);
            petImage = itemView.findViewById(R.id.pet_image);
        }
    }
}