package com.utp.wemake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adaptador para manejar la lista de tareas en un RecyclerView
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList; // Lista de tareas que se mostrará en el RecyclerView

    // Constructor que recibe la lista de tareas
    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    // Crea nuevas vistas (se llama cuando no hay una vista existente que pueda reutilizarse)
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout de cada ítem de la lista (item_task_card.xml)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    // Asocia los datos de una tarea con los elementos de la vista
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Obtiene la tarea en la posición actual
        Task task = taskList.get(position);

        // Asigna los valores de la tarea a los TextView correspondientes
        holder.title.setText(task.getTitulo());
        holder.description.setText(task.getDescripcion());
        holder.responsible.setText("Responsable: " + task.getResponsable());
        holder.dueDate.setText("10/09/2025");
    }

    // Retorna el tamaño de la lista (cuántos elementos mostrar)
    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Clase interna que representa la vista de cada ítem de la lista
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        // Referencias a los elementos gráficos del layout de la tarjeta
        TextView title, description, responsible, dueDate;

        // Constructor que vincula los TextView con sus IDs en item_task_card.xml
        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            responsible = itemView.findViewById(R.id.task_responsible);
            dueDate = itemView.findViewById(R.id.task_due_date);
        }
    }
}
