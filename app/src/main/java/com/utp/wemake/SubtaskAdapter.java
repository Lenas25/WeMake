package com.utp.wemake;

import android.graphics.Paint;
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

    private final List<Subtask> subtasks;
    private final OnSubtaskChangeListener listener;

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
        return new SubtaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubtaskViewHolder holder, int position) {
        Subtask subtask = subtasks.get(position);
        holder.bind(subtask, listener);
    }

    @Override
    public int getItemCount() {
        return subtasks.size();
    }
    static class SubtaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBox;
        private final TextView text;

        public SubtaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.subtask_checkbox);
            text = itemView.findViewById(R.id.subtask_text);
        }

        public void bind(final Subtask subtask, final OnSubtaskChangeListener listener) {
            text.setText(subtask.getText());

            updateTextStyle(subtask.isCompleted());

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(subtask.isCompleted());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateTextStyle(isChecked);

                if (listener != null) {
                    subtask.setCompleted(isChecked);
                    listener.onSubtaskChanged(subtask, isChecked);
                }
            });
        }

        private void updateTextStyle(boolean isCompleted) {
            if (isCompleted) {
                text.setAlpha(0.6f);
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                text.setAlpha(1.0f);
                text.setPaintFlags(text.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }
}