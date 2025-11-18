package com.utp.wemake;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Adaptador para manejar la lista de tareas en un RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public static final int VIEW_MODE_LIST = 0;
    public static final int VIEW_MODE_CARDS = 1;
    private int currentViewMode = VIEW_MODE_LIST;

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

    // Método para que el Fragment nos diga qué modo usar
    public void setViewMode(int viewMode) {
        this.currentViewMode = viewMode;
        notifyDataSetChanged(); // Forzar a todos los items a re-dibujarse con la nueva lógica
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
        holder.bind(task, currentViewMode);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, responsible, createdDate, dueDate;
        Button externalButton, changeButton;

        ConstraintLayout constraintLayout;
        View actionsDivider;
        ConstraintSet constraintSetList = new ConstraintSet();
        ConstraintSet constraintSetCard = new ConstraintSet();
        int marginPx;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            responsible = itemView.findViewById(R.id.task_responsible);
            createdDate = itemView.findViewById(R.id.task_created_date);
            dueDate = itemView.findViewById(R.id.task_due_date);
            externalButton = itemView.findViewById(R.id.button_external);
            changeButton = itemView.findViewById(R.id.button_change);

            // Captura los layouts y prepara los ConstraintSet
            constraintLayout = itemView.findViewById(R.id.card_constraint_layout);
            actionsDivider = itemView.findViewById(R.id.actions_divider);

            // Calcula un margen de 8dp en píxeles
            marginPx = (int) (8 * itemView.getContext().getResources().getDisplayMetrics().density);

            // 1. Clona el estado original (de lista) desde el XML
            constraintSetList.clone(constraintLayout);

            // 2. Clona y MODIFICA para el modo Tarjeta (Card)
            constraintSetCard.clone(constraintLayout);

            // Modificar createdDate:
            constraintSetCard.clear(R.id.task_created_date, ConstraintSet.END);
            constraintSetCard.clear(R.id.task_created_date, ConstraintSet.TOP);
            constraintSetCard.clear(R.id.task_created_date, ConstraintSet.BOTTOM);
            constraintSetCard.connect(R.id.task_created_date, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            constraintSetCard.connect(R.id.task_created_date, ConstraintSet.TOP, R.id.task_due_date, ConstraintSet.BOTTOM, marginPx);

            // Modificar el divisor:
            constraintSetCard.clear(R.id.actions_divider, ConstraintSet.TOP);
            constraintSetCard.connect(R.id.actions_divider, ConstraintSet.TOP, R.id.task_created_date, ConstraintSet.BOTTOM, marginPx);
        }

        public void bind(TaskModel task, int viewMode) {
            Context context = itemView.getContext();

            // APLICA EL CONSTRAINTSET ADECUADO
            if (viewMode == VIEW_MODE_CARDS) {
                constraintSetCard.applyTo(constraintLayout);
            } else {
                constraintSetList.applyTo(constraintLayout);
            }

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
                long diffInMillis = task.getDeadline().getTime() - new Date().getTime();
                long diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis);

                int dateColor;
                if (diffInDays < 0) {
                    // Ya se venció
                    dateColor = ContextCompat.getColor(context, R.color.deadline_overdue_color);
                } else if (diffInDays <= 3) {
                    // Vence pronto (en 3 días o menos)
                    dateColor = ContextCompat.getColor(context, R.color.deadline_soon_color);
                } else {
                    // Todavía falta
                    dateColor = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant);
                }
                dueDate.setTextColor(dateColor);

            } else {
                dueDate.setText("Sin Deadline");
            }

            externalButton.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, TaskDetailActivity.class);
                intent.putExtra("taskId", task.getId());
                ctx.startActivity(intent);
            });

            changeButton.setOnClickListener(v -> {
                if (NetworkUtils.isOnline(context)) {
                    if (listener != null) {
                        listener.onChangeStatusClicked(task);
                    }
                } else {
                    Toast.makeText(context, "El cambio de estado no está disponible sin conexión.", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    public void updateTaskList(List<TaskModel> newTaskList) {
        this.taskList = newTaskList != null ? newTaskList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public TaskModel getTaskAt(int position) {
        if (position >= 0 && position < taskList.size()) {
            return taskList.get(position);
        }
        return null;
    }
}