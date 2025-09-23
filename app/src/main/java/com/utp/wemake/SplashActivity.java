package com.utp.wemake;

// EN SplashActivity.java

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos de retraso

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Asegúrate de tener este layout

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Instancia de Firebase Auth
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                // Verificamos si hay un usuario con sesión iniciada
                if (currentUser != null) {
                    // Si hay un usuario, vamos directamente a MainActivity
                    // Obtener el nombre de Firestore primero.
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    // Pasamos el nombre del usuario para personalizar la bienvenida
                    String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Usuario";
                    intent.putExtra(LoginActivity.USER_NAME, userName);
                    startActivity(intent);
                } else {
                    // Si no hay usuario, vamos a LoginActivity
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                }

                // Finalizamos la SplashActivity para que el usuario no pueda volver a ella
                finish();
            }
        }, SPLASH_DELAY);
    }
}