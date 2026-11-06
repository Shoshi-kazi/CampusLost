
package com.example.campuslost;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivityJava extends Activity {

    Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Open Login Screen
        btnLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivityJava.this,
                    LoginActivity.class
            );

            startActivity(intent);

        });

        // Open Registration Screen
        btnRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivityJava.this,
                    RegisterActivity.class
            );

            startActivity(intent);

        });
    }
}
