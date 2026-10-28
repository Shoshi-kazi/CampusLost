
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends Activity {

    TextInputEditText etEmail, etPassword;
    CheckBox cbRemember;
    Button btnLogin, btnGoRegister;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> {
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

            btnLogin.setEnabled(false);

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            SharedPreferences prefs =
                                    getSharedPreferences(
                                            "CampusLostPrefs",
                                            MODE_PRIVATE);

                            prefs.edit()
                                    .putBoolean(
                                            "remember_me",
                                            cbRemember.isChecked())
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "Login successful",
                                    Toast.LENGTH_SHORT).show();

                            Intent intent =
                                    new Intent(
                                            LoginActivity.this,
                                            HomeActivity.class);

                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(
                                    this,
                                    "Login failed. Check your email and password.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnGoRegister.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            LoginActivity.this,
                            RegisterActivity.class);

            startActivity(intent);
        });
    }
}
