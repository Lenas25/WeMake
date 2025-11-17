package com.utp.wemake.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;

import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class TaskDetailViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<TaskModel> task = new MutableLiveData<>();
    private final MutableLiveData<List<User>> assignedUsers = new MutableLiveData<>();
    private final MutableLiveData<User> reviewer = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> taskUpdated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> taskDeleted = new MutableLiveData<>();

    // Observer para limpiar cuando el ViewModel se destruya
    private androidx.lifecycle.Observer<TaskModel> taskObserver;

    public TaskDetailViewModel(@NonNull Application application) {
        this.taskRepository = new TaskRepository(application);
        this.userRepository = new UserRepository();
    }

    public LiveData<TaskModel> getTask() { return task; }
    public LiveData<List<User>> getAssignedUsers() { return assignedUsers; }
    public LiveData<User> getReviewer() { return reviewer; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getTaskUpdated() { return taskUpdated; }
    public LiveData<Boolean> getTaskDeleted() { return taskDeleted; }

    public void loadTask(String taskId) {
        isLoading.setValue(true);

        // Usar LiveData desde Room (rápido y reactivo)
        LiveData<TaskModel> taskLiveData = taskRepository.getTaskById(taskId);

        // Remover observer anterior si existe
        if (taskObserver != null) {
            taskLiveData.removeObserver(taskObserver);
        }

        // Crear nuevo observer
        taskObserver = loadedTask -> {
            if (loadedTask != null) {
                task.setValue(loadedTask);
                loadAssignedUsers(loadedTask.getAssignedMembers());
                loadReviewer(loadedTask.getReviewerId());
                isLoading.setValue(false);
            } else {
                isLoading.setValue(false);
                errorMessage.setValue("Tarea no encontrada");
            }
        };

        taskLiveData.observeForever(taskObserver);
    }

    private void loadAssignedUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            assignedUsers.setValue(new ArrayList<>());
            return;
        }
        userRepository.getUsersByIds(userIds).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                assignedUsers.setValue(task.getResult());
            } else {
                errorMessage.setValue("Error al cargar miembros asignados.");
            }
        });
    }
    
    private void loadReviewer(String reviewerId) {
        if (reviewerId == null || reviewerId.isEmpty()) {
            reviewer.setValue(null);
            return;
        }
        userRepository.getUserData(reviewerId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                reviewer.setValue(task.getResult().toObject(User.class));
            } else {
                errorMessage.setValue("Error al cargar el revisor.");
            }
        });
    }

    public void deleteTask(String taskId) {
        isLoading.setValue(true);
        taskRepository.deleteTask(taskId)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    taskDeleted.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Error al eliminar la tarea: " + e.getMessage());
                });
    }

    public void updateSubtask(String taskId, String subtaskId, boolean isCompleted) {
        taskRepository.updateSubtaskStatus(taskId, subtaskId, isCompleted)
                .addOnSuccessListener(aVoid -> {
                    taskUpdated.setValue(true);
                    taskUpdated.setValue(false); // Reset para permitir nuevas actualizaciones
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Error al actualizar la subtarea: " + e.getMessage());
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.detachListeners();
    }

    /**
     * Factory para crear TaskDetailViewModel con Application
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
            if (modelClass.isAssignableFrom(TaskDetailViewModel.class)) {
                return (T) new TaskDetailViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}