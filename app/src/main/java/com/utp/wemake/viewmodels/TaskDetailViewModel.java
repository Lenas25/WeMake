package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

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

    public TaskDetailViewModel() {
        this.taskRepository = new TaskRepository();
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
        taskRepository.getTaskById(taskId).addOnCompleteListener(taskResult -> {
            isLoading.setValue(false);
            if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                TaskModel loadedTask = taskResult.getResult().toObject(TaskModel.class);
                if (loadedTask != null) {
                    loadedTask.setId(taskResult.getResult().getId()); // Asignar el ID
                    task.setValue(loadedTask);

                    loadAssignedUsers(loadedTask.getAssignedMembers());
                    loadReviewer(loadedTask.getReviewerId());
                }
            } else {
                errorMessage.setValue("Error al cargar la tarea.");
            }
        });
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
        taskRepository.deleteTask(taskId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                taskDeleted.setValue(true);
            } else {
                errorMessage.setValue("Error al eliminar la tarea.");
            }
        });
    }

    public void updateSubtask(String taskId, String subtaskId, boolean isCompleted) {
        taskRepository.updateSubtaskStatus(taskId, subtaskId, isCompleted)
                .addOnSuccessListener(aVoid -> {
                    taskUpdated.setValue(true);
                    taskUpdated.setValue(false); 
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Error al actualizar la subtarea.");
                });
    }
}
