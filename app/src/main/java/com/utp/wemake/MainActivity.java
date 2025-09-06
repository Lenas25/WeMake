package com.utp.wemake; // Asegúrate de que este sea tu paquete

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);
        FloatingActionButton fab = findViewById(R.id.fab);

        // 3. (Recomendado) Configurar la navegación para que los botones funcionen
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        bottomAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            // Lógica para navegar a diferentes pantallas
            if (itemId == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
                return true;
            }

            return false;
        });

        // 4. (Opcional) Configurar la acción del botón flotante
        fab.setOnClickListener(view -> {
            // Código que se ejecuta al presionar el botón '+'
        });
    }
}