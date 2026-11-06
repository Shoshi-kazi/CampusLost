package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends Activity {

    // Student Information
    private EditText etName;
    private EditText etDepartment;
    private EditText etStudentId;
    private EditText etBatch;

    // Account Information
    private EditText etEmail;
    private EditText etPassword;

    private Button btnRegister;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    // Firebase Realtime Database
    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Views
        etName = findViewById(R.id.etRegisterName);
        etDepartment = findViewById(R.id.etRegisterDepartment);
        etStudentId = findViewById(R.id.etRegisterStudentId);
        etBatch = findViewById(R.id.etRegisterBatch);

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);

        btnRegister = findViewById(R.id.btnRegister);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        usersReference = FirebaseDatabase
                .getInstance(
                        "https://campuslost-feaf4-default-rtdb.asia-southeast1.firebasedatabase.app"
                )
                .getReference("users");

        // Register Button
        btnRegister.setOnClickListener(v -> registerStudent());
    }

    // Register Student
    private void registerStudent() {

        // Get Student Information
        String name = etName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();

        // Get Account Information
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate Full Name
        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter your full name");
            etName.requestFocus();
            return;
        }

        // Validate Department
        if (TextUtils.isEmpty(department)) {
            etDepartment.setError("Enter your department");
            etDepartment.requestFocus();
            return;
        }

        // Validate Student ID
        if (TextUtils.isEmpty(studentId)) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return;
        }

        // Validate Batch
        if (TextUtils.isEmpty(batch)) {
            etBatch.setError("Enter your batch");
            etBatch.requestFocus();
            return;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Disable Button During Registration
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // Create Firebase Authentication Account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser currentUser =
                                firebaseAuth.getCurrentUser();

                        if (currentUser != null) {

                            String userId = currentUser.getUid();

                            // Prepare Student Profile Data
                            HashMap<String, Object> studentProfile =
                                    new HashMap<>();

                            studentProfile.put("name", name);
                            studentProfile.put("department", department);
                            studentProfile.put("studentId", studentId);
                            studentProfile.put("batch", batch);
                            studentProfile.put("email", email);

                            // Save Profile to Firebase Database
                            usersReference.child(userId)
                                    .setValue(studentProfile)
                                    .addOnCompleteListener(databaseTask -> {

                                        if (databaseTask.isSuccessful()) {

                                            Toast.makeText(
                                                    RegisterActivity.this,
                                                    "Student account created successfully!",
                                                    Toast.LENGTH_LONG
                                            ).show();

                                            // Open HomeActivity
                                            Intent intent = new Intent(
                                                    RegisterActivity.this,
                                                    HomeActivity.class
                                            );

                                            intent.setFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            );

                                            startActivity(intent);
                                            finish();

                                        } else {

                                            btnRegister.setEnabled(true);
                                            btnRegister.setText("Register");

                                            Toast.makeText(
                                                    RegisterActivity.this,
                                                    "Profile save failed: "
                                                            + databaseTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    });

                        } else {

                            btnRegister.setEnabled(true);
                            btnRegister.setText("Register");

                            Toast.makeText(
                                    RegisterActivity.this,
                                    "User account could not be loaded",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");

                        String errorMessage =
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Registration failed";

                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
