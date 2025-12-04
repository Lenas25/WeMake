package com.utp.wemake;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.utils.NetworkUtils;
import com.utp.wemake.viewmodels.TaskDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {
    private TextView title, description, dueDate, assignedMembers, rewardText, penaltyText, reviewerText;
    private Chip priorityChip, statusChip;
    private RecyclerView subtasksRecycler;
    private MaterialButton btnBack, btnEdit, btnDelete;
    private TaskDetailViewModel viewModel;
    private String taskId;
    private SubtaskAdapter subtaskAdapter;
    private final SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd 'de' MMMM, yyyy", new Locale("es", "ES"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_detail);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // El host debe coincidir con tu URL de Firebase Hosting
            if ("wemake-c1a90.web.app".equals(data.getHost())) {
                taskId = data.getQueryParameter("id");
            }
        } else {
            // Si no, fue abierto por un Intent normal
            taskId = intent.getStringExtra("taskId");
        }

        if (taskId == null) {
            Toast.makeText(this, "ID de tarea no encontrado.", Toast.LENGTH_SHORT).show();
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
        statusChip = findViewById(R.id.task_detail_status);
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

        viewModel.getTask().observe(this, this::displayTask);
        viewModel.getAssignedUsers().observe(this, this::displayAssignedUsers);

        viewModel.getReviewer().observe(this, this::displayReviewer);

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

        viewModel.getTaskUpdated().observe(this, event -> {
            Boolean updated = event.getContentIfNotHandled();
            if (updated != null && updated) {
                Toast.makeText(this, "Tarea actualizada correctamente", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getTaskDeleted().observe(this, event -> {
            Boolean deleted = event.getContentIfNotHandled();
            if (deleted != null && deleted) {
                Toast.makeText(this, "Tarea eliminada correctamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getToastMessage().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (NetworkUtils.isOnline(this)) {
                Intent intent = new Intent(this, CreateTaskActivity.class);
                intent.putExtra(CreateTaskActivity.EXTRA_TASK_ID, taskId);
                startActivity(intent);
            } else {
                viewModel.postToastMessage("La edición no está disponible sin conexión.");
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (NetworkUtils.isOnline(this)) {
                showDeleteConfirmation();
            } else {
                viewModel.postToastMessage("La eliminación no está disponible sin conexión.");
            }
        });
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
        if (task == null) return;

        title.setText(task.getTitle());
        description.setText(task.getDescription());

        setupPriorityChip(task.getPriority());
        setupStatusChip(task.getStatus());

        if (task.getDeadline() != null) {
            dueDate.setText(fullDateFormat.format(task.getDeadline()));
        } else {
            dueDate.setText("Sin fecha límite");
        }

        rewardText.setText(task.getRewardPoints() + " monedas");
        penaltyText.setText(task.getPenaltyPoints() + " monedas");

        setupSubtasksRecycler(task);
    }

    private void displayReviewer(User user) {
        if (user != null && user.getName() != null) {
            reviewerText.setText(user.getName());
        } else {
            reviewerText.setText("Sin asignar");
        }
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
        if (priorityValue == null) {
            priorityChip.setVisibility(View.GONE);
            return;
        }
        priorityChip.setVisibility(View.VISIBLE);
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

    private void setupStatusChip(String statusValue) {
        if (statusValue == null) {
            statusChip.setVisibility(View.GONE);
            return;
        }
        statusChip.setVisibility(View.VISIBLE);

        int backgroundColor;
        String statusText;

        switch (statusValue) {
            case TaskConstants.STATUS_PENDING:
                statusText = "Pendiente";
                backgroundColor = getColor(R.color.status_pending_bg);
                break;
            case TaskConstants.STATUS_IN_PROGRESS:
                statusText = "En Progreso";
                backgroundColor = getColor(R.color.status_inprogress_bg);
                break;
            case TaskConstants.STATUS_IN_REVIEW:
                statusText = "En Revisión";
                backgroundColor = getColor(R.color.status_inreview_bg);
                break;
            case TaskConstants.STATUS_COMPLETED:
                statusText = "Completado";
                backgroundColor = getColor(R.color.status_completed_bg);
                break;
            default:
                statusText = "Desconocido";
                backgroundColor = getColor(R.color.status_default_bg);
                break;
        }
        statusChip.setText(statusText);
        statusChip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
    }

    private void setupSubtasksRecycler(TaskModel task) {
        if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            subtaskAdapter = new SubtaskAdapter(task.getSubtasks(), (subtask, isCompleted) -> {
                if (NetworkUtils.isOnline(this)) {
                    viewModel.updateSubtask(taskId, subtask.getId(), isCompleted);
                } else {
                    viewModel.postToastMessage("No se puede actualizar sin conexión.");
                    subtaskAdapter.notifyDataSetChanged();
                }
            });
            subtasksRecycler.setAdapter(subtaskAdapter);
        } else {
            subtasksRecycler.setAdapter(null);
        }
    }
}