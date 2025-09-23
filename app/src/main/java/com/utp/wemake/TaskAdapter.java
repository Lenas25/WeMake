package com.utp.wemake;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.ClipData;
import android.content.ClipDescription;

import com.utp.wemake.models.Task;

import java.util.List;

// Adaptador para manejar la lista de tareas en un RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList; // Lista de tareas que se mostrará en el RecyclerView

    // CAMBIO: Se añade una variable para saber el índice de la columna a la que pertenece este adaptador.
    private int columnIndex;

    // CAMBIO: El constructor ahora también recibe el índice de la columna.
    public TaskAdapter(List<Task> taskList, int columnIndex) {
        this.taskList = taskList;
        this.columnIndex = columnIndex; // Guardamos el índice
    }

    // --- Tus métodos existentes (getTaskAt, removeItem, etc.) pueden quedarse, no afectan al drag & drop ---
    // --- No es necesario cambiar nada en ellos ---

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Asigna los valores de la tarea a los TextViews (esto se queda igual)
        holder.title.setText(task.getTitulo());
        holder.description.setText(task.getDescripcion());
        holder.responsible.setText(task.getResponsable());
        holder.dueDate.setText("10/09/2025");

        holder.externalButton.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, TaskDetailActivity.class);
            ctx.startActivity(intent);
        });

        // Listener de pulsación larga corregido
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

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, responsible, dueDate;
        Button externalButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            responsible = itemView.findViewById(R.id.task_responsible);
            dueDate = itemView.findViewById(R.id.task_due_date);
            externalButton = itemView.findViewById(R.id.button_external);
        }
    }
}