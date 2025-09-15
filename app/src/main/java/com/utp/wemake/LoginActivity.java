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
import com.utp.wemake.auth.FirebaseAuthHelper;
import com.utp.wemake.auth.GoogleSignInHelper;

public class LoginActivity extends AppCompatActivity implements FirebaseAuthHelper.AuthCallback, GoogleSignInHelper.GoogleSignInCallback {
    // Constante para identificar al usuario en otras actividades
    public static final String USER_NAME = "Administrador";
    // Número máximo de intentos permitidos antes de bloquear el acceso
    public static final int MAX_LOGIN_ATTEMPTS = 3;
    // Contador de intentos fallidos
    private int loginAttempts = 0;

    // Elementos de la interfaz
    TextInputLayout tilEmail, tilPassword;
    MaterialButton btnLogin, btnGoogle;
    MaterialTextView tvRegisterNow;

    //Asistente de autenticación de Firebase
    private FirebaseAuthHelper firebaseAuthHelper;
    //Asistende de inicio de sesión de Google
    private GoogleSignInHelper googleSignInHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Inicializa los elementos de la vista
        initViews();
        // Configura los listeners de botones y textos
        setupListeners();
        // Inicializa Firebase Auth
        initFirebaseAuth();
        // Inicializa el inicio de sesión de Google
        initGoogleSignIn();
    }

    private void initFirebaseAuth() {
        firebaseAuthHelper = new FirebaseAuthHelper(this, this);
    }

    private void initGoogleSignIn() {
        googleSignInHelper = new GoogleSignInHelper(this, this);
    }

    // Método para vincular los elementos del layout con las variables
    private void initViews() {
        tilEmail = findViewById(R.id.input_email);
        tilPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google);
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

        // Listener para el botón de inicio de sesión de Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignInHelper.signIn();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Gestionar el resultado del inicio de sesión de Google
        if (requestCode == GoogleSignInHelper.RC_SIGN_IN) {
            googleSignInHelper.handleSignInResult(data);
        }
    }

    // Lógica principal de validación de login
    private void handleLogin() {
        // Obtiene el email y contraseña ingresados
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();

        // Validación con credenciales fijas para pruebas
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Ingresa tu correo electrónico");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Ingresa tu contraseña");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingresa un correo válido");
            return;
        }

        // Limpiar errores
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Intentar inicio de sesión con Firebase
        firebaseAuthHelper.signInUser(email, password);
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

    // Métodos de devolución de llamada de inicio de sesión de Google
    @Override
    public void onSignInSuccess(String userName, String userEmail) {
        showToast("¡Inicio de sesión con Google exitoso!");
        navigateToMain(userName);
    }

    @Override
    public void onSignInError(String errorMessage) {
        showToast(errorMessage);
    }

    // Métodos de devolución de llamada de Firebase Auth
    @Override
    public void onSuccess(String userName, String userEmail) {
        showToast("¡Inicio de sesión exitoso!");
        navigateToMain(userName);
    }

    @Override
    public void onError(String errorMessage) {
        showToast(errorMessage);
    }
}