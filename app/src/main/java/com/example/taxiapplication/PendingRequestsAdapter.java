package com.example.taxiapplication;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.PendingRequestViewHolder> {

    private final List<Map<String, Object>> pendingRequestsList;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onRequestAction(Map<String, Object> request, String action);
    }

    public PendingRequestsAdapter(List<Map<String, Object>> pendingRequestsList, OnRequestActionListener listener) {
        this.pendingRequestsList = pendingRequestsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_request_item, parent, false);
        return new PendingRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingRequestViewHolder holder, int position) {
        Map<String, Object> request = pendingRequestsList.get(position);

        String passengerName = (String) request.get("passengerName");
        double passengerRating = (double) request.get("passengerRating");
        String pickupLocation = (String) request.get("pickupLocation");
        String dropoffLocation = (String) request.get("dropoffLocation");
        Timestamp pickupTimeStamp = (Timestamp) request.get("pickupTimestamp");

        holder.passengerName.setText("Passenger: " + passengerName);
        holder.passengerRating.setText("Rating: " + passengerRating);
        holder.pickupLocation.setText("Pickup: " + pickupLocation);
        holder.dropoffLocation.setText("Dropoff: " + dropoffLocation);

        if (pickupTimeStamp != null) {
            // Convert Firebase Timestamp to a formatted string
            Date pickupDate = pickupTimeStamp.toDate();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            holder.pickupTimestamp.setText("PickupTime: " + formatter.format(pickupDate));
        } else {
            // Log the issue
            Log.e("PendingRequestsAdapter", "pickupTimeStamp is null for request: " + request.toString());
            holder.pickupTimestamp.setText("PickupTime: Not Available");
        }

        holder.acceptButton.setOnClickListener(v -> listener.onRequestAction(request, "Accept"));
        holder.declineButton.setOnClickListener(v -> listener.onRequestAction(request, "Decline"));
    }

    @Override
    public int getItemCount() {
        return pendingRequestsList.size();
    }

    public static class PendingRequestViewHolder extends RecyclerView.ViewHolder {
        TextView passengerName, passengerRating, pickupLocation, dropoffLocation, pickupTimestamp;
        Button acceptButton, declineButton;

        public PendingRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            passengerName = itemView.findViewById(R.id.passengerName);
            passengerRating = itemView.findViewById(R.id.passengerRating);
            pickupLocation = itemView.findViewById(R.id.pickupLocation);
            dropoffLocation = itemView.findViewById(R.id.dropoffLocation);
            pickupTimestamp = itemView.findViewById(R.id.pickupTimestamp);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}