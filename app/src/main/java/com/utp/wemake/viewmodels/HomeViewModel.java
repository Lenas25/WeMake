package com.utp.wemake.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.dto.UserSummaryResponse;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.MemberRepository;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.services.ApiService;
import com.utp.wemake.services.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final FirebaseAuth auth;
    private final ApiService apiService;
    private final MemberRepository memberRepository;

    // --- Listeners para gestionar el ciclo de vida ---
    private ListenerRegistration memberListener;
    private androidx.lifecycle.Observer<List<TaskModel>> tasksObserver;

    // --- Variables LiveData ---
    private final MutableLiveData<List<KanbanColumn>> _kanbanColumns = new MutableLiveData<>();
    private final MutableLiveData<Integer> _totalTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _pendingTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _expiredTasks = new MutableLiveData<>();
    private final MutableLiveData<Integer> _totalPoints = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    // Agregar variable para rastrear el boardId actual
    private String currentBoardId = null;
    private LiveData<List<TaskModel>> currentTasksLiveData = null;

    public HomeViewModel(@NonNull Application application) {
        this.taskRepository = new TaskRepository(application);
        this.auth = FirebaseAuth.getInstance();
        this.apiService = RetrofitClient.getApiService();
        this.memberRepository = new MemberRepository();
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
     * Ahora usa Room como fuente principal y sincroniza con Firebase.
     */
    public void listenToBoardData(String boardId) {
        // Si es el mismo tablero, no hacer nada
        if (boardId.equals(currentBoardId)) {
            return;
        }

        detachAllListeners();

        _isLoading.setValue(true);
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            _errorMessage.setValue("Error: Usuario no autenticado.");
            _isLoading.setValue(false);
            return;
        }

        // Limpiar el MediatorLiveData anterior si existe
        if (currentBoardId != null && currentTasksLiveData != null) {
            taskRepository.clearMediatorForBoard(currentBoardId, currentUserId);
            if (tasksObserver != null) {
                try {
                    currentTasksLiveData.removeObserver(tasksObserver);
                } catch (Exception e) {
                    // Ignorar
                }
            }
        }

        // Actualizar el boardId actual
        currentBoardId = boardId;

        // Limpiar datos anteriores primero
        _kanbanColumns.setValue(new ArrayList<>());
        _totalTasks.setValue(0);
        _pendingTasks.setValue(0);

        loadSummaryCardsData(boardId, currentUserId);
        listenToMemberPoints(boardId, currentUserId);

        // Usar SOLO LiveData desde Room (inmediato y reactivo)
        currentTasksLiveData = taskRepository.getTasksForUserInBoard(boardId, currentUserId);

        // Crear nuevo observer
        tasksObserver = tasks -> {
            // Verificar que el boardId sigue siendo el mismo
            if (!boardId.equals(currentBoardId)) {
                return;
            }

            if (tasks != null) {
                // Filtrar estrictamente por boardId (doble verificación)
                List<TaskModel> filteredTasks = new ArrayList<>();
                for (TaskModel task : tasks) {
                    if (boardId.equals(task.getBoardId())) {
                        filteredTasks.add(task);
                    }
                }

                _isLoading.setValue(false);
                processTasks(filteredTasks);
                checkForOverdueTasks(filteredTasks);
            }
        };

        currentTasksLiveData.observeForever(tasksObserver);
    }

    /**
     * Llama al endpoint de FastAPI para obtener las métricas de las tarjetas.
     */
    private void loadSummaryCardsData(String boardId, String userId) {
        apiService.getUserSummary(boardId, userId).enqueue(new Callback<UserSummaryResponse>() {
            @Override
            public void onResponse(Call<UserSummaryResponse> call, Response<UserSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserSummaryResponse summary = response.body();
                    _totalTasks.setValue(summary.getTasksInvolved());
                    int pendingCount = summary.getTasksInvolved() - summary.getTasksCompleted();
                    _pendingTasks.setValue(pendingCount);
                    _expiredTasks.setValue(summary.getOverdueTasks());
                } else {
                    _errorMessage.setValue("Error al cargar el resumen.");
                }
            }
            @Override
            public void onFailure(Call<UserSummaryResponse> call, Throwable t) {
                _errorMessage.setValue("Error de red: " + t.getMessage());
            }
        });
    }

    /**
     * Escucha en tiempo real los puntos del miembro actual.
     */
    private void listenToMemberPoints(String boardId, String userId) {
        memberListener = memberRepository.listenToMemberDetails(boardId, userId, (snapshot, e) -> {
            if (e != null) {
                _errorMessage.setValue("Error al cargar puntos.");
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Member member = snapshot.toObject(Member.class);
                if (member != null) {
                    _totalPoints.setValue(member.getPoints());
                }
            } else {
                _totalPoints.setValue(0);
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

        _kanbanColumns.postValue(allColumns);
        _totalTasks.postValue(tasks.size());
        _pendingTasks.postValue(pending.size() + inProgress.size() + inReview.size());
    }

    /**
     * Pide al repositorio que actualice el estado de una tarea.
     */
    public void updateTaskStatus(String taskId, String newStatus) {
        if (taskId == null || taskId.isEmpty()) {
            _errorMessage.setValue("Error: ID de tarea inválido.");
            return;
        }

        taskRepository.updateStatusAndApplyPoints(taskId, newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TaskUpdateDebug", "Estado actualizado: " + newStatus);
                    // La actualización se reflejará automáticamente en LiveData
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskUpdateDebug", "Error al actualizar: " + taskId, e);
                    _errorMessage.setValue("Error al actualizar la tarea: " + e.getMessage());
                });
    }

    /**
     * Comprueba la lista de tareas y aplica penalidades si es necesario.
     */
    private void checkForOverdueTasks(List<TaskModel> tasks) {
        Date now = new Date();
        for (TaskModel task : tasks) {
            if (task.getDeadline() != null &&
                    task.getDeadline().before(now) &&
                    !TaskConstants.STATUS_COMPLETED.equals(task.getStatus()) &&
                    !task.isPenaltyApplied()) {
                Log.d("HomeViewModel", "Tarea vencida encontrada: " + task.getTitle() + ". Procesando reseteo...");

                taskRepository.processOverdueTask(task)
                        .addOnFailureListener(e -> {
                            _errorMessage.setValue("No se pudo procesar la tarea vencida: " + task.getTitle());
                        });
            }
        }
    }

    private void detachAllListeners() {
        if (memberListener != null) {
            memberListener.remove();
            memberListener = null;
        }
        // Detener listeners de Firebase
        taskRepository.detachListeners();

        // Limpiar el observer de LiveData
        if (tasksObserver != null && currentTasksLiveData != null) {
            try {
                currentTasksLiveData.removeObserver(tasksObserver);
            } catch (Exception e) {
                // Ignorar
            }
            tasksObserver = null;
        }

        // Limpiar datos visuales
        _kanbanColumns.setValue(new ArrayList<>());
        _totalTasks.setValue(0);
        _pendingTasks.setValue(0);

        // Limpiar referencia al boardId actual
        if (currentBoardId != null) {
            String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (currentUserId != null) {
                taskRepository.clearMediatorForBoard(currentBoardId, currentUserId);
            }
            currentBoardId = null;
        }
        currentTasksLiveData = null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detachAllListeners();
    }

    /**
     * Factory para crear HomeViewModel con Application
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
            if (modelClass.isAssignableFrom(HomeViewModel.class)) {
                return (T) new HomeViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}