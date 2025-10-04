package com.utp.wemake;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    // Declara el NavController como una variable de clase para que sea accesible en toda la actividad.
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // El método onCreate ahora es un resumen claro de lo que se configura.
        setupNavigation();
        setupFab();
        setupBackButton();
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
            // ¡AQUÍ ESTÁ LA LÓGICA PARA ABRIR EL BOTTOMSHEET!
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
                    // ...entonces navegamos a la pantalla de inicio.
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
        // Asumiendo que has creado una clase AddTaskBottomSheet que hereda de BottomSheetDialogFragment
        // y que usa el nuevo layout que diseñaste.
        AddTaskBottomSheet bottomSheet = new AddTaskBottomSheet();

        // Muestra el BottomSheet.
        bottomSheet.show(getSupportFragmentManager(), "AddTaskBottomSheetTag");
    }

    /**
     * Método público para que los fragments puedan obtener el nombre del usuario.
     */
    public String getUserName() {
        // Es una buena práctica verificar si el Intent tiene la clave antes de acceder a ella.
        if (getIntent() != null && getIntent().hasExtra(LoginActivity.USER_NAME)) {
            return getIntent().getStringExtra(LoginActivity.USER_NAME);
        }
        return null; // Devuelve null si no se encuentra el nombre.
    }
}