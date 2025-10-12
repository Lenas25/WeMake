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
    private OnSubtaskChangeListener listener;

    public interface OnSubtaskChangeListener {
        void onSubtaskChanged(Subtask subtask, boolean isCompleted);
    }

    public SubtaskAdapter(List<Subtask> subtasks, OnSubtaskChangeListener listener) {
        this.subtasks = subtasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubtaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subtask, parent, false);
        return new SubtaskViewHolder(view, listener);
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

    public void updateSubtask(int position, boolean completed) {
        if (position >= 0 && position < subtasks.size()) {
            subtasks.get(position).setCompleted(completed);
            notifyItemChanged(position);
        }
    }

    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBox;
        private TextView text;
        private OnSubtaskChangeListener listener;

        public SubtaskViewHolder(@NonNull View itemView, OnSubtaskChangeListener listener) {
            super(itemView);
            this.listener = listener;
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

            // Listener para cambios en el checkbox
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSubtaskChanged(subtask, isChecked);
                }
            });
        }
    }
}
