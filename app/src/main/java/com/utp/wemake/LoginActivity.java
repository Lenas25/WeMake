package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

public class LoginActivity extends AppCompatActivity {
    // Constante para identificar al usuario en otras actividades
    public static final String USER_NAME = "Administrador";
    // Número máximo de intentos permitidos antes de bloquear el acceso
    public static final int MAX_LOGIN_ATTEMPTS = 3;
    // Contador de intentos fallidos
    private int loginAttempts = 0;

    // Elementos de la interfaz
    TextInputLayout tilEmail, tilPassword;
    MaterialButton btnLogin;
    MaterialTextView tvRegisterNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Inicializa los elementos de la vista
        initViews();
        // Configura los listeners de botones y textos
        setupListeners();
    }

    // Método para vincular los elementos del layout con las variables
    private void initViews() {
        tilEmail = findViewById(R.id.input_email);
        tilPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterNow = findViewById(R.id.register_now);
    }

    // Configura los eventos de los botones (login y registro)
    private void setupListeners() {
        // Listener para el botón de login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin(); // Llama al método que valida el login
            }
        });

        // Listener para el texto "Regístrate ahora"
        tvRegisterNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navega hacia la actividad de registro
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    // Lógica principal de validación de login
    private void handleLogin() {
        // Obtiene el email y contraseña ingresados
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();

        // Validación con credenciales fijas para pruebas
        if (email.equals("admi@utp.pe") && password.equals("admi1234")) {
            showToast("¡Inicio de sesión exitoso!");
            loginAttempts = 0; // Reinicia los intentos fallidos
            navigateToMain("Administrador Jorge"); // Pasa el nombre del usuario a MainActivity
        } else {
            // Incrementa los intentos fallidos
            loginAttempts++;

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                // Si se superan los intentos, cierra la aplicación
                showToast("Demasiados intentos fallidos. La aplicación se cerrará.");
                finish();
            } else {
                // Si aún hay intentos disponibles, muestra un mensaje de error
                int attemptsLeft = MAX_LOGIN_ATTEMPTS - loginAttempts;
                String errorMessage = "Correo o contraseña incorrectos!";
                showToast(errorMessage);
            }
        }
    }

    // Método para navegar hacia la actividad principal (MainActivity)
    private void navigateToMain(String userName) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Envía el nombre del usuario como parámetro extra
        intent.putExtra(USER_NAME, userName);
        startActivity(intent);
        finish(); // Finaliza la actividad actual para no volver con "back"
    }

    // Método auxiliar para mostrar mensajes cortos (Toast)
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}