package com.utp.wemake.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.utils.Event;
import com.utp.wemake.utils.TaskCreationHelper;
import com.utp.wemake.workers.SyncWorker;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CreateTaskViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final FirebaseAuth auth;
    private final TaskCreationHelper taskCreationHelper;
    private final WorkManager workManager;

    // --- Estado de la Interfaz ---
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Event<String>> _toastMessage = new MutableLiveData<>();
    public LiveData<Event<String>> getToastMessage() { return _toastMessage; }

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


    public CreateTaskViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.memberRepository = new MemberRepository();
        this.auth = FirebaseAuth.getInstance();
        this.workManager = WorkManager.getInstance(application);
        this.taskCreationHelper = new TaskCreationHelper(application);
    }

    /**
     * Activa el WorkManager para que intente subir los datos
     * a Firebase cuando haya conexión.
     */
    private void scheduleSync() {
        final String UNIQUE_SYNC_WORK_NAME = "sync_tasks_to_firebase";
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();
        workManager.enqueueUniqueWork(
                UNIQUE_SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
        );
    }

    /**
     * Carga los datos necesarios para la pantalla. Si se provee un taskId,
     * también carga los datos de esa tarea para edición.
     *
     * @param boardId ID del tablero actual.
     * @param taskId  ID de la tarea a editar (puede ser null para crear una nueva).
     */
    public void loadInitialData(String boardId, String taskId) {
        this.editingTaskId = taskId;

        checkUserRoleForBoard(boardId);
        loadBoardMembers(boardId);

        if (taskId != null && !taskId.isEmpty()) {
            _isLoading.setValue(true);
            taskRepository.getTaskById(taskId).addOnCompleteListener(task -> {
                _isLoading.setValue(false);
                if (task.isSuccessful() && task.getResult() != null) {
                    _taskToEdit.setValue(task.getResult().toObject(TaskModel.class));
                } else {
                    _errorMessage.setValue("Error al cargar la tarea para editar.");
                }
            });
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
     * Ahora maneja los 4 casos: Admin-Crear, Admin-Editar, Usuario-Crear, Usuario-Editar.
     * REFACTORIZADO: Usa TaskCreationHelper para crear, mantiene lógica propia para editar.
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
            taskCreationHelper.createTask(boardId,
                    title,
                    description,
                    priority,
                    assignedMemberIds,
                    deadline,
                    subtasks,
                    rewardPoints,
                    penaltyPoints,
                    reviewerId,
                    (success, wasOffline, errorMessage)  -> {
                        _isLoading.setValue(false);
                        if (success) {
                            _taskSaved.setValue(true);

                            if (wasOffline) {
                                _toastMessage.setValue(new Event<>("Guardado localmente. Se sincronizará al conectar."));
                            } else {
                                _toastMessage.setValue(new Event<>("Tarea creada con éxito."));
                            }

                            scheduleSync();
                        } else {
                            _errorMessage.setValue(errorMessage);
                        }
                    }
            );
            return; // Salir temprano, el callback maneja el resultado
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

            // Preservar los datos originales que no están en el formulario
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
     * Carga la tarea original y solo modifica los campos que un usuario normal puede cambiar.
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
        taskRepository.updateTask(task).addOnCompleteListener(taskResult -> {
            _isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                _taskSaved.setValue(true);
            } else {
                _errorMessage.setValue("Error al actualizar la tarea: " + taskResult.getException().getMessage());
            }
        });
    }
}