package com.utp.wemake.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class NotificationPrefs {

    // Nombre del archivo de preferencias
    private static final String PREFS_NAME = "AppNotificationPrefs";
    // Clave para guardar el valor booleano
    private static final String KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled";

    private final SharedPreferences sharedPreferences;

    public NotificationPrefs(Context context) {
        // Obtenemos una instancia de SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Guarda la preferencia de notificaciones del usuario.
     */
    public void setNotificationsEnabled(boolean isEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled);
        editor.apply(); // apply() guarda los cambios en segundo plano
    }

    /**
     * Obtiene la preferencia de notificaciones guardada.
     */
    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
}