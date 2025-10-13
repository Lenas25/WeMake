package com.utp.wemake;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.viewmodels.TaskDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private TextView title, description, dueDate, assignedMembers, rewardText, penaltyText, reviewerText;
    private Chip priorityChip;
    private RecyclerView subtasksRecycler;
    private MaterialButton btnBack, btnEdit, btnDelete;
    private TaskDetailViewModel viewModel;
    private String taskId;
    private SubtaskAdapter subtaskAdapter;

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
        setupClickListeners();
        loadTaskDetails();
    }

    private void initializeViews() {
        title = findViewById(R.id.task_detail_title);
        description = findViewById(R.id.task_detail_description);
        priorityChip = findViewById(R.id.task_detail_priority);
        dueDate = findViewById(R.id.task_detail_due_date);
        assignedMembers = findViewById(R.id.task_detail_assigned_members);
        rewardText = findViewById(R.id.tvReward);
        penaltyText = findViewById(R.id.tvPenalty);
        reviewerText = findViewById(R.id.tvReviewer);
        subtasksRecycler = findViewById(R.id.subtasks_recycler);
        
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);

        subtasksRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);
        
        viewModel.getTask().observe(this, task -> {
            if (task != null) {
                displayTask(task);
            }
        });

        viewModel.getAssignedUsers().observe(this, users -> {
            displayAssignedUsers(users);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            // Mostrar/ocultar indicador de carga
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getTaskUpdated().observe(this, updated -> {
            if (updated) {
                Toast.makeText(this, "Tarea actualizada correctamente", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getTaskDeleted().observe(this, deleted -> {
            if (deleted) {
                Toast.makeText(this, "Tarea eliminada correctamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnEdit.setOnClickListener(v -> {
            // Implementar edición de tarea
            Toast.makeText(this, "Funcionalidad de edición en desarrollo", Toast.LENGTH_SHORT).show();
        });
        
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Tarea")
                .setMessage("¿Estás seguro de que quieres eliminar esta tarea?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteTask(taskId);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadTaskDetails() {
        viewModel.loadTask(taskId);
    }

    private void displayTask(TaskModel task) {
        title.setText(task.getTitle());
        description.setText(task.getDescription());
        
        // Configurar prioridad con colores
        setupPriorityChip(task.getPriority());
        
        // Mostrar fecha de vencimiento
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            dueDate.setText(sdf.format(task.getDueDate()));
        } else {
            dueDate.setText("Sin fecha");
        }
        
        // Mostrar recompensa y penalidad
        rewardText.setText(task.getRewardPoints() + " monedas");
        penaltyText.setText(task.getPenaltyPoints() + " monedas");
        
        // Mostrar revisor (placeholder por ahora)
        reviewerText.setText("Josue R.");
        
        // Configurar RecyclerView de subtareas
        setupSubtasksRecycler(task);
    }

    private void displayAssignedUsers(List<User> users) {
        if (users != null && !users.isEmpty()) {
            StringBuilder userNames = new StringBuilder();
            for (int i = 0; i < users.size(); i++) {
                if (i > 0) userNames.append(", ");
                userNames.append(users.get(i).getPublicName() != null ? 
                    users.get(i).getPublicName() : users.get(i).getName());
            }
            assignedMembers.setText(userNames.toString());
        } else {
            assignedMembers.setText("Sin asignar");
        }
    }

    private void setupPriorityChip(String priorityValue) {
        // Configurar el chip de prioridad con colores apropiados
        switch (priorityValue) {
            case "high":
                priorityChip.setText("Alta");
                priorityChip.setChipBackgroundColor(getResources().getColorStateList(R.color.priority_high));
                break;
            case "medium":
                priorityChip.setText("Media");
                priorityChip.setChipBackgroundColor(getResources().getColorStateList(R.color.priority_medium));
                break;
            case "low":
                priorityChip.setText("Baja");
                priorityChip.setChipBackgroundColor(getResources().getColorStateList(R.color.priority_low));
                break;
            default:
                priorityChip.setText("Sin prioridad");
                break;
        }
    }

    private void setupSubtasksRecycler(TaskModel task) {
        if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            subtaskAdapter = new SubtaskAdapter(task.getSubtasks(), (subtask, isCompleted) -> {
                // Callback para cuando se actualiza una subtarea
                viewModel.updateSubtask(taskId, subtask.getId(), isCompleted);
            });
            subtasksRecycler.setAdapter(subtaskAdapter);
        } else {
            // Mostrar mensaje cuando no hay subtareas
            subtasksRecycler.setAdapter(null);
        }
    }
}