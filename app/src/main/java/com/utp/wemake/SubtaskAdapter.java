package com.utp.wemake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.models.Subtask;
import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder> {
    private List<Subtask> subtasks;

    public SubtaskAdapter(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Subtask subtask = subtasks.get(position);
        holder.bind(subtask);
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }

    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView text;

        public SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.subtask_checkbox);
            text = itemView.findViewById(R.id.subtask_text);
        }

        public void bind(Subtask subtask) {
            text.setText(subtask.getText());
            checkBox.setChecked(subtask.isCompleted());
            
            // Cambiar estilo si está completada
            if (subtask.isCompleted()) {
                text.setAlpha(0.6f);
                text.setPaintFlags(text.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                text.setAlpha(1.0f);
                text.setPaintFlags(text.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }
}
