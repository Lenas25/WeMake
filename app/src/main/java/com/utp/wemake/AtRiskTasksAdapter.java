package com.utp.wemake;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.dto.DashboardResponse;
import java.util.List;

public class AtRiskTasksAdapter extends RecyclerView.Adapter<AtRiskTasksAdapter.ViewHolder> {
    private final List<DashboardResponse.AtRiskTask> tasks;
    public AtRiskTasksAdapter(List<DashboardResponse.AtRiskTask> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_at_risk_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(tasks.get(position));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPriorityIcon;
        TextView tvTaskTitle;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPriorityIcon = itemView.findViewById(R.id.iv_priority_icon);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
        }

        void bind(DashboardResponse.AtRiskTask task) {
            Context context = itemView.getContext();
            tvTaskTitle.setText(task.title);
            if ("high".equals(task.priority)) {
                ivPriorityIcon.setColorFilter(ContextCompat.getColor(context, R.color.priority_high_border));
            } else if ("medium".equals(task.priority)) {
                ivPriorityIcon.setColorFilter(ContextCompat.getColor(context, R.color.priority_medium_border));
            } else {
                ivPriorityIcon.setColorFilter(ContextCompat.getColor(context, R.color.priority_low_border));
            }
        }
    }
}