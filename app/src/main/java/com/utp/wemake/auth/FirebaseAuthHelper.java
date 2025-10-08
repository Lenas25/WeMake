package com.utp.wemake.auth;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utp.wemake.models.User;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthHelper {
    private static final String TAG = "FirebaseAuthHelper";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private Activity activity;
    private AuthCallback callback;

    public interface AuthCallback {
        void onSuccess(String userName, String userEmail, boolean isRegistration);
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
                                createUserDocument(user, name, true); // true indica que es registro
                                // Llama al callback
                                callback.onSuccess(name, email, true);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "Error al crear la cuenta: ";
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error.contains("email address is already in use")) {
                                    errorMessage = "Este correo ya está registrado. ¡Inicia sesión!";
                                } else if (error.contains("password should be at least 6 characters")) {
                                    errorMessage = "La contraseña debe tener al menos 6 caracteres";
                                } else if (error.contains("invalid email")) {
                                    errorMessage = "Correo electrónico inválido";
                                } else {
                                    errorMessage = "Ocurrió un error inesperado. Inténtalo de nuevo.";
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
                                getUserFromFirestore(user, false); // false indica que es login
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
    private void createUserDocument(FirebaseUser firebaseUser, String fullName, boolean isRegistration) {
        String publicName = "";
        if (fullName != null && !fullName.trim().isEmpty()) {
            // Divide el nombre por los espacios y toma la primera palabra
            publicName = fullName.trim().split("\\s+")[0];
        }

        User user = new User(
                firebaseUser.getUid(),
                fullName,
                publicName,
                firebaseUser.getEmail(),
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                null,
                null,
                true);

        firestore.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Llama al callback de éxito solo después de que el documento se haya creado
                        callback.onSuccess(user.getName(), user.getEmail(), isRegistration);
                    } else {
                        Log.w(TAG, "Error creating user document", task.getException());
                        callback.onError("Error al crear el perfil de usuario");
                    }
                });
    }

    // Método para obtener información del usuario desde Firestore
    private void getUserFromFirestore(FirebaseUser firebaseUser, boolean isRegistration) {
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
                        callback.onSuccess(userName, firebaseUser.getEmail(), isRegistration);
                    } else {
                        // Si no existe el documento, crear uno
                        createUserDocument(firebaseUser, firebaseUser.getDisplayName() != null ?
                                firebaseUser.getDisplayName() : "Usuario", isRegistration);
                    }
                } else {
                    Log.w(TAG, "Error getting user document", task.getException());
                    callback.onError("Error al obtener información del usuario");
                }
            }
        });
    }

    // Método para cerrar sesión
    public void signOut() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = firestore.collection("users").document(userId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", null);

            userDocRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        firebaseAuth.signOut();
                        Log.d(TAG, "FCM Token cleared and user signed out successfully.");
                    })
                    .addOnFailureListener(e -> {
                        firebaseAuth.signOut();
                        Log.w(TAG, "Error clearing FCM token, but signed out anyway.", e);
                    });
        } else {
            // Si no hay usuario, por si acaso, llamamos a signOut.
            firebaseAuth.signOut();
        }
    }

    // Método para obtener el usuario actual
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}