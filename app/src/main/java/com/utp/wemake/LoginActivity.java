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
    public static final String USER_NAME = "Administrador";
    public static final int MAX_LOGIN_ATTEMPTS = 3;
    private int loginAttempts = 0;

    TextInputLayout tilEmail, tilPassword;
    MaterialButton btnLogin;
    MaterialTextView tvRegisterNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.input_email);
        tilPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterNow = findViewById(R.id.register_now);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        tvRegisterNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();

        if (email.equals("admi@utp.pe") && password.equals("admi1234")) {
            showToast("¡Inicio de sesión exitoso!");
            loginAttempts = 0;
            navigateToMain("Administrador Jorge");
        } else {
            loginAttempts++;

            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                showToast("Demasiados intentos fallidos. La aplicación se cerrará.");
                finish();
            } else {
                int attemptsLeft = MAX_LOGIN_ATTEMPTS - loginAttempts;
                String errorMessage = "Correo o contraseña incorrectos!";
                showToast(errorMessage);
            }
        }
    }

    private void navigateToMain(String userName) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra(USER_NAME, userName);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}