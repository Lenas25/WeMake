package com.utp.wemake.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtils {

    /**
     * Comprueba si el dispositivo tiene una conexión a internet activa (WiFi o Datos Móviles).
     * @param context El contexto de la aplicación.
     * @return true si hay conexión, false en caso contrario.
     */
    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null) {
            return false;
        }
        // Comprueba si la conexión tiene capacidad para WiFi, Datos Móviles o Ethernet.
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

}