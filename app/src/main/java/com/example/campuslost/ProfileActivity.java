
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

        loadStudentProfile();
    }

    // Load logged-in student's profile from Firebase
    private void loadStudentProfile() {

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
                                            "Student profile not found. Please register again.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finish();
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

    // Handle empty or missing values
    private String getValue(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not available" : value;
    }
}
