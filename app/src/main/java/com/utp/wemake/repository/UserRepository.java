package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.utp.wemake.models.User;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

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
}