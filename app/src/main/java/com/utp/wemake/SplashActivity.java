package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // Infla el layout correspondiente a la pantalla de splash
        setContentView(R.layout.activity_splash);

        // Handler que permite ejecutar código después de 3 segundos (3000 ms)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Lanza la actividad de bienvenida (WelcomeActivity)
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                // Finaliza la SplashActivity para que no se pueda volver atrás
                finish();
            }
        }, 3000); // Tiempo en milisegundos (3 segundos)

    }
}