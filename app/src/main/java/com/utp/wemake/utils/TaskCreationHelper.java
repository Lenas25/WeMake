package com.utp.wemake.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clase helper que centraliza la lógica de creación de tareas
 * para evitar duplicación entre CreateTaskViewModel y VoiceTaskViewModel
 */
public class TaskCreationHelper {
    private static final String TAG = "TaskCreationHelper";
    
    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final FirebaseAuth auth;

    public TaskCreationHelper(Context context) {
        this.taskRepository = new TaskRepository(context);
        this.memberRepository = new MemberRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Calcula los puntos de recompensa basándose en la prioridad
     */
    public static int calculateRewardPoints(String priority) {
        if (priority == null) return 50;
        switch (priority.toLowerCase()) {
            case "alta": return 100;
            case "media": return 50;
            case "baja": return 25;
            default: return 50;
        }
    }

    /**
     * Calcula los puntos de penalización basándose en la prioridad
     */
    public static int calculatePenaltyPoints(String priority) {
        if (priority == null) return 10;
        switch (priority.toLowerCase()) {
            case "alta": return 20;
            case "media": return 10;
            case "baja": return 5;
            default: return 10;
        }
    }

    /**
     * Asegura que todas las subtareas tengan IDs
     */
    public static void ensureSubtasksHaveIds(List<Subtask> subtasks) {
        if (subtasks != null && !subtasks.isEmpty()) {
            for (Subtask subtask : subtasks) {
                if (subtask.getId() == null || subtask.getId().isEmpty()) {
                    subtask.setId(java.util.UUID.randomUUID().toString());
                }
            }
        }
    }

    /**
     * Crea una tarea usando la misma lógica que CreateTaskViewModel.saveTask()
     * Maneja automáticamente si el usuario es admin o no
     * 
     * @param boardId ID del tablero
     * @param title Título de la tarea
     * @param description Descripción de la tarea
     * @param priority Prioridad (alta/media/baja)
     * @param assignedMemberIds Lista de IDs de miembros asignados
     * @param deadline Fecha límite (puede ser null)
     * @param subtasks Lista de subtareas
     * @param rewardPoints Puntos de recompensa
     * @param penaltyPoints Puntos de penalización
     * @param reviewerId ID del revisor (puede ser null)
     * @param callback Callback con el resultado (success: true si se creó, false si hubo error, errorMessage: mensaje de error si hubo)
     */
    public void createTask(
            String boardId,
            String title,
            String description,
            String priority,
            List<String> assignedMemberIds,
            Date deadline,
            List<Subtask> subtasks,
            int rewardPoints,
            int penaltyPoints,
            String reviewerId,
            TaskCreationCallback callback) {

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e(TAG, "User not authenticated");
            if (callback != null) {
                callback.onResult(false, "Usuario no autenticado");
            }
            return;
        }

        // Verificar si el usuario es admin
        memberRepository.isCurrentUserAdminOfBoard(boardId).addOnCompleteListener(adminTask -> {
            boolean isAdmin = adminTask.isSuccessful() && Boolean.TRUE.equals(adminTask.getResult());
            
            if (isAdmin) {
                createTaskAsAdmin(
                    boardId, title, description, priority, assignedMemberIds, deadline,
                    subtasks, rewardPoints, penaltyPoints, reviewerId, currentUserId, callback
                );
            } else {
                createTaskProposalAsUser(
                    boardId, title, description, priority, assignedMemberIds, deadline,
                    subtasks, reviewerId, currentUserId, callback
                );
            }
        });
    }

