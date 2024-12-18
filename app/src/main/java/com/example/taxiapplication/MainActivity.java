package com.example.taxiapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "User not logged in, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Log.d(TAG, "User logged in: " + currentUser.getUid());
            determineUserRole(currentUser.getUid());
        }
    }

    private void determineUserRole(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Log.d(TAG, "User role fetched: " + role);

                        if ("driver".equalsIgnoreCase(role)) {
                            startActivity(new Intent(this, DriverActivity.class));
                        } else if ("passenger".equalsIgnoreCase(role)) {
                            startActivity(new Intent(this, PassengerActivity.class));
                        } else {
                            Log.e(TAG, "Unknown role, redirecting to LoginActivity");
                            startActivity(new Intent(this, LoginActivity.class));
                        }
                    } else {
                        Log.e(TAG, "No document found for user, redirecting to LoginActivity");
                        startActivity(new Intent(this, LoginActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching role: " + e.getMessage(), e);
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }
}
