package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends Activity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Read Remember Me preference
            SharedPreferences prefs =
                    getSharedPreferences(
                            "CampusLostPrefs",
                            MODE_PRIVATE
                    );

            boolean remember =
                    prefs.getBoolean("remember_me", false);

            // Check Firebase login status
            boolean isLoggedIn =
                    FirebaseAuth.getInstance().getCurrentUser() != null;

            if (remember && isLoggedIn) {

                // Open Home Screen
                Intent intent = new Intent(
                        SplashActivity.this,
                        HomeActivity.class
                );

                startActivity(intent);

            } else {

                // Open Welcome Screen
                Intent intent = new Intent(
                        SplashActivity.this,
                        MainActivityJava.class
                );

                startActivity(intent);
            }

            finish();

        }, SPLASH_DELAY);
    }
}
