package com.utp.wemake;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineTaskAdapter extends ListAdapter<TaskModel, TimelineTaskAdapter.TaskViewHolder> {

    private static final DiffUtil.ItemCallback<TaskModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskModel oldItem, @NonNull TaskModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskModel oldItem, @NonNull TaskModel newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getStatus().equals(newItem.getStatus()) &&
                    oldItem.getPriority().equals(newItem.getPriority());
        }
    };

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm\na", Locale.getDefault());
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    public TimelineTaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_task_row, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = getItem(position);
        holder.bind(task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTime, tvTitle, tvDesc, badgeOverdue, badgeStatus, tvRewardPoints;
        private View viewDot, viewTimelineLine;
        private ImageView ivPriority;
        private LinearLayout layoutMembers;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_task_time);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDesc = itemView.findViewById(R.id.tv_task_desc);
            viewDot = itemView.findViewById(R.id.view_dot);
            viewTimelineLine = itemView.findViewById(R.id.view_timeline_line);
            badgeOverdue = itemView.findViewById(R.id.badge_overdue);
            badgeStatus = itemView.findViewById(R.id.badge_status);
            ivPriority = itemView.findViewById(R.id.iv_priority);
            layoutMembers = itemView.findViewById(R.id.layout_members);
            tvRewardPoints = itemView.findViewById(R.id.tv_reward_points);
        }

        void bind(TaskModel task) {
            // Título y descripción
            tvTitle.setText(task.getTitle());
            if (TextUtils.isEmpty(task.getDescription())) {
                tvDesc.setVisibility(View.GONE);
            } else {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(task.getDescription());
            }

            // Hora
            if (task.getDeadline() != null) {
                tvTime.setText(timeFormat.format(task.getDeadline()));
            } else {
                tvTime.setText("--:--");
            }

            // Prioridad
            bindPriority(task.getPriority());

            // Estado
            bindStatus(task.getStatus());

            // Tarea atrasada
            boolean isOverdue = isTaskOverdue(task);
            badgeOverdue.setVisibility(isOverdue ? View.VISIBLE : View.GONE);

            // Puntos de recompensa
            if (task.getRewardPoints() > 0) {
                tvRewardPoints.setVisibility(View.VISIBLE);
                tvRewardPoints.setText("+" + task.getRewardPoints() + " pts");
            } else {
                tvRewardPoints.setVisibility(View.GONE);
            }

            // Miembros asignados (avatares)
            bindMembers(task.getAssignedMembers());

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            // Estilo para tareas completadas
            if (TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) {
                tvTitle.setAlpha(0.6f);
                tvDesc.setAlpha(0.6f);
                itemView.setAlpha(0.7f);
            } else {
                tvTitle.setAlpha(1.0f);
                tvDesc.setAlpha(1.0f);
                itemView.setAlpha(1.0f);
            }
        }

        private void bindPriority(String priority) {
            if (priority == null) {
                ivPriority.setVisibility(View.GONE);
                viewDot.setBackgroundTintList(ContextCompat.getColorStateList(
                        itemView.getContext(), R.color.priority_default_border));
                return;
            }

            ivPriority.setVisibility(View.VISIBLE);
            int colorRes;
            int iconRes;

            switch (priority) {
                case TaskConstants.PRIORITY_HIGH:
                    colorRes = R.color.priority_high_border;
                    iconRes = R.drawable.ic_priority_high;
                    break;
                case TaskConstants.PRIORITY_MEDIUM:
                    colorRes = R.color.priority_medium_border;
                    iconRes = R.drawable.ic_priority_medium;
                    break;
                case TaskConstants.PRIORITY_LOW:
                    colorRes = R.color.priority_low_border;
                    iconRes = R.drawable.ic_priority_low;
                    break;
                default:
                    colorRes = R.color.priority_default_border;
                    iconRes = R.drawable.ic_priority_medium;
                    break;
            }

            viewDot.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), colorRes));
            ivPriority.setImageResource(iconRes);
            ivPriority.setColorFilter(ContextCompat.getColor(itemView.getContext(), colorRes));
        }

        private void bindStatus(String status) {
            if (status == null) {
                badgeStatus.setVisibility(View.GONE);
                return;
            }

            badgeStatus.setVisibility(View.VISIBLE);
            String statusText;
            int bgColorRes;

            switch (status) {
                case TaskConstants.STATUS_PENDING:
                    statusText = "Pendiente";
                    bgColorRes = R.color.status_pending_bg;
                    break;
                case TaskConstants.STATUS_IN_PROGRESS:
                    statusText = "En progreso";
                    bgColorRes = R.color.status_inprogress_bg;
                    break;
                case TaskConstants.STATUS_IN_REVIEW:
                    statusText = "En revisión";
                    bgColorRes = R.color.status_inreview_bg;
                    break;
                case TaskConstants.STATUS_COMPLETED:
                    statusText = "Completada";
                    bgColorRes = R.color.status_completed_bg;
                    break;
                default:
                    statusText = "Desconocido";
                    bgColorRes = R.color.status_default_bg;
                    break;
            }

            badgeStatus.setText(statusText);
            badgeStatus.setBackgroundTintList(ContextCompat.getColorStateList(
                    itemView.getContext(), bgColorRes));
        }

        private boolean isTaskOverdue(TaskModel task) {
            if (task.getDeadline() == null) return false;
            if (TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) return false;

            Date now = new Date();
            return task.getDeadline().before(now);
        }

        private void bindMembers(List<String> memberIds) {
            layoutMembers.removeAllViews();

            if (memberIds == null || memberIds.isEmpty()) {
                layoutMembers.setVisibility(View.GONE);
                return;
            }

            layoutMembers.setVisibility(View.VISIBLE);

            // Mostrar máximo 3 avatares
            int maxAvatares = Math.min(memberIds.size(), 3);
            for (int i = 0; i < maxAvatares; i++) {
                TextView avatar = new TextView(itemView.getContext());
                avatar.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density),
                        (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density)));

                // Avatar circular simple con iniciales
                String memberId = memberIds.get(i);
                String initial = memberId.length() > 0 ? memberId.substring(0, 1).toUpperCase() : "?";
                avatar.setText(initial);
                avatar.setTextSize(10);
                avatar.setTextColor(Color.WHITE);
                avatar.setGravity(android.view.Gravity.CENTER);
                avatar.setBackgroundResource(R.drawable.circle_background);
                avatar.getBackground().setTint(Color.parseColor("#7986CB"));

                // Margen entre avatares
                if (i > 0) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) avatar.getLayoutParams();
                    params.setMarginStart((int) (-8 * itemView.getContext().getResources().getDisplayMetrics().density));
                    avatar.setLayoutParams(params);
                }

                layoutMembers.addView(avatar);
            }

            // Si hay más de 3 miembros, mostrar un indicador
            if (memberIds.size() > 3) {
                TextView moreIndicator = new TextView(itemView.getContext());
                moreIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density),
                        (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density)));
                moreIndicator.setText("+" + (memberIds.size() - 3));
                moreIndicator.setTextSize(8);
                moreIndicator.setTextColor(Color.WHITE);
                moreIndicator.setGravity(android.view.Gravity.CENTER);
                moreIndicator.setBackgroundResource(R.drawable.circle_background);
                moreIndicator.getBackground().setTint(Color.parseColor("#9E9E9E"));

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) moreIndicator.getLayoutParams();
                params.setMarginStart((int) (-8 * itemView.getContext().getResources().getDisplayMetrics().density));
                moreIndicator.setLayoutParams(params);

                layoutMembers.addView(moreIndicator);
            }
        }
    }
}