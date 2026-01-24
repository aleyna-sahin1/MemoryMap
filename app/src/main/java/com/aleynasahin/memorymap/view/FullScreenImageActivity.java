package com.aleynasahin.memorymap.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aleynasahin.memorymap.R;
import com.aleynasahin.memorymap.databinding.ActivityFullScreenImageBinding;

import java.io.File;

public class FullScreenImageActivity extends AppCompatActivity {
    private ActivityFullScreenImageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding= ActivityFullScreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String photoUri = getIntent().getStringExtra("photoUri");

        if (photoUri == null) return;

        if (photoUri.startsWith("content://")) {
            binding.imageFullScreen.setImageURI(Uri.parse(photoUri));
        } else {
            File imgFile = new File(photoUri);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                binding.imageFullScreen.setImageBitmap(bitmap);
            }
        }


        binding.imageFullScreen.setOnClickListener(v -> finish());
    }

    }

