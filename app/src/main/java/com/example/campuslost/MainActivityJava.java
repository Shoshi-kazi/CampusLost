package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivityJava extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences(
                        "CampusLostPrefs",
                        MODE_PRIVATE);

        boolean remember =
                prefs.getBoolean("remember_me", false);

        if (remember
                && FirebaseAuth.getInstance().getCurrentUser() != null) {

            startActivity(
                    new Intent(this, HomeActivity.class));

            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            MainActivityJava.this,
                            LoginActivity.class);

            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent =
                    new Intent(
                            MainActivityJava.this,
                            RegisterActivity.class);

            startActivity(intent);
        });
    }
}
