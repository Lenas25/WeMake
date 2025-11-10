package com.utp.wemake;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.utp.wemake.utils.ShakeDetector;

public class MainActivity extends AppCompatActivity {

    // Declara el NavController como una variable de clase para que sea accesible en toda la actividad.
    private NavController navController;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // El método onCreate ahora es un resumen claro de lo que se configura.
        setupNavigation();
        setupFab();
        setupBackButton();
        setupShakeDetector();
    }

    private void setupShakeDetector() {
        shakeDetector = new ShakeDetector(this);
        shakeDetector.setOnShakeListener(() -> {
            vibratePhone();
            showAddTaskBottomSheet();
        });
    }

    /**
     * Genera una vibración corta de confirmación.
     */
    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return; // No hacer nada si el dispositivo no puede vibrar
        }

        // La API de vibración cambió en Android Oreo (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Para versiones nuevas de Android
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            // Para versiones antiguas (deprecated en API 26)
            vibrator.vibrate(50); // Vibra por 50 milisegundos
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Empieza a escuchar los sensores cuando la actividad está en primer plano.
        shakeDetector.resume();
    }

    // --- AÑADIR ---
    @Override
    protected void onPause() {
        super.onPause();
        // Deja de escuchar los sensores para ahorrar batería cuando la actividad no está en primer plano.
        shakeDetector.pause();
    }

    /**
     * Configura el NavController y lo vincula con el BottomNavigationView.
     */
    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        // Asigna el NavController a la variable de clase.
        navController = navHostFragment.getNavController();

        // Vincula el BottomNavigationView con el NavController.
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
    }

    /**
     * Configura el OnClickListener para el FloatingActionButton (FAB).
     */
    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            showAddTaskBottomSheet();
        });
    }

    /**
     * Configura el comportamiento personalizado del botón de retroceso.
     */
    private void setupBackButton() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Primero, intenta que el NavController maneje el "atrás".
                // Esto es útil si estás en un sub-fragmento.
                if (navController.navigateUp()) {
                    return;
                }

                // Si no se pudo navegar hacia atrás y no estamos en la pantalla de inicio...
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                } else {
                    // Si ya estamos en la pantalla de inicio, cerramos la app.
                    finish();
                }
            }
        };
        // Registra el callback.
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    /**
     * Crea y muestra el BottomSheet para añadir una nueva tarea.
     */
    private void showAddTaskBottomSheet() {
        if (getSupportFragmentManager().findFragmentByTag("AddTaskBottomSheetTag") != null) {
            return;
        }

        // Si no está visible, entonces sí creamos y mostramos una nueva instancia.
        AddTaskBottomSheet bottomSheet = new AddTaskBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "AddTaskBottomSheetTag");
    }


}