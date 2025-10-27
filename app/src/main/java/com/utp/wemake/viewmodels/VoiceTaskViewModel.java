package com.utp.wemake.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.models.VoiceTaskResponse;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.services.VoiceTaskAIService;
import com.utp.wemake.utils.Event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VoiceTaskViewModel extends AndroidViewModel {
    private final TaskRepository taskRepository;
    private final VoiceTaskAIService aiService;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private MemberRepository memberRepository;

    // LiveData para UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> recognizedText = new MutableLiveData<>("");
    private final MutableLiveData<VoiceTaskResponse> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> taskCreated = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public VoiceTaskViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository();
        this.aiService = new VoiceTaskAIService();
        this.memberRepository = new MemberRepository();
    }

    // Getters
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getRecognizedText() { return recognizedText; }
    public LiveData<VoiceTaskResponse> getAiResponse() { return aiResponse; }
    public LiveData<Event<Boolean>> getTaskCreated() { return taskCreated; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void processVoiceText(String voiceText, String boardId) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        // Validar boardId
        if (boardId == null || boardId.isEmpty()) {
            isLoading.setValue(false);
            errorMessage.setValue("No se seleccionó un tablero. Por favor, selecciona un tablero primero.");
            return;
        }

        aiService.processVoiceText(voiceText, boardId)
                .thenAccept(response -> {
                    isLoading.setValue(false);
                    aiResponse.setValue(response);

                    if (response.isSuccess()) {
                        // Automáticamente crear la tarea
                        createTaskFromAIResponse(response, boardId);
                    } else {
                        errorMessage.setValue(response.getError());
                    }
                })
                .exceptionally(throwable -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Error procesando texto: " + throwable.getMessage());
                    return null;
                });
    }

    private boolean checkIfUserIsAdmin(String boardId) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null || boardId == null) {
            Log.w("VoiceTaskViewModel", "checkIfUserIsAdmin: userId or boardId is null");
            return false;
        }

        // Cargar el miembro del usuario en el board
        Task<DocumentSnapshot> memberTask = memberRepository.getMember(boardId, currentUserId);
        try {
            DocumentSnapshot memberDoc = Tasks.await(memberTask, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (memberDoc.exists()) {
                Member member = memberDoc.toObject(Member.class);
                boolean isAdmin = member != null && "admin".equals(member.getRole());
                Log.d("VoiceTaskViewModel", "User is admin: " + isAdmin);
                return isAdmin;
            }
            Log.w("VoiceTaskViewModel", "Member document does not exist for userId: " + currentUserId);
        } catch (Exception e) {
            Log.e("VoiceTaskViewModel", "Error checking admin status", e);
            errorMessage.postValue("Error verificando permisos: " + e.getMessage());
        }
        return false;
    }

    private void createTaskFromAIResponse(VoiceTaskResponse response, String boardId) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            errorMessage.setValue("Usuario no autenticado");
            return;
        }

        boolean isAdmin = checkIfUserIsAdmin(boardId);

        // Validar que la respuesta sea exitosa
        if (response.getTitle() == null || response.getTitle().isEmpty()) {
            errorMessage.setValue("No se pudo generar la tarea. Intenta de nuevo.");
            return;
        }

        // Asegurar que assignedMembers no sea null
        List<String> assignedMembers = response.getAssignedMembers();
        if (assignedMembers == null || assignedMembers.isEmpty()) {
            assignedMembers = new ArrayList<>();
            assignedMembers.add(currentUserId); // Asignar al usuario actual
        }

        // Asignar puntos basados en la prioridad
        int rewardPoints = calculateRewardPoints(response.getPriority());
        int penaltyPoints = calculatePenaltyPoints(response.getPriority());

        if (isAdmin) {
            // Crear tarea directamente
            TaskModel task = new TaskModel();
            task.setTitle(response.getTitle());
            task.setDescription(response.getDescription() != null ? response.getDescription() : "");
            task.setPriority(response.getPriority() != null ? response.getPriority() : "media");
            task.setDeadline(response.getDeadline());

            // Asegurar que subtasks tenga IDs
            if (response.getSubtasks() != null && !response.getSubtasks().isEmpty()) {
                for (Subtask subtask : response.getSubtasks()) {
                    if (subtask.getId() == null || subtask.getId().isEmpty()) {
                        subtask.setId(java.util.UUID.randomUUID().toString());
                    }
                }
            }

            task.setSubtasks(response.getSubtasks() != null ? response.getSubtasks() : new ArrayList<>());
            task.setAssignedMembers(assignedMembers);
            task.setReviewerId(response.getReviewerId());
            task.setBoardId(boardId);
            task.setCreatedBy(currentUserId);
            task.setCreatedAt(new Date());
            task.setStatus("pending");
            task.setRewardPoints(rewardPoints);
            task.setPenaltyPoints(penaltyPoints);

            taskRepository.createTask(task).addOnCompleteListener(taskResult -> {
                if (taskResult.isSuccessful()) {
                    String taskId = taskResult.getResult().getId();
                    Log.d("VoiceTaskViewModel", "Task created successfully with ID: " + taskId);
                    taskCreated.setValue(new Event<>(true));
                } else {
                    Exception exception = taskResult.getException();
                    String error = "Error creando tarea: " + (exception != null ? exception.getMessage() : "Desconocido");
                    Log.e("VoiceTaskViewModel", error, exception);
                    errorMessage.setValue(error);
                    isLoading.setValue(false);
                }
            });
        } else {
            // Crear propuesta de tarea
            TaskProposal proposal = new TaskProposal();
            proposal.setTitle(response.getTitle());
            proposal.setDescription(response.getDescription() != null ? response.getDescription() : "");
            proposal.setPriority(response.getPriority() != null ? response.getPriority() : "media");
            proposal.setDeadline(response.getDeadline());
            proposal.setSubtasks(response.getSubtasks() != null ? response.getSubtasks() : new ArrayList<>());
            proposal.setAssignedMembers(assignedMembers);
            proposal.setReviewerId(response.getReviewerId());
            proposal.setBoardId(boardId);
            proposal.setProposedBy(currentUserId);
            proposal.setProposedAt(new Date());
            proposal.setStatus("awaiting_approval");

            taskRepository.createTaskProposal(proposal).addOnCompleteListener(taskResult -> {
                if (taskResult.isSuccessful()) {
                    String proposalId = taskResult.getResult().getId();
                    Log.d("VoiceTaskViewModel", "Proposal created successfully with ID: " + proposalId);
                    taskCreated.setValue(new Event<>(true));
                } else {
                    Exception exception = taskResult.getException();
                    String error = "Error creando propuesta: " + (exception != null ? exception.getMessage() : "Desconocido");
                    Log.e("VoiceTaskViewModel", error, exception);
                    errorMessage.setValue(error);
                    isLoading.setValue(false);
                }
            });
        }
    }

    private int calculateRewardPoints(String priority) {
        if (priority == null) return 50;
        switch (priority) {
            case "alta": return 100;
            case "media": return 50;
            case "baja": return 25;
            default: return 50;
        }
    }

    private int calculatePenaltyPoints(String priority) {
        if (priority == null) return 10;
        switch (priority) {
            case "alta": return 20;
            case "media": return 10;
            case "baja": return 5;
            default: return 10;
        }
    }

    public void updateRecognizedText(String text) {
        recognizedText.setValue(text);
    }
}