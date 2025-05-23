package com.example.techpet;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.util.Log;
public class PetFragment extends Fragment implements PetAdapter.OnItemClickListener {


    private TextView titulo;
    private ImageView imageViewPet;
    private TextInputEditText etPetName, etBirthDate, etBreed, etColor;
    private RadioGroup rgPetType, rgGender;
    private Button btnSelectPhoto, btnSave, btnCancel;
    private Uri selectedImageUri;
    private FloatingActionButton fabAddPet;
    private RecyclerView rvPets;
    private MaterialCardView cardForm;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private String userId;

    private PetAdapter petAdapter;
    private List<Mascota> petList = new ArrayList<Mascota>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference("pet_images");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pet, container, false);

        initializeViews(view);
        setupListeners();
        setupRecyclerView();
        loadPets();

        return view;
    }

    private void initializeViews(View view) {
        titulo = view.findViewById(R.id.titleTextView);
        etPetName = view.findViewById(R.id.etPetName);
        etBirthDate = view.findViewById(R.id.etBirthDate);
        etBreed = view.findViewById(R.id.etBreed);
        etColor = view.findViewById(R.id.etColor);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        fabAddPet = view.findViewById(R.id.fabAddPet);
        rvPets = view.findViewById(R.id.rvPets);
        cardForm = view.findViewById(R.id.cardForm);

        rgPetType = view.findViewById(R.id.rgPetType);
        rgGender = view.findViewById(R.id.rgGender);
    }

    private void setupRecyclerView() {
        petAdapter = new PetAdapter(petList, getContext(), this);
        rvPets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPets.setAdapter(petAdapter);
    }

    // Cambiar la forma de cargar las mascotas en loadPets()
    private void loadPets() {
        if (userId == null) return;

        mDatabase.child("usuarios").child(userId).child("mascotas")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        petList.clear();
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            DataSnapshot infoSnapshot = dataSnapshot.child("informacion");
                            Mascota pet = infoSnapshot.getValue(Mascota.class);
                            if (pet != null) {
                                pet.setId(dataSnapshot.getKey());
                                petList.add(pet);
                            }
                        }
                        petAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PetFragment", "Error al cargar mascotas", error.toException());
                        Toast.makeText(getContext(), "Error al cargar mascotas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupListeners() {
        if (btnSelectPhoto != null) {
            btnSelectPhoto.setOnClickListener(v -> openGallery());
        }

        if (etBirthDate != null) {
            etBirthDate.setOnClickListener(v -> showDatePickerDialog());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> savePetData());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                cardForm.setVisibility(View.GONE);
                closeForm();
            });
        }

        if (fabAddPet != null) {
            fabAddPet.setOnClickListener(v -> {
                if (cardForm.getVisibility() == View.VISIBLE) {
                    cardForm.setVisibility(View.GONE);
                } else {
                    cardForm.setVisibility(View.VISIBLE);
                    if (rvPets != null) {
                        rvPets.smoothScrollToPosition(0);
                    }
                }
            });
        }
    }

    private void closeForm() {
        cardForm.setVisibility(View.GONE);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageViewPet.setImageURI(selectedImageUri);
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    etBirthDate.setText(formattedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void savePetData() {
        String name = etPetName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String color = etColor.getText().toString().trim();

        int selectedPetTypeId = rgPetType.getCheckedRadioButtonId();
        if (selectedPetTypeId == -1) {
            Toast.makeText(getContext(), "Selecciona el tipo de mascota", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedPetType = getView().findViewById(selectedPetTypeId);
        String petType = selectedPetType.getText().toString().toLowerCase();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(getContext(), "Selecciona el género", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedGender = getView().findViewById(selectedGenderId);
        String gender = selectedGender.getText().toString();

        if (name.isEmpty() || birthDate.isEmpty() || breed.isEmpty() || color.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadImageAndSavePet(name, birthDate, petType, breed, gender, color);
        } else {
            savePetToDatabase(null, name, birthDate, petType, breed, gender, color);
        }
    }

    private void uploadImageAndSavePet(String name, String birthDate, String petType, String breed, String gender, String color) {
        String imageName = UUID.randomUUID().toString();
        StorageReference fileReference = mStorageRef.child(imageName);

        fileReference.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileReference.getDownloadUrl().addOnSuccessListener(uri ->
                                savePetToDatabase(uri.toString(), name, birthDate, petType, breed, gender, color)
                        )
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void savePetToDatabase(String imageUrl, String name, String birthDate, String petType, String breed, String gender, String color) {
        String petId = mDatabase.child("usuarios").child(userId).child("mascotas").push().getKey();

        // Nueva estructura anidada
        Map<String, Object> petData = new HashMap<>();
        Map<String, Object> informacion = new HashMap<>();
        informacion.put("nombre", name);
        informacion.put("nacimiento", birthDate); // Cambiado de fechaNacimiento a nacimiento
        informacion.put("tipo", petType);
        informacion.put("raza", breed);
        informacion.put("genero", gender);
        informacion.put("color", color);

        petData.put("informacion", informacion);

        if (imageUrl != null) {
            // Agregar fotoUrl dentro de informacion si es necesario
            informacion.put("fotoUrl", imageUrl);
        }

        mDatabase.child("usuarios").child(userId).child("mascotas").child(petId)
                .updateChildren(petData) // Cambiado de setValue a updateChildren
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mascota registrada con éxito", Toast.LENGTH_SHORT).show();
                    resetForm();
                    cardForm.setVisibility(View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    private void resetForm() {
        etPetName.setText("");
        etBirthDate.setText("");
        etBreed.setText("");
        etColor.setText("");
        rgPetType.clearCheck();
        rgGender.clearCheck();
        selectedImageUri = null;
    }

    @Override
    public void onItemClick(Mascota mascota) {
        // Navegar al PetDetailFragment
        PetDetailFragment detailFragment = PetDetailFragment.newInstance(mascota);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}
