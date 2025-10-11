package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.TaskRepository;

public class TaskDetailViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final MutableLiveData<TaskModel> task = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public TaskDetailViewModel() {
        this.taskRepository = new TaskRepository();
    }

    public LiveData<TaskModel> getTask() { return task; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadTask(String taskId) {
        isLoading.setValue(true);
        taskRepository.getTaskById(taskId).addOnCompleteListener(taskResult -> {
            isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                task.setValue(taskResult.getResult());
            } else {
                errorMessage.setValue("Error al cargar la tarea");
            }
        });
    }
}
