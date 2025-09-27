package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla el layout que creaste para añadir tareas
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton voiceButton = view.findViewById(R.id.button_voice);
        MaterialButton manualButton = view.findViewById(R.id.button_manual);

        voiceButton.setOnClickListener(v -> {
            // Lógica para crear tarea por voz
            Toast.makeText(getContext(), "Iniciando creación por voz...", Toast.LENGTH_SHORT).show();
            dismiss(); // Cierra el BottomSheet
        });

        manualButton.setOnClickListener(v -> {
            // Lógica para crear tarea manualmente
            Toast.makeText(getContext(), "Iniciando creación manual...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), CreateTaskActivity.class);
            startActivity(intent);
        });
    }
}