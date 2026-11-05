package com.example.campuslost;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditItemActivity extends Activity {

    EditText etTitle;
    EditText etDescription;
    EditText etLocation;
    EditText etType;

    Button btnUpdate;

    String itemId;
    String ownerId;

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_item);

        etTitle = findViewById(R.id.etEditTitle);
        etDescription = findViewById(R.id.etEditDescription);
        etLocation = findViewById(R.id.etEditLocation);
        etType = findViewById(R.id.etEditType);

        btnUpdate = findViewById(R.id.btnUpdate);

        itemId = getIntent().getStringExtra("itemId");
        ownerId = getIntent().getStringExtra("userId");

        etTitle.setText(getIntent().getStringExtra("title"));
        etDescription.setText(
                getIntent().getStringExtra("description"));
        etLocation.setText(
                getIntent().getStringExtra("location"));
        etType.setText(
                getIntent().getStringExtra("type"));

        databaseReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        btnUpdate.setOnClickListener(v -> updateItem());
    }

    private void updateItem() {

        String currentUserId = FirebaseAuth
                .getInstance()
                .getUid();

        if (currentUserId == null ||
                !currentUserId.equals(ownerId)) {

            Toast.makeText(this,
                    "You cannot edit this item",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        String title = etTitle.getText().toString().trim();

        String description =
                etDescription.getText().toString().trim();

        String location =
                etLocation.getText().toString().trim();

        String type =
                etType.getText().toString().trim();

        if (title.isEmpty() ||
                description.isEmpty() ||
                location.isEmpty() ||
                type.isEmpty()) {

            Toast.makeText(this,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Map<String, Object> updates = new HashMap<>();

        updates.put("title", title);
        updates.put("description", description);
        updates.put("location", location);
        updates.put("type", type);

        databaseReference.child(itemId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Item updated successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}