
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ItemDetailsActivity extends Activity {

    ImageView ivDetailsImage;

    TextView tvDetailsTitle;
    TextView tvDetailsDescription;
    TextView tvDetailsLocation;
    TextView tvDetailsType;
    TextView tvDetailsPoster;
    TextView tvDetailsContact;

    Button btnEdit;
    Button btnDelete;

    String itemId;
    String title;
    String description;
    String location;
    String type;
    String userId;
    String imageUrl;
    String posterName;
    String contactNumber;

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
        tvDetailsPoster = findViewById(R.id.tvDetailsPoster);
        tvDetailsContact = findViewById(R.id.tvDetailsContact);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        itemId = getIntent().getStringExtra("itemId");

        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Item not found",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase
                .getInstance("https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("lostItems");

        btnEdit.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);

        loadItemDetails();
    }

    private void loadItemDetails() {

        databaseReference.child(itemId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(ItemDetailsActivity.this,
                                    "This item no longer exists",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        LostItem item = snapshot.getValue(LostItem.class);

                        if (item == null) {
                            Toast.makeText(ItemDetailsActivity.this,
                                    "Unable to load item details",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        title = item.getTitle();
                        description = item.getDescription();
                        location = item.getLocation();
                        type = item.getType();
                        userId = item.getUserId();
                        imageUrl = item.getImageUrl();
                        posterName = item.getPosterName();
                        contactNumber = item.getContactNumber();
                        timestamp = item.getTimestamp();

                        tvDetailsTitle.setText(
                                title != null ? title : "");

                        tvDetailsDescription.setText(
                                description != null ? description : "");

                        tvDetailsLocation.setText(
                                "Location: " +
                                        (location != null ? location : "Not available"));

                        tvDetailsType.setText(
                                "Status: " +
                                        (type != null ? type : "Not available"));

                        tvDetailsPoster.setText(
                                "Posted By: " +
                                        (posterName != null && !posterName.trim().isEmpty()
                                                ? posterName : "Not available"));

                        tvDetailsContact.setText(
                                "Contact Number: " +
                                        (contactNumber != null && !contactNumber.trim().isEmpty()
                                                ? contactNumber : "Not available"));

                        Glide.with(ItemDetailsActivity.this)
                                .load(imageUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_gallery)
                                .centerCrop()
                                .into(ivDetailsImage);

                        checkItemOwnership();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ItemDetailsActivity.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkItemOwnership() {

        String currentUserId = FirebaseAuth.getInstance().getUid();

        boolean isOwner = currentUserId != null
                && currentUserId.equals(userId);

        if (isOwner) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
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
            editIntent.putExtra("imageUrl", imageUrl);
            editIntent.putExtra("posterName", posterName);
            editIntent.putExtra("contactNumber", contactNumber);

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

                                    Toast.makeText(this,
                                            "Item deleted successfully",
                                            Toast.LENGTH_SHORT).show();

                                    finish();
                                })
                                .addOnFailureListener(error -> {

                                    Toast.makeText(this,
                                            "Delete failed: " + error.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
