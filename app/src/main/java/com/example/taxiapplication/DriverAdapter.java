package com.example.taxiapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {
    private List<Map<String, Object>> driverList;
    private final OnDriverClickListener listener;

    // Listener interface for handling driver item clicks
    public interface OnDriverClickListener {
        void onDriverClick(Map<String, Object> driver);
    }

    // Constructor to initialize the driver list and listener
    public DriverAdapter(List<Map<String, Object>> driverList, OnDriverClickListener listener) {
        this.driverList = driverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driver_item, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Map<String, Object> driver = driverList.get(position);

        // Safely extract values from the Map
        String name = driver.containsKey("name") ? (String) driver.get("name") : "Unknown";

        // Handle rating with type checking and conversion
        Object ratingObj = driver.get("rating");
        double rating = 0.0; // Default rating
        if (ratingObj instanceof Long) {
            rating = ((Long) ratingObj).doubleValue();
        } else if (ratingObj instanceof Double) {
            rating = (Double) ratingObj;
        }

        // Handle price with type checking and conversion
        Object priceObj = driver.get("price");
        double price = 0.0; // Default price
        if (priceObj instanceof Long) {
            price = ((Long) priceObj).doubleValue();
        } else if (priceObj instanceof Double) {
            price = (Double) priceObj;
        }

        // Extract vehicle details
        Map<String, Object> vehicleDetails = (Map<String, Object>) driver.get("vehicleDetails");
        String licensePlate = (String) vehicleDetails.get("LicensePlate");
        String make = (String) vehicleDetails.get("Make");
        String type = (String) vehicleDetails.get("Type");
        String year = (String) vehicleDetails.get("Year");

        // Populate the view holder with data
        holder.driverName.setText(name);
        holder.driverRating.setText("Rating: " + (rating < 0 ? "N/A" : rating));
        holder.ridePrice.setText("Price: $" + (price > 0 ? String.format("%.2f", price) : "N/A"));

        // Set vehicle details in the views
        holder.licensePlate.setText("License Plate: " + (licensePlate != null ? licensePlate : "N/A"));
        holder.make.setText("Make: " + (make != null ? make : "N/A"));
        holder.type.setText("Type: " + (type != null ? type : "N/A"));
        holder.year.setText("Year: " + (year != null ? year : "N/A"));

        // Handle item click event
        holder.itemView.setOnClickListener(v -> listener.onDriverClick(driver));
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    // Method to update the driver list and notify the adapter of the change
    public void updateDriverList(List<Map<String, Object>> newDriverList) {
        this.driverList = newDriverList;
        notifyDataSetChanged(); // Notify adapter to update the UI
    }

    // ViewHolder class for driver items
    public static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverRating, ridePrice, licensePlate, make, type, year;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            driverName = itemView.findViewById(R.id.driverName);
            driverRating = itemView.findViewById(R.id.driverRating);
            ridePrice = itemView.findViewById(R.id.ridePrice);
            licensePlate = itemView.findViewById(R.id.licensePlate);
            make = itemView.findViewById(R.id.make);
            type = itemView.findViewById(R.id.type);
            year = itemView.findViewById(R.id.year);
        }
    }
}
