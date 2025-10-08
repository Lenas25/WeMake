package com.utp.wemake.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.utp.wemake.R;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    /**
     * Se llama cuando se genera un nuevo token de FCM o se actualiza.
     * Aquí es donde debes guardar el token en Firestore.
     *
     * @param token El nuevo token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token de FCM generado: " + token);
        // Guarda el token en el documento del usuario actual si está logueado
        sendRegistrationToServer(token);
    }

    /**
     * Se llama cuando se recibe un mensaje mientras la app está en primer plano.
     * Aquí puedes mostrar una notificación personalizada.
     *
     * @param remoteMessage Objeto que representa el mensaje recibido de FCM.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }
    }

    private void showNotification(String title, String message) {
        String channelId = "default_channel_id";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Se necesita un Canal de Notificación para Android 8.0 (API 26) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());
    }

    /**
     * Persiste el token de FCM en Firestore para el usuario actual.
     *
     * @param token El token de registro de FCM.
     */
    private void sendRegistrationToServer(String token) {
        // Obtenemos el usuario actual de Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // El safe call (?.) y la comprobación de nulidad en Kotlin se traducen a esto en Java
        if (currentUser != null && token != null) {
            String userId = currentUser.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection("users").document(userId);

            // Creamos un mapa para actualizar solo el campo del token
            // El `mapOf("fcmToken" to token)` de Kotlin se traduce a esto:
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fcmToken", token);

            // Actualizamos el documento
            userDocRef.update(tokenData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token actualizado en Firestore."))
                    .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar token", e));
        }
    }
}