// ESTE ES EL CÓDIGO CORRECTO PARA MainActivity.java
package com.utp.wemake;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Infla el layout principal que contiene el BottomNavigationView, el FAB y el NavHostFragment
        setContentView(R.layout.activity_main);

        // Referencia al menú de navegación inferior (BottomNavigationView)
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Referencia al botón flotante (FloatingActionButton)
        FloatingActionButton fab = findViewById(R.id.fab);

        // Obtiene el fragmento host de navegación desde el layout (NavHostFragment)
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        // Controlador que maneja la navegación entre fragmentos
        NavController navController = navHostFragment.getNavController();

        // Vincula el BottomNavigationView con el NavController
        // Esta línea evita el crash porque conecta correctamente la navegación con el menú
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Configuración del listener para el FAB (FloatingActionButton)
        fab.setOnClickListener(view -> {
            // Código que se ejecutará al presionar el botón flotante
        });
    }
}