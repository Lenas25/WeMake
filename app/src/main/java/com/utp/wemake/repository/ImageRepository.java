package com.utp.wemake.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRepository {

    // Interfaz para notificar el resultado de la subida
    public interface UploadCallbackListener {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    // Constructor para inicializar MediaManager (llámalo en tu clase Application)
    public static void initialize(Context context) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "TU_CLOUD_NAME");
        config.put("api_key", "TU_API_KEY");
        config.put("api_secret", "TU_API_SECRET");
        MediaManager.init(context, config);
    }

    /**
     * Sube una imagen a Cloudinary.
     * @param imageUri La URI de la imagen seleccionada por el usuario.
     * @param listener El callback para notificar el resultado.
     */
    public void uploadImage(Uri imageUri, UploadCallbackListener listener) {
        // La subida se hace en un hilo de fondo para no bloquear la UI
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            MediaManager.get().upload(imageUri)
                    .unsigned("TU_UPLOAD_PRESET") // ¡Importante! Usa un upload preset sin firmar
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            handler.post(() -> listener.onSuccess(imageUrl));
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            handler.post(() -> listener.onError(error.getDescription()));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    })
                    .dispatch();
        });
    }
}