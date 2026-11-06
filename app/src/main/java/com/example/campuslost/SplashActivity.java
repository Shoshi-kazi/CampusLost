
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends Activity {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPreferences prefs =
                    getSharedPreferences(
                            "CampusLostPrefs",
                            MODE_PRIVATE
                    );

            boolean remember =
                    prefs.getBoolean("remember_me", false);

            if (remember &&
                    FirebaseAuth.getInstance().getCurrentUser() != null) {

                Intent intent = new Intent(
                        SplashActivity.this,
                        HomeActivity.class
                );

                startActivity(intent);

            } else {

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
