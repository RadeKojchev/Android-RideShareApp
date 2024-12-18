package com.example.taxiapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {

    private final List<Map<String, Object>> rideRequests;
    private final SimpleDateFormat dateFormat;
    private final Context context;

    public RideRequestAdapter(List<Map<String, Object>> rideRequests, Context context) {
        this.rideRequests = rideRequests;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> rideRequest = rideRequests.get(position);

        holder.pickupLocationTextView.setText("Pickup: " + rideRequest.get("pickupLocation"));
        holder.dropoffLocationTextView.setText("Dropoff: " + rideRequest.get("dropoffLocation"));
        holder.statusTextView.setText("Status: " + rideRequest.get("status"));

        Object timestampObj = rideRequest.get("pickupTimestamp");
        if (timestampObj != null) {
            Date date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            holder.pickupTimestampTextView.setText("Pickup Time: " + dateFormat.format(date));
        } else {
            holder.pickupTimestampTextView.setText("Pickup Time: Not Available");
        }

        String status = (String) rideRequest.get("status");
        if ("Finished".equals(status)) {
            holder.rateButton.setEnabled(true);
            holder.rateButton.setOnClickListener(v -> showRatingDialog(rideRequest, holder));
        } else {
            holder.rateButton.setEnabled(false);
        }

        holder.commentButton.setOnClickListener(v -> fetchPassengerNameAndShowCommentDialog(rideRequest));
    }

    private void showRatingDialog(Map<String, Object> rideRequest, ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Rate the Driver");

        final EditText input = new EditText(context);
        input.setHint("Enter rating (e.g., 4.5)");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String ratingInput = input.getText().toString();
            try {
                float rating = Float.parseFloat(ratingInput);
                if (rating < 0 || rating > 5) {
                    Toast.makeText(context, "Rating must be between 0 and 5.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String driverId = (String) rideRequest.get("driverId");
                if (driverId == null) {
                    Toast.makeText(context, "Driver ID not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateDriverRating(driverId, rating);

                // Disable the rate button
                holder.rateButton.setEnabled(false);

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid rating. Please enter a number.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void updateDriverRating(String driverId, float newRating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference driverDoc = db.collection("drivers").document(driverId);

        driverDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double currentRating = documentSnapshot.getDouble("rating");
                Long numberOfRides = documentSnapshot.getLong("number_of_rides");

                if (currentRating == null || numberOfRides == null || numberOfRides == 0) {
                    Toast.makeText(context, "Driver rating or ride count is missing or invalid.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Calculate the new rating
                double updatedRating = (currentRating + newRating) / numberOfRides;

                // Update Firestore
                driverDoc.update("rating", updatedRating)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Rating submitted successfully.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to submit rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(context, "Driver not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Error retrieving driver data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void fetchPassengerNameAndShowCommentDialog(Map<String, Object> rideRequest) {
        String passengerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch the passenger's name from Firestore
        db.collection("passengers")
                .document(passengerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String passengerName = documentSnapshot.getString("name");
                        if (passengerName != null && !passengerName.isEmpty()) {
                            showCommentDialog(rideRequest, passengerName);
                        } else {
                            Toast.makeText(context, "Passenger name not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Passenger record not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to fetch passenger name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showCommentDialog(Map<String, Object> rideRequest, String passengerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Leave a Comment");

        // Create a larger EditText for the comment input
        final EditText input = new EditText(context);
        input.setHint("Write your comment here...");
        input.setMinLines(5); // Larger text box
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String commentText = input.getText().toString().trim();
            if (commentText.isEmpty()) {
                Toast.makeText(context, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            String driverId = (String) rideRequest.get("driverId");
            if (driverId == null) {
                Toast.makeText(context, "Driver ID not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            sendCommentToDriver(driverId, commentText, passengerName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void sendCommentToDriver(String driverId, String commentText, String passengerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Construct the comment data
        Map<String, Object> comment = new HashMap<>();
        comment.put("text", commentText);
        comment.put("timestamp", new Date());
        comment.put("passengerName", passengerName);

        // Add the comment to the driver's "comments" subcollection
        db.collection("drivers")
                .document(driverId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(context, "Comment submitted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to submit comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return rideRequests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView pickupLocationTextView, dropoffLocationTextView, statusTextView, pickupTimestampTextView;
        Button rateButton, commentButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            pickupLocationTextView = itemView.findViewById(R.id.pickupLocationTextView);
            dropoffLocationTextView = itemView.findViewById(R.id.dropoffLocationTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            pickupTimestampTextView = itemView.findViewById(R.id.pickupTimestampTextView);
            rateButton = itemView.findViewById(R.id.rateButton);
            commentButton = itemView.findViewById(R.id.commentButton);
        }
    }
}
