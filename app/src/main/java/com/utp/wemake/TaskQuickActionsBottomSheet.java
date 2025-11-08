package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.util.List;

public class TaskQuickActionsBottomSheet extends BottomSheetDialogFragment {

    private TasksViewModel viewModel;
    private String taskId;

    public static TaskQuickActionsBottomSheet newInstance(String taskId) {
        TaskQuickActionsBottomSheet fragment = new TaskQuickActionsBottomSheet();
        Bundle args = new Bundle();
        args.putString("TASK_ID", taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            taskId = getArguments().getString("TASK_ID");
        }
        viewModel = new ViewModelProvider(requireParentFragment()).get(TasksViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_task_quick_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener la tarea actual para verificar su estado
        TaskModel currentTask = getTaskById(taskId);
        if (currentTask == null) {
            dismiss();
            return;
        }

        // Configurar listeners de los chips
        setupChipListeners(view, currentTask);
    }

    private TaskModel getTaskById(String taskId) {
        List<TaskModel> allTasks = viewModel.getAllTasks().getValue();
        if (allTasks != null) {
            for (TaskModel task : allTasks) {
                if (task.getId().equals(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    private void setupChipListeners(View view, TaskModel task) {
        // Chip: Marcar como hecho
        Chip chipMarkDone = view.findViewById(R.id.chip_mark_done);
        if (chipMarkDone != null) {
            chipMarkDone.setOnClickListener(v -> {
                if (!TaskConstants.STATUS_COMPLETED.equals(task.getStatus())) {
                    viewModel.updateTaskStatus(taskId, TaskConstants.STATUS_COMPLETED);
                    Toast.makeText(getContext(), "Tarea completada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "La tarea ya está completada", Toast.LENGTH_SHORT).show();
                }
                dismiss();
            });
        }

        // Chip: Editar
        Chip chipEdit = view.findViewById(R.id.chip_edit);
        if (chipEdit != null) {
            chipEdit.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TaskDetailActivity.class);
                intent.putExtra("taskId", taskId);
                startActivity(intent);
                dismiss();
            });
        }

        // Chip: Re-asignar
        Chip chipReassign = view.findViewById(R.id.chip_reassign);
        if (chipReassign != null) {
            chipReassign.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Re-asignar próximamente", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        // Chip: Comentar
        Chip chipComment = view.findViewById(R.id.chip_comment);
        if (chipComment != null) {
            chipComment.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Comentarios próximamente", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        // Chip: Checklist
        Chip chipChecklist = view.findViewById(R.id.chip_checklist);
        if (chipChecklist != null) {
            chipChecklist.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TaskDetailActivity.class);
                intent.putExtra("taskId", taskId);
                startActivity(intent);
                dismiss();
            });
        }

        // Chip: Adjuntar
        Chip chipAttach = view.findViewById(R.id.chip_attach);
        if (chipAttach != null) {
            chipAttach.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Adjuntar archivo próximamente", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        // Chip: Posponer
        Chip chipSnooze = view.findViewById(R.id.chip_snooze);
        if (chipSnooze != null) {
            chipSnooze.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Posponer tarea próximamente", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        // Chip: Compartir
        Chip chipShare = view.findViewById(R.id.chip_share);
        if (chipShare != null) {
            chipShare.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Compartir próximamente", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }
    }
}