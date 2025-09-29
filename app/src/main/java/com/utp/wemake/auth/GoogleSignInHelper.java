package com.utp.wemake.auth;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utp.wemake.R;
import com.utp.wemake.models.User;

public class GoogleSignInHelper {
    private static final String TAG = "GoogleSignInHelper";
    public static final int RC_SIGN_IN = 9001;

    private Activity activity;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInCallback callback;

    public interface GoogleSignInCallback {
        void onSignInSuccess(String userName, String userEmail, boolean isRegistration);
        void onSignInError(String errorMessage);
    }

    public GoogleSignInHelper(Activity activity, GoogleSignInCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();

        // Configurar el inicio de sesión de Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        this.googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signIn() {
        // Forzar la selección de cuenta para evitar usar la cuenta en caché
        googleSignInClient.signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Después de cerrar sesión, iniciar el flujo de selección de cuenta
                Intent signInIntent = googleSignInClient.getSignInIntent();
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                callback.onSignInError("No se obtuvo la cuenta de Google.");
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            String errorMessage;

            switch (e.getStatusCode()) {
                case 7: // ERROR DE RED
                    errorMessage = "Error de conexión. Verifica tu conexión a internet.";
                    break;
                case 10: // ERROR DE DESARROLLADOR
                    errorMessage = "Error de configuración en la aplicación. Contacta al administrador.";
                    break;
                case 12501: // INICIO DE SESIÓN CANCELADO
                    errorMessage = "Inicio de sesión cancelado por el usuario.";
                    break;
                case 12500: // FALLÓ DE INICIO DE SESIÓN
                    errorMessage = "No se pudo completar el inicio de sesión. Intenta nuevamente.";
                    break;
                default:
                    errorMessage = "Ocurrió un error al iniciar sesión con Google. Intenta otra vez.";
                    break;
            }
            callback.onSignInError(errorMessage);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                                String name = user.getDisplayName() != null ? user.getDisplayName() : "Usuario";
                                String email = user.getEmail() != null ? user.getEmail() : "";

                                if (isNewUser) {
                                    // Guardar en Firestore sin bloquear el flujo
                                    createNewUser(user);
                                }

                                // Llamamos al callback de inmediato
                                callback.onSignInSuccess(name, email, isNewUser);
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String errorMessage = "Error de autenticación: ";
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error.contains("account-exists-with-different-credential")) {
                                    errorMessage = "Ya existe una cuenta con este correo usando otro método de inicio de sesión.";
                                } else if (error.contains("network error")) {
                                    errorMessage = "Error de red. Revisa tu conexión a internet.";
                                } else {
                                    errorMessage = "No se pudo autenticar con Google. Intenta nuevamente.";
                                }
                            }
                            callback.onSignInError(errorMessage);
                        }
                    }
                });
    }

    private void createNewUser(FirebaseUser user) {
        String publicName = "";
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            // Divide el nombre por los espacios y toma la primera palabra
            publicName = user.getDisplayName().trim().split("\\s+")[0];
        }
        // Crear documento de usuario en Firestore
        User newUser = new User(
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                publicName,
                user.getEmail(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                user.getPhoneNumber() != null ? user.getPhoneNumber().toString() : "" ,
                null);

        firestore.collection("users").document(user.getUid())
                .set(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            callback.onSignInSuccess(newUser.getName(), newUser.getEmail(), true);
                        } else {
                            Log.w(TAG, "Error creating user", task.getException());
                            callback.onSignInError("Error al crear el usuario: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Error desconocido"));
                        }
                    }
                });
    }

    public void signOut() {
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "User signed out");
            }
        });
    }

    // Método para limpiar completamente la sesión de Google
    public void clearGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(activity, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Google sign in cleared");
            }
        });
    }
}