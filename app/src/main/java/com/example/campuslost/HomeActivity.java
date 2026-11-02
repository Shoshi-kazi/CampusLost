package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
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

    ArrayList<LostItem> itemList;
    LostItemAdapter adapter;

    DatabaseReference databaseReference;
    ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recyclerView = findViewById(R.id.recyclerView);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnLogout = findViewById(R.id.btnLogout);

        itemList = new ArrayList<>();

        adapter = new LostItemAdapter(itemList);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        recyclerView.setAdapter(adapter);

        databaseReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        btnAddItem.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    AddItemActivity.class);

            startActivity(intent);
        });

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
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });

        loadItems();
    }

    private void loadItems() {

        valueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                itemList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    LostItem item =
                            data.getValue(LostItem.class);

                    if (item != null) {

                        item.setItemId(data.getKey());

                        itemList.add(item);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {

                Toast.makeText(
                        HomeActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        databaseReference.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }
}
