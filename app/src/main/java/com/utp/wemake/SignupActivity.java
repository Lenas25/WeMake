package com.utp.wemake; // Define el paquete al que pertenece esta clase

import android.content.Intent; // Importa la clase Intent, que se usa para iniciar nuevas pantallas
import android.os.Bundle; // Importa la clase Bundle, usada para pasar datos entre actividades y guardar el estado
import android.view.View; // Importa la clase View, la clase para todos los componentes de la interfaz de usuario

import androidx.activity.EdgeToEdge; // Importa la clase para habilitar el modo de pantalla completa
import androidx.appcompat.app.AppCompatActivity; // Importa la clase base para actividades que usan la barra de aplicaciones de la librería de compatibilidad

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView; // Importa el componente de texto específico de Material Design
import com.utp.wemake.auth.GoogleSignInHelper;

public class SignupActivity extends AppCompatActivity implements GoogleSignInHelper.GoogleSignInCallback { // Declara la clase de la actividad, que hereda de AppCompatActivity

    MaterialTextView tvLogin; // Declara una variable llamada 'tvLogin' que contendrá una referencia al TextView del XML.
    MaterialButton btnGoogle; // Botón para registro con Google

    // Asistente de inicio de sesión de Google
    private GoogleSignInHelper googleSignInHelper;

    @Override // Anotación que indica que estamos sobrescribiendo
    protected void onCreate(Bundle savedInstanceState) { // Este método se ejecuta automáticamente cuando se crea la actividad por primera vez
        super.onCreate(savedInstanceState); // Llama al método onCreate de la clase padre
        EdgeToEdge.enable(this); // Habilita el modo de pantalla completa
        setContentView(R.layout.activity_signup); // Conecta este archivo Java con su archivo de diseño XML

        //Inicializa los elementos de la vista
        initViews();
        // Configura los listeners
        setupListeners();
        // Inicializa el inicio de sesión de Google
        initGoogleSignIn();
    }

    private void initViews() {
        tvLogin = findViewById(R.id.tv_login); // Busca la vista en el XML que tiene el id 'tv_login'
        btnGoogle = findViewById(R.id.btn_google); // Busca el botón de Google con id 'btn_google' en el layout
    }

    private void initGoogleSignIn() {
        googleSignInHelper = new GoogleSignInHelper(this, this);
    }

    // Configura los listeners
    private void setupListeners() {
        tvLogin.setOnClickListener(new View.OnClickListener() { // Establece un "escuchador de clics".
            @Override // Indica que estamos sobrescribiendo el método 'onClick'
            public void onClick(View v) { // Este bloque de código se ejecutará cada vez que el usuario haga clic
                // Crea un nuevo intent para ir desde la actividad actual SignupActivity.this hacia LoginActivity.
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent); // Ejecuta el intent, lo que provoca que se abra la pantalla de LoginActivity.
                finish(); // Cierra la actividad actual para que el usuario no pueda volver a ella con el botón de retroceso
            }
        });

        // Listener para el botón de inicio de sesión de Google
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    googleSignInHelper.signIn();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Gestionar el resultado del inicio de sesión de Google
        if (requestCode == GoogleSignInHelper.RC_SIGN_IN) {
            googleSignInHelper.handleSignInResult(data);
        }
    }

    // Métodos de devolución de llamada de inicio de sesión de Google
    @Override
    public void onSignInSuccess(String userName, String userEmail) {
        // Ir a la actividad principal después de una actividad exitosa
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.putExtra(LoginActivity.USER_NAME, userName);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSignInError(String errorMessage) {
        // Mostrar mensaje de error
        // Implementar Notificación o diálogo
    }
}