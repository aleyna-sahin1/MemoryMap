package com.aleynasahin.memorymap;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.aleynasahin.memorymap.databinding.DialogAddMemoryBinding;
import com.aleynasahin.memorymap.model.Memory;
import com.aleynasahin.memorymap.roomdb.MemoryDao;
import com.aleynasahin.memorymap.roomdb.MemoryDatabase;
import com.aleynasahin.memorymap.view.MainActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.aleynasahin.memorymap.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    FirebaseAuth mAuth;
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
    String title;
    String note;
    LatLng selectedLocation;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    MemoryDatabase db;
    MemoryDao memoryDao;

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


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();
        registerCameraLaunchers();

        sharedPreferences = this.getSharedPreferences("com.aleynasahin.memorymap", MODE_PRIVATE);
        info = false;
        db = Room.databaseBuilder(getApplicationContext(), MemoryDatabase.class, "Memories")
                .fallbackToDestructiveMigration(true)
                .build();
        memoryDao = db.memoryDao();
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;

        selectedPhotoUri="";
        binding.searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
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
                }
        );
        mAuth=FirebaseAuth.getInstance();

    }
    public void logoutClick(View view){
        mAuth.signOut();
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        startActivity(intent);

    }
    private void searchMemory(String query) {
        compositeDisposable.clear();
        compositeDisposable.add(
                memoryDao.searchMemories(query)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(memories -> {
                            mMap.clear();

                            for (Memory memory : memories) {
                                LatLng latLng = new LatLng(
                                        memory.memoryLatitude,
                                        memory.memoryLongitude
                                );

                                mMap.addMarker(
                                        new MarkerOptions()
                                                .position(latLng)
                                                .title(memory.memoryTitle)
                                                .snippet(memory.memoryNote)
                                );
                            }
                        })
        );
    }


    public void getDataFromDatabase() {
        compositeDisposable.add(memoryDao.getAllMemories().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse));

    }



    private void handleResponse(List<Memory> memoryList) {
        if (memoryList != null) {
            for (Memory memory : memoryList) {
                LatLng memoryLocation = new LatLng(memory.memoryLatitude, memory.memoryLongitude);
                mMap.addMarker(new MarkerOptions()
                        .position(memoryLocation)
                        .title(memory.memoryTitle)
                        .snippet(memory.memoryNote)
                );
            }

        }

    }

    public void save() {
        title = dialogBinding.editTitle.getText().toString();
        note = dialogBinding.editNote.getText().toString();
        Memory memory = new Memory(title, note, selectedLatitude, selectedLongitude,selectedPhotoUri);
        compositeDisposable.add(memoryDao.insert(memory)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {

                    getDataFromDatabase();

                    Toast.makeText(this, "Anı başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                }, throwable -> {

                    Toast.makeText(this, "Hata: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SAVE_ERROR", "Kayıt hatası", throwable);
                }));
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

                Snackbar.make(binding.getRoot(), "Permission needed for location", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }).show();
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                LatLng lastUserLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
            }

            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnInfoWindowClickListener(marker -> {
            Intent intent = new Intent(MapsActivity.this, MemoryDetailsActivity.class);
            intent.putExtra("title", marker.getTitle());
            intent.putExtra("note", marker.getSnippet());

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        });


    }

    private void registerLauncher() {

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {


                        selectedPhotoUri = uri.toString();


                        dialogBinding.imgPreview.setVisibility(View.VISIBLE);
                        dialogBinding.imgPreview.setImageURI(uri);


                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    }
                }
        );

        galleryPermissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    galleryLauncher.launch("image/*");
                }else {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    Toast.makeText(MapsActivity.this,"İzin verilmemiş",Toast.LENGTH_LONG).show();

                }
            }
        });



        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {

            @Override
            public void onActivityResult(Boolean result) {
                if (result) {

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            LatLng lastUserLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                        }
                        return;
                    }

                } else {
                    Toast.makeText(MapsActivity.this, "Permission needed!", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    private void showAddMemoryDialog(LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogBinding = DialogAddMemoryBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());

        ImageView imgPreview = dialogBinding.imgPreview;

        dialogBinding.btnTakePhoto.setOnClickListener(v -> {
            imageUri = createImageUri();
            if (imageUri == null) {
                Toast.makeText(MapsActivity.this, "Fotoğraf oluşturulamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ContextCompat.checkSelfPermission(
                    MapsActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                cameraLauncher.launch(imageUri);
            }
        });

        dialogBinding.btnSelectImage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,Manifest.permission.READ_MEDIA_IMAGES)){
                        Snackbar.make(binding.getRoot(),"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);

                                    }
                                }
                        ).show();
                    }else {
                        galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                    }
                }else{
                    galleryLauncher.launch("image/*");

                }

            }else{
                if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                        Snackbar.make(binding.getRoot(),"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

                                    }
                                }
                        ).show();
                    }else {
                        galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }else{
                    galleryLauncher.launch("image/*");

                }

            }

        });

        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            save();
        });

        builder.setNegativeButton("İptal", null);

        AlertDialog memoryDialog = builder.create();
        memoryDialog.show();
    }


    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        selectedLocation = latLng;
        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;
        showAddMemoryDialog(latLng);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
    private void registerCameraLaunchers() {

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraLauncher.launch(imageUri);
                    } else {
                        Toast.makeText(MapsActivity.this,
                                "Kamera izni gerekli",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );


        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && imageUri != null) {

                        Toast.makeText(MapsActivity.this,
                                "Fotoğraf çekildi!",
                                Toast.LENGTH_SHORT).show();

                        selectedPhotoUri = imageUri.toString();


                        showPreviewInDialog();
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

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(),
                        imageUri
                );

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        800,
                        600,
                        true
                );

                dialogBinding.imgPreview.setImageBitmap(scaledBitmap);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Fotoğraf yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        }
    }


}


