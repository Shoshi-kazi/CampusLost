
package com.example.campuslost;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddItemActivity extends Activity {

    EditText etTitle, etDescription, etLocation, etType;
    Button btnSaveItem;

    DatabaseReference databaseReference;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        etTitle = findViewById(R.id.etItemTitle);
        etDescription = findViewById(R.id.etItemDescription);
        etLocation = findViewById(R.id.etItemLocation);
        etType = findViewById(R.id.etItemType);

        btnSaveItem = findViewById(R.id.btnSaveItem);

        auth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        btnSaveItem.setOnClickListener(v -> saveItem());
    }

    private void saveItem() {

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String type = etType.getText().toString().trim();

        // Validate title
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        // Validate description
        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        // Validate location
        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        // Validate item type
        if (!type.equalsIgnoreCase("Lost")
                && !type.equalsIgnoreCase("Found")) {

            etType.setError("Enter Lost or Found");
            etType.requestFocus();
            return;
        }

        // Check logged-in user
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Generate item ID
        String itemId = databaseReference.push().getKey();

        if (itemId == null) {
            Toast.makeText(this,
                    "Unable to create item ID",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // Create LostItem object
        LostItem item = new LostItem(
                itemId,
                title,
                description,
                location,
                type,
                userId,
                timestamp
        );

        // Disable button while saving
        btnSaveItem.setEnabled(false);

        databaseReference.child(itemId)
                .setValue(item)
                .addOnCompleteListener(task -> {

                    btnSaveItem.setEnabled(true);

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Item saved successfully",
                                Toast.LENGTH_SHORT).show();

                        finish();

                    } else {

                        Exception error = task.getException();

                        Log.e("AddItemActivity",
                                "Firebase save failed", error);

                        String errorMessage = (error != null)
                                ? error.getMessage()
                                : "Unknown error";

                        Toast.makeText(this,
                                "Failed: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
