package com.example.taxiapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, vehicleTypeEditText, vehicleMakeEditText, vehicleYearEditText, vehicleLicensePlateEditText, pricePerKmEditText;
    private RadioGroup roleGroup;
    private Button continueButton;
    private TextView moveToLogin;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private GeoPoint currentLocation;
    private static final double INITIAL_RATING = 3.0;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize views
        nameEditText = findViewById(R.id.name);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        roleGroup = findViewById(R.id.roleGroup);
        continueButton = findViewById(R.id.continueBtn);
        moveToLogin = findViewById(R.id.move);

        // Vehicle details fields
        vehicleTypeEditText = findViewById(R.id.vehicleType);
        vehicleMakeEditText = findViewById(R.id.vehicleMake);
        vehicleYearEditText = findViewById(R.id.vehicleYear);
        vehicleLicensePlateEditText = findViewById(R.id.vehicleLicensePlate);
        pricePerKmEditText = findViewById(R.id.pricePerKm);

        toggleDriverFieldsVisibility(false);

        // Listen for role selection changes
        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedButton = findViewById(checkedId);
            if (selectedButton != null && selectedButton.getText().toString().equals("Driver")) {
                toggleDriverFieldsVisibility(true);
            } else {
                toggleDriverFieldsVisibility(false);
            }
        });

        // Fetch user location
        fetchUserLocation();

        // Handle signup process
        continueButton.setOnClickListener(view -> createUser());

        moveToLogin.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void toggleDriverFieldsVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        vehicleTypeEditText.setVisibility(visibility);
        vehicleMakeEditText.setVisibility(visibility);
        vehicleYearEditText.setVisibility(visibility);
        vehicleLicensePlateEditText.setVisibility(visibility);
        pricePerKmEditText.setVisibility(visibility);
    }

    private void fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void createUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        int selectedRoleId = roleGroup.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || selectedRoleId == -1) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = ((RadioButton) findViewById(selectedRoleId)).getText().toString();
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("rating", INITIAL_RATING);
        userData.put("location", currentLocation);

        if (role.equals("Driver")) {
            String vehicleType = vehicleTypeEditText.getText().toString().trim();
            String vehicleMake = vehicleMakeEditText.getText().toString().trim();
            String vehicleYear = vehicleYearEditText.getText().toString().trim();
            String licensePlate = vehicleLicensePlateEditText.getText().toString().trim();
            String pricePerKmText = pricePerKmEditText.getText().toString().trim();

            if (TextUtils.isEmpty(vehicleType) || TextUtils.isEmpty(vehicleMake) ||
                    TextUtils.isEmpty(vehicleYear) || TextUtils.isEmpty(licensePlate) || TextUtils.isEmpty(pricePerKmText)) {
                Toast.makeText(this, "Please provide all driver-specific details", Toast.LENGTH_SHORT).show();
                return;
            }

            double pricePerKm;
            try {
                pricePerKm = Double.parseDouble(pricePerKmText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price per km", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> vehicleDetails = new HashMap<>();
            vehicleDetails.put("Type", vehicleType);
            vehicleDetails.put("Make", vehicleMake);
            vehicleDetails.put("Year", vehicleYear);
            vehicleDetails.put("LicensePlate", licensePlate);

            userData.put("vehicleDetails", vehicleDetails);
            userData.put("price_per_km", pricePerKm);
            saveUserToFirestore("drivers", userData);
        } else {
            saveUserToFirestore("passengers", userData);
        }
    }

    private void saveUserToFirestore(String collection, Map<String, Object> userData) {
        auth.createUserWithEmailAndPassword(
                        userData.get("email").toString(),
                        passwordEditText.getText().toString().trim())
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    userData.put("uid", uid);

                    db.collection(collection).document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
