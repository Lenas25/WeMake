package com.utp.wemake.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.User;
import com.utp.wemake.models.VoiceTaskResponse;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.services.VoiceTaskAIService;
import com.utp.wemake.utils.Event;
import com.utp.wemake.utils.TaskCreationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VoiceTaskViewModel extends AndroidViewModel {
    private static final String TAG = "VoiceTaskViewModel";
    private final VoiceTaskAIService aiService;
    private final TaskCreationHelper taskCreationHelper;
    private final MemberRepository memberRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newCachedThreadPool();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // LiveData para UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> recognizedText = new MutableLiveData<>("");
    private final MutableLiveData<VoiceTaskResponse> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> taskCreated = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public VoiceTaskViewModel(@NonNull Application application) {
        super(application);
        this.aiService = new VoiceTaskAIService();
        this.taskCreationHelper = new TaskCreationHelper();
        this.memberRepository = new MemberRepository();
    }

    // Getters
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getRecognizedText() { return recognizedText; }
    public LiveData<VoiceTaskResponse> getAiResponse() { return aiResponse; }
    public LiveData<Event<Boolean>> getTaskCreated() { return taskCreated; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void processVoiceText(String voiceText, String boardId) {
        Log.d(TAG, "processVoiceText called. Text: " + voiceText + ", BoardId: " + boardId);
        
        isLoading.postValue(true);
        errorMessage.postValue(null);

        if (boardId == null || boardId.isEmpty()) {
            Log.e(TAG, "boardId is null or empty");
            isLoading.postValue(false);
            errorMessage.postValue("No se seleccionó un tablero. Por favor, selecciona un tablero primero.");
            return;
        }

        Log.d(TAG, "Calling AI service to process voice text");

        aiService.processVoiceText(voiceText, boardId)
                .thenAcceptAsync(response -> {
                    Log.d(TAG, "AI response received. Success: " + response.isSuccess());
                    
                    aiResponse.postValue(response);

                    if (response.isSuccess()) {
                        Log.d(TAG, "Creating task from AI response. Title: " + response.getTitle());
                        // Mapear nombres a IDs antes de crear la tarea
                        mapNamesToIdsAndCreateTask(response, boardId);
                    } else {
                        Log.e(TAG, "AI response error: " + response.getError());
                        isLoading.postValue(false);
                        errorMessage.postValue(response.getError() != null ? response.getError() : "Error desconocido de la IA");
                    }
                }, executor)
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error processing voice text", throwable);
                    String errorMsg = throwable.getMessage();
                    if (errorMsg == null && throwable.getCause() != null) {
                        errorMsg = throwable.getCause().getMessage();
                    }
                    final String finalErrorMsg = errorMsg != null ? errorMsg : "Error desconocido";
                    
                    isLoading.postValue(false);
                    errorMessage.postValue("Error procesando texto: " + finalErrorMsg);
                    return null;
                });
    }

    /**
     * Mapea los nombres de miembros mencionados en la respuesta de la IA a sus IDs reales,
     * y luego crea la tarea.
     */
    private void mapNamesToIdsAndCreateTask(VoiceTaskResponse response, String boardId) {
        Log.d(TAG, "mapNamesToIdsAndCreateTask called. Title: " + response.getTitle());

        // Obtener todos los miembros del tablero
        memberRepository.getBoardMembers(boardId).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error obteniendo miembros del tablero", task.getException());
                isLoading.postValue(false);
                errorMessage.postValue("Error obteniendo miembros del tablero. Intenta de nuevo.");
                return;
            }

            List<Map<String, Object>> membersList = task.getResult();
            if (membersList == null) {
                membersList = new ArrayList<>();
            }

            // Crear un mapa de nombres a IDs (busca coincidencias parciales)
            Map<String, String> nameToIdMap = new HashMap<>();
            for (Map<String, Object> memberData : membersList) {
                User user = (User) memberData.get("user");
                if (user != null && user.getName() != null) {
                    String name = user.getName().toLowerCase().trim();
                    String userId = user.getUserid();
                    nameToIdMap.put(name, userId);
                    
                    // También agregar coincidencias parciales (nombre completo, primer nombre, etc.)
                    String[] nameParts = name.split("\\s+");
                    if (nameParts.length > 1) {
                        // Agregar solo el primer nombre
                        nameToIdMap.put(nameParts[0], userId);
                        // Agregar combinaciones comunes
                        nameToIdMap.put(nameParts[0] + " " + nameParts[nameParts.length - 1], userId);
                    }
                }
            }

            Log.d(TAG, "Mapa de nombres a IDs creado: " + nameToIdMap);

            // Mapear assignedMembers
            List<String> assignedMemberIds = new ArrayList<>();
            List<String> assignedMemberNames = response.getAssignedMembers();
            if (assignedMemberNames != null && !assignedMemberNames.isEmpty()) {
                for (String memberName : assignedMemberNames) {
                    String memberId = findMemberIdByName(memberName, nameToIdMap);
                    if (memberId != null) {
                        assignedMemberIds.add(memberId);
                        Log.d(TAG, "Mapeado: " + memberName + " -> " + memberId);
                    } else {
                        Log.w(TAG, "No se encontró ID para el miembro: " + memberName);
                    }
                }
            }

            // Mapear reviewerId
            String reviewerId = null;
            String reviewerName = response.getReviewerId();
            if (reviewerName != null && !reviewerName.trim().isEmpty()) {
                reviewerId = findMemberIdByName(reviewerName, nameToIdMap);
                if (reviewerId != null) {
                    Log.d(TAG, "Mapeado revisor: " + reviewerName + " -> " + reviewerId);
                } else {
                    Log.w(TAG, "No se encontró ID para el revisor: " + reviewerName);
                }
            }

            // Si no se encontró revisor o no se mencionó, usar el usuario actual
            if (reviewerId == null || reviewerId.isEmpty()) {
                if (auth.getCurrentUser() != null) {
                    reviewerId = auth.getCurrentUser().getUid();
                    Log.d(TAG, "Usando ID del usuario actual como revisor: " + reviewerId);
                } else {
                    Log.w(TAG, "No hay usuario autenticado, reviewerId será null");
                }
            }

            // Crear la tarea con los IDs mapeados
            createTaskFromAIResponse(response, boardId, assignedMemberIds, reviewerId);
        });
    }

    /**
     * Busca el ID de un miembro por su nombre usando coincidencias parciales
     */
    private String findMemberIdByName(String name, Map<String, String> nameToIdMap) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalizedName = name.toLowerCase().trim();
        
        // Buscar coincidencia exacta primero
        if (nameToIdMap.containsKey(normalizedName)) {
            return nameToIdMap.get(normalizedName);
        }

        // Buscar coincidencias parciales
        for (Map.Entry<String, String> entry : nameToIdMap.entrySet()) {
            String mapKey = entry.getKey();
            // Si el nombre mencionado contiene el nombre del mapa o viceversa
            if (normalizedName.contains(mapKey) || mapKey.contains(normalizedName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Crea la tarea con los IDs ya mapeados
     */
    private void createTaskFromAIResponse(VoiceTaskResponse response, String boardId, 
                                         List<String> assignedMemberIds, String reviewerId) {
        Log.d(TAG, "createTaskFromAIResponse called. Title: " + response.getTitle() + ", boardId: " + boardId);

        if (response.getTitle() == null || response.getTitle().isEmpty()) {
            isLoading.postValue(false);
            errorMessage.postValue("No se pudo generar la tarea. Intenta de nuevo.");
            return;
        }

        // Preparar los datos para crear la tarea
        String title = response.getTitle();
        String description = response.getDescription() != null ? response.getDescription() : "";
        String priority = response.getPriority() != null ? response.getPriority() : "media";
        java.util.Date deadline = response.getDeadline();

        // Preparar subtasks
        List<com.utp.wemake.models.Subtask> subtasks = response.getSubtasks();
        if (subtasks == null) {
            subtasks = new ArrayList<>();
        }

        int rewardPoints = TaskCreationHelper.calculateRewardPoints(priority);
        int penaltyPoints = TaskCreationHelper.calculatePenaltyPoints(priority);

        taskCreationHelper.createTask(
                boardId,
                title,
                description,
                priority,
                assignedMemberIds,
                deadline,
                subtasks,
                rewardPoints,
                penaltyPoints,
                reviewerId,
                new TaskCreationHelper.TaskCreationCallback() {
                    @Override
                    public void onResult(boolean success, String errorMessage) {
                        isLoading.postValue(false);
                        if (success) {
                            Log.d(TAG, "Task created successfully");
                            taskCreated.postValue(new Event<>(true));
                        } else {
                            Log.e(TAG, "Error creating task: " + errorMessage);
                            VoiceTaskViewModel.this.errorMessage.postValue(errorMessage);
                        }
                    }
                }
        );
    }

    public void updateRecognizedText(String text) {
        recognizedText.postValue(text);
    }
}