
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.EditText;
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
    Button btnAddItem, btnLogout, btnProfile;
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

    private static final float SHAKE_THRESHOLD = 3.5f;

    // Three Shake Settings
    private int shakeCount = 0;
    private long lastShakeTime = 0;

    private static final long SHAKE_SEQUENCE_WINDOW = 2500;
    private static final long SHAKE_EVENT_GAP = 350;

    // Gravity Filter
    private float gravityX = 0;
    private float gravityY = 0;
    private float gravityZ = 0;

    private boolean firstSensorReading = true;

    private static final float GRAVITY_ALPHA = 0.8f;

    // Voice Assistant
    private CampusLostVoiceAssistant voiceAssistant;

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

        // Make Search Text Black
        int searchTextId = getResources().getIdentifier(
                "search_src_text",
                "id",
                "android"
        );

        EditText searchTextView = searchView.findViewById(searchTextId);

        if (searchTextView != null) {
            searchTextView.setTextColor(Color.BLACK);
            searchTextView.setHintTextColor(Color.GRAY);
        }

        // Profile Button
        btnProfile = findViewById(R.id.btnProfile);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    ProfileActivity.class
            );
            startActivity(intent);
        });

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

            removeFirebaseListener();

            FirebaseAuth.getInstance().signOut();

            SharedPreferences prefs = getSharedPreferences(
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

        // Initialize Voice Assistant
        voiceAssistant = new CampusLostVoiceAssistant(
                this,
                this::handleVoiceCommand
        );

        // Firebase Real-time Listener
        loadItems();

        // Initialize Shake Sensor
        initializeShakeSensor();
    }

    // Handle Voice Commands
    private void handleVoiceCommand(String command) {

        if (command == null || command.trim().isEmpty()) {

            voiceAssistant.speak(
                    "I could not understand your command."
            );

            return;
        }

        String input = command.toLowerCase(Locale.ROOT).trim();

        if (input.contains("lost")) {

            selectedFilter = "Lost";
            applyFilters();

            voiceAssistant.speak("Showing lost items.");

        } else if (input.contains("found")) {

            selectedFilter = "Found";
            applyFilters();

            voiceAssistant.speak("Showing found items.");

        } else if (input.contains("all")) {

            selectedFilter = "All";
            applyFilters();

            voiceAssistant.speak("Showing all items.");

        } else if (input.contains("add")) {

            voiceAssistant.speak("Opening add item.");

            Intent intent = new Intent(
                    HomeActivity.this,
                    AddItemActivity.class
            );

            startActivity(intent);

        } else {

            voiceAssistant.speak(
                    "Sorry, I don't know that command."
            );
        }
    }

    // Attach Firebase Listener
    private void loadItems() {

        if (listenerAttached || databaseReference == null) {
            return;
        }

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                updateItemList(snapshot);
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

        databaseReference.addValueEventListener(valueEventListener);
        listenerAttached = true;
    }

    // Update RecyclerView Data
    private void updateItemList(DataSnapshot snapshot) {

        allItems.clear();

        for (DataSnapshot data : snapshot.getChildren()) {

            LostItem item = data.getValue(LostItem.class);

            if (item != null) {

                item.setItemId(data.getKey());
                allItems.add(item);
            }
        }

        applyFilters();
    }

    // Remove Firebase Listener
    private void removeFirebaseListener() {

        if (listenerAttached &&
                valueEventListener != null &&
                databaseReference != null) {

            databaseReference.removeEventListener(valueEventListener);

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

    // Initialize Shake Sensor
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

                if (firstSensorReading) {

                    gravityX = x;
                    gravityY = y;
                    gravityZ = z;

                    firstSensorReading = false;
                    return;
                }

                gravityX = GRAVITY_ALPHA * gravityX
                        + (1 - GRAVITY_ALPHA) * x;

                gravityY = GRAVITY_ALPHA * gravityY
                        + (1 - GRAVITY_ALPHA) * y;

                gravityZ = GRAVITY_ALPHA * gravityZ
                        + (1 - GRAVITY_ALPHA) * z;

                float linearX = x - gravityX;
                float linearY = y - gravityY;
                float linearZ = z - gravityZ;

                double shakeForce = Math.sqrt(
                        linearX * linearX +
                                linearY * linearY +
                                linearZ * linearZ
                );

                long currentTime = System.currentTimeMillis();

                if (shakeForce > SHAKE_THRESHOLD) {

                    if (currentTime - lastShakeTime > SHAKE_SEQUENCE_WINDOW) {
                        shakeCount = 0;
                    }

                    if (currentTime - lastShakeTime >= SHAKE_EVENT_GAP) {

                        shakeCount++;
                        lastShakeTime = currentTime;

                        Toast.makeText(
                                HomeActivity.this,
                                "Shake " + shakeCount + "/3",
                                Toast.LENGTH_SHORT
                        ).show();

                        if (shakeCount >= 3) {

                            shakeCount = 0;
                            lastShakeTime = 0;

                            Toast.makeText(
                                    HomeActivity.this,
                                    "Voice Assistant Activated!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            if (voiceAssistant != null) {
                                voiceAssistant.startListening();
                            }
                        }
                    }
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

    // Register Sensor
    @Override
    protected void onResume() {
        super.onResume();

        if (sensorManager != null &&
                accelerometer != null &&
                sensorEventListener != null) {

            sensorManager.registerListener(
                    sensorEventListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
            );
        }
    }

    // Unregister Sensor
    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null &&
                sensorEventListener != null) {

            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    // Microphone Permission Result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (voiceAssistant != null) {

            voiceAssistant.onPermissionResult(
                    requestCode,
                    grantResults
            );
        }
    }

    // Clean Up Resources
    @Override
    protected void onDestroy() {
        super.onDestroy();

        removeFirebaseListener();

        if (voiceAssistant != null) {
            voiceAssistant.destroy();
            voiceAssistant = null;
        }
    }
}
