package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Board;
import com.utp.wemake.viewmodels.TasksViewModel;

import java.util.ArrayList;
import java.util.List;

public class TasksFiltersBottomSheet extends BottomSheetDialogFragment {

    private TasksViewModel viewModel;
    private ChipGroup chipGroupBoards;
    private ChipGroup chipGroupPriority;
    private ChipGroup chipGroupAssignee;
    private ChipGroup chipGroupDue;
    private List<Board> currentBoards = new ArrayList<>();

    public static TasksFiltersBottomSheet newInstance() {
        return new TasksFiltersBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_tasks_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener el ViewModel del Fragment padre
        viewModel = new ViewModelProvider(requireParentFragment()).get(TasksViewModel.class);

        // Inicializar vistas
        chipGroupBoards = view.findViewById(R.id.chip_group_boards);
        chipGroupPriority = view.findViewById(R.id.chip_group_priority);
        chipGroupAssignee = view.findViewById(R.id.chip_group_assignee);
        chipGroupDue = view.findViewById(R.id.chip_group_due);

        MaterialButton btnApply = view.findViewById(R.id.btn_apply_filters);
        MaterialButton btnClear = view.findViewById(R.id.btn_clear_filters);

        // Configurar chips
        setupBoardsChips();
        setupPriorityChips();
        setupAssigneeChips();
        setupDueChips();

        // Restaurar estado de filtros actuales
        restoreCurrentFilters();

        // Listeners
        btnApply.setOnClickListener(v -> {
            applyFilters();
            dismiss();
        });

        btnClear.setOnClickListener(v -> {
            clearAllSelections();
            viewModel.clearFilters();
            dismiss();
        });
    }

    private void setupBoardsChips() {
        viewModel.getUserBoards().observe(this, boards -> {
            if (boards != null && !boards.isEmpty()) {
                currentBoards = boards;
                chipGroupBoards.removeAllViews();

                for (Board board : boards) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(board.getName());
                    chip.setCheckable(true);
                    chip.setTag(board.getId());
                    chipGroupBoards.addView(chip);
                }

                // Restaurar selección
                restoreBoardSelection();
            }
        });
    }

    private void setupPriorityChips() {
        String[] priorities = {"Alta", "Media", "Baja"};
        String[] priorityValues = {
                TaskConstants.PRIORITY_HIGH,
                TaskConstants.PRIORITY_MEDIUM,
                TaskConstants.PRIORITY_LOW
        };

        for (int i = 0; i < priorities.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(priorities[i]);
            chip.setTag(priorityValues[i]);
            chip.setCheckable(true);
            chipGroupPriority.addView(chip);
        }

        // Restaurar selección
        restorePrioritySelection();
    }

    private void setupAssigneeChips() {
        // Opciones de filtro por asignado
        String[] assigneeOptions = {"Yo", "No asignado"};
        String[] assigneeValues = {"me", "unassigned"};

        for (int i = 0; i < assigneeOptions.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(assigneeOptions[i]);
            chip.setTag(assigneeValues[i]);
            chip.setCheckable(true);
            chipGroupAssignee.addView(chip);
        }

        // Restaurar selección
        restoreAssigneeSelection();
    }

    private void setupDueChips() {
        String[] dueOptions = {"Vencidas", "Hoy", "Mañana", "Esta semana"};
        String[] dueValues = {"overdue", "today", "tomorrow", "this_week"};

        for (int i = 0; i < dueOptions.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(dueOptions[i]);
            chip.setTag(dueValues[i]);
            chip.setCheckable(true);
            chipGroupDue.addView(chip);
        }

        // Restaurar selección
        restoreDueSelection();
    }

    private void restoreCurrentFilters() {
        // Este método se llama después de que los chips están creados
        restoreBoardSelection();
        restorePrioritySelection();
        restoreAssigneeSelection();
        restoreDueSelection();
    }

    private void restoreBoardSelection() {
        List<String> selectedBoardIds = viewModel.getSelectedBoardIds();
        if (selectedBoardIds != null && !selectedBoardIds.isEmpty()) {
            for (int i = 0; i < chipGroupBoards.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupBoards.getChildAt(i);
                String boardId = (String) chip.getTag();
                if (selectedBoardIds.contains(boardId)) {
                    chip.setChecked(true);
                }
            }
        }
    }

    private void restorePrioritySelection() {
        String selectedPriority = viewModel.getSelectedPriority();
        if (selectedPriority != null) {
            for (int i = 0; i < chipGroupPriority.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupPriority.getChildAt(i);
                if (selectedPriority.equals(chip.getTag())) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    private void restoreAssigneeSelection() {
        String selectedAssignee = viewModel.getSelectedAssignee();
        if (selectedAssignee != null) {
            for (int i = 0; i < chipGroupAssignee.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupAssignee.getChildAt(i);
                if (selectedAssignee.equals(chip.getTag())) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    private void restoreDueSelection() {
        String selectedDue = viewModel.getSelectedDueFilter();
        if (selectedDue != null) {
            for (int i = 0; i < chipGroupDue.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupDue.getChildAt(i);
                if (selectedDue.equals(chip.getTag())) {
                    chip.setChecked(true);
                    break;
                }
            }
        }
    }

    private void clearAllSelections() {
        // Limpiar selección de tableros
        for (int i = 0; i < chipGroupBoards.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupBoards.getChildAt(i);
            chip.setChecked(false);
        }

        // Limpiar selección de prioridad
        chipGroupPriority.clearCheck();

        // Limpiar selección de asignado
        chipGroupAssignee.clearCheck();

        // Limpiar selección de fecha
        chipGroupDue.clearCheck();
    }

    private void applyFilters() {
        // Aplicar filtros de tableros
        List<String> selectedBoardIds = new ArrayList<>();
        for (int i = 0; i < chipGroupBoards.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupBoards.getChildAt(i);
            if (chip.isChecked()) {
                String boardId = (String) chip.getTag();
                selectedBoardIds.add(boardId);
            }
        }
        viewModel.setSelectedBoards(selectedBoardIds);

        // Aplicar filtro de prioridad
        String selectedPriority = null;
        int checkedPriorityId = chipGroupPriority.getCheckedChipId();
        if (checkedPriorityId != View.NO_ID) {
            Chip checkedChip = chipGroupPriority.findViewById(checkedPriorityId);
            if (checkedChip != null) {
                selectedPriority = (String) checkedChip.getTag();
            }
        }
        viewModel.setSelectedPriority(selectedPriority);

        // Aplicar filtro de asignado
        String selectedAssignee = null;
        int checkedAssigneeId = chipGroupAssignee.getCheckedChipId();
        if (checkedAssigneeId != View.NO_ID) {
            Chip checkedChip = chipGroupAssignee.findViewById(checkedAssigneeId);
            if (checkedChip != null) {
                selectedAssignee = (String) checkedChip.getTag();
            }
        }
        viewModel.setSelectedAssignee(selectedAssignee);

        // Aplicar filtro de fecha
        String selectedDue = null;
        int checkedDueId = chipGroupDue.getCheckedChipId();
        if (checkedDueId != View.NO_ID) {
            Chip checkedChip = chipGroupDue.findViewById(checkedDueId);
            if (checkedChip != null) {
                selectedDue = (String) checkedChip.getTag();
            }
        }
        viewModel.setSelectedDueFilter(selectedDue);
    }
}