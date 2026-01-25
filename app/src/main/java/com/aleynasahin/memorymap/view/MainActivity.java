package com.aleynasahin.memorymap.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.aleynasahin.memorymap.MapsActivity;
import com.aleynasahin.memorymap.R;
import com.aleynasahin.memorymap.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    String email;
    String password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
    }

    public void signInClick(View view) {

        email = binding.etEmail.getText().toString();
        password = binding.etPassword.getText().toString();
        if (email.isEmpty()&& password.isEmpty()) {
            binding.etEmail.setError("Lütfen e-mail adresinizi giriniz");
            binding.etPassword.setError("Lütfen şifrenizi giriniz");

        } else if(email.isEmpty()){
            binding.etEmail.setError("Lütfen e-mail adresinizi giriniz");

        }else if(password.isEmpty()){
            binding.etPassword.setError("Lütfen şifrenizi giriniz");
        }
        else {

            mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    Toast.makeText(MainActivity.this, "başarılı", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }


    }

    public void signUpClick(View view) {

    }

}
