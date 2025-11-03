package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.utp.wemake.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final String COLLECTION_USERS = "users";

    /**
     * Obtiene los datos del usuario actualmente logueado.
     */
    public Task<DocumentSnapshot> getCurrentUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return db.collection("users").document(currentUser.getUid()).get();
    }

    /**
     * Obtiene los datos de un usuario específico por su ID.
     * @param userId El ID del usuario a buscar.
     * @return Una Tarea que devuelve el DocumentSnapshot del usuario.
     */
    public Task<DocumentSnapshot> getUserData(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("User ID cannot be null or empty."));
        }
        return db.collection(COLLECTION_USERS).document(userId).get();
    }

    /**
     * Actualiza los datos del perfil del usuario actual.
     */
    public Task<Void> updateProfileData(User user) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return db.collection("users").document(currentUser.getUid()).set(user, SetOptions.merge());
    }

    /**
     * Crea un documento para un nuevo usuario o actualiza los datos básicos si ya existe.
     * @param firebaseUser El objeto FirebaseUser del usuario autenticado.
     * @return Una Tarea de Firebase que se completa cuando la operación de escritura termina.
     */
    public Task<Void> createOrUpdateUser(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("FirebaseUser cannot be null"));
        }

        // Calcula el publicName
        String fullName = firebaseUser.getDisplayName();
        String publicName = "";
        if (fullName != null && !fullName.trim().isEmpty()) {
            publicName = fullName.trim().split("\\s+")[0];
        }

        // Crea el objeto User con los datos básicos
        User user = new User();
        user.setUserid(firebaseUser.getUid());
        user.setName(fullName);
        user.setPublicName(publicName);
        user.setEmail(firebaseUser.getEmail());
        user.setNotificationsEnabled(true);
        if (firebaseUser.getPhotoUrl() != null) {
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        }

        // Usa SetOptions.merge() para no sobreescribir campos que ya existan
        // y que no se estén actualizando (como 'coins' o 'birthDate').
        return db.collection(COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(user, SetOptions.merge());
    }

    /**
     * Comprueba si el documento del usuario actual existe en Firestore.
     */
    public Task<DocumentSnapshot> doesUserDocumentExist() {
        String uid = auth.getUid();
        if (uid == null) return Tasks.forException(new Exception("No user logged in"));
        return db.collection("users").document(uid).get();
    }

    /**
     * Actualiza la preferencia de notificaciones para el usuario actual en Firestore.
     * @param isEnabled El nuevo estado de la preferencia.
     * @return Una Tarea que se completa cuando la actualización termina.
     */
    public Task<Void> updateNotificationPreference(boolean isEnabled) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new IllegalStateException("No user is currently logged in."));
        }

        return db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .update("notificationsEnabled", isEnabled);
    }

    /**
     * Actualiza o añade el token de FCM para el usuario actual en su documento de Firestore.
     * @param token El token de FCM del dispositivo actual.
     * @return Una Tarea que se completa cuando la actualización termina.
     */
    public Task<Void> updateUserFcmToken(String token) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || token == null) {
            return Tasks.forException(new IllegalStateException("User not logged in or token is null."));
        }
        return db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .update("fcmToken", token);
    }

    /**
     * Obtiene usuarios por sus IDs
     */
    public Task<List<User>> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }

        return db.collection(COLLECTION_USERS)
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .continueWith(task -> {
                    List<User> users = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setUserid(doc.getId());
                                users.add(user);
                            }
                        }
                    }
                    return users;
                });
    }
}