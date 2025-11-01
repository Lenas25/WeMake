package com.utp.wemake.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.utp.wemake.models.VoiceTaskResponse;
import com.utp.wemake.services.VoiceTaskAIService;
import com.utp.wemake.utils.Event;
import com.utp.wemake.utils.TaskCreationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VoiceTaskViewModel extends AndroidViewModel {
    private final VoiceTaskAIService aiService;
    private final TaskCreationHelper taskCreationHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newCachedThreadPool();

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
    }

    // Getters
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getRecognizedText() { return recognizedText; }
    public LiveData<VoiceTaskResponse> getAiResponse() { return aiResponse; }
    public LiveData<Event<Boolean>> getTaskCreated() { return taskCreated; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void processVoiceText(String voiceText, String boardId) {
        Log.d("VoiceTaskViewModel", "processVoiceText called. Text: " + voiceText + ", BoardId: " + boardId);
        
        // Usar postValue que es thread-safe
        isLoading.postValue(true);
        errorMessage.postValue(null);

        // Validar boardId
        if (boardId == null || boardId.isEmpty()) {
            Log.e("VoiceTaskViewModel", "boardId is null or empty");
            isLoading.postValue(false);
            errorMessage.postValue("No se seleccionó un tablero. Por favor, selecciona un tablero primero.");
            return;
        }

        Log.d("VoiceTaskViewModel", "Calling AI service to process voice text");

        aiService.processVoiceText(voiceText, boardId)
                .thenAcceptAsync(response -> {
                    Log.d("VoiceTaskViewModel", "AI response received. Success: " + response.isSuccess());
                    
                    // Usar postValue que es thread-safe (se puede llamar desde cualquier hilo)
                    aiResponse.postValue(response);

                    if (response.isSuccess()) {
                        Log.d("VoiceTaskViewModel", "Creating task from AI response. Title: " + response.getTitle());
                        // Reutilizar la lógica existente de creación de tareas
                        createTaskFromAIResponse(response, boardId);
                    } else {
                        Log.e("VoiceTaskViewModel", "AI response error: " + response.getError());
                        isLoading.postValue(false);
                        errorMessage.postValue(response.getError() != null ? response.getError() : "Error desconocido de la IA");
                    }
                }, executor)
                .exceptionally(throwable -> {
                    Log.e("VoiceTaskViewModel", "Error processing voice text", throwable);
                    String errorMsg = throwable.getMessage();
                    if (errorMsg == null && throwable.getCause() != null) {
                        errorMsg = throwable.getCause().getMessage();
                    }
                    final String finalErrorMsg = errorMsg != null ? errorMsg : "Error desconocido";
                    
                    // Usar postValue que es thread-safe
                    isLoading.postValue(false);
                    errorMessage.postValue("Error procesando texto: " + finalErrorMsg);
                    return null;
                });
    }

    /**
     * Reutiliza la lógica de creación de tareas a través del helper
     */
    private void createTaskFromAIResponse(VoiceTaskResponse response, String boardId) {
        Log.d("VoiceTaskViewModel", "createTaskFromAIResponse called. Title: " + response.getTitle() + ", boardId: " + boardId);

        // Validar que la respuesta sea exitosa
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
        // El helper se encargará de asegurar que tengan IDs

        // Preparar assignedMembers
        List<String> assignedMemberIds = response.getAssignedMembers();
        if (assignedMemberIds == null || assignedMemberIds.isEmpty()) {
            assignedMemberIds = new ArrayList<>();
        }

        // Calcular puntos basados en la prioridad usando el helper
        int rewardPoints = TaskCreationHelper.calculateRewardPoints(priority);
        int penaltyPoints = TaskCreationHelper.calculatePenaltyPoints(priority);
        
        String reviewerId = response.getReviewerId();

        // Usar el helper para crear la tarea (reutiliza toda la lógica existente)
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
                        // Usar postValue que es thread-safe
                        isLoading.postValue(false);
                        if (success) {
                            Log.d("VoiceTaskViewModel", "Task created successfully");
                            taskCreated.postValue(new Event<>(true));
                        } else {
                            Log.e("VoiceTaskViewModel", "Error creating task: " + errorMessage);
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