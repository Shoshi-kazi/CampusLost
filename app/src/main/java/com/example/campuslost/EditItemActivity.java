
package com.example.campuslost;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditItemActivity extends Activity {

    EditText etTitle, etDescription, etLocation, etType;
    Button btnUpdate;

    String itemId;
    String currentUserId;

    DatabaseReference databaseReference;

    boolean canEdit = false;

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

        currentUserId = FirebaseAuth.getInstance().getUid();

        databaseReference = FirebaseDatabase.getInstance(
                "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("lostItems");

        btnUpdate.setEnabled(false);

        if (currentUserId == null || itemId == null ||
                itemId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Invalid item or login session",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        loadItemData();

        btnUpdate.setOnClickListener(v -> updateItem());
    }

    // Load item and verify its actual owner
    private void loadItemData() {

        databaseReference.child(itemId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot snapshot) {

                                if (!snapshot.exists()) {

                                    Toast.makeText(
                                            EditItemActivity.this,
                                            "Item not found",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    finish();
                                    return;
                                }

                                String ownerId = snapshot.child("userId")
                                        .getValue(String.class);

                                if (ownerId == null ||
                                        !ownerId.equals(currentUserId)) {

                                    Toast.makeText(
                                            EditItemActivity.this,
                                            "You cannot edit this item",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    finish();
                                    return;
                                }

                                etTitle.setText(getValue(snapshot, "title"));
                                etDescription.setText(
                                        getValue(snapshot, "description"));
                                etLocation.setText(
                                        getValue(snapshot, "location"));
                                etType.setText(getValue(snapshot, "type"));

                                canEdit = true;
                                btnUpdate.setEnabled(true);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {

                                Toast.makeText(
                                        EditItemActivity.this,
                                        "Database Error: " + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                            }
                        }
                );
    }

    // Update existing item
    private void updateItem() {

        if (!canEdit) {
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String type = etType.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter item title");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Enter description");
            etDescription.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etLocation.setError("Enter location");
            etLocation.requestFocus();
            return;
        }

        if (type.isEmpty()) {
            etType.setError("Enter item type");
            etType.requestFocus();
            return;
        }

        if (!type.equalsIgnoreCase("Lost") &&
                !type.equalsIgnoreCase("Found")) {

            etType.setError("Enter Lost or Found");
            etType.requestFocus();
            return;
        }

        type = type.substring(0, 1).toUpperCase()
                + type.substring(1).toLowerCase();

        Map<String, Object> updates = new HashMap<>();

        updates.put("title", title);
        updates.put("description", description);
        updates.put("location", location);
        updates.put("type", type);

        btnUpdate.setEnabled(false);

        databaseReference.child(itemId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            EditItemActivity.this,
                            "Item updated successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnUpdate.setEnabled(true);

                    Toast.makeText(
                            EditItemActivity.this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Read values safely from Firebase
    private String getValue(DataSnapshot snapshot, String key) {

        String value = snapshot.child(key).getValue(String.class);

        return value == null ? "" : value;
    }
}
