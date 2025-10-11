package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.ImageRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final TaskRepository taskRepository;

    private final MutableLiveData<List<KanbanColumn>> kanbanColumns = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> pendingTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> expiredTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalPoints = new MutableLiveData<>();
    private final UserRepository userRepository = new UserRepository();
    private final ImageRepository imageRepository = new ImageRepository();

    // LiveData para los datos del usuario que la vista observará
    private final MutableLiveData<User> userData = new MutableLiveData<>();
    // LiveData para el estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    // LiveData para mensajes de error
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel() {
        this.taskRepository = new TaskRepository();
    }

    // --- Getters para que la Vista los observe ---
    public LiveData<List<KanbanColumn>> getKanbanColumns() { return kanbanColumns; }
    public LiveData<Integer> getTotalTasks() { return totalTasks; }
    public LiveData<Integer> getPendingTasks() { return pendingTasks; }
    public LiveData<Integer> getExpiredTasks() { return expiredTasks; }
    public LiveData<Integer> getTotalPoints() { return totalPoints; }
    public LiveData<User> getUserData() { return userData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadBoardData(String boardId) {
        isLoading.setValue(true);

        taskRepository.getTasksByBoard(boardId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                List<TaskModel> allTasks = task.getResult();
                organizeTasksIntoColumns(allTasks);
                calculateSummaryStats(allTasks);
            } else {
                errorMessage.setValue("Error al cargar las tareas del tablero");
            }
        });
    }

    private void organizeTasksIntoColumns(List<TaskModel> tasks) {
        List<TaskModel> pendingTasks = new ArrayList<>();
        List<TaskModel> inProgressTasks = new ArrayList<>();
        List<TaskModel> completedTasks = new ArrayList<>();

        for (TaskModel task : tasks) {
            switch (task.getStatus()) {
                case TaskConstants.STATUS_PENDING:
                    pendingTasks.add(task);
                    break;
                case TaskConstants.STATUS_IN_PROGRESS:
                    inProgressTasks.add(task);
                    break;
                case TaskConstants.STATUS_COMPLETED:
                    completedTasks.add(task);
                    break;
            }
        }

        List<KanbanColumn> columns = new ArrayList<>();
        columns.add(new KanbanColumn("Pendiente", pendingTasks));
        columns.add(new KanbanColumn("En Progreso", inProgressTasks));
        columns.add(new KanbanColumn("Completado", completedTasks));

        kanbanColumns.setValue(columns);
    }

    private void calculateSummaryStats(List<TaskModel> tasks) {
        int total = tasks.size();
        int pending = 0;
        int expired = 0;
        int points = 0;

        long currentTime = System.currentTimeMillis();

        for (TaskModel task : tasks) {
            if (TaskConstants.STATUS_PENDING.equals(task.getStatus())) {
                pending++;
            }

            if (task.getDueDate() != null && task.getDueDate().getTime() < currentTime
                    && !TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) {
                expired++;
            }

            // Calcular puntos basado en prioridad
            switch (task.getPriority()) {
                case TaskConstants.PRIORITY_HIGH:
                    points += 10;
                    break;
                case TaskConstants.PRIORITY_MEDIUM:
                    points += 5;
                    break;
                case TaskConstants.PRIORITY_LOW:
                    points += 2;
                    break;
            }
        }

        totalTasks.setValue(total);
        pendingTasks.setValue(pending);
        expiredTasks.setValue(expired);
        totalPoints.setValue(points);
    }

    public void updateTaskStatus(String taskId, String newStatus) {
        taskRepository.updateTaskStatus(taskId, newStatus).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                errorMessage.setValue("Error al actualizar el estado de la tarea");
            }
        });
    }

    /**
     * Carga los datos del perfil del usuario actual desde Firestore.
     */
    public void loadUserData() {
        isLoading.setValue(true);
        userRepository.getCurrentUserData().addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                userData.setValue(user);
            } else {
                errorMessage.setValue("Error al cargar el perfil.");
            }
        });
    }
}