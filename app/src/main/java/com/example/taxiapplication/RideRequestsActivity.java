package com.example.taxiapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RideRequestsActivity extends AppCompatActivity {

    private RecyclerView rideRequestsRecyclerView;
    private RideRequestAdapter rideRequestAdapter;
    private List<Map<String, Object>> rideRequestsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_requests);

        // Initialize RecyclerView
        rideRequestsRecyclerView = findViewById(R.id.rideRequestsRecyclerView);
        rideRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data list and adapter
        rideRequestsList = new ArrayList<>();
        rideRequestAdapter = new RideRequestAdapter(rideRequestsList,this);
        rideRequestsRecyclerView.setAdapter(rideRequestAdapter);

        // Load ride requests from Firestore
        loadRideRequests();
    }

    private void loadRideRequests() {
        // Get the current passenger ID
        String passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Ensure the passenger ID is valid
        if (passengerId == null || passengerId.isEmpty()) {
            Toast.makeText(this, "Error: No logged-in user!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query the ride_requests collection for documents where passengerId matches
        db.collection("ride_requests")
                .whereEqualTo("passengerId", passengerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No ride requests found!", Toast.LENGTH_SHORT).show();
                        Log.d("RideRequestsActivity", "No ride requests found for passengerId: " + passengerId);
                        return;
                    }

                    // Clear the existing list
                    rideRequestsList.clear();

                    // Categorize ride requests by status
                    List<Map<String, Object>> pendingRequests = new ArrayList<>();
                    List<Map<String, Object>> acceptedRequests = new ArrayList<>();
                    List<Map<String, Object>> finishedRequests = new ArrayList<>();
                    List<Map<String, Object>> cancelledRequests = new ArrayList<>();

                    for (var document : querySnapshot.getDocuments()) {
                        Map<String, Object> rideRequest = document.getData();
                        rideRequest.put("id", document.getId()); // Add document ID to the map

                        // Categorize by status
                        String status = (String) rideRequest.get("status");
                        if (status != null) {
                            switch (status) {
                                case "Pending":
                                    pendingRequests.add(rideRequest);
                                    break;
                                case "Accepted":
                                    acceptedRequests.add(rideRequest);
                                    break;
                                case "Finished":
                                    finishedRequests.add(rideRequest);
                                    break;
                                case "Canceled":
                                    cancelledRequests.add(rideRequest);
                                    break;
                                case "Declined":
                                    acceptedRequests.add(rideRequest);
                                    break;
                            }
                        }
                    }

                    // Merge lists in desired order
                    rideRequestsList.addAll(pendingRequests);
                    rideRequestsList.addAll(acceptedRequests);
                    rideRequestsList.addAll(finishedRequests);
                    rideRequestsList.addAll(cancelledRequests);

                    // Notify the adapter of data changes
                    rideRequestAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load ride requests!", Toast.LENGTH_SHORT).show();
                    Log.e("RideRequestsActivity", "Error loading ride requests: " + e.getMessage());
                });
    }

}
