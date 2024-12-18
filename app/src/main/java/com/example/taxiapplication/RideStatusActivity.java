package com.example.taxiapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RideStatusActivity extends AppCompatActivity {
    private TextView rideStatusView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_status);

        rideStatusView = findViewById(R.id.rideStatusView);

        String driverId = getIntent().getStringExtra("driverId");
        if (driverId != null) {
            displayRideStatus(driverId);
        } else {
            rideStatusView.setText("Error: No driver selected.");
        }
    }

    private void displayRideStatus(String driverId) {
        rideStatusView.setText("Ride Status: Pending with Driver ID: " + driverId);
    }
}

