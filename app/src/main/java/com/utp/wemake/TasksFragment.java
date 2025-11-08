package com.utp.wemake;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {

    private TasksViewModel viewModel;
    private RecyclerView recyclerViewTasks;
    private TaskAdapter taskAdapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private TextView emptyStateSubtitle;
    private View emptyStateContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Búsqueda y filtros
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private MaterialButton btnViewToggle;
    private MaterialButton btnFilters;
    private ChipGroup chipGroupFilters;
    private Chip chipBoard;
    private Chip chipPriority;
    private Chip chipAssignee;
    private Chip chipDue;

    // Tabs
    private TabLayout tabLayout;

    // Estado de vista (lista/tarjetas/calendario)
    private int currentViewMode = VIEW_MODE_LIST;
    private static final int VIEW_MODE_LIST = 0;
    private static final int VIEW_MODE_CARDS = 1;
    private static final int VIEW_MODE_CALENDAR = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
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
        emptyStateText = view.findViewById(R.id.empty_state_text);
        emptyStateSubtitle = view.findViewById(R.id.empty_state_subtitle);

        // SwipeRefresh
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        // Búsqueda y filtros
        tilSearch = view.findViewById(R.id.til_search);
        etSearch = view.findViewById(R.id.et_search);
        btnViewToggle = view.findViewById(R.id.btn_view_toggle);
        btnFilters = view.findViewById(R.id.btn_filters);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);
        chipBoard = view.findViewById(R.id.chip_board);
        chipPriority = view.findViewById(R.id.chip_priority);
        chipAssignee = view.findViewById(R.id.chip_assignee);
        chipDue = view.findViewById(R.id.chip_due);

        // Tabs
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.item_tasks);
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
                updateFilterChips();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tasks_toolbar, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // El toolbar ya maneja el menú con setOnMenuItemClickListener
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), 0, this);
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

        // Chips de filtros
        chipBoard.setOnCloseIconClickListener(v -> {
            viewModel.setSelectedBoards(new ArrayList<>());
            updateFilterChips();
        });

        chipPriority.setOnCloseIconClickListener(v -> {
            viewModel.setSelectedPriority(null);
            updateFilterChips();
        });

        chipAssignee.setOnCloseIconClickListener(v -> {
            viewModel.setSelectedAssignee(null);
            updateFilterChips();
        });

        chipDue.setOnCloseIconClickListener(v -> {
            viewModel.setSelectedDueFilter(null);
            updateFilterChips();
        });

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

    private String getStatusForTab(int position) {
        switch (position) {
            case 0: return TaskConstants.STATUS_PENDING;
            case 1: return TaskConstants.STATUS_IN_PROGRESS;
            case 2: return TaskConstants.STATUS_COMPLETED;
            case 3: return TaskConstants.STATUS_IN_REVIEW;
            default: return TaskConstants.STATUS_PENDING;
        }
    }

    private void toggleViewMode() {
        currentViewMode = (currentViewMode + 1) % 3;

        switch (currentViewMode) {
            case VIEW_MODE_LIST:
                recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
                btnViewToggle.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_view_list));
                break;
            case VIEW_MODE_CARDS:
                recyclerViewTasks.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
                btnViewToggle.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_view_module));
                break;
            case VIEW_MODE_CALENDAR:
                // TODO: Implementar vista de calendario
                Snackbar.make(getView(), "Vista de calendario próximamente", Snackbar.LENGTH_SHORT).show();
                currentViewMode = VIEW_MODE_LIST; // Volver a lista por ahora
                break;
        }
    }

    private void openFiltersBottomSheet() {
        // TODO: Implementar BottomSheet de filtros
        TasksFiltersBottomSheet bottomSheet = TasksFiltersBottomSheet.newInstance();
        bottomSheet.show(getChildFragmentManager(), "TasksFiltersBottomSheet");
    }

    private void setupObservers() {
        viewModel.getFilteredTasks().observe(getViewLifecycleOwner(), tasks -> {
            progressBar.setVisibility(View.GONE); // oculta el loader cuando llegan datos
            if (tasks != null) {
                taskAdapter.updateTaskList(tasks);
                updateEmptyState(tasks.isEmpty());
            }
            // Actualizar chips cuando cambien los filtros
            updateFilterChips();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    emptyStateContainer.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(getView(), error, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getUserBoards().observe(getViewLifecycleOwner(), boards -> {
            // Actualizar chips cuando cambien los tableros
            updateFilterChips();
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerViewTasks.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            recyclerViewTasks.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void updateFilterChips() {
        // Actualizar visibilidad de chips según filtros activos
        List<String> selectedBoards = viewModel.getSelectedBoardIds();
        boolean hasBoardFilter = selectedBoards != null && !selectedBoards.isEmpty();
        chipBoard.setVisibility(hasBoardFilter ? View.VISIBLE : View.GONE);

        String priority = viewModel.getSelectedPriority();
        boolean hasPriorityFilter = priority != null;
        chipPriority.setVisibility(hasPriorityFilter ? View.VISIBLE : View.GONE);

        String assignee = viewModel.getSelectedAssignee();
        boolean hasAssigneeFilter = assignee != null;
        chipAssignee.setVisibility(hasAssigneeFilter ? View.VISIBLE : View.GONE);

        String due = viewModel.getSelectedDueFilter();
        boolean hasDueFilter = due != null;
        chipDue.setVisibility(hasDueFilter ? View.VISIBLE : View.GONE);

        // Actualizar textos de los chips
        if (hasBoardFilter) {
            chipBoard.setText("Tableros: " + selectedBoards.size());
        }
        if (hasPriorityFilter) {
            String p = priority.equals(TaskConstants.PRIORITY_HIGH) ? "Alta" :
                    priority.equals(TaskConstants.PRIORITY_MEDIUM) ? "Media" : "Baja";
            chipPriority.setText("Prioridad: " + p);
        }
        if (hasAssigneeFilter) {
            if ("me".equals(assignee)) {
                chipAssignee.setText("Asignado: Yo");
            } else if ("unassigned".equals(assignee)) {
                chipAssignee.setText("Asignado: Ninguno");
            } else {
                chipAssignee.setText("Asignado: " + assignee);
            }
        }
        if (hasDueFilter) {
            String d = due.equals("overdue") ? "Vencidas" :
                    due.equals("today") ? "Hoy" :
                            due.equals("tomorrow") ? "Mañana" : "Esta semana";
            chipDue.setText("Vence: " + d);
        }

        // Mostrar el HorizontalScrollView solo si hay al menos un filtro activo
        View filtersScroll = getView() != null ? getView().findViewById(R.id.filters_scroll) : null;
        if (filtersScroll != null) {
            boolean hasAnyFilter = hasBoardFilter || hasPriorityFilter || hasAssigneeFilter || hasDueFilter;
            filtersScroll.setVisibility(hasAnyFilter ? View.VISIBLE : View.GONE);
        }
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TaskModel task = taskAdapter.getTaskAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe izquierda: marcar como completado
                    if (!TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) {
                        viewModel.updateTaskStatus(task.getId(), TaskConstants.STATUS_COMPLETED);
                        Snackbar.make(getView(), "Tarea completada", Snackbar.LENGTH_SHORT)
                                .setAction("Deshacer", v -> {
                                    // TODO: Restaurar estado anterior
                                })
                                .show();
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe derecha: posponer (snooze)
                    openSnoozeDialog(task);
                }

                // Restaurar la vista después del swipe
                taskAdapter.notifyItemChanged(position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewTasks);
    }

    private void openSnoozeDialog(TaskModel task) {
        // TODO: Implementar diálogo de posponer
        Snackbar.make(getView(), "Posponer tarea: " + task.getTitle(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onChangeStatusClicked(TaskModel task) {
        // Abrir BottomSheet de acciones rápidas
        TaskQuickActionsBottomSheet bottomSheet =
                TaskQuickActionsBottomSheet.newInstance(task.getId());
        bottomSheet.show(getChildFragmentManager(), "TaskQuickActionsBottomSheet");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar datos cuando el fragment vuelve a estar visible
        viewModel.loadAllUserTasks();
    }
}