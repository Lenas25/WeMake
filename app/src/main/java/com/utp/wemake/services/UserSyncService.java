package com.utp.wemake.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.repository.UserRepository; // CAMBIO: Importar el repositorio correcto

public class UserSyncService extends Service {
    private static final String TAG = "UserSyncService";

    // CAMBIO: Usar UserRepository
    private UserRepository userRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        userRepository = new UserRepository();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        syncCurrentUser();
        return START_NOT_STICKY;
    }

    private void syncCurrentUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {

            userRepository.createOrUpdateUser(currentUser).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Usuario sincronizado con éxito: " + currentUser.getEmail());
                } else {
                    Log.e(TAG, "Error sincronizando usuario: ", task.getException());
                }
                stopSelf();
            });
        } else {
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}