package com.aleynasahin.memorymap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.aleynasahin.memorymap.databinding.ActivityMemoryDetailsBinding;
import com.aleynasahin.memorymap.model.Memory;
import com.aleynasahin.memorymap.roomdb.MemoryDao;
import com.aleynasahin.memorymap.roomdb.MemoryDatabase;
import com.aleynasahin.memorymap.view.FullScreenImageActivity;

import java.io.File;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MemoryDetailsActivity extends AppCompatActivity {
    private ActivityMemoryDetailsBinding binding;

    MemoryDatabase db;
    MemoryDao memoryDao;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    String photoUriString;
    Memory memory;


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
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String note = intent.getStringExtra("note");
        binding.textTitle.setText(title);
        binding.textNote.setText(note);

        db= Room.databaseBuilder(getApplicationContext(),MemoryDatabase.class,"Memories").build();
        memoryDao = db.memoryDao();

        getMemoryFromDatabase(title, note);
        binding.imageMemory.setOnClickListener(v -> {
            if (memory != null && memory.photoUri != null && !memory.photoUri.isEmpty()) {
                Intent intentPhoto = new Intent(this, FullScreenImageActivity.class);
                intentPhoto.putExtra("photoUri", memory.photoUri);
                startActivity(intentPhoto);
            }
        });






    }
    private void getMemoryFromDatabase(String title, String note) {
        compositeDisposable.add(
                memoryDao.getMemoryByTitleAndNote(title, note)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(memory -> {
                            this.memory = memory;
                            showMemoryDetails(memory);
                        }, throwable -> {

                            binding.textCoordinates.setText("Koordinatlar yüklenemedi");
                            showPlaceholderImage();
                            Toast.makeText(this, "Veri yüklenirken hata", Toast.LENGTH_SHORT).show();
                        })
        );

    }
    private void showMemoryDetails(Memory memory) {

        String coordinates = String.format("Koordinatlar: %.6f, %.6f",
                memory.memoryLatitude, memory.memoryLongitude);
        binding.textCoordinates.setText(coordinates);


        showPhoto(memory.photoUri);
    }

    private void showPhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            showPlaceholderImage();
            return;
        }

        try {
            if (photoPath.startsWith("content://")) {

                Uri uri = Uri.parse(photoPath);
                binding.imageMemory.setImageURI(uri);

            } else {

                File imgFile = new File(photoPath);
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    binding.imageMemory.setImageBitmap(bitmap);
                } else {
                    showPlaceholderImage();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showPlaceholderImage();
            Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPlaceholderImage() {
        binding.imageMemory.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        binding.imageMemory.setScaleType(android.widget.ImageView.ScaleType.CENTER);


        binding.imageMemory.setImageResource(android.R.drawable.ic_menu_camera);

        binding.imageMemory.setContentDescription("Fotoğraf yok");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}