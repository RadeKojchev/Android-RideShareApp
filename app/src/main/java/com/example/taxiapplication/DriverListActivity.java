package com.example.taxiapplication;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverListActivity extends AppCompatActivity {

    private RecyclerView driversRecyclerView;
    private PendingRequestsAdapter pendingRequestsAdapter;
    private DriverAdapter driverAdapter;
    private List<Map<String, Object>> driverList; // List of Map representing driver data
    private Timestamp pickupTimestamp; // Global variable for storing selected pickup time

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_list);

        // Initialize RecyclerView
        driversRecyclerView = findViewById(R.id.driversRecyclerView);
        driversRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve driver prices and total distance
        Intent intent = getIntent();
        ArrayList<HashMap<String, Object>> driverPrices =
                (ArrayList<HashMap<String, Object>>) intent.getSerializableExtra("driverPrices");
        double totalDistance = intent.getDoubleExtra("totalDistance", 0);

        if (driverPrices != null) {
            driverList = new ArrayList<>(driverPrices);
        } else {
            driverList = new ArrayList<>();
        }

        driverAdapter = new DriverAdapter(driverList, this::onDriverSelected);
        driversRecyclerView.setAdapter(driverAdapter);

        // Log distance for debugging
        Log.d("DriverListActivity", "Total Distance: " + totalDistance);
    }

    // Method to handle driver selection
    private void onDriverSelected(Map<String, Object> driver) {
        String driverId = (String) driver.get("uid"); // Assuming "uid" holds the driver's unique ID
        String driverName = (String) driver.get("name");

        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, "Driver ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (passengerId == null || passengerId.isEmpty()) {
            Toast.makeText(this, "Passenger ID is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve the pickup and dropoff points passed from the previous activity
        LatLng pickupLatLng = getIntent().getParcelableExtra("pickupPointLatLng");
        LatLng dropOffLatLng = getIntent().getParcelableExtra("dropOffPointLatLng");

        String pickupLocation = pickupLatLng != null
                ? "Lat: " + pickupLatLng.latitude + ", Lon: " + pickupLatLng.longitude
                : "Pickup location not available";

        String dropoffLocation = dropOffLatLng != null
                ? "Lat: " + dropOffLatLng.latitude + ", Lon: " + dropOffLatLng.longitude
                : "Dropoff location not available";

        // Show the dialog to input the desired pickup time
        showPickupTimeDialog(pickupTimestamp -> {
            if (pickupTimestamp == null) {
                Log.e("DriverListActivity", "Pickup timestamp is null!");
                Toast.makeText(this, "Pickup time not set. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call Firestore once we have the pickup time
            sendRideRequest(driverId, driverName, passengerId, pickupLocation, dropoffLocation, pickupTimestamp);
        });
    }

    // Method to send the ride request to Firestore
    private void sendRideRequest(String driverId, String driverName, String passengerId,
                                 String pickupLocation, String dropoffLocation, Timestamp pickupTimestamp) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("passengers")
                .document(passengerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String passengerName = documentSnapshot.getString("name");
                        Float passengerRating = documentSnapshot.getDouble("rating") != null
                                ? documentSnapshot.getDouble("rating").floatValue()
                                : 0f; // Default rating is 0

                        if (passengerName == null || passengerName.isEmpty()) {
                            Toast.makeText(this, "Passenger name is missing!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Prepare ride request data
                        Map<String, Object> rideRequest = new HashMap<>();
                        rideRequest.put("driverId", driverId);
                        rideRequest.put("passengerId", passengerId);
                        rideRequest.put("passengerName", passengerName);
                        rideRequest.put("passengerRating", passengerRating);
                        rideRequest.put("pickupLocation", pickupLocation);
                        rideRequest.put("dropoffLocation", dropoffLocation);
                        rideRequest.put("pickupTimestamp", pickupTimestamp);
                        rideRequest.put("status", "Pending");
                        rideRequest.put("timestamp", System.currentTimeMillis());

                        // Save to "ride_requests" for the driver
                        db.collection("ride_requests")
                                .add(rideRequest)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("DriverListActivity", "Ride request created with ID: " + documentReference.getId());
                                    // Save the same request to the passenger-specific collection
                                    db.collection("passenger_ride_requests")
                                            .document(passengerId)
                                            .collection("requests")
                                            .add(rideRequest)
                                            .addOnSuccessListener(ref -> {
                                                Toast.makeText(this, "Request sent to " + driverName, Toast.LENGTH_SHORT).show();
                                                Log.d("DriverListActivity", "Ride request also stored for passenger.");
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed to save request for passenger.", Toast.LENGTH_SHORT).show();
                                                Log.e("DriverListActivity", "Error storing ride request for passenger: " + e.getMessage());
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to send request to " + driverName, Toast.LENGTH_SHORT).show();
                                    Log.e("DriverListActivity", "Error creating ride request: " + e.getMessage());
                                });
                    } else {
                        Toast.makeText(this, "Passenger data not found!", Toast.LENGTH_SHORT).show();
                        Log.e("DriverListActivity", "Passenger data not found in Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching passenger details", Toast.LENGTH_SHORT).show();
                    Log.e("DriverListActivity", "Error fetching passenger details: " + e.getMessage());
                });
    }

    // Method to show a dialog for the user to input the desired pickup time
    private void showPickupTimeDialog(OnPickupTimeSetListener listener) {
        Calendar currentTime = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                Calendar selectedTime = Calendar.getInstance();
                                selectedTime.set(year, month, dayOfMonth, hourOfDay, minute);

                                // Validate the selected time
                                if (selectedTime.before(Calendar.getInstance())) {
                                    Toast.makeText(this, "Pickup time cannot be in the past!", Toast.LENGTH_SHORT).show();
                                    listener.onPickupTimeSet(null);
                                    return;
                                }

                                // Convert to Firestore-compatible Timestamp
                                Timestamp pickupTimestamp = new Timestamp(new Date(selectedTime.getTimeInMillis()));
                                listener.onPickupTimeSet(pickupTimestamp);
                            },
                            currentTime.get(Calendar.HOUR_OF_DAY),
                            currentTime.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                currentTime.get(Calendar.YEAR),
                currentTime.get(Calendar.MONTH),
                currentTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // Interface for the pickup time listener
    private interface OnPickupTimeSetListener {
        void onPickupTimeSet(Timestamp pickupTimestamp);
    }
}
