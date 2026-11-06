package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class HomeActivity extends Activity {

    RecyclerView recyclerView;

    Button btnAddItem, btnLogout;
    Button btnAll, btnLost, btnFound;

    SearchView searchView;

    ArrayList<LostItem> allItems;
    ArrayList<LostItem> displayedItems;

    LostItemAdapter adapter;

    DatabaseReference databaseReference;

    ValueEventListener valueEventListener;

    String selectedFilter = "All";
    String searchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recyclerView);

        btnAddItem = findViewById(R.id.btnAddItem);
        btnLogout = findViewById(R.id.btnLogout);

        btnAll = findViewById(R.id.btnAll);
        btnLost = findViewById(R.id.btnLost);
        btnFound = findViewById(R.id.btnFound);

        searchView = findViewById(R.id.searchView);

        allItems = new ArrayList<>();
        displayedItems = new ArrayList<>();

        adapter = new LostItemAdapter(displayedItems);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    ItemDetailsActivity.class);

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

        databaseReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        // Add Item

        btnAddItem.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    AddItemActivity.class);

            startActivity(intent);
        });

        // Logout

        btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            SharedPreferences prefs =
                    getSharedPreferences(
                            "CampusLostPrefs",
                            MODE_PRIVATE);

            prefs.edit().clear().apply();

            Intent intent = new Intent(
                    HomeActivity.this,
                    MainActivityJava.class);

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            finish();
        });

        // Search

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {

                        searchText = query.toLowerCase().trim();

                        applyFilters();

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {

                        searchText = newText.toLowerCase().trim();

                        applyFilters();

                        return true;
                    }
                });

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

        loadItems();
    }

    private void loadItems() {

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
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        databaseReference.addValueEventListener(
                valueEventListener);
    }


    private void applyFilters() {

        displayedItems.clear();

        for (LostItem item : allItems) {

            // Get item information
            String title = item.getTitle() == null
                    ? "" : item.getTitle().trim().toLowerCase();

            String description = item.getDescription() == null
                    ? "" : item.getDescription().trim().toLowerCase();

            String location = item.getLocation() == null
                    ? "" : item.getLocation().trim().toLowerCase();

            String type = item.getType() == null
                    ? "" : item.getType().trim();

            // Search in title, description and location
            boolean matchesSearch =
                    title.contains(searchText) ||
                            description.contains(searchText) ||
                            location.contains(searchText);

            // Lost / Found filter
            boolean matchesFilter =
                    selectedFilter.equalsIgnoreCase("All") ||
                            type.equalsIgnoreCase(selectedFilter);

            if (matchesSearch && matchesFilter) {
                displayedItems.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (valueEventListener != null &&
                databaseReference != null) {

            databaseReference.removeEventListener(
                    valueEventListener);
        }
    }
}