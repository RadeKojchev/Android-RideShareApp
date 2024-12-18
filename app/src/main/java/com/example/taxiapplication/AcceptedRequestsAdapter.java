package com.example.taxiapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AcceptedRequestsAdapter extends RecyclerView.Adapter<AcceptedRequestsAdapter.AcceptedRequestViewHolder> {

    private final List<Map<String, Object>> acceptedRequestsList;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onRequestAction(Map<String, Object> request, String action);
    }

    public AcceptedRequestsAdapter(List<Map<String, Object>> acceptedRequestsList, OnRequestActionListener listener) {
        this.acceptedRequestsList = acceptedRequestsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AcceptedRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.accepted_request_item, parent, false);
        return new AcceptedRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AcceptedRequestViewHolder holder, int position) {
        Map<String, Object> request = acceptedRequestsList.get(position);

        String passengerName = (String) request.get("passengerName");
        String pickupLocation = (String) request.get("pickupLocation");
        String dropoffLocation = (String) request.get("dropoffLocation");

        holder.passengerName.setText("Passenger: " + passengerName);
        holder.pickupLocation.setText("Pickup: " + pickupLocation);
        holder.dropoffLocation.setText("Dropoff: " + dropoffLocation);

        holder.finishButton.setOnClickListener(v -> listener.onRequestAction(request, "Finish"));
        holder.cancelButton.setOnClickListener(v -> listener.onRequestAction(request, "Cancel"));
    }

    @Override
    public int getItemCount() {
        return acceptedRequestsList.size();
    }

    public static class AcceptedRequestViewHolder extends RecyclerView.ViewHolder {
        TextView passengerName, pickupLocation, dropoffLocation;
        Button finishButton, cancelButton;

        public AcceptedRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            passengerName = itemView.findViewById(R.id.passengerName);
            pickupLocation = itemView.findViewById(R.id.pickupLocation);
            dropoffLocation = itemView.findViewById(R.id.dropoffLocation);
            finishButton = itemView.findViewById(R.id.finishButton);
            cancelButton = itemView.findViewById(R.id.cancelButton);
        }
    }
}
