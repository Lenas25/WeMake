package com.utp.wemake;

import android.graphics.Color;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.ClipDescription;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.Task;

import java.util.List;

public class ColumnAdapter extends RecyclerView.Adapter<ColumnAdapter.ColumnViewHolder> {

    private List<KanbanColumn> columnList;

    public ColumnAdapter(List<KanbanColumn> columnList) {
        this.columnList = columnList;
    }

    @NonNull
    @Override
    public ColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kanban, parent, false);
        return new ColumnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColumnViewHolder holder, int position) {
        KanbanColumn column = columnList.get(position);
        holder.bind(column);
    }

    @Override
    public int getItemCount() {
        return columnList.size();
    }


    class ColumnViewHolder extends RecyclerView.ViewHolder {
        TextView columnTitle;
        RecyclerView tasksRecyclerView;

        public ColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            columnTitle = itemView.findViewById(R.id.tv_column_title);
            tasksRecyclerView = itemView.findViewById(R.id.recycler_tasks);
        }

        public void bind(KanbanColumn column) {
            columnTitle.setText(column.getTitle());
            tasksRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            int columnIndex = getAdapterPosition();
            TaskAdapter taskAdapter = new TaskAdapter(column.getTasks(), columnIndex);
            tasksRecyclerView.setAdapter(taskAdapter);

            tasksRecyclerView.setOnDragListener((v, event) -> {
                View draggedView = (View) event.getLocalState();
                int highlightColor = getColorFromAttr(v.getContext(), com.google.android.material.R.attr.colorSurfaceContainerHighest);

                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

                    case DragEvent.ACTION_DRAG_ENTERED:
                        tasksRecyclerView.setBackgroundColor(highlightColor);
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:
                        tasksRecyclerView.setBackground(null);
                        return true;

                    case DragEvent.ACTION_DROP:
                        String payload = event.getClipData().getItemAt(0).getText().toString();
                        String[] parts = payload.split(",");
                        int fromColumnIndex = Integer.parseInt(parts[0]);
                        int fromTaskIndex = Integer.parseInt(parts[1]);
                        int toColumnIndex = getAdapterPosition();

                        if (fromColumnIndex != toColumnIndex) {
                            // Mover los datos y actualizar la base de datos/API
                            Task taskToMove = columnList.get(fromColumnIndex).getTasks().remove(fromTaskIndex);
                            columnList.get(toColumnIndex).getTasks().add(taskToMove);

                            // Aquí iría tu lógica de actualización de estado
                            // String newState = columnList.get(toColumnIndex).getTitle();
                            // taskToMove.setEstado(newState);
                            // updateTaskInDatabase(taskToMove);

                            notifyItemChanged(fromColumnIndex);
                            notifyItemChanged(toColumnIndex);
                        }

                        // Restauramos la vista original, sin importar si se movió o no.
                        draggedView.setAlpha(1.0f);
                        draggedView.setVisibility(View.VISIBLE);
                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        // Limpiamos el fondo
                        tasksRecyclerView.setBackground(null);

                        if (!event.getResult()) {
                            // Restauramos la vista si el drop falló
                            draggedView.setAlpha(1.0f);
                            draggedView.setVisibility(View.VISIBLE);
                        }
                        return true;

                    default:
                        return false;
                }
            });
        }

        private int getColorFromAttr(android.content.Context context, int attr) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            context.getTheme().resolveAttribute(attr, typedValue, true);
            return typedValue.data;
        }
    }
}