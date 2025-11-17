package com.utp.wemake.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.utils.TaskCreationHelper;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class CreateTaskViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final FirebaseAuth auth;
    private final TaskCreationHelper taskCreationHelper;

    // --- Estado de la Interfaz ---
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _taskSaved = new MutableLiveData<>(false);
    public final LiveData<Boolean> taskSaved = _taskSaved;

    // --- Datos ---
    private final MutableLiveData<List<Map<String, Object>>> _boardMembers = new MutableLiveData<>();
    public final LiveData<List<Map<String, Object>>> boardMembers = _boardMembers;

    private final MutableLiveData<TaskModel> _taskToEdit = new MutableLiveData<>();
    public final LiveData<TaskModel> taskToEdit = _taskToEdit;

    // --- Rol del Usuario ---
    private final MutableLiveData<Boolean> _isUserAdmin = new MutableLiveData<>();
    public final LiveData<Boolean> isUserAdmin = _isUserAdmin;

    // Variable para saber si estamos editando
    private String editingTaskId = null;

    // Observer para limpiar cuando el ViewModel se destruya
    private androidx.lifecycle.Observer<TaskModel> taskObserver;

    public CreateTaskViewModel(@NonNull Application application) {
        this.taskRepository = new TaskRepository(application);
        this.memberRepository = new MemberRepository();
        this.auth = FirebaseAuth.getInstance();
        this.taskCreationHelper = new TaskCreationHelper(application);
    }

    /**
     * Carga los datos necesarios para la pantalla. Si se provee un taskId,
     * también carga los datos de esa tarea para edición.
     */
    public void loadInitialData(String boardId, String taskId) {
        this.editingTaskId = taskId;

        checkUserRoleForBoard(boardId);
        loadBoardMembers(boardId);

        if (taskId != null && !taskId.isEmpty()) {
            _isLoading.setValue(true);

            // Usar LiveData desde Room
            LiveData<TaskModel> taskLiveData = taskRepository.getTaskById(taskId);

            // Remover observer anterior si existe
            if (taskObserver != null) {
                taskLiveData.removeObserver(taskObserver);
            }

            // Crear nuevo observer
            taskObserver = loadedTask -> {
                _isLoading.setValue(false);
                if (loadedTask != null) {
                    _taskToEdit.setValue(loadedTask);
                } else {
                    _errorMessage.setValue("Error al cargar la tarea para editar.");
                }
            };

            taskLiveData.observeForever(taskObserver);
        }
    }

    private void checkUserRoleForBoard(String boardId) {
        memberRepository.isCurrentUserAdminOfBoard(boardId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _isUserAdmin.setValue(task.getResult());
            } else {
                _isUserAdmin.setValue(false);
            }
        });
    }

    private void loadBoardMembers(String boardId) {
        _isLoading.setValue(true);
        memberRepository.getBoardMembers(boardId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                _boardMembers.setValue(task.getResult());
            } else {
                _errorMessage.setValue("Error al cargar miembros del tablero.");
            }
        });
    }

    /**
     * Lógica central para guardar o actualizar una tarea.
     */
    public void saveTask(String boardId, String title, String description, String priority,
                         List<String> assignedMemberIds, Date deadline, List<Subtask> subtasks,
                         int rewardPoints, int penaltyPoints, String reviewerId) {

        _isLoading.setValue(true);
        boolean isEditing = editingTaskId != null;

        // Si es creación (no edición), usar el helper
        if (!isEditing) {
            // Asegurar que subtasks tenga IDs antes de pasar al helper
            if (subtasks != null) {
                TaskCreationHelper.ensureSubtasksHaveIds(subtasks);
            }
            
            // Usar el helper para crear la tarea (maneja admin/usuario automáticamente)
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
                    (success, errorMessage) -> {
                        _isLoading.setValue(false);
                        if (success) {
                            _taskSaved.setValue(true);
                        } else {
                            _errorMessage.setValue(errorMessage);
                        }
                    }
            );
            return;
        }

        // --- CASO 1: El usuario es Administrador Editando ---
        if (Boolean.TRUE.equals(_isUserAdmin.getValue())) {
            TaskModel task = new TaskModel();
            task.setTitle(title);
            task.setDescription(description);
            task.setPriority(priority);
            task.setDeadline(deadline);
            task.setSubtasks(subtasks);
            task.setAssignedMembers(assignedMemberIds);
            task.setBoardId(boardId);
            task.setRewardPoints(rewardPoints);
            task.setPenaltyPoints(penaltyPoints);
            task.setPenaltyApplied(false);
            task.setReviewerId(reviewerId);

            TaskModel originalTask = _taskToEdit.getValue();
            if (originalTask != null) {
                task.setCreatedBy(originalTask.getCreatedBy());
                task.setCreatedAt(originalTask.getCreatedAt());
                task.setStatus(originalTask.getStatus());
                task.setApprovedBy(originalTask.getApprovedBy());
                task.setApprovedAt(originalTask.getApprovedAt());
            }
            task.setId(editingTaskId);
            updateExistingTask(task);
        }
        // --- CASO 2: El usuario es un Miembro Normal Editando ---
        else {
            performNonAdminUpdate(title, description, priority, assignedMemberIds, subtasks, deadline, reviewerId);
        }
    }

    /**
     * Función específica para la actualización por parte de un usuario normal.
     */
    private void performNonAdminUpdate(String title, String description, String priority,
                                       List<String> assignedMemberIds, List<Subtask> subtasks,
                                       Date deadline, String reviewerId) {
        TaskModel originalTask = _taskToEdit.getValue();

        if (originalTask == null) {
            _errorMessage.setValue("Error: No se pudo cargar la tarea original para actualizar.");
            _isLoading.setValue(false);
            return;
        }

        originalTask.setTitle(title);
        originalTask.setDescription(description);
        originalTask.setPriority(priority);
        originalTask.setAssignedMembers(assignedMemberIds);
        originalTask.setSubtasks(subtasks);
        originalTask.setDeadline(deadline);
        originalTask.setReviewerId(reviewerId);
        originalTask.setPenaltyApplied(false);

        updateExistingTask(originalTask);
    }

    private void updateExistingTask(TaskModel task) {
        taskRepository.updateTask(task)
                .addOnSuccessListener(aVoid -> {
                    _isLoading.setValue(false);
                    _taskSaved.setValue(true);
                })
                .addOnFailureListener(e -> {
                    _isLoading.setValue(false);
                    _errorMessage.setValue("Error al actualizar la tarea: " + e.getMessage());
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.detachListeners();
    }

    /**
     * Factory para crear CreateTaskViewModel con Application
     */
    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;

        public Factory(Application application) {
            this.application = application;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(CreateTaskViewModel.class)) {
                return (T) new CreateTaskViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}