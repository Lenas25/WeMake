package com.utp.wemake.auth;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utp.wemake.models.User;

public class FirebaseAuthHelper {
    private static final String TAG = "FirebaseAuthHelper";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private Activity activity;
    private AuthCallback callback;

    public interface AuthCallback {
        void onSuccess(String userName, String userEmail);
        void onError(String errorMessage);
    }

    public FirebaseAuthHelper(Activity activity, AuthCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Método para registrar usuario con email y contraseña
    public void registerUser(String email, String password, String name) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                // Crear documento de usuario en Firestore
                                createUserDocument(user, name);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Error al crear la cuenta: ";
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error.contains("email address is already in use")) {
                                    errorMessage = "Este correo ya está registrado";
                                } else if (error.contains("password should be at least 6 characters")) {
                                    errorMessage = "La contraseña debe tener al menos 6 caracteres";
                                } else if (error.contains("invalid email")) {
                                    errorMessage = "Correo electrónico inválido";
                                } else {
                                    errorMessage += error;
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }
                });
    }

    // Método para iniciar sesión con email y contraseña
    public void signInUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                // Obtener información del usuario desde Firestore
                                getUserFromFirestore(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Error al iniciar sesión: ";
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error.contains("user not found")) {
                                    errorMessage = "Usuario no encontrado";
                                } else if (error.contains("wrong password")) {
                                    errorMessage = "Contraseña incorrecta";
                                } else if (error.contains("invalid email")) {
                                    errorMessage = "Correo electrónico inválido";
                                } else {
                                    errorMessage += error;
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }
                });
    }

    // Método para crear documento de usuario en Firestore
    private void createUserDocument(FirebaseUser firebaseUser, String name) {
        User user = new User(
                firebaseUser.getUid(),
                name,
                firebaseUser.getEmail(),
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : ""
        );

        firestore.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            callback.onSuccess(user.getName(), user.getEmail());
                        } else {
                            Log.w(TAG, "Error creating user document", task.getException());
                            callback.onError("Error al crear el perfil de usuario");
                        }
                    }
                });
    }

    // Método para obtener información del usuario desde Firestore
    private void getUserFromFirestore(FirebaseUser firebaseUser) {
        DocumentReference userDoc = firestore.collection("users").document(firebaseUser.getUid());
        userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String userName = document.getString("name");
                        if (userName == null || userName.isEmpty()) {
                            userName = firebaseUser.getDisplayName() != null ?
                                    firebaseUser.getDisplayName() : "Usuario";
                        }
                        callback.onSuccess(userName, firebaseUser.getEmail());
                    } else {
                        // Si no existe el documento, crear uno
                        createUserDocument(firebaseUser, firebaseUser.getDisplayName() != null ?
                                firebaseUser.getDisplayName() : "Usuario");
                    }
                } else {
                    Log.w(TAG, "Error getting user document", task.getException());
                    callback.onError("Error al obtener información del usuario");
                }
            }
        });
    }

    // Método para verificar si el usuario está autenticado
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Método para cerrar sesión
    public void signOut() {
        firebaseAuth.signOut();
    }

    // Método para obtener el usuario actual
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}
