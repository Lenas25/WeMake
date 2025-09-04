package com.utp.wemake; // Define el paquete donde se encuentra esta clase

import android.content.Intent; // Importa la clase para navegar entre pantallas
import android.os.Bundle; // Importa la clase para manejar el estado de la actividad
import android.view.View; // Importa la clase para todos los componentes de la UI

import androidx.activity.EdgeToEdge; // Importa la clase para el modo de pantalla completa
import androidx.appcompat.app.AppCompatActivity; // Importa la clase base para crear una actividad

import com.google.android.material.button.MaterialButton; // Importa el componente de botón de Material Design

public class WelcomeActivity extends AppCompatActivity { // Declara la clase de la actividad de bienvenida

    MaterialButton btnLogin, btnSignup; // Declara las variables para los dos botones de la UI

    @Override // Indica que se está sobrescribiendo
    protected void onCreate(Bundle savedInstanceState) { // Este método se ejecuta cuando se crea la pantalla
        super.onCreate(savedInstanceState); // Llama al código original del método 'onCreate'
        EdgeToEdge.enable(this); // Habilita el modo de pantalla de borde a borde
        setContentView(R.layout.activity_welcome); // Conecta este archivo Java con su diseño XML

        btnLogin = findViewById(R.id.btn_login); // Asigna el botón de login del XML a la variable
        btnSignup = findViewById(R.id.btn_create_account); // Asigna el botón de registro del XML a la variable

        btnLogin.setOnClickListener(new View.OnClickListener() { // Define la acción que ocurrirá al hacer clic en el botón
            @Override // Sobrescribe el método de la interfaz de clic
            public void onClick(View v) {
                // Inicia la pantalla de Login
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                finish(); // Cierra la pantalla actual de bienvenida
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() { // Define la acción que ocurrirá al hacer clic en el botón de registro
            @Override // Sobrescribe el método de la interfaz de clic
            public void onClick(View v) { // Código que se ejecuta cuando se presiona el botón
                // Inicia la pantalla de Registro
                startActivity(new Intent(WelcomeActivity.this, SignupActivity.class));
                finish(); // Cierra la pantalla actual de bienvenida
            }
        });
    }
}