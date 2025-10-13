package com.utp.wemake;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.content.ClipData;
import android.content.ClipDescription;

import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Adaptador para manejar la lista de tareas en un RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskModel> taskList; // Lista de tareas que se mostrará en el RecyclerView

    private int columnIndex;

    private final OnTaskInteractionListener listener;


    public interface OnTaskInteractionListener {
        void onChangeStatusClicked(TaskModel task);
    }

    public TaskAdapter(List<TaskModel> taskList, int columnIndex, OnTaskInteractionListener listener) {
        this.taskList = taskList;
        this.columnIndex = columnIndex; // Guardamos el índice
        this.listener = listener; // Guardamos la referencia.
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

        // Asigna los valores de la tarea a los TextViews
        holder.title.setText(task.getTitle());
        holder.description.setText(task.getDescription());

        // Mostrar miembros asignados
        if (task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
            holder.responsible.setText(task.getAssignedMembers().size() + " miembro(s)");
        } else {
            holder.responsible.setText("Sin asignar");
        }

        // Mostrar fecha de vencimiento
        if (task.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.dueDate.setText(sdf.format(task.getDueDate()));

            // Cambiar color si está vencida
            if (task.getDueDate().getTime() < System.currentTimeMillis() &&
                    !TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) {
                holder.dueDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_error));
            }
        } else {
            holder.dueDate.setText("Sin fecha");
        }

        // Mostrar indicador de prioridad con colores mejorados
        int priorityColor = getPriorityColor(task.getPriority(), holder.itemView.getContext());
        int priorityBorderColor = getPriorityBorderColor(task.getPriority(), holder.itemView.getContext());

        // Aplicar color de fondo sutil
        holder.itemView.setBackgroundTintList(ColorStateList.valueOf(priorityColor));

        // Aplicar color de borde sutil
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) holder.itemView)
                .setStrokeColor(priorityBorderColor);
        }

        holder.externalButton.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, TaskDetailActivity.class);
            intent.putExtra("taskId", task.getId());
            ctx.startActivity(intent);
        });

        holder.changeButton.setOnClickListener(v -> {
            // Verificamos que nuestro oyente exista por seguridad.
            if (listener != null) {
                // Llamamos al método de la interfaz, "emitiendo la señal" y enviando la tarea actual.
                listener.onChangeStatusClicked(task);
            }
        });

        // Listener de pulsación larga para drag & drop
        holder.itemView.setOnLongClickListener(view -> {

            // 1. Preparar los datos del drag (esto se queda igual)
            String dataPayload = columnIndex + "," + holder.getAdapterPosition();
            ClipData.Item item = new ClipData.Item(dataPayload);
            String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData dragData = new ClipData("task_drag", mimeTypes, item);

            // 2. Crear un DragShadowBuilder personalizado y CORREGIDO
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view) {
                @Override
                public void onDrawShadow(android.graphics.Canvas canvas) {
                    // Obtenemos la vista que se está arrastrando
                    View shadowView = getView();

                    // Guardamos el estado original de la opacidad de la vista
                    float originalAlpha = shadowView.getAlpha();

                    // CAMBIO: Aplicamos la transparencia directamente a la VISTA
                    shadowView.setAlpha(0.7f);

                    // Opcional: Escalar la sombra para que parezca que "se levanta"
                    canvas.save();

                    // Dejamos que el sistema dibuje la vista (que ahora es semitransparente) en el canvas
                    super.onDrawShadow(canvas);

                    // Restauramos la opacidad original de la vista para no afectar a nada más
                    shadowView.setAlpha(originalAlpha);

                    // Restauramos el estado del canvas
                    canvas.restore();
                }
            };

            // 3. Iniciar el drag and drop (esto se queda igual)
            view.startDragAndDrop(dragData, shadowBuilder, view, 0);

            // 4. Desvanecer la vista original (esto se queda igual)
            view.animate()
                    .alpha(0.3f)
                    .setDuration(200)
                    .withEndAction(() -> view.setVisibility(View.INVISIBLE))
                    .start();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private int getPriorityColor(String priority, Context context) {
        switch (priority) {
            case TaskConstants.PRIORITY_HIGH:
                return ContextCompat.getColor(context, R.color.priority_high);
            case TaskConstants.PRIORITY_MEDIUM:
                return ContextCompat.getColor(context, R.color.priority_medium);
            case TaskConstants.PRIORITY_LOW:
                return ContextCompat.getColor(context, R.color.priority_low);
            default:
                return ContextCompat.getColor(context, R.color.priority_default);
        }
    }

    private int getPriorityBorderColor(String priority, Context context) {
        switch (priority) {
            case TaskConstants.PRIORITY_HIGH:
                return ContextCompat.getColor(context, R.color.priority_high_border);
            case TaskConstants.PRIORITY_MEDIUM:
                return ContextCompat.getColor(context, R.color.priority_medium_border);
            case TaskConstants.PRIORITY_LOW:
                return ContextCompat.getColor(context, R.color.priority_low_border);
            default:
                return ContextCompat.getColor(context, R.color.priority_default_border);
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, responsible, dueDate;
        Button externalButton, changeButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            responsible = itemView.findViewById(R.id.task_responsible);
            dueDate = itemView.findViewById(R.id.task_due_date);
            externalButton = itemView.findViewById(R.id.button_external);
            changeButton = itemView.findViewById(R.id.button_change);
        }
    }
}