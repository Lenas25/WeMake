package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final FirebaseAuth auth;

    // --- Variables LiveData ---
    private final MutableLiveData<List<KanbanColumn>> _kanbanColumns = new MutableLiveData<>();
    private final MutableLiveData<Integer> _totalTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _pendingTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _expiredTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _totalPoints = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    public HomeViewModel() {
        this.taskRepository = new TaskRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    // --- Getters para que la Vista los observe ---
    public LiveData<List<KanbanColumn>> getKanbanColumns() { return _kanbanColumns; }
    public LiveData<Integer> getTotalTasks() { return _totalTasks; }
    public LiveData<Integer> getPendingTasks() { return _pendingTasks; }
    public LiveData<Integer> getExpiredTasks() { return _expiredTasks; }
    public LiveData<Integer> getTotalPoints() { return _totalPoints; }
    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }

    /**
     * Inicia la escucha en tiempo real de los datos del tablero.
     */
    public void listenToBoardData(String boardId) {
        _isLoading.setValue(true);
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            _errorMessage.setValue("Error: Usuario no autenticado.");
            _isLoading.setValue(false);
            return;
        }

        taskRepository.listenToTasksForUserInBoard(boardId, currentUserId, new TaskRepository.OnTasksUpdatedListener() {
            @Override
            public void onTasksUpdated(List<TaskModel> tasks) {
                _isLoading.setValue(false); // La carga inicial ya terminó
                processTasks(tasks);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _errorMessage.setValue("Error al escuchar las tareas: " + e.getMessage());
            }
        });
    }

    /**
     * Procesa las tareas y las organiza en columnas.
     */
    private void processTasks(List<TaskModel> tasks) {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        List<TaskModel> pending = new ArrayList<>();
        List<TaskModel> inProgress = new ArrayList<>();
        List<TaskModel> inReview = new ArrayList<>();
        List<TaskModel> completed = new ArrayList<>();

        for (TaskModel task : tasks) {
            switch (task.getStatus()) {
                case TaskConstants.STATUS_PENDING:
                    pending.add(task);
                    break;
                case TaskConstants.STATUS_IN_PROGRESS:
                    inProgress.add(task);
                    break;
                case TaskConstants.STATUS_IN_REVIEW:
                    inReview.add(task);
                    break;
                case TaskConstants.STATUS_COMPLETED:
                    completed.add(task);
                    break;
            }
        }
        KanbanColumn pendingColumn = new KanbanColumn("Pendiente", pending);
        KanbanColumn inProgressColumn = new KanbanColumn("En Progreso", inProgress);
        KanbanColumn inReviewColumn = new KanbanColumn("En Revisión", inReview);
        KanbanColumn completedColumn = new KanbanColumn("Completado", completed);

        List<KanbanColumn> allColumns = new ArrayList<>();
        allColumns.add(pendingColumn);
        allColumns.add(inProgressColumn);
        allColumns.add(inReviewColumn);
        allColumns.add(completedColumn);

        _kanbanColumns.setValue(allColumns);
        _totalTasks.setValue(tasks.size());
        _pendingTasks.setValue(pending.size() + inProgress.size() + inReview.size());
    }

    /**
     * Limpia los listeners cuando el ViewModel es destruido.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.detachListeners();
        Log.d("HomeViewModel", "Listeners de Firestore detenidos.");
    }

    /**
     * Pide al repositorio que actualice el estado de una tarea.
     */
    public void updateTaskStatus(String taskId, String newStatus) {
        taskRepository.updateTaskStatus(taskId, newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("HomeViewModel", "Estado de la tarea actualizado con éxito.");
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error al actualizar el estado: " + e.getMessage());
                });
    }
}