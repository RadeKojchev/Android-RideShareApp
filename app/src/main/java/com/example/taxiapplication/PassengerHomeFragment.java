package com.example.taxiapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PassengerHomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public PassengerHomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_passenger_home, container, false);

        try {
            // Initialize the map fragment
            Log.d("PassengerHomeFragment", "Initializing SupportMapFragment...");
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.fragment_map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Log.e("PassengerHomeFragment", "Error: MapFragment is null!");
            }
        } catch (Exception e) {
            Log.e("PassengerHomeFragment", "Error initializing map fragment: " + e.getMessage(), e);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("PassengerHomeFragment", "Google Map is ready");
        mMap = googleMap;

        // Enable user location
        enableUserLocation();

        // Add a default marker for demonstration
        LatLng defaultLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }

    @SuppressLint("MissingPermission")
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request location permissions
            Log.d("PassengerHomeFragment", "Requesting location permissions...");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Enable location layer on the map
        Log.d("PassengerHomeFragment", "Enabling location on the map");
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location
                Log.d("PassengerHomeFragment", "Location permission granted");
                enableUserLocation();
            } else {
                Log.e("PassengerHomeFragment", "Location permission denied");
                Toast.makeText(requireContext(), "Location permission required to show your position", Toast.LENGTH_LONG).show();
            }
        }
    }
}
