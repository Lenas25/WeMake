package com.utp.wemake;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.util.ArrayList;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {

    private TasksViewModel viewModel;
    private RecyclerView recyclerViewTasks;
    private TaskAdapter taskAdapter;
    private ProgressBar progressBar;
    private View emptyStateContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MotionLayout motionFilters;

    // Búsqueda y filtros
    private TextInputEditText etSearch;
    private MaterialButton btnViewToggle;
    private MaterialButton btnFilters;

    // Chips de filtros
    private Chip chipBoard;
    private Chip chipPriority;
    private Chip chipAssignee;
    private Chip chipDue;

    // Tabs
    private TabLayout tabLayout;

    // Estado de vista (lista/tarjetas/calendario)
    private int currentViewMode = TaskAdapter.VIEW_MODE_LIST;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this,
                new TasksViewModel.Factory(requireActivity().getApplication()))
                .get(TasksViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupToolbar(view);
        setupRecyclerView();
        setupListeners();
        setupObservers();
        setupSwipeActions();

        // Cargar datos iniciales
        viewModel.loadAllUserTasks();
    }

    private void initializeViews(View view) {
        // RecyclerView y estados
        recyclerViewTasks = view.findViewById(R.id.recycler_view_tasks);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);

        // SwipeRefresh
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        motionFilters = view.findViewById(R.id.motion_filters);

        // Búsqueda y filtros
        etSearch = view.findViewById(R.id.et_search);
        btnViewToggle = view.findViewById(R.id.btn_view_toggle);
        btnFilters = view.findViewById(R.id.btn_filters);

        chipBoard = view.findViewById(R.id.chip_board);
        chipPriority = view.findViewById(R.id.chip_priority);
        chipAssignee = view.findViewById(R.id.chip_assignee);
        chipDue = view.findViewById(R.id.chip_due);

        // Tabs
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.top_app_bar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        // Configurar el toolbar para que maneje el menú
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_filters) {
                viewModel.clearFilters();
                etSearch.setText(""); // Limpiar también el campo de búsqueda
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), 0, this);
        taskAdapter.setViewMode(currentViewMode);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        // Búsqueda
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Toggle de vista
        btnViewToggle.setOnClickListener(v -> toggleViewMode());

        // Botón de filtros
        btnFilters.setOnClickListener(v -> openFiltersBottomSheet());

        // Chips - Close icons
        setupChipCloseListener(chipBoard, () -> viewModel.setSelectedBoards(new ArrayList<>()));
        setupChipCloseListener(chipPriority, () -> viewModel.setSelectedPriority(null));
        setupChipCloseListener(chipAssignee, () -> viewModel.setSelectedAssignee(null));
        setupChipCloseListener(chipDue, () -> viewModel.setSelectedDueFilter(null));

        // Tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String status = getStatusForTab(tab.getPosition());
                viewModel.setSelectedStatus(status);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // SwipeRefresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadAllUserTasks();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void setupChipCloseListener(Chip chip, Runnable action) {
        if (chip != null) {
            chip.setOnCloseIconClickListener(v -> action.run());
        }
    }

    private void setupObservers() {
        // Observar tareas filtradas
        viewModel.getFilteredTasks().observe(getViewLifecycleOwner(), tasks -> {
            progressBar.setVisibility(View.GONE);
            if (tasks != null) {
                taskAdapter.updateTaskList(tasks);
                updateEmptyState(tasks.isEmpty());
            }
            // Actualizar chips cuando cambien las tareas
            updateFilterChips();
        });

        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    emptyStateContainer.setVisibility(View.GONE);
                }
            }
        });

        // Observar errores
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observar tableros
        viewModel.getUserBoards().observe(getViewLifecycleOwner(), boards -> {
            // Actualizar chips cuando cambien los tableros
            updateFilterChips();
        });
    }

    private void updateFilterChips() {
        if (viewModel == null) return;

        // Tablero
        updateChip(chipBoard, viewModel.getSelectedBoardIds(),
                list -> !list.isEmpty(),
                list -> "Tableros: " + list.size());

        // Prioridad
        updateChip(chipPriority, viewModel.getSelectedPriority(),
                p -> p != null,
                p -> "Prioridad: " + getPriorityText(p));

        // Asignado
        updateChip(chipAssignee, viewModel.getSelectedAssignee(),
                a -> a != null,
                a -> "Asignado: " + getAssigneeText(a));

        // Vencimiento
        updateChip(chipDue, viewModel.getSelectedDueFilter(),
                d -> d != null,
                d -> "Vence: " + getDueText(d));

        // Controlar visibilidad del MotionLayout
        boolean hasAnyFilter = isChipVisible(chipBoard) || isChipVisible(chipPriority) ||
                isChipVisible(chipAssignee) || isChipVisible(chipDue);

        // Controlar el MotionLayout
        if (motionFilters != null) {
            if (hasAnyFilter) {
                // Animar a estado "end" (visible)
                motionFilters.transitionToEnd();
            } else {
                // Animar a estado "start" (gone)
                motionFilters.transitionToStart();
            }
        }
    }

    private <T> void updateChip(Chip chip, T value, FilterPredicate<T> predicate, FilterTextProvider<T> textProvider) {
        if (chip == null) return;
        boolean hasFilter = predicate.test(value);
        chip.setVisibility(hasFilter ? View.VISIBLE : View.GONE);
        if (hasFilter) {
            chip.setText(textProvider.getText(value));
        }
    }

    private boolean isChipVisible(Chip chip) {
        return chip != null && chip.getVisibility() == View.VISIBLE;
    }

    private String getPriorityText(String priority) {
        switch (priority) {
            case TaskConstants.PRIORITY_HIGH: return "Alta";
            case TaskConstants.PRIORITY_MEDIUM: return "Media";
            case TaskConstants.PRIORITY_LOW: return "Baja";
            default: return priority;
        }
    }

    private String getAssigneeText(String assignee) {
        switch (assignee) {
            case "me": return "Yo";
            case "unassigned": return "Ninguno";
            default: return assignee;
        }
    }

    private String getDueText(String due) {
        switch (due) {
            case "overdue": return "Vencidas";
            case "today": return "Hoy";
            case "tomorrow": return "Mañana";
            case "this_week": return "Esta semana";
            default: return due;
        }
    }

    private String getStatusForTab(int position) {
        switch (position) {
            case 0: return TaskConstants.STATUS_PENDING;
            case 1: return TaskConstants.STATUS_IN_PROGRESS;
            case 2: return TaskConstants.STATUS_IN_REVIEW;
            case 3: return TaskConstants.STATUS_COMPLETED;
            default: return TaskConstants.STATUS_PENDING;
        }
    }

    private void toggleViewMode() {

        currentViewMode = (currentViewMode + 1) % 2;
        switch (currentViewMode) {
            case TaskAdapter.VIEW_MODE_LIST:
                recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
                btnViewToggle.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_list));
                break;
            case TaskAdapter.VIEW_MODE_CARDS:
                recyclerViewTasks.setLayoutManager(new GridLayoutManager(getContext(), 2));
                btnViewToggle.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_view_module));
                break;
        }

        // Informa al adapter sobre el cambio
        taskAdapter.setViewMode(currentViewMode);
    }

    private void openFiltersBottomSheet() {
        // TODO: Implementar BottomSheet de filtros
        TasksFiltersBottomSheet bottomSheet = TasksFiltersBottomSheet.newInstance();
        bottomSheet.show(getChildFragmentManager(), "TasksFiltersBottomSheet");
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                final TaskModel task = taskAdapter.getTaskAt(position);

                if (task == null) return;

                if (direction == ItemTouchHelper.LEFT) {
                    handleSwipeLeft(task);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    handleSwipeRight(task);
                }

                taskAdapter.notifyItemChanged(position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewTasks);
    }

    private void handleSwipeLeft(final TaskModel task) {
        String currentUserId = viewModel.getCurrentUserId();
        final String originalStatus = task.getStatus();

        String newStatus = null;
        String successMessage = null;
        String errorMessage = null;

        switch (originalStatus) {
            case TaskConstants.STATUS_PENDING:
                newStatus = TaskConstants.STATUS_IN_PROGRESS;
                successMessage = "Tarea movida a 'En Progreso'";
                break;
            case TaskConstants.STATUS_IN_PROGRESS:
                newStatus = TaskConstants.STATUS_IN_REVIEW;
                successMessage = "Tarea enviada a 'En Revisión'";
                break;
            case TaskConstants.STATUS_IN_REVIEW:
                if (currentUserId != null && currentUserId.equals(task.getReviewerId())) {
                    newStatus = TaskConstants.STATUS_COMPLETED;
                    successMessage = "¡Tarea completada!";
                } else {
                    errorMessage = "Solo el revisor puede completar la tarea.";
                }
                break;
            case TaskConstants.STATUS_COMPLETED:
                errorMessage = "La tarea ya está completada.";
                break;
        }

        if (newStatus != null) {
            viewModel.updateTaskStatus(task.getId(), newStatus);
            View anchorView = requireActivity().findViewById(R.id.fab);
            if (anchorView == null) {
                anchorView = requireView();
            }

            View finalAnchorView = anchorView;
            Snackbar.make(requireView(), successMessage, Snackbar.LENGTH_LONG)
                    .setAnchorView(anchorView)
                    .setAction("Deshacer", v -> {
                        viewModel.updateTaskStatus(task.getId(), originalStatus);
                        Snackbar.make(requireView(), "Cambio de estado deshecho.", Snackbar.LENGTH_SHORT)
                                .setAnchorView(finalAnchorView)
                                .show();
                    })
                    .show();
        } else if (errorMessage != null) {
            View anchorView = requireActivity().findViewById(R.id.fab);
            if (anchorView == null) {
                anchorView = requireView();
            }
            Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG)
                    .setAnchorView(anchorView)
                    .show();        }
    }

    private void handleSwipeRight(final TaskModel task) {
        final String originalPriority = task.getPriority(); // Guardar para deshacer
        String newPriority;
        String message;

        if (TaskConstants.PRIORITY_HIGH.equals(originalPriority)) {
            newPriority = TaskConstants.PRIORITY_MEDIUM;
            message = "Prioridad cambiada a Media";
        } else {
            newPriority = TaskConstants.PRIORITY_HIGH;
            message = "Prioridad cambiada a Alta";
        }

        viewModel.updateTaskPriority(task.getId(), newPriority);

        View anchorView = requireActivity().findViewById(R.id.fab);
        if (anchorView == null) {
            anchorView = requireView();
        }

        View finalAnchorView = anchorView;
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAnchorView(anchorView)
                .setAction("Deshacer", v -> {
                    viewModel.updateTaskPriority(task.getId(), originalPriority);
                    Snackbar.make(requireView(), "Cambio de prioridad deshecho.", Snackbar.LENGTH_SHORT)
                            .setAnchorView(finalAnchorView)
                            .show();
                })
                .show();
    }

    private void updateEmptyState(boolean isEmpty) {
        recyclerViewTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onChangeStatusClicked(TaskModel task) {
        // Abrir BottomSheet de acciones rápidas
        TaskQuickActionsBottomSheet bottomSheet = TaskQuickActionsBottomSheet.newInstance(task.getId());
        bottomSheet.show(getChildFragmentManager(), "TaskQuickActionsBottomSheet");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Solo recarga si no hay datos o si fueron limpiados
        if (viewModel.getAllTasks().getValue() == null || viewModel.getAllTasks().getValue().isEmpty()) {
            viewModel.loadAllUserTasks();
        }
    }

    // Interfaces funcionales para reducir duplicación
    @FunctionalInterface
    private interface FilterPredicate<T> {
        boolean test(T value);
    }

    @FunctionalInterface
    private interface FilterTextProvider<T> {
        String getText(T value);
    }
}