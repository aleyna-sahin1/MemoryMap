package com.aleynasahin.memorymap;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.aleynasahin.memorymap.databinding.ActivityMapsBinding;
import com.aleynasahin.memorymap.databinding.DialogAddMemoryBinding;
import com.aleynasahin.memorymap.model.Memory;
import com.aleynasahin.memorymap.view.MainActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    FirebaseAuth mAuth;
    FirebaseFirestore firestore;
    CollectionReference memoriesRef;

    private ActivityMapsBinding binding;
    private DialogAddMemoryBinding dialogBinding;
    private GoogleMap mMap;
    SharedPreferences sharedPreferences;
    boolean info;
    LocationListener locationListener;
    LocationManager locationManager;
    ActivityResultLauncher<String> permissionLauncher;

    double selectedLatitude;
    double selectedLongitude;
    LatLng selectedLocation;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri imageUri;

    ActivityResultLauncher<String> galleryPermissionLauncher;
    private String selectedPhotoUri;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher();
        registerCameraLaunchers();

        sharedPreferences = this.getSharedPreferences("com.aleynasahin.memorymap", MODE_PRIVATE);
        info = false;

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        String uid = mAuth.getCurrentUser().getUid();
        memoriesRef = firestore.collection("users").document(uid).collection("memories");

        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        selectedPhotoUri = "";

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchMemory(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    getDataFromDatabase();
                } else {
                    searchMemory(newText);
                }
                return true;
            }
        });
    }
    public void getDataFromDatabase() {
        memoriesRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (mMap == null) return;
                    mMap.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Memory memory = documentToMemory(doc);
                        addMarker(memory);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Veriler yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void searchMemory(String query) {
        String queryLower = query.toLowerCase();
        memoriesRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (mMap == null) return;
                    mMap.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String title = doc.getString("memoryTitle");
                        if (title != null && title.toLowerCase().contains(queryLower)) {
                            addMarker(documentToMemory(doc));
                        }
                    }
                });
    }


    public void save() {
        String title = dialogBinding.editTitle.getText().toString().trim();
        String note = dialogBinding.editNote.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Lütfen bir başlık girin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!selectedPhotoUri.isEmpty()) {

            uploadPhotoAndSave(title, note);
        } else {

            saveToFirestore(title, note, "");
        }
    }

    private void uploadPhotoAndSave(String title, String note) {
        Uri uri = Uri.parse(selectedPhotoUri);
        String fileName = "memories/" + mAuth.getCurrentUser().getUid() + "/" + UUID.randomUUID().toString();
        StorageReference ref = FirebaseStorage.getInstance().getReference(fileName);

        Toast.makeText(this, "Fotoğraf yükleniyor...", Toast.LENGTH_SHORT).show();

        ref.putFile(uri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            saveToFirestore(title, note, downloadUrl.toString());
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e("STORAGE", "Fotoğraf yüklenemedi", e);

                    saveToFirestore(title, note, "");
                    Toast.makeText(this, "Fotoğraf yüklenemedi, anı fotoğrafsız kaydedildi", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String title, String note, String photoUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("memoryTitle", title);
        data.put("memoryNote", note);
        data.put("memoryLatitude", selectedLatitude);
        data.put("memoryLongitude", selectedLongitude);
        data.put("photoUri", photoUrl);

        memoriesRef.add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Anı başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                    getDataFromDatabase();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SAVE_ERROR", "Kayıt hatası", e);
                });
    }



    private Memory documentToMemory(DocumentSnapshot doc) {
        Memory m = new Memory(
                doc.getString("memoryTitle"),
                doc.getString("memoryNote"),
                doc.getDouble("memoryLatitude"),
                doc.getDouble("memoryLongitude"),
                doc.getString("photoUri")
        );
        m.firestoreId = doc.getId();
        return m;
    }

    private void addMarker(Memory memory) {
        if (memory.memoryLatitude == null || memory.memoryLongitude == null) return;
        LatLng loc = new LatLng(memory.memoryLatitude, memory.memoryLongitude);
        mMap.addMarker(new MarkerOptions()
                .position(loc)
                .title(memory.memoryTitle)
                .snippet(memory.memoryNote));
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getDataFromDatabase();
        mMap.setOnMapLongClickListener(this);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                info = sharedPreferences.getBoolean("info", false);
                if (!info) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                    sharedPreferences.edit().putBoolean("info", true).apply();
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(binding.getRoot(), "Konum izni gerekli", Snackbar.LENGTH_INDEFINITE)
                        .setAction("İzin Ver", v -> permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                        .show();
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude()), 15));
            }
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnInfoWindowClickListener(marker -> {
            Intent intent = new Intent(MapsActivity.this, MemoryDetailsActivity.class);
            intent.putExtra("title", marker.getTitle());
            intent.putExtra("note", marker.getSnippet());
            startActivity(intent);
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        selectedLocation = latLng;
        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;
        selectedPhotoUri = "";
        showAddMemoryDialog(latLng);
    }

    private void showAddMemoryDialog(LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogBinding = DialogAddMemoryBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());

        dialogBinding.btnTakePhoto.setOnClickListener(v -> {
            imageUri = createImageUri();
            if (imageUri == null) {
                Toast.makeText(MapsActivity.this, "Fotoğraf oluşturulamadı", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                cameraLauncher.launch(imageUri);
            }
        });

        dialogBinding.btnSelectImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, Manifest.permission.READ_MEDIA_IMAGES)) {
                        Snackbar.make(binding.getRoot(), "Galeri izni gerekli", Snackbar.LENGTH_INDEFINITE)
                                .setAction("İzin Ver", v2 -> galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES))
                                .show();
                    } else {
                        galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                    }
                } else {
                    galleryLauncher.launch("image/*");
                }
            } else {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    galleryLauncher.launch("image/*");
                }
            }
        });

        builder.setPositiveButton("Kaydet", (dialog, which) -> save());
        builder.setNegativeButton("İptal", null);
        builder.create().show();
    }


    public void logoutClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Çıkış Yap")
                .setMessage("Çıkış yapmak istediğine emin misin?")
                .setCancelable(false)
                .setPositiveButton("Evet", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(MapsActivity.this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void registerLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri.toString();
                        dialogBinding.imgPreview.setVisibility(View.VISIBLE);
                        dialogBinding.imgPreview.setImageURI(uri);
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        galleryLauncher.launch("image/*");
                    } else {
                        Toast.makeText(MapsActivity.this, "İzin verilmemiş", Toast.LENGTH_LONG).show();
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (last != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(last.getLatitude(), last.getLongitude()), 15));
                            }
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "Konum izni gerekli!", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void registerCameraLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraLauncher.launch(imageUri);
                    } else {
                        Toast.makeText(MapsActivity.this, "Kamera izni gerekli", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && imageUri != null) {
                        selectedPhotoUri = imageUri.toString();
                        showPreviewInDialog();
                        Toast.makeText(MapsActivity.this, "Fotoğraf çekildi!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    private Uri createImageUri() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "memory_" + System.currentTimeMillis());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MemoryMap");
        }
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void showPreviewInDialog() {
        if (dialogBinding != null && imageUri != null) {
            try {
                dialogBinding.imgPreview.setVisibility(View.VISIBLE);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 800, 600, true);
                dialogBinding.imgPreview.setImageBitmap(scaled);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        }
    }
}