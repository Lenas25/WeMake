package com.utp.wemake; // Define el paquete al que pertenece esta clase

import android.content.Intent; // Importa la clase Intent, que se usa para iniciar nuevas pantallas
import android.os.Bundle; // Importa la clase Bundle, usada para pasar datos entre actividades y guardar el estado
import android.text.TextUtils;
import android.util.Log;
import android.view.View; // Importa la clase View, la clase para todos los componentes de la interfaz de usuario
import android.widget.Toast;

import androidx.activity.EdgeToEdge; // Importa la clase para habilitar el modo de pantalla completa
import androidx.appcompat.app.AppCompatActivity; // Importa la clase base para actividades que usan la barra de aplicaciones de la librería de compatibilidad

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView; // Importa el componente de texto específico de Material Design
import com.google.firebase.messaging.FirebaseMessaging;
import com.utp.wemake.auth.FirebaseAuthHelper;
import com.utp.wemake.auth.GoogleSignInHelper;
import com.utp.wemake.repository.UserRepository;

public class SignupActivity extends AppCompatActivity implements FirebaseAuthHelper.AuthCallback, GoogleSignInHelper.GoogleSignInCallback { // Declara la clase de la actividad, que hereda de AppCompatActivity

    // Elementos de la interfaz
    TextInputLayout tilName, tilEmail, tilPassword;
    MaterialTextView tvLogin; // Declara una variable llamada 'tvLogin' que contendrá una referencia al TextView del XML.
    MaterialButton btnSignup, btnGoogle;
    MaterialCheckBox checkBox;

    // Asistente de autenticación de Firebase
    private FirebaseAuthHelper firebaseAuthHelper;
    // Asistente de inicio de sesión de Google
    private GoogleSignInHelper googleSignInHelper;
    private UserRepository userRepository;
    private static final String TAG = "SignupActivity";

    @Override // Anotación que indica que estamos sobrescribiendo
    protected void onCreate(Bundle savedInstanceState) { // Este método se ejecuta automáticamente cuando se crea la actividad por primera vez
        super.onCreate(savedInstanceState); // Llama al método onCreate de la clase padre
        EdgeToEdge.enable(this); // Habilita el modo de pantalla completa
        setContentView(R.layout.activity_signup); // Conecta este archivo Java con su archivo de diseño XML

        //Inicializa los elementos de la vista
        initViews();
        // Configura los listeners
        setupListeners();
        // Inicializa Firebase Auth
        initFirebaseAuth();
        // Inicializa el inicio de sesión de Google
        initGoogleSignIn();
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        btnSignup = findViewById(R.id.button_signup);
        btnGoogle = findViewById(R.id.btn_google); // Busca el botón de Google con id 'btn_google' en el layout
        tvLogin = findViewById(R.id.tv_login); // Busca la vista en el XML que tiene el id 'tv_login'
        checkBox = findViewById(R.id.checkBox);

        userRepository = new UserRepository();
    }

    private void initFirebaseAuth() {
        firebaseAuthHelper = new FirebaseAuthHelper(this, this);
    }

    private void initGoogleSignIn() {
        googleSignInHelper = new GoogleSignInHelper(this, this);
    }

    // Configura los listeners
    private void setupListeners() {
        // Listener para el botón de registro
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignup();
            }
        });

        // Listener para ir a la pantalla de Login
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override // Indica que estamos sobrescribiendo el método 'onClick'
            public void onClick(View v) { // Este bloque de código se ejecutará cada vez que el usuario haga clic
                // Crea un nuevo intent para ir desde la actividad actual SignupActivity.this hacia LoginActivity.
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent); // Ejecuta el intent, lo que provoca que se abra la pantalla de LoginActivity.
                finish(); // Cierra la actividad actual para que el usuario no pueda volver a ella con el botón de retroceso
            }
        });

        // Listener para el botón de inicio de sesión de Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignInHelper.signIn();
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

    private void handleSignup() {
        // Obtener los valores de los campos
        String name = tilName.getEditText().getText().toString().trim();
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Ingrese su nombre completo");
            return;
        }

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

        if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        if (!checkBox.isChecked()) {
            showToast("Debes aceptar los términos y condiciones");
            return;
        }

        // Limpiar errores
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Registrar usuario con Firebase
        firebaseAuthHelper.registerUser(email, password, name);
    }

    // Método auxiliar para mostrar mensajes cortos (Toast)
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Método para navegar hacia la actividad principal
    private void navigateToMain(String userName) {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.putExtra(LoginActivity.USER_NAME, userName);
        startActivity(intent);
        finish();
    }

    // Métodos de devolución de llamada de inicio de sesión de Google
    @Override
    public void onSignInSuccess(String userName, String userEmail, boolean isRegistration) {
        if (isRegistration) {
            showToast("¡Registro con Google exitoso!");
            getAndSaveFcmToken();

            Intent intent = new Intent(SignupActivity.this, SetupActivity.class);
            intent.putExtra(SetupActivity.EXTRA_USER_NAME, userName); // Pasa el nombre
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        } else {
            showToast("¡Inicio de sesión con Google exitoso!");
            navigateToMain(userName);
        }
    }

    @Override
    public void onSignInError(String errorMessage) {
        showToast(errorMessage);
    }

    // Métodos de devolución de llamada de Firebase Auth
    @Override
    public void onSuccess(String userName, String userEmail, boolean isRegistration) {
        if (isRegistration) {
            showToast("¡Registro de cuenta exitoso! Inicia sesión con tus credenciales");
            getAndSaveFcmToken();
            Intent intent = new Intent(SignupActivity.this, SetupActivity.class);
            intent.putExtra(SetupActivity.EXTRA_USER_NAME, userName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            showToast("¡Inicio de sesión exitoso!");
            navigateToMain(userName);
        }
    }

    @Override
    public void onError(String errorMessage) {
        showToast(errorMessage);
    }

    /**
     * Obtiene el token de FCM actual del dispositivo y lo guarda en Firestore.
     * Esta función se llama después de un registro exitoso.
     */
    private void getAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }

            // Obtener el nuevo token de registro de FCM
            String token = task.getResult();
            Log.d(TAG, "FCM Token obtained: " + token);

            // Usamos el repositorio para registrar el token en Firestore.
            // Es importante que tengas el método updateUserFcmToken en tu UserRepository.
            userRepository.updateUserFcmToken(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated successfully in Firestore."))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM Token in Firestore", e));
        });
    }
}