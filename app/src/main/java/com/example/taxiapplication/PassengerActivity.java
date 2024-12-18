package com.example.taxiapplication;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.SearchView;
public class PassengerActivity extends AppCompatActivity implements OnMapReadyCallback, DriverAdapter.OnDriverClickListener {

    private static final String TAG = "PassengerActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2;

    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db;
    private String PASSENGER_ID;

    private LatLng startPoint = null, dropOffPoint = null;

    private DriverAdapter driverAdapter; // For managing the RecyclerView of drivers
    private List<Map<String, Object>> nearbyDrivers = new ArrayList<>();  // To hold the list of nearby drivers


    public void onDriverClick(Map<String, Object> driver) {
        // Handle the driver click here. For example:
        String selectedDriverId = (String) driver.get("id");
        String selectedDriverName = (String) driver.get("name");
        String selectedDriverRating = (String) driver.get("rating");

        Log.d(TAG, "Driver selected: " + selectedDriverName + " (ID: " + selectedDriverId + ")");
        // You can proceed with additional logic like showing driver details or starting the ride request
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");

        setContentView(R.layout.activity_passenger);
        // Initialize Reset Locations Button
        Button resetLocationsButton = findViewById(R.id.resetLocationsButton);
        if (resetLocationsButton == null) {
            Log.e(TAG, "resetLocationsButton is null");
        } else {
            resetLocationsButton.setOnClickListener(view -> resetLocations()); // Add reset locations logic
        }

        // Initialize Map Fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        if (mapFragment == null) {
            Log.e(TAG, "mapFragment is null");
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.mapFragmentContainer, mapFragment).commit();

        // Set up the map asynchronously
        mapFragment.getMapAsync(this);

        // Initialize Firebase Firestore and Location Services
        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if the user is logged in via Firebase Authentication
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Log.d(TAG, "onCreate: Checking if user is logged in");

