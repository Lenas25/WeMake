package com.utp.wemake;

import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.ClipDescription;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.repository.TaskRepository;

import java.util.List;

public class ColumnAdapter extends RecyclerView.Adapter<ColumnAdapter.ColumnViewHolder> {
    private List<KanbanColumn> columnList;

    private final TaskAdapter.OnTaskInteractionListener taskInteractionListener;

    public ColumnAdapter(List<KanbanColumn> columnList, TaskAdapter.OnTaskInteractionListener listener) {
        this.columnList = columnList;
        this.taskInteractionListener = listener;
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


    public void updateData(List<KanbanColumn> newColumns) {
        this.columnList.clear();
        this.columnList.addAll(newColumns);
        notifyDataSetChanged();
    }


    class ColumnViewHolder extends RecyclerView.ViewHolder {
        TextView columnTitle;
        RecyclerView tasksRecyclerView;
        View emptyTaskListView;

        public ColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            columnTitle = itemView.findViewById(R.id.tv_column_title);
            tasksRecyclerView = itemView.findViewById(R.id.recycler_tasks);
            emptyTaskListView = itemView.findViewById(R.id.empty_task_list_view);
        }

        public void bind(KanbanColumn column) {
            columnTitle.setText(column.getTitle());

            if (column.getTasks() == null || column.getTasks().isEmpty()){
                tasksRecyclerView.setVisibility(View.GONE);
                emptyTaskListView.setVisibility(View.VISIBLE);
            } else {
                tasksRecyclerView.setVisibility(View.VISIBLE);
                emptyTaskListView.setVisibility(View.GONE);

                LinearLayoutManager layoutManager = new LinearLayoutManager(
                        itemView.getContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                );
                tasksRecyclerView.setLayoutManager(layoutManager);

                TaskAdapter taskAdapter = new TaskAdapter(column.getTasks(), getAdapterPosition(), taskInteractionListener);
                tasksRecyclerView.setAdapter(taskAdapter);
            }
        }
    }
}