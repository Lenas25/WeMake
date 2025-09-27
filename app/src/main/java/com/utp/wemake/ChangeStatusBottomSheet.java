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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class ChangeStatusBottomSheet extends BottomSheetDialogFragment {

    // Método de fábrica para crear una instancia y pasar datos de forma segura
    public static ChangeStatusBottomSheet newInstance(String taskId, String currentStatus) {
        ChangeStatusBottomSheet fragment = new ChangeStatusBottomSheet();
        Bundle args = new Bundle();
        args.putString("TASK_ID", taskId);
        args.putString("CURRENT_STATUS", currentStatus);
        fragment.setArguments(args);
        return fragment;
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

        // Referencias a las vistas de tu layout
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_status);
        MaterialButton saveButton = view.findViewById(R.id.button_save);

        // Recuperar los datos pasados
        String taskId = getArguments().getString("TASK_ID");
        String currentStatus = getArguments().getString("CURRENT_STATUS");

        // Marcar el estado actual por defecto
        if (currentStatus != null) {
            // Asumiendo que el texto del RadioButton coincide con el estado
            if (currentStatus.equals(getString(R.string.state_inprogress))) {
                radioGroup.check(R.id.radio_in_progress);
            } else if (currentStatus.equals(getString(R.string.state_done))) {
                radioGroup.check(R.id.radio_done);
            } else {
                // Por defecto, marcamos "Pendiente"
                radioGroup.check(R.id.radio_pending);
            }
        }

        saveButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = view.findViewById(selectedId);
                String newStatus = selectedRadioButton.getText().toString();

                // Logica de Update de Firebase
                Toast.makeText(getContext(), "Guardando '" + newStatus + "' para la tarea " + taskId, Toast.LENGTH_SHORT).show();

                // Aquí llamarías a tu ViewModel o a una interfaz para notificar al HomeFragment del cambio

                dismiss(); // Cierra el BottomSheet
            }
        });
    }
}