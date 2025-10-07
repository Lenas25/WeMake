package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.utp.wemake.viewmodels.SplashViewModel;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Inicializa el ViewModel. Será el "cerebro" que decida a dónde ir.
        SplashViewModel viewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        // 2. Observa el "destino". La actividad espera pasivamente a que el ViewModel le diga a dónde navegar.
        viewModel.getDestination().observe(this, destination -> {
            if (destination != null) {
                // Cuando el ViewModel publica un destino, llamamos a nuestro método de navegación.
                navigateTo(destination);
            }
        });

        // 3. Después de un retraso, le pide al ViewModel que inicie el proceso de decisión.
        new Handler(Looper.getMainLooper()).postDelayed(viewModel::decideNextScreen, SPLASH_DELAY);
    }

    /**
     * Navega a la actividad correspondiente según el destino decidido por el ViewModel.
     * @param dest El destino (LOGIN, WELCOME, o MAIN).
     */
    private void navigateTo(SplashViewModel.Destination dest) {
        Class<?> targetActivity;
        switch (dest) {
            case SETUP:
                // El usuario está logueado pero no tiene tableros.
                targetActivity = SetupActivity.class;
                break;
            case MAIN:
                // El usuario está logueado y ya tiene tableros.
                targetActivity = MainActivity.class;
                break;
            case LOGIN:
            default:
                // No hay usuario logueado.
                targetActivity = LoginActivity.class;
                break;
        }

        Intent intent = new Intent(this, targetActivity);

        startActivity(intent);
        finish();
    }
}