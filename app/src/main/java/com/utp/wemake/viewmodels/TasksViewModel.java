package com.utp.wemake.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.BoardRepository;
import com.utp.wemake.repository.TaskRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TasksViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final FirebaseAuth auth;

    // LiveData para todas las tareas
    private final MutableLiveData<List<TaskModel>> _allTasks = new MutableLiveData<>(new ArrayList<>());
    // LiveData para tareas filtradas
    private final MutableLiveData<List<TaskModel>> _filteredTasks = new MutableLiveData<>(new ArrayList<>());
    // LiveData para tableros del usuario
    private final MutableLiveData<List<Board>> _userBoards = new MutableLiveData<>(new ArrayList<>());
    // LiveData para estado de carga
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    // LiveData para mensajes de error
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    // LiveData para estado del tab seleccionado
    private final MutableLiveData<String> _selectedStatus = new MutableLiveData<>(TaskConstants.STATUS_PENDING);

    // Filtros actuales
    private String searchQuery = "";
    private List<String> selectedBoardIds = new ArrayList<>();
    private String selectedPriority = null;
    private String selectedAssignee = null;
    private String selectedDueFilter = null;

    // Observer para limpiar cuando el ViewModel se destruya
    private androidx.lifecycle.Observer<List<TaskModel>> tasksObserver;

    public TasksViewModel(@NonNull Application application) {
        this.taskRepository = new TaskRepository(application);
        this.boardRepository = new BoardRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    // Getters
    public LiveData<List<TaskModel>> getAllTasks() { return _allTasks; }
    public LiveData<List<TaskModel>> getFilteredTasks() { return _filteredTasks; }
    public LiveData<List<Board>> getUserBoards() { return _userBoards; }
    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<String> getSelectedStatus() { return _selectedStatus; }

    /**
     * Inicia la carga de todas las tareas de todos los tableros del usuario.
     */
    public void loadAllUserTasks() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            _errorMessage.setValue("Usuario no autenticado");
            return;
        }

        _isLoading.setValue(true);

        // Primero cargar los tableros del usuario
        boardRepository.getBoardsForCurrentUser().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Board> boards = task.getResult().toObjects(Board.class);
                List<String> boardIds = boards.stream()
                        .map(Board::getId)
                        .collect(Collectors.toList());

                _userBoards.setValue(boards);

                // Observar tareas desde SQLite (LiveData) - se actualiza automáticamente
                LiveData<List<TaskModel>> tasksLiveData = taskRepository.getTasksForUserInBoards(boardIds, currentUserId);

                // Remover observer anterior si existe
                if (tasksObserver != null) {
                    tasksLiveData.removeObserver(tasksObserver);
                }

                // Crear nuevo observer
                tasksObserver = tasks -> {
                    if (tasks != null) {
                        _allTasks.setValue(tasks);
                        applyFilters();
                        _isLoading.setValue(false);
                    }
                };

                tasksLiveData.observeForever(tasksObserver);

                // Sincronizar con Firebase en segundo plano (si hay conexión)
                for (String boardId : boardIds) {
                    taskRepository.syncTasksFromFirebase(boardId, currentUserId);
                }

                // Sincronizar cambios pendientes (tareas creadas offline)
                taskRepository.syncPendingChangesToFirebase();
            } else {
                _isLoading.setValue(false);
                _errorMessage.setValue("Error al cargar los tableros");
            }
        });
    }

    /**
     * Actualiza el estado seleccionado del tab.
     */
    public void setSelectedStatus(String status) {
        _selectedStatus.setValue(status);
        applyFilters();
    }

    /**
     * Actualiza la búsqueda y aplica filtros.
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
        applyFilters();
    }

    /**
     * Establece los tableros seleccionados para filtrar.
     */
    public void setSelectedBoards(List<String> boardIds) {
        this.selectedBoardIds = boardIds != null ? new ArrayList<>(boardIds) : new ArrayList<>();
        applyFilters();
    }

    /**
     * Establece la prioridad seleccionada para filtrar.
     */
    public void setSelectedPriority(String priority) {
        this.selectedPriority = priority;
        applyFilters();
    }

    /**
     * Establece el asignado seleccionado para filtrar.
     */
    public void setSelectedAssignee(String assigneeId) {
        this.selectedAssignee = assigneeId;
        applyFilters();
    }

    /**
     * Establece el filtro de fecha de vencimiento.
     */
    public void setSelectedDueFilter(String dueFilter) {
        this.selectedDueFilter = dueFilter;
        applyFilters();
    }

    /**
     * Limpia todos los filtros.
     */
    public void clearFilters() {
        searchQuery = "";
        selectedBoardIds.clear();
        selectedPriority = null;
        selectedAssignee = null;
        selectedDueFilter = null;
        applyFilters();
    }

    /**
     * Aplica todos los filtros a las tareas.
     */
    private void applyFilters() {
        List<TaskModel> tasks = _allTasks.getValue();
        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        List<TaskModel> filtered = tasks.stream()
                .filter(task -> {
                    // Filtro por estado (tab)
                    String selectedStatus = _selectedStatus.getValue();
                    if (selectedStatus != null && !selectedStatus.equals(task.getStatus())) {
                        return false;
                    }

                    // Filtro de búsqueda
                    if (!searchQuery.isEmpty()) {
                        boolean matchesTitle = task.getTitle() != null &&
                                task.getTitle().toLowerCase().contains(searchQuery);
                        boolean matchesDescription = task.getDescription() != null &&
                                task.getDescription().toLowerCase().contains(searchQuery);
                        if (!matchesTitle && !matchesDescription) {
                            return false;
                        }
                    }

                    // Filtro por tablero
                    if (!selectedBoardIds.isEmpty() &&
                            (task.getBoardId() == null || !selectedBoardIds.contains(task.getBoardId()))) {
                        return false;
                    }

                    // Filtro por prioridad
                    if (selectedPriority != null && !selectedPriority.equals(task.getPriority())) {
                        return false;
                    }

                    // Filtro por asignado
                    if (selectedAssignee != null) {
                        if (selectedAssignee.equals("me")) {
                            String currentUserId = auth.getCurrentUser() != null ?
                                    auth.getCurrentUser().getUid() : null;
                            if (currentUserId == null ||
                                    task.getAssignedMembers() == null ||
                                    !task.getAssignedMembers().contains(currentUserId)) {
                                return false;
                            }
                        } else if (selectedAssignee.equals("unassigned")) {
                            if (task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
                                return false;
                            }
                        } else if (task.getAssignedMembers() == null ||
                                !task.getAssignedMembers().contains(selectedAssignee)) {
                            return false;
                        }
                    }

                    // Filtro por fecha de vencimiento
                    if (selectedDueFilter != null) {
                        Date now = new Date();
                        Date taskDeadline = task.getDeadline();
                        if (taskDeadline == null) return false;

                        switch (selectedDueFilter) {
                            case "overdue":
                                if (!taskDeadline.before(now)) return false;
                                break;
                            case "today":
                                if (!isSameDay(taskDeadline, now)) return false;
                                break;
                            case "tomorrow":
                                Date tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
                                if (!isSameDay(taskDeadline, tomorrow)) return false;
                                break;
                            case "this_week":
                                Date weekFromNow = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
                                if (taskDeadline.after(weekFromNow)) return false;
                                break;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        _filteredTasks.setValue(filtered);
    }

    /**
     * Verifica si dos fechas son del mismo día.
     */
    private boolean isSameDay(Date date1, Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /**
     * Actualiza el estado de una tarea.
     */
    public void updateTaskStatus(String taskId, String newStatus) {
        List<TaskModel> allTasks = _allTasks.getValue();
        if (allTasks == null) return;

        TaskModel taskToUpdate = allTasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);

        if (taskToUpdate == null) {
            _errorMessage.setValue("Tarea no encontrada");
            return;
        }

        taskRepository.updateStatusAndApplyPoints(taskToUpdate.getId(), newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TasksViewModel", "Estado actualizado: " + newStatus);
                    // La actualización se reflejará automáticamente en LiveData
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error al actualizar: " + e.getMessage());
                });
    }

    /**
     * Actualiza la prioridad de una tarea.
     */
    public void updateTaskPriority(String taskId, String newPriority) {
        List<TaskModel> allTasks = _allTasks.getValue();
        if (allTasks == null) return;

        TaskModel taskToUpdate = allTasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);

        if (taskToUpdate == null) {
            _errorMessage.setValue("Tarea no encontrada para actualizar prioridad");
            return;
        }

        taskRepository.updatePriority(taskToUpdate.getId(), newPriority)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TasksViewModel", "Prioridad actualizada a: " + newPriority);
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error al actualizar la prioridad: " + e.getMessage());
                });
    }

    /**
     * Getters para los filtros actuales
     */
    public List<String> getSelectedBoardIds() {
        return new ArrayList<>(selectedBoardIds);
    }

    public String getSelectedPriority() {
        return selectedPriority;
    }

    public String getSelectedAssignee() {
        return selectedAssignee;
    }

    public String getSelectedDueFilter() {
        return selectedDueFilter;
    }

    /**
     * Obtiene el ID del usuario actualmente autenticado desde FirebaseAuth.
     */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * Limpia los listeners cuando el ViewModel es destruido.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        taskRepository.detachListeners();
        Log.d("TasksViewModel", "ViewModel limpiado");
    }

    /**
     * Factory para crear TasksViewModel con Application
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
            if (modelClass.isAssignableFrom(TasksViewModel.class)) {
                return (T) new TasksViewModel(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}