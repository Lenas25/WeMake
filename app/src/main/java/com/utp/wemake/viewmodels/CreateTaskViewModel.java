package com.utp.wemake.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.repository.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CreateTaskViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final FirebaseAuth auth;

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


    public CreateTaskViewModel() {
        this.taskRepository = new TaskRepository();
        this.memberRepository = new MemberRepository();
        this.auth = FirebaseAuth.getInstance();
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
     */
    public void saveTask(String boardId, String title, String description, String priority,
                         List<String> assignedMemberIds, Date deadline, List<Subtask> subtasks,
                         int rewardPoints, int penaltyPoints, String reviewerId) {

        _isLoading.setValue(true);
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        boolean isEditing = editingTaskId != null;

        // --- CASO 1: El usuario es Administrador ---
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
            task.setReviewerId(reviewerId);

            if (isEditing) {
                // Admin Editando: Es importante preservar los datos que no están en el formulario.
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
            } else {
                // Admin Creando: Crea una nueva tarea completa y aprobada.
                task.setCreatedBy(currentUserId);
                task.setCreatedAt(new Date());
                task.setApprovedBy(currentUserId);
                task.setApprovedAt(new Date());
                task.setStatus("pending");
                createNewTask(task);
            }
        }
        // --- CASO 2: El usuario es un Miembro Normal ---
        else {
            if (isEditing) {
                // Usuario Editando: Actualiza solo los campos permitidos.
                performNonAdminUpdate(title, description, priority, assignedMemberIds, subtasks);
            } else {
                // Usuario Creando: Crea una nueva propuesta de tarea.
                createTaskProposal(boardId, title, description, priority, deadline, subtasks,
                        assignedMemberIds, reviewerId, currentUserId);
            }
        }
    }

    /**
     * Función específica para la actualización por parte de un usuario normal.
     * Carga la tarea original y solo modifica los campos que un usuario normal puede cambiar.
     */
    private void performNonAdminUpdate(String title, String description, String priority,
                                       List<String> assignedMemberIds, List<Subtask> subtasks) {
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


        updateExistingTask(originalTask);
    }

    private void createNewTask(TaskModel task) {
        taskRepository.createTask(task).addOnCompleteListener(taskResult -> {
            _isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                _taskSaved.setValue(true);
            } else {
                _errorMessage.setValue("Error al crear la tarea: " + taskResult.getException().getMessage());
            }
        });
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

    private void createTaskProposal(String boardId, String title, String description,
                                    String priority, Date deadline, List<Subtask> subtasks,
                                    List<String> assignedMemberIds, String reviewerId, String proposerId) {

        String proposerName = "Usuario desconocido";
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null) {
            proposerName = auth.getCurrentUser().getDisplayName();
        }

        TaskProposal proposal = new TaskProposal();
        proposal.setTitle(title);
        proposal.setDescription(description);
        proposal.setPriority(priority);
        proposal.setDeadline(deadline);
        proposal.setSubtasks(subtasks);
        proposal.setBoardId(boardId);
        proposal.setProposedBy(proposerId);
        proposal.setProposerName(proposerName);
        proposal.setProposedAt(new Date());
        proposal.setStatus("awaiting_approval");
        proposal.setAssignedMembers(assignedMemberIds);
        proposal.setReviewerId(reviewerId);

        taskRepository.createTaskProposal(proposal).addOnCompleteListener(taskResult -> {
            _isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                _taskSaved.setValue(true);
            } else {
                _errorMessage.setValue("Error al crear la propuesta: " + taskResult.getException().getMessage());
            }
        });
    }
}