    /**
     * Crea una tarea directamente como administrador
     */
    private void createTaskAsAdmin(
            String boardId,
            String title,
            String description,
            String priority,
            List<String> assignedMemberIds,
            Date deadline,
            List<Subtask> subtasks,
            int rewardPoints,
            int penaltyPoints,
            String reviewerId,
            String currentUserId,
            TaskCreationCallback callback) {

        // Asegurar que subtasks tenga IDs
        if (subtasks == null) {
            subtasks = new ArrayList<>();
        }
        ensureSubtasksHaveIds(subtasks);

        TaskModel task = new TaskModel();
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setPriority(priority != null ? priority : "media");
        task.setDeadline(deadline);
        task.setSubtasks(subtasks);
        task.setAssignedMembers(assignedMemberIds != null ? assignedMemberIds : new ArrayList<>());
        task.setBoardId(boardId);
        task.setRewardPoints(rewardPoints);
        task.setPenaltyPoints(penaltyPoints);
        task.setReviewerId(reviewerId);
        task.setCreatedBy(currentUserId);
        task.setCreatedAt(new Date());
        task.setApprovedBy(currentUserId);
        task.setApprovedAt(new Date());
        task.setStatus("pending");

        Log.d(TAG, "Creating task as admin. Title: " + task.getTitle());
        
        taskRepository.createTask(task).addOnCompleteListener(taskResult -> {
            if (taskResult.isSuccessful()) {
                String taskId = taskResult.getResult().getId();
                Log.d(TAG, "Task created successfully with ID: " + taskId);
                if (callback != null) {
                    callback.onResult(true, null);
                }
            } else {
                Exception exception = taskResult.getException();
                String error = "Error creando tarea: " + (exception != null ? exception.getMessage() : "Desconocido");
                Log.e(TAG, error, exception);
                if (callback != null) {
                    callback.onResult(false, error);
                }
            }
        });
    }

    /**
     * Crea una propuesta de tarea como usuario normal
     */
    private void createTaskProposalAsUser(
            String boardId,
            String title,
            String description,
            String priority,
            List<String> assignedMemberIds,
            Date deadline,
            List<Subtask> subtasks,
            String reviewerId,
            String currentUserId,
            TaskCreationCallback callback) {

        // Asegurar que subtasks tenga IDs
        if (subtasks == null) {
            subtasks = new ArrayList<>();
        }
        ensureSubtasksHaveIds(subtasks);

        String proposerName = "Usuario desconocido";
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null) {
            proposerName = auth.getCurrentUser().getDisplayName();
        }

        TaskProposal proposal = new TaskProposal();
        proposal.setTitle(title);
        proposal.setDescription(description != null ? description : "");
        proposal.setPriority(priority != null ? priority : "media");
        proposal.setDeadline(deadline);
        proposal.setSubtasks(subtasks);
        proposal.setBoardId(boardId);
        proposal.setProposedBy(currentUserId);
        proposal.setProposerName(proposerName);
        proposal.setProposedAt(new Date());
        proposal.setStatus("awaiting_approval");
        proposal.setAssignedMembers(assignedMemberIds != null ? assignedMemberIds : new ArrayList<>());
        proposal.setReviewerId(reviewerId);

        Log.d(TAG, "Creating task proposal as regular user. Title: " + proposal.getTitle());
        
        taskRepository.createTaskProposal(proposal).addOnCompleteListener(taskResult -> {
            if (taskResult.isSuccessful()) {
                String proposalId = taskResult.getResult().getId();
                Log.d(TAG, "Proposal created successfully with ID: " + proposalId);
                if (callback != null) {
                    callback.onResult(true, null);
                }
            } else {
                Exception exception = taskResult.getException();
                String error = "Error creando propuesta: " + (exception != null ? exception.getMessage() : "Desconocido");
                Log.e(TAG, error, exception);
                if (callback != null) {
                    callback.onResult(false, error);
                }
            }
        });
    }

    /**
     * Interface para el callback de creación de tareas
     */
    public interface TaskCreationCallback {
        /**
         * Se llama cuando la operación termina
         * @param success true si la tarea se creó exitosamente, false si hubo error
         * @param errorMessage mensaje de error si hubo (null si success es true)
         */
        void onResult(boolean success, String errorMessage);
    }
}
