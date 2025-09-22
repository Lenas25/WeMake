package com.utp.wemake;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateTaskActivity extends AppCompatActivity {

    // Elementos de la interfaz
    private MaterialButton btnBack, btnSave;
    private TextInputEditText inputTitle, inputDescription;
    private ChipGroup chipGroupMembers;
    private Chip chipTime, chipDate;
    private MaterialButtonToggleGroup togglePriority;
    
    // Variables para fecha y hora
    private Calendar selectedDate;
    private Calendar selectedTime;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task);

        // Inicializar formateadores de fecha y hora
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        // Inicializar calendarios
        selectedDate = Calendar.getInstance();
        selectedTime = Calendar.getInstance();

        // Inicializar elementos de la interfaz
        initViews();
        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save_task);
        inputTitle = findViewById(R.id.input_task_title);
        inputDescription = findViewById(R.id.input_task_description);
        chipGroupMembers = findViewById(R.id.chipGroupMembers);
        chipTime = findViewById(R.id.chip_time);
        chipDate = findViewById(R.id.chip_date);
        togglePriority = findViewById(R.id.toggle_priority);
    }

    private void setupListeners() {
        // Botón de regreso
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Botón de guardar
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        // Chips de tiempo y fecha
        chipTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        chipDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
    }

    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        String description = inputDescription.getText().toString().trim();

        // Validaciones básicas
        if (title.isEmpty()) {
            inputTitle.setError("El título es requerido");
            return;
        }

        if (description.isEmpty()) {
            inputDescription.setError("La descripción es requerida");
            return;
        }

        // Obtener prioridad seleccionada
        int selectedPriorityId = togglePriority.getCheckedButtonId();
        String priority = "Bajo"; // Por defecto

        if (selectedPriorityId == R.id.btn_priority_medium) {
            priority = "Medio";
        } else if (selectedPriorityId == R.id.btn_priority_high) {
            priority = "Alto";
        }

        // Obtener miembros seleccionados
        StringBuilder selectedMembers = new StringBuilder();
        for (int i = 0; i < chipGroupMembers.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupMembers.getChildAt(i);
            if (chip.isChecked()) {
                if (selectedMembers.length() > 0) {
                    selectedMembers.append(", ");
                }
                selectedMembers.append(chip.getText());
            }
        }

        // Obtener fecha y hora seleccionadas
        String selectedDateStr = dateFormat.format(selectedDate.getTime());
        String selectedTimeStr = timeFormat.format(selectedTime.getTime());
        
        // TODO: Guardar la tarea en la base de datos
        String taskInfo = String.format("Tarea: %s\nFecha: %s\nHora: %s\nPrioridad: %s", 
            title, selectedDateStr, selectedTimeStr, priority);
        
        Toast.makeText(this, taskInfo, Toast.LENGTH_LONG).show();

        // Cerrar la actividad
        finish();
    }
    
    /**
     * Muestra el DatePickerDialog para seleccionar una fecha
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // Actualizar el texto del chip con la fecha seleccionada
                chipDate.setText(dateFormat.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        
        // Establecer fecha mínima como hoy
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    /**
     * Muestra el TimePickerDialog para seleccionar una hora
     */
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                
                // Actualizar el texto del chip con la hora seleccionada
                chipTime.setText(timeFormat.format(selectedTime.getTime()));
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            selectedTime.get(Calendar.MINUTE),
            true // Formato de 24 horas
        );
        
        timePickerDialog.show();
    }
}