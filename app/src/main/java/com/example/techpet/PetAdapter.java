package com.example.techpet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private List<Mascota> petList;
    private Context context;

    // Interfaz para manejar clics
    public interface OnItemClickListener {
        void onItemClick(Mascota mascota);
    }

    private OnItemClickListener listener;

    public PetAdapter(List<Mascota> petList, Context context, OnItemClickListener listener) {
        this.petList = petList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Mascota pet = petList.get(position);

        holder.tvName.setText(pet.getNombre());
        holder.tvBreed.setText(pet.getRaza());
        holder.tvType.setText(pet.getTipo());

        if (pet.getFotoUrl() != null && !pet.getFotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(pet.getFotoUrl())
                    .into(holder.ivPet);
        }

        // Manejamos el clic en la tarjeta
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPet;
        TextView tvName, tvBreed, tvType;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPet = itemView.findViewById(R.id.ivPet);
            tvName = itemView.findViewById(R.id.tvName);
            tvBreed = itemView.findViewById(R.id.tvBreed);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }
}
