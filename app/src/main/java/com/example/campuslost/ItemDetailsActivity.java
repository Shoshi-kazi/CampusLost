package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ItemDetailsActivity extends Activity {

    ImageView ivDetailsImage;

    TextView tvDetailsTitle;
    TextView tvDetailsDescription;
    TextView tvDetailsLocation;
    TextView tvDetailsType;

    Button btnEdit;
    Button btnDelete;

    String itemId;
    String title;
    String description;
    String location;
    String type;
    String userId;
    String imageUrl;

    long timestamp;

    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_details);

        ivDetailsImage = findViewById(R.id.ivDetailsImage);

        tvDetailsTitle = findViewById(R.id.tvDetailsTitle);
        tvDetailsDescription = findViewById(R.id.tvDetailsDescription);
        tvDetailsLocation = findViewById(R.id.tvDetailsLocation);
        tvDetailsType = findViewById(R.id.tvDetailsType);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        Intent intent = getIntent();

        itemId = intent.getStringExtra("itemId");
        title = intent.getStringExtra("title");
        description = intent.getStringExtra("description");
        location = intent.getStringExtra("location");
        type = intent.getStringExtra("type");
        userId = intent.getStringExtra("userId");
        imageUrl = intent.getStringExtra("imageUrl");

        timestamp = intent.getLongExtra("timestamp", 0);

        tvDetailsTitle.setText(title);
        tvDetailsDescription.setText(description);
        tvDetailsLocation.setText("Location: " + location);
        tvDetailsType.setText("Status: " + type);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(ivDetailsImage);

        databaseReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        String currentUserId = FirebaseAuth
                .getInstance()
                .getUid();

        boolean isOwner = currentUserId != null &&
                currentUserId.equals(userId);

        if (!isOwner) {

            btnEdit.setVisibility(android.view.View.GONE);
            btnDelete.setVisibility(android.view.View.GONE);
        }

        btnEdit.setOnClickListener(v -> {

            Intent editIntent = new Intent(
                    ItemDetailsActivity.this,
                    EditItemActivity.class);

            editIntent.putExtra("itemId", itemId);
            editIntent.putExtra("title", title);
            editIntent.putExtra("description", description);
            editIntent.putExtra("location", location);
            editIntent.putExtra("type", type);
            editIntent.putExtra("userId", userId);

            startActivity(editIntent);
        });

        btnDelete.setOnClickListener(v -> {

            if (!isOwner) {
                Toast.makeText(this,
                        "You cannot delete this item",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        databaseReference.child(itemId)
                                .removeValue()
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(
                                            this,
                                            "Item deleted successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    finish();
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            this,
                                            "Delete failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}