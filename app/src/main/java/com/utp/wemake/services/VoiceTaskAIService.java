package com.utp.wemake.services;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.utp.wemake.BuildConfig;
import com.utp.wemake.models.VoiceTaskResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceTaskAIService {
    private static final String TAG = "VoiceTaskAIService";
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String MODEL_NAME = "gemini-1.5-flash";

    private Gson gson;

    public VoiceTaskAIService() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .create();
    }

    public CompletableFuture<VoiceTaskResponse> processVoiceText(String voiceText, String boardId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(voiceText, boardId);
                Log.d(TAG, "Enviando prompt a Gemini: " + prompt.substring(0, Math.min(200, prompt.length())));

                // Llamar a la API de Gemini usando HTTP
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + GEMINI_API_KEY);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                // Crear el JSON request
                JSONObject requestBodyObj = new JSONObject();
                try {
                    JSONArray contents = new JSONArray();
                    JSONObject content = new JSONObject();
                    JSONArray parts = new JSONArray();
                    JSONObject textPart = new JSONObject();
                    textPart.put("text", prompt);
                    parts.put(textPart);
                    content.put("parts", parts);
                    contents.put(content);
                    requestBodyObj.put("contents", contents);

                    String requestBody = requestBodyObj.toString();
                    Log.d(TAG, "Request body: " + requestBody);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error building request", e);
                    return createErrorResponse("Error construyendo petición: " + e.getMessage());
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        // Parsear respuesta
                        String jsonResponse = response.toString();
                        Log.d(TAG, "Gemini Response: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));

                        // Extraer y limpiar el JSON
                        String extractedJson = extractJsonFromResponse(jsonResponse);
                        Log.d(TAG, "Extracted JSON: " + extractedJson);

                        // Parsear con Gson
                        VoiceTaskResponse result;
                        try {
                            result = gson.fromJson(extractedJson, VoiceTaskResponse.class);

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

                            Log.d(TAG, "Parsed successfully. Title: " + result.getTitle());
                            return result;
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
                            return createErrorResponse("Error parseando la respuesta: " + e.getMessage());
                        }
                    }
                } else {
                    String errorMessage = "HTTP Error: " + responseCode;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponse.append(responseLine.trim());
                        }
                        Log.e(TAG, "Error response: " + errorResponse.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error stream", e);
                    }
                    return createErrorResponse(errorMessage);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
                e.printStackTrace();
                return createErrorResponse("Error de conexión: " + e.getMessage());
            }
        });
    }

    /**
     * Limpia el JSON de cualquier markdown o texto adicional que Gemini pueda devolver
     */
    private String extractJsonFromResponse(String rawResponse) {
        try {
            // Intentar parsear el JSON completo primero
            JSONObject jsonObject = new JSONObject(rawResponse);
            JSONArray candidates = jsonObject.getJSONArray("candidates");
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            String text = parts.getJSONObject(0).getString("text");

            Log.d(TAG, "Raw text from Gemini: " + text);

            // Eliminar markdown code blocks si existen
            text = text.replaceAll("```json", "").replaceAll("```", "").trim();

            // Buscar el JSON dentro del texto (por si hay texto adicional)
            Pattern pattern = Pattern.compile("\\{[^\\{]*\"title\"[^}]*\\}", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String jsonText = matcher.group(0);
                Log.d(TAG, "Extracted JSON from text: " + jsonText);
                return jsonText;
            }

            // Si no hay markdown, devolver el texto completo
            return text;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting JSON: " + e.getMessage());
            return rawResponse;
        }
    }

    private String buildPrompt(String voiceText, String boardId) {
        return "Eres un asistente que convierte texto de voz a datos estructurados de tareas.\n\n" +
                "Texto de voz: \"" + voiceText + "\"\n" +
                "Board ID: \"" + boardId + "\"\n\n" +
                "Analiza el texto y extrae la información de la tarea. Responde SOLO con un JSON válido en este formato exacto:\n" +
                "{\n" +
                "    \"title\": \"Título de la tarea\",\n" +
                "    \"description\": \"Descripción detallada\",\n" +
                "    \"priority\": \"alta|media|baja\",\n" +
                "    \"deadline\": null,\n" +
                "    \"subtasks\": [],\n" +
                "    \"assignedMembers\": [],\n" +
                "    \"reviewerId\": null,\n" +
                "    \"success\": true,\n" +
                "    \"error\": null\n" +
                "}\n\n" +
                "Reglas importantes:\n" +
                "- Responde SOLO con JSON válido, sin texto adicional\n" +
                "- NO uses markdown code blocks\n" +
                "- Si no hay deadline específico, usa null\n" +
                "- Si no hay subtareas, usa array vacío\n" +
                "- Si no hay miembros asignados, usa array vacío\n" +
                "- Si hay error, pon success: false y error con el mensaje";
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
                        // Continuar con el siguiente formato
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