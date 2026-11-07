package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class HomeActivity extends Activity {

    private DrawerLayout drawerLayout;

    private RecyclerView recyclerView;
    private FloatingActionButton btnAddItem;

    private Button btnLogout, btnProfile;
    private Button btnAll, btnLost, btnFound;
    private Button btnMenu;

    private Button navHome, navAddItem, navProfile, navLogout;

    private SearchView searchView;

    private TextView tvTotalCount, tvLostCount, tvFoundCount;
    private TextView tvEmptyState;

    private ArrayList<LostItem> allItems;
    private ArrayList<LostItem> displayedItems;
    private LostItemAdapter adapter;

    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private boolean listenerAttached = false;

    private String selectedFilter = "All";
    private String searchText = "";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener sensorEventListener;

    private static final float SHAKE_THRESHOLD = 3.5f;
    private static final long SHAKE_SEQUENCE_WINDOW = 2500;
    private static final long SHAKE_EVENT_GAP = 350;

    private int shakeCount = 0;
    private long lastShakeTime = 0;

    private float gravityX = 0;
    private float gravityY = 0;
    private float gravityZ = 0;

    private boolean firstSensorReading = true;
    private static final float GRAVITY_ALPHA = 0.8f;

    private CampusLostVoiceAssistant voiceAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        initializeRecyclerView();
        initializeButtons();
        initializeDrawer();
        initializeSearch();
        initializeFirebase();
        initializeVoiceAssistant();
        initializeShakeSensor();

        updateFilterButtonStyles();
        loadItems();
    }

    private void initializeViews() {

        drawerLayout = findViewById(R.id.drawerLayout);

        recyclerView = findViewById(R.id.recyclerView);

        btnAddItem = findViewById(R.id.btnAddItem);
        btnLogout = findViewById(R.id.btnLogout);
        btnProfile = findViewById(R.id.btnProfile);
        btnMenu = findViewById(R.id.btnMenu);

        btnAll = findViewById(R.id.btnAll);
        btnLost = findViewById(R.id.btnLost);
        btnFound = findViewById(R.id.btnFound);

        navHome = findViewById(R.id.navHome);
        navAddItem = findViewById(R.id.navAddItem);
        navProfile = findViewById(R.id.navProfile);
        navLogout = findViewById(R.id.navLogout);

        searchView = findViewById(R.id.searchView);

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvLostCount = findViewById(R.id.tvLostCount);
        tvFoundCount = findViewById(R.id.tvFoundCount);

        tvEmptyState = findViewById(R.id.tvEmptyState);

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
    }

    private void initializeRecyclerView() {

        allItems = new ArrayList<>();
        displayedItems = new ArrayList<>();

        adapter = new LostItemAdapter(displayedItems);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerView.setAdapter(adapter);

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
    }

    private void initializeButtons() {

        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.START)
        );

        btnProfile.setOnClickListener(v -> openProfile());

        btnAddItem.setOnClickListener(v -> openAddItem());

        btnLogout.setOnClickListener(v -> logoutUser());

        btnAll.setOnClickListener(v -> changeFilter("All"));
        btnLost.setOnClickListener(v -> changeFilter("Lost"));
        btnFound.setOnClickListener(v -> changeFilter("Found"));
    }

    private void initializeDrawer() {

        navHome.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            selectedFilter = "All";
            updateFilterButtonStyles();
            applyFilters();
            showSnackbar("Home");
        });

        navAddItem.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            openAddItem();
        });

        navProfile.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            openProfile();
        });

        navLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.START);
            logoutUser();
        });
    }

    private void openProfile() {

        startActivity(new Intent(
                HomeActivity.this,
                ProfileActivity.class
        ));
    }

    private void openAddItem() {

        startActivity(new Intent(
                HomeActivity.this,
                AddItemActivity.class
        ));
    }

    private void logoutUser() {

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
    }

    private void changeFilter(String filter) {

        selectedFilter = filter;

        updateFilterButtonStyles();
        applyFilters();

        showSnackbar("Showing " + filter + " items");
    }

    private void showSnackbar(String message) {

        Snackbar.make(
                drawerLayout,
                message,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void updateFilterButtonStyles() {

        int selectedColor = Color.parseColor("#315C85");
        int normalColor = Color.parseColor("#E8EEF5");

        int selectedText = Color.WHITE;
        int normalText = Color.parseColor("#29445F");

        setFilterStyle(
                btnAll, "All",
                selectedColor, normalColor,
                selectedText, normalText
        );

        setFilterStyle(
                btnLost, "Lost",
                selectedColor, normalColor,
                selectedText, normalText
        );

        setFilterStyle(
                btnFound, "Found",
                selectedColor, normalColor,
                selectedText, normalText
        );
    }

    private void setFilterStyle(
            Button button,
            String filter,
            int selectedColor,
            int normalColor,
            int selectedText,
            int normalText
    ) {

        boolean selected = selectedFilter.equals(filter);

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        selected ? selectedColor : normalColor
                )
        );

        button.setTextColor(
                selected ? selectedText : normalText
        );
    }

    private void initializeSearch() {

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
    }

    private void initializeFirebase() {

        databaseReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app"
                )
                .getReference("lostItems");
    }

    private void initializeVoiceAssistant() {

        voiceAssistant = new CampusLostVoiceAssistant(
                this,
                this::handleVoiceCommand
        );
    }

    private void handleVoiceCommand(String command) {

        if (command == null || command.trim().isEmpty()) {

            voiceAssistant.speak(
                    "I could not understand your command."
            );

            return;
        }

        String input = command.toLowerCase(Locale.ROOT).trim();

        if (input.contains("lost")) {

            changeFilter("Lost");
            voiceAssistant.speak("Showing lost items.");

        } else if (input.contains("found")) {

            changeFilter("Found");
            voiceAssistant.speak("Showing found items.");

        } else if (input.contains("all")) {

            changeFilter("All");
            voiceAssistant.speak("Showing all items.");

        } else if (input.contains("add")) {

            voiceAssistant.speak("Opening add item.");
            openAddItem();

        } else {

            voiceAssistant.speak(
                    "Sorry, I don't know that command."
            );
        }
    }

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

    private void updateItemList(DataSnapshot snapshot) {

        allItems.clear();

        for (DataSnapshot data : snapshot.getChildren()) {

            LostItem item = data.getValue(LostItem.class);

            if (item != null) {
                item.setItemId(data.getKey());
                allItems.add(item);
            }
        }

        updateDashboardSummary();
        applyFilters();
    }

    private void updateDashboardSummary() {

        int totalCount = allItems.size();
        int lostCount = 0;
        int foundCount = 0;

        for (LostItem item : allItems) {

            String type = item.getType();

            if (type == null) {
                continue;
            }

            if (type.equalsIgnoreCase("Lost")) {
                lostCount++;
            } else if (type.equalsIgnoreCase("Found")) {
                foundCount++;
            }
        }

        tvTotalCount.setText(String.valueOf(totalCount));
        tvLostCount.setText(String.valueOf(lostCount));
        tvFoundCount.setText(String.valueOf(foundCount));
    }

    private void removeFirebaseListener() {

        if (listenerAttached &&
                valueEventListener != null &&
                databaseReference != null) {

            databaseReference.removeEventListener(valueEventListener);

            listenerAttached = false;
            valueEventListener = null;
        }
    }

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

        updateEmptyState();
    }

    private void updateEmptyState() {

        if (!displayedItems.isEmpty()) {

            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

        } else {

            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);

            if (allItems.isEmpty()) {

                tvEmptyState.setText(
                        "No items posted yet.\nBe the first to add a lost or found item!"
                );

            } else if (!searchText.isEmpty()) {

                tvEmptyState.setText(
                        "No matching items found.\nTry another search."
                );

            } else {

                tvEmptyState.setText(
                        "No " + selectedFilter.toLowerCase(Locale.ROOT)
                                + " items available right now."
                );
            }
        }
    }

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
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // No action needed
            }
        };
    }

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

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null &&
                sensorEventListener != null) {

            sensorManager.unregisterListener(sensorEventListener);
        }
    }

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
