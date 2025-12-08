package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DailyTimelineActivity extends AppCompatActivity implements TimelineTaskAdapter.OnTaskClickListener {

    private String boardIdFilter = null;
    private String boardName = null;

    private RecyclerView recyclerDates;
    private RecyclerView recyclerTasks;
    private TextView tvTaskCount;
    private TextView tvCompletedCount;
    private TextView tvTodayLabel;
    private TextView tvSelectedDate;
    private View emptyStateContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TasksViewModel viewModel;
    private DateAdapter dateAdapter;
    private TimelineTaskAdapter taskAdapter;

    private List<TaskModel> allTasks = new ArrayList<>();
    private Date selectedDate = new Date();
    private SimpleDateFormat headerFormat = new SimpleDateFormat("d 'de' MMMM", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_timeline);

        initializeViews();
        setupListeners();
        setupDatesRecyclerView();
        setupTasksRecyclerView();
        setupViewModel();

        // Obtener extras del intent
        boardIdFilter = getIntent().getStringExtra("EXTRA_BOARD_ID");
        boardName = getIntent().getStringExtra("EXTRA_BOARD_NAME");

        if (boardName != null) {
            tvTodayLabel.setText("Timeline - " + boardName);
        }
    }

    private void initializeViews() {
        recyclerDates = findViewById(R.id.recycler_dates);
        recyclerTasks = findViewById(R.id.recycler_daily_tasks);
        tvTaskCount = findViewById(R.id.tv_task_count);
        tvCompletedCount = findViewById(R.id.tv_completed_count);
        tvTodayLabel = findViewById(R.id.tv_today_label);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (viewModel != null) {
                viewModel.loadAllUserTasks();
            }
        });
    }

    private void setupDatesRecyclerView() {
        // Generar fechas: 15 días atrás y 30 días adelante
        List<Date> dateList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -15);

        for (int i = 0; i < 45; i++) {
            dateList.add(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        dateAdapter = new DateAdapter(dateList, date -> {
            selectedDate = date;
            filterTasksByDate(selectedDate);
            updateSelectedDateLabel(selectedDate);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerDates.setLayoutManager(layoutManager);
        recyclerDates.setAdapter(dateAdapter);

        // Scroll automático al día de hoy (posición 15)
        recyclerDates.post(() -> layoutManager.scrollToPosition(15));

        // Actualizar label inicial
        updateSelectedDateLabel(selectedDate);
    }

    private void setupTasksRecyclerView() {
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TimelineTaskAdapter(this);
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
        viewModel.loadAllUserTasks();
        viewModel.getAllTasks().observe(this, tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            filterTasksByDate(selectedDate);
            updateDatesWithTasks();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void filterTasksByDate(Date date) {
        List<TaskModel> dailyTasks = new ArrayList<>();

        for (TaskModel task : allTasks) {
            boolean sameDate = task.getDeadline() != null && isSameDay(task.getDeadline(), date);
            boolean sameBoard = boardIdFilter == null || boardIdFilter.equals(task.getBoardId());
            boolean isCompleted = TaskConstants.STATUS_COMPLETED.equals(task.getStatus());

            if (sameDate && sameBoard) {
                dailyTasks.add(task);
            }
        }

        // Ordenar: primero atrasadas, luego por hora
        Collections.sort(dailyTasks, (t1, t2) -> {
            boolean t1Overdue = isTaskOverdue(t1);
            boolean t2Overdue = isTaskOverdue(t2);

            if (t1Overdue && !t2Overdue) return -1;
            if (!t1Overdue && t2Overdue) return 1;

            if (t1.getDeadline() != null && t2.getDeadline() != null) {
                return t1.getDeadline().compareTo(t2.getDeadline());
            }
            return 0;
        });

        // Actualizar contadores
        int totalTasks = dailyTasks.size();
        int completedTasks = (int) dailyTasks.stream()
                .filter(t -> TaskConstants.STATUS_COMPLETED.equals(t.getStatus()))
                .count();

        tvTaskCount.setText(totalTasks + " tarea" + (totalTasks != 1 ? "s" : ""));
        tvCompletedCount.setText(completedTasks + " completada" + (completedTasks != 1 ? "s" : ""));

        // Actualizar adapter
        taskAdapter.submitList(dailyTasks);

        // Mostrar y ocultar empty state
        boolean isEmpty = dailyTasks.isEmpty();
        emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateDatesWithTasks() {
        List<Date> datesWithTasks = allTasks.stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> boardIdFilter == null || boardIdFilter.equals(task.getBoardId()))
                .map(TaskModel::getDeadline)
                .distinct()
                .collect(Collectors.toList());

        dateAdapter.setDatesWithTasks(datesWithTasks);
    }

    private void updateSelectedDateLabel(Date date) {
        Calendar cal = Calendar.getInstance();
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(date);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        if (selectedCal.equals(today)) {
            tvSelectedDate.setText("Hoy");
        } else if (selectedCal.getTimeInMillis() == today.getTimeInMillis() + 86400000) {
            tvSelectedDate.setText("Mañana");
        } else if (selectedCal.getTimeInMillis() == today.getTimeInMillis() - 86400000) {
            tvSelectedDate.setText("Ayer");
        } else {
            tvSelectedDate.setText(headerFormat.format(date));
        }

        dateAdapter.setSelectedDate(date);
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private boolean isTaskOverdue(TaskModel task) {
        if (task.getDeadline() == null) return false;
        if (TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) return false;

        Date now = new Date();
        return task.getDeadline().before(now);
    }

    @Override
    public void onTaskClick(TaskModel task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            Toast.makeText(this, "ID de tarea no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId", task.getId());
        startActivity(intent);
    }
}