package com.example.taxiapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverActivity extends FragmentActivity {

    private static final String TAG = "DriverActivity";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;
    private GeoPoint driverLocation;
    private String DRIVER_ID;
    private RecyclerView recyclerViewPendingRequests;
    private PendingRequestsAdapter pendingRequestsAdapter;
    private List<Map<String, Object>> pendingRequestsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        recyclerViewPendingRequests = findViewById(R.id.recyclerViewPendingRequests);
        recyclerViewPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        pendingRequestsList = new ArrayList<>();
        pendingRequestsAdapter = new PendingRequestsAdapter(pendingRequestsList, this::onRequestAction);
        recyclerViewPendingRequests.setAdapter(pendingRequestsAdapter);

        // Get current user ID
        DRIVER_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchDriverLocation();
        fetchPendingRequests(); // Fetch ride requests after location update

        Button btnGoToAcceptedRequests = findViewById(R.id.btnGoToAcceptedRequests);
        btnGoToAcceptedRequests.setOnClickListener(v -> navigateToAcceptedRequests());

        // Add the button to navigate to CommentsActivity
        Button btnViewComments = findViewById(R.id.btnViewComments);
        btnViewComments.setOnClickListener(v -> navigateToCommentsActivity());
    }

    private void navigateToCommentsActivity() {
        // Pass driver ID to CommentsActivity to fetch comments for this driver
        Intent intent = new Intent(DriverActivity.this, CommentsActivity.class);
        intent.putExtra("driverId", DRIVER_ID); // Pass the driverId
        startActivity(intent);
    }

    private void fetchDriverLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                driverLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Driver location: " + driverLocation.getLatitude() + ", " + driverLocation.getLongitude());
                updateDriverLocationInFirestore();
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Location is null.");
            }
        });
    }

    private void updateDriverLocationInFirestore() {
        if (driverLocation == null) {
            Toast.makeText(this, "Location not found. Please enable GPS.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("drivers").document(DRIVER_ID)
                .update("location", driverLocation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Driver location updated successfully");
                    fetchPendingRequests(); // Fetch ride requests after location update
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating driver location: " + e.getMessage()));
    }

    private void fetchPendingRequests() {
        db.collection("ride_requests")
                .whereEqualTo("driverId", DRIVER_ID)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pendingRequestsList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> request = new HashMap<>();
                        request.put("requestId", doc.getId());
                        request.put("passengerName", doc.getString("passengerName"));
                        request.put("passengerRating", doc.getDouble("passengerRating"));
                        request.put("pickupLocation", doc.getString("pickupLocation"));
                        request.put("dropoffLocation", doc.getString("dropoffLocation"));
                        request.put("pickupTimestamp", doc.getTimestamp("pickupTimestamp"));
                        pendingRequestsList.add(request);
                    }
                    pendingRequestsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching pending requests: " + e.getMessage()));
    }

    private void onRequestAction(Map<String, Object> request, String action) {
        String requestId = (String) request.get("requestId");
        if (action.equals("Accept")) {
            acceptRequest(requestId);
        } else if (action.equals("Decline")) {
            declineRequest(requestId);
        }
    }

    private void acceptRequest(String requestId) {
        db.collection("ride_requests")
                .document(requestId)
                .update("status", "Accepted")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request accepted");
                    fetchPendingRequests(); // Refresh pending requests
                    navigateToAcceptedRequests(); // Redirect to AcceptedRequestsActivity
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error accepting request: " + e.getMessage()));
    }

    private void navigateToAcceptedRequests() {
        Intent intent = new Intent(this, AcceptedRequestsActivity.class);
        startActivity(intent);
    }
    private void declineRequest(String requestId) {
        db.collection("ride_requests")
                .document(requestId)
                .update("status", "Declined")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request declined");
                    fetchPendingRequests(); // Refresh the list after declining
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error declining request: " + e.getMessage()));
    }
}
