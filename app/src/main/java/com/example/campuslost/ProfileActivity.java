
package com.example.campuslost;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class ProfileActivity extends Activity {

    private TextView tvName, tvDepartment, tvStudentId;
    private TextView tvBatch, tvEmail;
    private Button btnBack;

    private DatabaseReference usersReference;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvProfileName);
        tvDepartment = findViewById(R.id.tvProfileDepartment);
        tvStudentId = findViewById(R.id.tvProfileStudentId);
        tvBatch = findViewById(R.id.tvProfileBatch);
        tvEmail = findViewById(R.id.tvProfileEmail);

        btnBack = findViewById(R.id.btnBackProfile);

        firebaseAuth = FirebaseAuth.getInstance();

        usersReference = FirebaseDatabase.getInstance(
                "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).getReference("users");

        btnBack.setOnClickListener(v -> finish());

        saveOldAccountProfile();
    }

    // Save profile for the existing account if it does not exist
    private void saveOldAccountProfile() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String userId = currentUser.getUid();

        DatabaseReference profileReference =
                usersReference.child(userId);

        profileReference.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (snapshot.exists()) {

                            // Profile already exists
                            loadStudentProfile();

                        } else {

                            // Create profile for the old account
                            HashMap<String, Object> studentProfile =
                                    new HashMap<>();

                            studentProfile.put(
                                    "name",
                                    "Nusaiba Jannat Shoshi"
                            );

                            studentProfile.put("department", "CSE");

                            studentProfile.put(
                                    "studentId",
                                    "4r9874934"
                            );

                            studentProfile.put("batch", "D-85");

                            studentProfile.put(
                                    "email",
                                    currentUser.getEmail()
                            );

                            profileReference.setValue(studentProfile)
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(
                                                ProfileActivity.this,
                                                "Profile created successfully!",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        loadStudentProfile();
                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                ProfileActivity.this,
                                                "Profile save failed: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "Database Error: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // Load student profile from Firebase
    private void loadStudentProfile() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        String userId = currentUser.getUid();

        usersReference.child(userId)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(DataSnapshot snapshot) {

                                if (snapshot.exists()) {

                                    String name = snapshot.child("name")
                                            .getValue(String.class);

                                    String department = snapshot.child("department")
                                            .getValue(String.class);

                                    String studentId = snapshot.child("studentId")
                                            .getValue(String.class);

                                    String batch = snapshot.child("batch")
                                            .getValue(String.class);

                                    String email = snapshot.child("email")
                                            .getValue(String.class);

                                    tvName.setText(
                                            "Name: " + getValue(name)
                                    );

                                    tvDepartment.setText(
                                            "Department: " + getValue(department)
                                    );

                                    tvStudentId.setText(
                                            "Student ID: " + getValue(studentId)
                                    );

                                    tvBatch.setText(
                                            "Batch: " + getValue(batch)
                                    );

                                    tvEmail.setText(
                                            "Email: " + getValue(email)
                                    );

                                } else {

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Student profile not found",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Database Error: " + error.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    // Handle empty values
    private String getValue(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not available" : value;
    }
}
