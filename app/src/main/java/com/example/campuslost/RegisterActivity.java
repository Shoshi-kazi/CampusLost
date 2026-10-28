package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends Activity {

    TextInputEditText etEmail, etPassword;
    Button btnRegister;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);

        auth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            if (password.length() < 6) {
                etPassword.setError(
                        "Password must be at least 6 characters");
                return;
            }

            btnRegister.setEnabled(false);

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        btnRegister.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(
                                    this,
                                    "Account created successfully",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent =
                                    new Intent(
                                            RegisterActivity.this,
                                            HomeActivity.class);

                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(
                                    this,
                                    "Registration failed. Try another email.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
