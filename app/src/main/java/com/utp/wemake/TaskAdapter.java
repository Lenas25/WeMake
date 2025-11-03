package com.utp.wemake;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.models.TaskModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adaptador para manejar la lista de tareas en un RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskModel> taskList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private int columnIndex;
    private final OnTaskInteractionListener listener;


    public interface OnTaskInteractionListener {
        void onChangeStatusClicked(TaskModel task);
    }

    public TaskAdapter(List<TaskModel> taskList, int columnIndex, OnTaskInteractionListener listener) {
        this.taskList = taskList;
        this.columnIndex = columnIndex;
        this.listener = listener;
    }


    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, responsible, createdDate, dueDate;
        Button externalButton, changeButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            responsible = itemView.findViewById(R.id.task_responsible);
            createdDate = itemView.findViewById(R.id.task_created_date);
            dueDate = itemView.findViewById(R.id.task_due_date);
            externalButton = itemView.findViewById(R.id.button_external);
            changeButton = itemView.findViewById(R.id.button_change);
        }

        public void bind(TaskModel task) {
            Context context = itemView.getContext();

            title.setText(task.getTitle());

            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                description.setText(task.getDescription());
                description.setVisibility(View.VISIBLE);
            } else {
                description.setVisibility(View.GONE);
            }

            if (task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
                int memberCount = task.getAssignedMembers().size();
                responsible.setText(memberCount + (memberCount > 1 ? " miembros" : " miembro"));
                responsible.setVisibility(View.VISIBLE);
            } else {
                responsible.setText("Sin asignar");
                responsible.setVisibility(View.VISIBLE);
            }

            if (task.getCreatedAt() != null) {
                createdDate.setText("Creado: " + dateFormat.format(task.getCreatedAt()));
                createdDate.setVisibility(View.VISIBLE);
            } else {
                createdDate.setVisibility(View.GONE);
            }

            // --- FECHA LÍMITE (DEADLINE) ---
            if (task.getDeadline() != null) {
                dueDate.setText("Vence: " + dateFormat.format(task.getDeadline()));
                dueDate.setVisibility(View.VISIBLE);

                // Lógica para colorear la fecha si está vencida
                if (task.getDeadline().before(new Date())) {
                    dueDate.setTextColor(ContextCompat.getColor(context, R.color.priority_high_border)); // Usa un color de error
                } else {
                    dueDate.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant)); // Color por defecto
                }
            } else {
                dueDate.setVisibility(View.GONE);
            }

            externalButton.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, TaskDetailActivity.class);
                intent.putExtra("taskId", task.getId());
                ctx.startActivity(intent);
            });

            changeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChangeStatusClicked(task);
                }
            });

        }
    }
}