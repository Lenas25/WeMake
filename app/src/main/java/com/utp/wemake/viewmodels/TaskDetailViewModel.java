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
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getTaskUpdated() { return taskUpdated; }
    public LiveData<Boolean> getTaskDeleted() { return taskDeleted; }

    public void loadTask(String taskId) {
        isLoading.setValue(true);
        
        // Cargar tarea con subtareas
        taskRepository.getTaskWithSubtasks(taskId).addOnCompleteListener(taskResult -> {
            if (taskResult.isSuccessful() && taskResult.getResult() != null) {
                TaskModel loadedTask = taskResult.getResult();
                task.setValue(loadedTask);
                
                // Cargar usuarios asignados
                loadAssignedUsers(loadedTask.getAssignedMembers());
            } else {
                errorMessage.setValue("Error al cargar la tarea");
            }
            isLoading.setValue(false);
        });
    }

    private void loadAssignedUsers(List<String> assignedMemberIds) {
        if (assignedMemberIds == null || assignedMemberIds.isEmpty()) {
            assignedUsers.setValue(new ArrayList<>());
            return;
        }

        userRepository.getUsersByIds(assignedMemberIds).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                assignedUsers.setValue(task.getResult());
            } else {
                assignedUsers.setValue(new ArrayList<>());
            }
        });
    }

    public void updateTask(TaskModel task) {
        isLoading.setValue(true);
        taskRepository.updateTaskWithSubtasks(task).addOnCompleteListener(taskResult -> {
            isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                taskUpdated.setValue(true);
                // Recargar la tarea para obtener los datos actualizados
                loadTask(task.getId());
            } else {
                errorMessage.setValue("Error al actualizar la tarea");
            }
        });
    }

    public void deleteTask(String taskId) {
        isLoading.setValue(true);
        taskRepository.deleteTask(taskId).addOnCompleteListener(taskResult -> {
            isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                taskDeleted.setValue(true);
            } else {
                errorMessage.setValue("Error al eliminar la tarea");
            }
        });
    }

    public void updateSubtask(String taskId, String subtaskId, boolean completed) {
        taskRepository.updateSubtask(taskId, subtaskId, completed).addOnCompleteListener(taskResult -> {
            if (taskResult.isSuccessful()) {
                // Recargar la tarea para obtener los datos actualizados
                loadTask(taskId);
            } else {
                errorMessage.setValue("Error al actualizar la subtarea");
            }
        });
    }
}
