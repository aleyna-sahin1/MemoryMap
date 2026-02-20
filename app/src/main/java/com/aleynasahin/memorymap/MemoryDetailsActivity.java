package com.aleynasahin.memorymap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aleynasahin.memorymap.Firestore.FirestoreRepository;
import com.aleynasahin.memorymap.databinding.ActivityMemoryDetailsBinding;
import com.aleynasahin.memorymap.model.Memory;
import com.aleynasahin.memorymap.view.FullScreenImageActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MemoryDetailsActivity extends AppCompatActivity {

    private ActivityMemoryDetailsBinding binding;
    private Memory currentMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMemoryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String title = getIntent().getStringExtra("title");
        String note = getIntent().getStringExtra("note");

        binding.textTitle.setText(title);
        binding.textNote.setText(note);

        getMemoryFromFirestore(title, note);

        binding.imageMemory.setOnClickListener(v -> {
            if (currentMemory != null && currentMemory.photoUri != null && !currentMemory.photoUri.isEmpty()) {
                Intent intent = new Intent(this, FullScreenImageActivity.class);
                intent.putExtra("photoUri", currentMemory.photoUri);
                startActivity(intent);
            }
        });
    }

    private void getMemoryFromFirestore(String title, String note) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference memoriesRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("memories");

        memoriesRef
                .whereEqualTo("memoryTitle", title)
                .whereEqualTo("memoryNote", note)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        currentMemory = new Memory(
                                doc.getString("memoryTitle"),
                                doc.getString("memoryNote"),
                                doc.getDouble("memoryLatitude"),
                                doc.getDouble("memoryLongitude"),
                                doc.getString("photoUri")
                        );
                        currentMemory.firestoreId = doc.getId();
                        showMemoryDetails(currentMemory);
                    } else {
                        Toast.makeText(this, "Anı bulunamadı", Toast.LENGTH_SHORT).show();
                        showPlaceholderImage();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Veri yüklenirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showPlaceholderImage();
                });
    }

    private void showMemoryDetails(Memory memory) {
        String coordinates = String.format("Koordinatlar: %.6f, %.6f",
                memory.memoryLatitude, memory.memoryLongitude);
        binding.textCoordinates.setText(coordinates);
        showPhoto(memory.photoUri);
    }

    private void showPhoto(String photoUri) {
        if (photoUri == null || photoUri.isEmpty()) {
            showPlaceholderImage();
            return;
        }

        if (photoUri.startsWith("https://")) {
            Glide.with(this)
                    .load(photoUri)
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .centerCrop()
                    .into(binding.imageMemory);

        } else if (photoUri.startsWith("content://")) {
            binding.imageMemory.setImageURI(Uri.parse(photoUri));

        } else {
            showPlaceholderImage();
        }
    }


    private void showPlaceholderImage() {
        binding.imageMemory.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        binding.imageMemory.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        binding.imageMemory.setImageResource(android.R.drawable.ic_menu_camera);
        binding.imageMemory.setContentDescription("Fotoğraf yok");
    }
}