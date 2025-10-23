package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.viewmodels.HomeViewModel;

public class ChangeStatusBottomSheet extends BottomSheetDialogFragment {
    private HomeViewModel homeViewModel; // Referencia al ViewModel del HomeFragment

    // Método de fábrica actualizado para recibir el rol del usuario
    public static ChangeStatusBottomSheet newInstance(String taskId, String currentStatus, String userRole) {
        ChangeStatusBottomSheet fragment = new ChangeStatusBottomSheet();
        Bundle args = new Bundle();
        args.putString("TASK_ID", taskId);
        args.putString("CURRENT_STATUS", currentStatus);
        args.putString("USER_ROLE", userRole);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla TU layout
        return inflater.inflate(R.layout.bottom_sheet_change_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Referencias a las vistas
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_status);
        RadioButton radioPending = view.findViewById(R.id.radio_pending);
        RadioButton radioInProgress = view.findViewById(R.id.radio_in_progress);
        RadioButton radioInReview = view.findViewById(R.id.radio_in_review);
        RadioButton radioDone = view.findViewById(R.id.radio_done);
        MaterialButton saveButton = view.findViewById(R.id.button_save);

        // Recuperar los datos
        String taskId = getArguments().getString("TASK_ID");
        String currentStatus = getArguments().getString("CURRENT_STATUS");
        String userRole = getArguments().getString("USER_ROLE");

        // --- LÓGICA DE PERMISOS ---
        // Por defecto, deshabilitar todos los que no son para miembros
        radioDone.setEnabled(false);

        // Si el usuario es REVISOR, puede hacer todo
        if ("REVIEWER".equals(userRole)) {
            radioDone.setEnabled(true);
        }

        // Marcar el estado actual por defecto
        if (currentStatus != null) {
            switch (currentStatus) {
                case TaskConstants.STATUS_PENDING:
                    radioGroup.check(R.id.radio_pending);
                    break;
                case TaskConstants.STATUS_IN_PROGRESS:
                    radioGroup.check(R.id.radio_in_progress);
                    break;
                case TaskConstants.STATUS_IN_REVIEW:
                    radioGroup.check(R.id.radio_in_review);
                    break;
                case TaskConstants.STATUS_COMPLETED:
                    radioGroup.check(R.id.radio_done);
                    break;
            }
        }

        saveButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String newStatus = "";

            if (selectedId == R.id.radio_pending) {
                newStatus = TaskConstants.STATUS_PENDING;
            } else if (selectedId == R.id.radio_in_progress) {
                newStatus = TaskConstants.STATUS_IN_PROGRESS;
            } else if (selectedId == R.id.radio_in_review) {
                newStatus = TaskConstants.STATUS_IN_REVIEW;
            } else if (selectedId == R.id.radio_done) {
                newStatus = TaskConstants.STATUS_COMPLETED;
            }

            if (!newStatus.isEmpty() && !newStatus.equals(currentStatus)) {
                // --- LLAMAR AL VIEWMODEL PARA ACTUALIZAR EN FIREBASE ---
                homeViewModel.updateTaskStatus(taskId, newStatus);
                Toast.makeText(getContext(), "Actualizando estado...", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
    }
}