package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyTimelineActivity extends AppCompatActivity {


    private String boardIdFilter = null;

    private RecyclerView recyclerDates;
    private RecyclerView recyclerTasks;
    private TextView tvTaskCount;
    private TextView tvTodayLabel;
    private TasksViewModel viewModel;

    private List<TaskModel> allTasks = new ArrayList<>();
    private Date selectedDate = new Date(); // Por defecto hoy
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm\na", Locale.getDefault());
    private SimpleDateFormat headerFormat = new SimpleDateFormat("d 'de' MMMM", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_timeline);

        recyclerDates = findViewById(R.id.recycler_dates);
        recyclerTasks = findViewById(R.id.recycler_daily_tasks);
        tvTaskCount = findViewById(R.id.tv_task_count);
        tvTodayLabel = findViewById(R.id.tv_today_label);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());


        boardIdFilter = getIntent().getStringExtra("EXTRA_BOARD_ID");

        setupDatesRecyclerView();
        setupTasksRecyclerView();
        setupViewModel();
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

        DateAdapter dateAdapter = new DateAdapter(dateList, date -> {
            selectedDate = date;
            filterTasksByDate(selectedDate);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerDates.setLayoutManager(layoutManager);
        recyclerDates.setAdapter(dateAdapter);

        // Hacer scroll automático al día de hoy (aprox en la posición 15)
        layoutManager.scrollToPosition(12);
    }

    private void setupTasksRecyclerView() {
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        // El adaptador se setea vacío al inicio, se llena en filterTasksByDate
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
        viewModel.loadAllUserTasks();
        viewModel.getAllTasks().observe(this, tasks -> {
            allTasks = tasks;
            filterTasksByDate(selectedDate);
        });
    }

    private void filterTasksByDate(Date date) {
        List<TaskModel> dailyTasks = new ArrayList<>();

        for (TaskModel task : allTasks) {
            boolean sameDate = task.getDeadline() != null && isSameDay(task.getDeadline(), date);
            boolean sameBoard = boardIdFilter == null || boardIdFilter.equals(task.getBoardId());

            if (sameDate && sameBoard) {
                dailyTasks.add(task);
            }
        }

        // Ordenar por hora (deadline)
        Collections.sort(dailyTasks, (t1, t2) -> t1.getDeadline().compareTo(t2.getDeadline()));

        // Actualizar contador
        tvTaskCount.setText(dailyTasks.size() + " Tareas");

        // Actualizar Adapter
        TimelineTaskAdapter adapter = new TimelineTaskAdapter(dailyTasks);
        recyclerTasks.setAdapter(adapter);
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance(); c1.setTime(d1);
        Calendar c2 = Calendar.getInstance(); c2.setTime(d2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    // --- INNER ADAPTER CLASS PARA TAREAS ---
    class TimelineTaskAdapter extends RecyclerView.Adapter<TimelineTaskAdapter.TaskViewHolder> {
        List<TaskModel> tasks;

        public TimelineTaskAdapter(List<TaskModel> tasks) {
            this.tasks = tasks;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_task_row, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            TaskModel task = tasks.get(position);
            holder.tvTitle.setText(task.getTitle());
            holder.tvDesc.setText(task.getDescription());

            if (task.getDeadline() != null) {
                holder.tvTime.setText(timeFormat.format(task.getDeadline()));
            } else {
                holder.tvTime.setText("--:--");
            }

            holder.itemView.setOnClickListener(v ->
                    Toast.makeText(DailyTimelineActivity.this, task.getTitle(), Toast.LENGTH_SHORT).show()
            );
        }

        @Override
        public int getItemCount() { return tasks.size(); }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvTitle, tvDesc;
            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_task_time);
                tvTitle = itemView.findViewById(R.id.tv_task_title);
                tvDesc = itemView.findViewById(R.id.tv_task_desc);
            }
        }
    }
}