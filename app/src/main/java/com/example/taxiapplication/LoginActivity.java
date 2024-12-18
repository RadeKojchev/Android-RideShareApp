package com.example.taxiapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taxiapplication.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Handle login button click
        binding.continueBtn.setOnClickListener(v -> {
            String email = binding.email.getText().toString().trim();
            String password = binding.password.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                checkUserRole(user.getUid());
                            }
                        } else {
                            Log.w("LoginActivity", "signInWithEmail:failure", task.getException());
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Handle "Sign up here" click
        binding.move.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void checkUserRole(String userId) {
        // Check in "drivers" collection
        db.collection("drivers").document(userId).get()
                .addOnCompleteListener(driverTask -> {
                    if (driverTask.isSuccessful() && driverTask.getResult() != null && driverTask.getResult().exists()) {
                        // User is a driver
                        redirectToActivity(DriverActivity.class);
                    } else {
                        // Check in "passengers" collection
                        db.collection("passengers").document(userId).get()
                                .addOnCompleteListener(passengerTask -> {
                                    if (passengerTask.isSuccessful() && passengerTask.getResult() != null && passengerTask.getResult().exists()) {
                                        // User is a passenger
                                        redirectToActivity(PassengerActivity.class);
                                    } else {
                                        Toast.makeText(this, "Role not found. Please contact support.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(LoginActivity.this, activityClass);
        startActivity(intent);
        finish();
    }
}
