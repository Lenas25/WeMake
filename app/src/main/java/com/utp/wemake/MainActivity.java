package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    TextView tvWelcomeMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);

        Intent intent = getIntent();

        if (intent != null && intent.hasExtra(LoginActivity.USER_NAME)) {
            String userName = intent.getStringExtra(LoginActivity.USER_NAME);

            String welcomeText = "¡Bienvenido, " + userName + "!";
            tvWelcomeMessage.setText(welcomeText);
        }
    }
}