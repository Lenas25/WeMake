package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
     * Busca usuarios cuyo email empiece con el texto de búsqueda.
     * @param query El email o parte del email a buscar.
     * @return Una Tarea que contendrá la lista de objetos User.
     */
    public Task<List<User>> searchUsersByEmail(String query) {
        String lowerQuery = query.toLowerCase().trim();
        Query searchQuery = db.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("email", lowerQuery)
                .whereLessThanOrEqualTo("email", lowerQuery + "\uf8ff")
                .limit(10);

        return searchQuery.get().continueWith(task ->
                task.getResult().toObjects(User.class)
        );
    }

    /**
     * Obtiene los detalles completos de una lista de usuarios a partir de sus IDs.
     * @param userIds La lista de UIDs a buscar.
     */
    public Task<List<User>> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            // Devuelve una tarea exitosa con una lista vacía si no hay IDs
            return Tasks.forResult(new ArrayList<>());
        }

        // Una consulta 'whereIn' puede buscar hasta 30 IDs a la vez.
        return db.collection(COLLECTION_USERS)
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .continueWith(task -> task.getResult().toObjects(User.class));
    }

    /**
     * Crea un documento para un nuevo usuario o actualiza los datos básicos si ya existe.
     * @param firebaseUser El objeto FirebaseUser del usuario autenticado.
     * @return Una Tarea de Firebase que se completa cuando la operación de escritura termina.
     */
    public Task<Void> createOrUpdateUser(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            // Devuelve una tarea fallida si el usuario es nulo
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
}