        if (auth.getCurrentUser() != null) {
            PASSENGER_ID = auth.getCurrentUser().getUid();
            Log.d(TAG, "onCreate: User logged in as: " + PASSENGER_ID);
        } else {
            Log.e(TAG, "onCreate: User not logged in.");
            Toast.makeText(this, "User not logged in. Redirecting to login.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize SearchView for location search
        SearchView searchView = findViewById(R.id.search_view);
        if (searchView == null) {
            Log.e(TAG, "searchView is null");
        }

        // Set up listener for search input
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: Query = " + query);
                searchForLocation(query); // Call the location search method
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: New Text = " + newText);
                return false; // No action required for text change
            }
        });

        // Initialize DriverAdapter
        driverAdapter = new DriverAdapter(nearbyDrivers, this);


        // Handle location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: Location permission not granted, requesting permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "onCreate: Location permission granted, fetching user location");
            fetchUserLocation(); // Fetch passenger's location if permission is granted
        }
        Button viewRideRequestsButton = findViewById(R.id.viewRideRequestsButton);
        viewRideRequestsButton.setOnClickListener(v -> {
            Intent intent = new Intent(PassengerActivity.this, RideRequestsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        // Enable location permissions if not already granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        // Fetch current location and set it as the default start point
        fetchUserLocation();

        // Initialize the map click listener
        initializeMapClickListener();

        // Add UI to reset start/drop-off points
        findViewById(R.id.resetLocationsButton).setOnClickListener(v -> resetLocations());
    }

    private void initializeMapClickListener() {
        googleMap.setOnMapClickListener(this::onMapClickHandler);
    }

    private void onMapClickHandler(LatLng latLng) {
        if (startPoint == null) {
            // Set the selected point as the start point
            startPoint = latLng;
            googleMap.addMarker(new MarkerOptions()
                    .position(startPoint)
                    .title("Start Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            Toast.makeText(this, "Start point selected.", Toast.LENGTH_SHORT).show();
            LatLng pickupLocation = null;  // New pickup location (replace with actual value)

// Overwrite the startLocation with the pickupLocation.
            pickupLocation = startPoint;

// Convert pickupLocation to GeoPoint for Firestore.
            GeoPoint geoPoint = new GeoPoint(pickupLocation.latitude, pickupLocation.longitude);
            updatePassengerLocationInFirestore(geoPoint);
        } else if (dropOffPoint == null) {
            // Set the selected point as the drop-off point
            dropOffPoint = latLng;
            googleMap.addMarker(new MarkerOptions()
                    .position(dropOffPoint)
                    .title("Drop-Off Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            Toast.makeText(this, "Drop-off point selected.", Toast.LENGTH_SHORT).show();
            confirmDropOffPoint(); // Confirm once both points are selected
        } else {
            Toast.makeText(this, "Both locations already selected. Reset to choose again.", Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmDropOffPoint() {
        if (startPoint == null || dropOffPoint == null) {
            Toast.makeText(this, "Please select both start and drop-off points.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert LatLng to GeoPoint (if needed for Firestore) and calculate the distance
        double distance = calculateDistance(
                new LatLng(startPoint.latitude, startPoint.longitude),
                new LatLng(dropOffPoint.latitude, dropOffPoint.longitude)
        );

        calculatePrice(distance, driverPrices -> {
            if (driverPrices == null || driverPrices.isEmpty()) {
                Toast.makeText(this, "No drivers available.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare data to pass via Intent
            ArrayList<HashMap<String, Object>> serializableDriverPrices = new ArrayList<>();
            for (Map<String, Object> driver : driverPrices) {
                HashMap<String, Object> serializedDriver = new HashMap<>(driver);
                serializedDriver.remove("location"); // Remove GeoPoint or non-serializable fields
                serializableDriverPrices.add(serializedDriver);
            }

            // Create Intent to pass data to DriverListActivity
            Intent intent = new Intent(this, DriverListActivity.class);
            intent.putExtra("driverPrices", serializableDriverPrices);
            intent.putExtra("totalDistance", distance);

            // Pass pickup and dropoff points as LatLng or GeoPoint
            intent.putExtra("pickupPointLatLng", new LatLng(startPoint.latitude, startPoint.longitude)); // Pickup point
            intent.putExtra("dropOffPointLatLng", new LatLng(dropOffPoint.latitude, dropOffPoint.longitude)); // Dropoff point

            // Start DriverListActivity
            startActivity(intent);
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocation();
            } else {
                Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private double calculateDistance(LatLng start, LatLng end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start or end location cannot be null");
        }

        double lat1 = start.latitude;
        double lon1 = start.longitude;
        double lat2 = end.latitude;
        double lon2 = end.longitude;

        final double EARTH_RADIUS_KM = 6371.0;

        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
    private void calculatePrice(double distance, OnPriceCalculatedListener listener) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("drivers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> driverPrices = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Double pricePerKm = doc.getDouble("price_per_km");
                        if (pricePerKm != null) {
                            double price = distance * pricePerKm;
                            Map<String, Object> driverData = doc.getData();
                            if (driverData != null) {
                                driverData.put("price", price); // Add calculated price
                                driverPrices.add(driverData);
                            }
                        }
                    }
                    listener.onPricesCalculated(driverPrices);
                })
                .addOnFailureListener(e -> {
                    Log.e("PassengerActivity", "Error fetching price_per_km", e);
                    Toast.makeText(this, "Error calculating prices", Toast.LENGTH_SHORT).show();
                });
    }

    // Listener interface to handle asynchronous price calculations
    public interface OnPriceCalculatedListener {
        void onPricesCalculated(List<Map<String, Object>> driverPrices);
    }

    private void fetchUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                       // updatePassengerLocationInFirestore(location);
                        if (googleMap != null) {
                            googleMap.addMarker(new MarkerOptions()
                                    .position(userLatLng)
                                    .title("Your Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                        }
                    } else {
                        Toast.makeText(this, "Unable to fetch location. Check GPS settings.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching location: " + e.getMessage()));
    }

    private void updatePassengerLocationInFirestore(GeoPoint location) {
        Map<String, Object> passengerLocation = new HashMap<>();
        passengerLocation.put("latitude", location.getLatitude());
        passengerLocation.put("longitude", location.getLongitude());

        db.collection("passengers").document(PASSENGER_ID)
                .set(Collections.singletonMap("location", passengerLocation), SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Passenger location updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "updatePassengerLocationInFirestore: Error updating location", e));

    }
    private void searchForLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                if (googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    googleMap.addMarker(new MarkerOptions().position(latLng).title(locationName));
                }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
        }
    }
    private void resetLocations() {
        startPoint = null;
        dropOffPoint = null;
        googleMap.clear(); // Clear all markers and the route
        Toast.makeText(this, "Locations reset. Select new points.", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}

