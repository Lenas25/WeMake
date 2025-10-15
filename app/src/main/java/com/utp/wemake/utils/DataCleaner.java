package com.utp.wemake.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class DataCleaner {

    /**
     * Borra todos los datos de todas las SharedPreferences conocidas de la aplicación.
     * Es ideal para llamar durante el cierre de sesión.
     *
     * @param context El contexto de la aplicación para acceder a SharedPreferences.
     */
    public static void clearAllLocalData(Context context) {
        String[] prefFiles = {
                "AppBoardPrefs",       // BoardSelectionPrefs
                "AppNotificationPrefs" // NotificationPrefs
        };

        for (String prefFile : prefFiles) {
            SharedPreferences prefs = context.getSharedPreferences(prefFile, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }

        Log.d("DataCleaner", "Limpieza de datos locales completada.");
    }
}