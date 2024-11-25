package com.example.shodh;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.shodh.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker currentMarker;



    // Declare the ActivityResultLauncher
    private ActivityResultLauncher<Intent> mapTypeActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize Location Callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    updateLocationUI(location);
                }
            }
        };

        // Setup ActivityResultLauncher for map type selection
        mapTypeActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Get the selected map type and update the Google Map
                        int selectedMapType = result.getData().getIntExtra("selectedMapType", GoogleMap.MAP_TYPE_NORMAL);
                        if (mMap != null) {
                            mMap.setMapType(selectedMapType);
                        }
                    }
                });

        // Setup Hamburger Menu
        ImageView hamburgerIcon = findViewById(R.id.toolbar_hamburger);
        hamburgerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show popup menu
                showHamburgerMenu(v);
            }
        });

        // Setup Location Icon for showing shapes and camera options
        ImageView locationIcon = findViewById(R.id.toolbar_location);
        locationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show Popup menu with shapes and camera options
                showLocationPopupMenu(view);
            }
        });
    }

    private void showHamburgerMenu(View view) {
        // Create and show a PopupMenu when the hamburger icon is clicked
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.hamburger_menu, popupMenu.getMenu());

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.action_new_file) {
                    // Add your logic to handle creating a new file
                    Toast.makeText(HomeActivity.this, "New File Selected", Toast.LENGTH_SHORT).show();
                    return true;

                } else if (itemId == R.id.action_load_file) {
                    // Add your logic to handle loading a file
                    Toast.makeText(HomeActivity.this, "Load File Selected", Toast.LENGTH_SHORT).show();
                    return true;

                } else if (itemId == R.id.action_select_map) {
                    // Launch the MapTypeActivity
                    Intent intent = new Intent(HomeActivity.this, MapTypeActivity.class);
                    mapTypeActivityResultLauncher.launch(intent);
                    return true;

                } else if (itemId == R.id.action_settings) {
                    // Add your logic to open settings activity
                    Toast.makeText(HomeActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
                    return true;

                } else if (itemId == R.id.action_share_file) {
                    // Add your logic to handle sharing files
                    Toast.makeText(HomeActivity.this, "Share File Selected", Toast.LENGTH_SHORT).show();
                    return true;

                } else {
                    return false;
                }
            }
        });

        // Show the menu
        popupMenu.show();
    }

    // Show PopupMenu for Location Icon with Shapes and Camera options
    private void showLocationPopupMenu(View view) {
        // Create a PopupMenu
        PopupMenu popupMenu = new PopupMenu(HomeActivity.this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.location_shapes_menu, popupMenu.getMenu());

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();


                if (itemId == R.id.action_circle) {
                    // Handle circle shape selection
                    Intent intent = new Intent(HomeActivity.this, ShapesActivity.class);
                    Toast.makeText(HomeActivity.this, "Circle Selected", Toast.LENGTH_SHORT).show();
                    intent.putExtra("SHAPE_TYPE", "CIRCLE");
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.action_polyline) {
                    // Handle polyline shape selection
                    Intent intent = new Intent(HomeActivity.this, ShapesActivity.class);
                    Toast.makeText(HomeActivity.this, "Polyline Selected", Toast.LENGTH_SHORT).show();
                    intent.putExtra("SHAPE_TYPE", "POLYLINE");
                    startActivity(intent);
                    return true;

                } else if (itemId == R.id.action_polygon) {
                    // Handle polygon shape selection
                    Intent intent = new Intent(HomeActivity.this, ShapesActivity.class);
                    Toast.makeText(HomeActivity.this, "Polygon Selected", Toast.LENGTH_SHORT).show();
                    intent.putExtra("SHAPE_TYPE", "POLYGON");
                    startActivity(intent);
                    return true;
                }
                else if (itemId == R.id.action_camera) {
                    // Handle camera action
                    Intent intent = new Intent(HomeActivity.this, CameraActivity.class);
                    startActivity(intent);
                    Toast.makeText(HomeActivity.this, "Camera Selected", Toast.LENGTH_SHORT).show();
                    // Launch the camera activity or functionality
                    openCamera();
                    return true;

                } else {
                    return false;
                }
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void openCamera() {
        // Logic to open the camera
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(cameraIntent);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }

        // Do not enable My Location layer, blue dot is not shown

        // Start location updates manually
        startLocationUpdates();
    }

    private void updateLocationUI(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        // Update the marker if it exists, otherwise create a new one
        if (currentMarker != null) {
            currentMarker.setPosition(userLocation);
        } else {
            currentMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        }

        // Move the camera to the user's location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop location updates when the activity is destroyed
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
