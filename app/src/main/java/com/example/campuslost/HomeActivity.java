
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class HomeActivity extends Activity {

    // UI Components
    RecyclerView recyclerView;

    Button btnAddItem, btnLogout;
    Button btnAll, btnLost, btnFound;

    SearchView searchView;

    // Item Lists
    ArrayList<LostItem> allItems;
    ArrayList<LostItem> displayedItems;

    LostItemAdapter adapter;

    // Firebase
    DatabaseReference databaseReference;
    ValueEventListener valueEventListener;

    private boolean listenerAttached = false;

    // Search and Filter
    String selectedFilter = "All";
    String searchText = "";

    // Shake Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorEventListener;

    private static final float SHAKE_THRESHOLD = 15.0f;
    private static final long SHAKE_COOLDOWN = 1500;

    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerView);

        btnAddItem = findViewById(R.id.btnAddItem);
        btnLogout = findViewById(R.id.btnLogout);

        btnAll = findViewById(R.id.btnAll);
        btnLost = findViewById(R.id.btnLost);
        btnFound = findViewById(R.id.btnFound);

        searchView = findViewById(R.id.searchView);

        // Initialize Lists
        allItems = new ArrayList<>();
        displayedItems = new ArrayList<>();

        // RecyclerView Setup
        adapter = new LostItemAdapter(displayedItems);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerView.setAdapter(adapter);

        // Item Click - Open Details
        adapter.setOnItemClickListener(item -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    ItemDetailsActivity.class
            );

            intent.putExtra("itemId", item.getItemId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("location", item.getLocation());
            intent.putExtra("type", item.getType());
            intent.putExtra("userId", item.getUserId());
            intent.putExtra("imageUrl", item.getImageUrl());
            intent.putExtra("timestamp", item.getTimestamp());

            startActivity(intent);
        });

        // Firebase Database Connection
        databaseReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app"
                )
                .getReference("lostItems");

        // Add Item Button
        btnAddItem.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    AddItemActivity.class
            );

            startActivity(intent);
        });

        // Logout Button
        btnLogout.setOnClickListener(v -> {

            // Remove Firebase Listener
            removeFirebaseListener();

            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Reset Remember Me
            SharedPreferences prefs =
                    getSharedPreferences(
                            "CampusLostPrefs",
                            MODE_PRIVATE
                    );

            prefs.edit()
                    .putBoolean("remember_me", false)
                    .apply();

            Toast.makeText(
                    HomeActivity.this,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
            ).show();

            // Return to Welcome Screen
            Intent intent = new Intent(
                    HomeActivity.this,
                    MainActivityJava.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);

            finish();
        });

        // Search Functionality
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {

                        searchText = query.trim().toLowerCase(Locale.ROOT);

                        applyFilters();

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {

                        searchText = newText.trim().toLowerCase(Locale.ROOT);

                        applyFilters();

                        return true;
                    }
                }
        );

        // All Filter
        btnAll.setOnClickListener(v -> {

            selectedFilter = "All";

            applyFilters();
        });

        // Lost Filter
        btnLost.setOnClickListener(v -> {

            selectedFilter = "Lost";

            applyFilters();
        });

        // Found Filter
        btnFound.setOnClickListener(v -> {

            selectedFilter = "Found";

            applyFilters();
        });

        // Load Firebase Items
        loadItems();

        // Initialize Shake Sensor
        initializeShakeSensor();
    }

    // Load Items from Firebase
    private void loadItems() {

        // Prevent duplicate Firebase listeners
        removeFirebaseListener();

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                allItems.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    LostItem item =
                            data.getValue(LostItem.class);

                    if (item != null) {

                        item.setItemId(data.getKey());

                        allItems.add(item);
                    }
                }

                applyFilters();
            }

            @Override
            public void onCancelled(DatabaseError error) {

                Toast.makeText(
                        HomeActivity.this,
                        "Database Error: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        databaseReference.addValueEventListener(
                valueEventListener
        );

        listenerAttached = true;
    }

    // Remove Firebase Listener
    private void removeFirebaseListener() {

        if (listenerAttached &&
                valueEventListener != null &&
                databaseReference != null) {

            databaseReference.removeEventListener(
                    valueEventListener
            );

            listenerAttached = false;
            valueEventListener = null;
        }
    }

    // Search and Filter Logic
    private void applyFilters() {

        displayedItems.clear();

        for (LostItem item : allItems) {

            String title = item.getTitle() == null
                    ? ""
                    : item.getTitle().trim().toLowerCase(Locale.ROOT);

            String description = item.getDescription() == null
                    ? ""
                    : item.getDescription().trim().toLowerCase(Locale.ROOT);

            String location = item.getLocation() == null
                    ? ""
                    : item.getLocation().trim().toLowerCase(Locale.ROOT);

            String type = item.getType() == null
                    ? ""
                    : item.getType().trim().toLowerCase(Locale.ROOT);

            boolean matchesSearch =
                    title.contains(searchText) ||
                            description.contains(searchText) ||
                            location.contains(searchText);

            boolean matchesFilter =
                    selectedFilter.equalsIgnoreCase("All") ||
                            type.equals(selectedFilter.toLowerCase(Locale.ROOT));

            if (matchesSearch && matchesFilter) {

                displayedItems.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }

    // Initialize Accelerometer Shake Sensor
    private void initializeShakeSensor() {

        sensorManager = (SensorManager)
                getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {

            accelerometer = sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER
            );
        }

        if (accelerometer == null) {

            Toast.makeText(
                    this,
                    "Accelerometer not available",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        sensorEventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double acceleration = Math.sqrt(
                        x * x + y * y + z * z
                );

                long currentTime = System.currentTimeMillis();

                if (acceleration > SHAKE_THRESHOLD &&
                        currentTime - lastShakeTime > SHAKE_COOLDOWN) {

                    lastShakeTime = currentTime;

                    // Refresh Firebase Items
                    loadItems();

                    Toast.makeText(
                            HomeActivity.this,
                            "Refreshing items...",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onAccuracyChanged(
                    Sensor sensor,
                    int accuracy
            ) {
                // No action needed
            }
        };
    }

    // Register Sensor when Activity resumes
    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null &&
                accelerometer != null &&
                sensorEventListener != null) {

            sensorManager.registerListener(
                    sensorEventListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
        }
    }

    // Unregister Sensor when Activity pauses
    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null &&
                sensorEventListener != null) {

            sensorManager.unregisterListener(
                    sensorEventListener
            );
        }
    }

    // Clean up Firebase Listener
    @Override
    protected void onDestroy() {
        super.onDestroy();

        removeFirebaseListener();
    }
}
