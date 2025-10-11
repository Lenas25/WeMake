package com.utp.wemake;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TaskDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private TextView title, description, priority, dueDate, assignedMembers;
    private RecyclerView subtasksRecycler;
    private TaskDetailViewModel viewModel;
    private String taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null) {
            finish();
            return;
        }

        initializeViews();
        setupViewModel();
        loadTaskDetails();
    }

    private void initializeViews() {
        title = findViewById(R.id.task_detail_title);
        description = findViewById(R.id.task_detail_description);
        priority = findViewById(R.id.task_detail_priority);
        dueDate = findViewById(R.id.task_detail_due_date);
        assignedMembers = findViewById(R.id.task_detail_assigned_members);
        subtasksRecycler = findViewById(R.id.subtasks_recycler);
        
        subtasksRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);
        
        viewModel.getTask().observe(this, task -> {
            if (task != null) {
                displayTask(task);
            }
        });
    }

    private void loadTaskDetails() {
        viewModel.loadTask(taskId);
    }

    private void displayTask(TaskModel task) {
        title.setText(task.getTitle());
        description.setText(task.getDescription());
        priority.setText(task.getPriority());
        
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            dueDate.setText(sdf.format(task.getDueDate()));
        }
        
        // Mostrar miembros asignados
        if (task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
            assignedMembers.setText("Asignado a " + task.getAssignedMembers().size() + " miembro(s)");
        } else {
            assignedMembers.setText("Sin asignar");
        }
        
        // Configurar RecyclerView de subtareas
        if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            SubtaskAdapter adapter = new SubtaskAdapter(task.getSubtasks());
            subtasksRecycler.setAdapter(adapter);
        }
    }
}