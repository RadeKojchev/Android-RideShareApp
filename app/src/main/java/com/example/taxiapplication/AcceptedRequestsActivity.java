package com.example.taxiapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcceptedRequestsActivity extends AppCompatActivity {

    private static final String TAG = "AcceptedRequestsActivity";
    private RecyclerView recyclerViewAcceptedRequests;
    private AcceptedRequestsAdapter acceptedRequestsAdapter;
    private List<Map<String, Object>> acceptedRequestsList;
    private FirebaseFirestore db;
    private String DRIVER_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted_requests);

        recyclerViewAcceptedRequests = findViewById(R.id.recyclerViewAcceptedRequests);
        recyclerViewAcceptedRequests.setLayoutManager(new LinearLayoutManager(this));
        acceptedRequestsList = new ArrayList<>();
        acceptedRequestsAdapter = new AcceptedRequestsAdapter(acceptedRequestsList, this::onRequestAction);
        recyclerViewAcceptedRequests.setAdapter(acceptedRequestsAdapter);

        db = FirebaseFirestore.getInstance();
        DRIVER_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchAcceptedRequests();
    }

    private void fetchAcceptedRequests() {
        db.collection("ride_requests")
                .whereEqualTo("driverId", DRIVER_ID)
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    acceptedRequestsList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> request = new HashMap<>();
                        request.put("requestId", doc.getId());
                        request.put("passengerName", doc.getString("passengerName"));
                        request.put("pickupLocation", doc.getString("pickupLocation"));
                        request.put("dropoffLocation", doc.getString("dropoffLocation"));
                        request.put("passengerId", doc.getString("passengerId"));

                        acceptedRequestsList.add(request);
                    }
                    acceptedRequestsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching accepted requests: " + e.getMessage()));
    }

    private void onRequestAction(Map<String, Object> request, String action) {
        String requestId = (String) request.get("requestId");
        if (action.equals("Finish")) {
            String passengerId = (String) request.get("passengerId"); // Ensure passengerId is available in your data
            if (passengerId != null) {
                finishRide(passengerId, requestId);
            } else {
                Toast.makeText(this, "Passenger ID not found.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (action.equals("Cancel")) {
        cancelRequest(requestId);  // Call cancelRequest method when cancel button is clicked
    }
    }

    private void updateRequestStatus(String requestId, String status) {
        db.collection("ride_requests")
                .document(requestId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request marked as " + status, Toast.LENGTH_SHORT).show();
                    fetchAcceptedRequests(); // Refresh the list
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating request status: " + e.getMessage()));
    }
    private void cancelRequest(String requestId) {
        // Remove the request from Firestore and the list
        db.collection("ride_requests")
                .document(requestId)
                .update("status", "Canceled")  // Optionally, mark the status as 'Canceled' in Firestore
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request canceled", Toast.LENGTH_SHORT).show();
                    // Remove the request from the acceptedRequestsList
                    removeRequestFromList(requestId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error canceling request: " + e.getMessage()));
    }

    private void removeRequestFromList(String requestId) {
        // Find the request in the list by requestId and remove it
        for (int i = 0; i < acceptedRequestsList.size(); i++) {
            if (acceptedRequestsList.get(i).get("requestId").equals(requestId)) {
                acceptedRequestsList.remove(i);
                acceptedRequestsAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void finishRide(String passengerId, String requestId) {
        // Increment number_of_rides for both driver and passenger
        db.collection("drivers").document(DRIVER_ID)
                .update("number_of_rides", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Driver number_of_rides updated"));

        db.collection("passengers").document(passengerId)
                .update("number_of_rides", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Passenger number_of_rides updated");

                    // Show rating dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Rate the Passenger");

                    // Create an input field for rating
                    final EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setHint("Rate 1-5");
                    builder.setView(input);

                    builder.setPositiveButton("Submit", (dialog, which) -> {
                        int rating = Integer.parseInt(input.getText().toString());
                        updatePassengerRating(passengerId, rating);

                        // Update the ride request status to "Finished"
                        updateRequestStatus(requestId, "Finished");
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                    builder.show();
                });
    }

    private void updatePassengerRating(String passengerId, int newRating) {
        db.collection("passengers").document(passengerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double currentRating = documentSnapshot.getDouble("rating");
                        long numberOfRides = documentSnapshot.getLong("number_of_rides");

                        // Update rating
                        double updatedRating = (currentRating * (numberOfRides - 1) + newRating) / numberOfRides;
                        db.collection("passengers").document(passengerId)
                                .update("rating", updatedRating)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Passenger rating updated"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating passenger rating: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching passenger rating: " + e.getMessage()));
    }
}
