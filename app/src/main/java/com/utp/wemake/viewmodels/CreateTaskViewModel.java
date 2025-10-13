package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Map;

public class CreateTaskViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final FirebaseAuth auth;
    
    private final MutableLiveData<List<Map<String, Object>>> _boardMembers = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> boardMembers = _boardMembers;
    
    private final MutableLiveData<Boolean> _taskCreated = new MutableLiveData<>();
    public LiveData<Boolean> taskCreated = _taskCreated;
    
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;
    
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public CreateTaskViewModel() {
        this.taskRepository = new TaskRepository();
        this.memberRepository = new MemberRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    public void loadBoardMembers(String boardId) {
        _isLoading.setValue(true);
        memberRepository.getBoardMembers(boardId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful()) {
                _boardMembers.setValue(task.getResult());
            } else {
                _errorMessage.setValue("Error al cargar miembros del tablero");
            }
        });
    }

    public void createTask(String boardId, String title, String description,
                           String priority, List<String> assignedMemberIds,
                           java.util.Date dueDate, List<com.utp.wemake.models.Subtask> subtasks) {
        _isLoading.setValue(true);

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            _errorMessage.setValue("Usuario no autenticado");
            return;
        }

        TaskModel task = new TaskModel(title, description, priority, assignedMemberIds, boardId, currentUserId);
        task.setDueDate(dueDate);
        task.setSubtasks(subtasks);

        taskRepository.createTask(task).addOnCompleteListener(taskResult -> {
            _isLoading.setValue(false);
            if (taskResult.isSuccessful()) {
                _taskCreated.setValue(true);
            } else {
                _errorMessage.setValue("Error al crear la tarea: " + taskResult.getException().getMessage());
            }
        });
    }
}
