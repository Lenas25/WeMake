package com.utp.wemake.services;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.utp.wemake.BuildConfig;
import com.utp.wemake.models.VoiceTaskResponse;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class VoiceTaskAIService {
    private static final String TAG = "VoiceTaskAIService";
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String MODEL_NAME = "gemini-2.5-flash";

    private GenerativeModel model;
    private Gson gson;

    public VoiceTaskAIService() {
        this.model = new GenerativeModel(MODEL_NAME, GEMINI_API_KEY);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
    }

    public CompletableFuture<VoiceTaskResponse> processVoiceText(String voiceText, String boardId) {
        Log.d(TAG, "processVoiceText iniciado. Texto: " + voiceText);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(voiceText, boardId);
                Log.d(TAG, "Enviando prompt a Gemini (primeros 200 chars): " + prompt.substring(0, Math.min(200, prompt.length())));

                // Usar el helper de Kotlin para llamar a Gemini
                String responseText = GeminiHelper.INSTANCE.generateContentSync(model, prompt);
                Log.d(TAG, "Respuesta de Gemini recibida. Longitud: " + (responseText != null ? responseText.length() : 0));
                Log.d(TAG, "Respuesta completa: " + responseText);

                if (responseText == null || responseText.trim().isEmpty()) {
                    Log.e(TAG, "Respuesta vacía de Gemini");
                    return createErrorResponse("La IA no devolvió ninguna respuesta.");
                }

                // Limpiar la respuesta de markdown si existe
                String cleanJson = cleanJsonResponse(responseText);
                Log.d(TAG, "JSON limpio: " + cleanJson);

                // Parsear con Gson
                VoiceTaskResponse result = gson.fromJson(cleanJson, VoiceTaskResponse.class);

                // Validar que se parseó correctamente
                if (result == null) {
                    Log.e(TAG, "Parsed result is null");
                    return createErrorResponse("No se pudo parsear la respuesta del asistente.");
                }

                // Validar que tiene los campos mínimos requeridos
                if (result.getTitle() == null || result.getTitle().trim().isEmpty()) {
                    Log.e(TAG, "Parsed result has no title");
                    return createErrorResponse("La respuesta del asistente no contiene un título válido.");
                }

                // Establecer success=true por defecto si no hay error
                if (!result.isSuccess()) {
                    result.setSuccess(true);
                }

                Log.d(TAG, "Parsed successfully. Title: " + result.getTitle());
                return result;

            } catch (Exception e) {
                Log.e(TAG, "Error llamando a Gemini API: " + e.getMessage(), e);
                e.printStackTrace();
                return createErrorResponse("Error de conexión con la IA: " + e.getMessage());
            }
        });
    }

    /**
     * Limpia el JSON de cualquier markdown o texto adicional que Gemini pueda devolver
     */
    private String cleanJsonResponse(String rawResponse) {
        try {
            String cleaned = rawResponse.trim();

            // Eliminar markdown code blocks si existen
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();

            // Buscar el JSON dentro del texto (por si hay texto adicional)
            int startIndex = cleaned.indexOf("{");
            int lastIndex = cleaned.lastIndexOf("}");

            if (startIndex != -1 && lastIndex != -1 && lastIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, lastIndex + 1);
            }

            return cleaned;
        } catch (Exception e) {
            Log.e(TAG, "Error limpiando JSON: " + e.getMessage());
            return rawResponse;
        }
    }

    private String buildPrompt(String voiceText, String boardId) {
        return "Eres un asistente que convierte texto de voz a datos estructurados de tareas.\n\n" +
                "Texto de voz del usuario: \"" + voiceText + "\"\n\n" +
                "Analiza el texto y extrae la información de la tarea. Devuelve SOLO un JSON válido en este formato exacto (sin markdown, sin texto adicional):\n" +
                "{\n" +
                "    \"title\": \"Título de la tarea\",\n" +
                "    \"description\": \"Descripción detallada de la tarea\",\n" +
                "    \"priority\": \"alta|media|baja\",\n" +
                "    \"deadline\": null,\n" +
                "    \"subtasks\": [],\n" +
                "    \"assignedMembers\": [],\n" +
                "    \"reviewerId\": null,\n" +
                "    \"success\": true,\n" +
                "    \"error\": null\n" +
                "}\n\n" +
                "INSTRUCCIONES IMPORTANTES:\n" +
                "1. Responde SOLO con el JSON, sin markdown (sin ```json o ```)\n" +
                "2. Si el usuario menciona una fecha límite, parséala y ponla en formato ISO: \"yyyy-MM-dd\" en el campo deadline\n" +
                "3. Si el usuario menciona prioridad, usa \"alta\", \"media\" o \"baja\"\n" +
                "4. Si no hay deadline, usa null en el campo deadline\n" +
                "5. Si no hay subtareas, usa array vacío [] en subtasks\n" +
                "6. Si no hay miembros asignados mencionados, usa array vacío [] en assignedMembers\n" +
                "7. Siempre incluye success: true y error: null\n" +
                "8. El título es OBLIGATORIO\n\n" +
                "IMPORTANTE: Responde SOLO con el JSON, nada más.";
    }

    private VoiceTaskResponse createErrorResponse(String errorMessage) {
        VoiceTaskResponse response = new VoiceTaskResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }

    /**
     * Deserializer personalizado para Date que maneja strings y null
     */
    private static class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }

            if (json.isJsonPrimitive()) {
                String dateString = json.getAsString();
                if (dateString == null || dateString.isEmpty() || "null".equals(dateString)) {
                    return null;
                }

                // Intentar varios formatos
                List<String> formats = List.of(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd",
                        "yyyy-MM-dd HH:mm:ss"
                );

                for (String format : formats) {
                    try {
                        return new SimpleDateFormat(format, Locale.US).parse(dateString);
                    } catch (Exception e) {
                    }
                }

                // Si ninguno funciona, retornar null
                Log.w("DateDeserializer", "No se pudo parsear la fecha: " + dateString);
                return null;
            }

            return null;
        }
    }
}