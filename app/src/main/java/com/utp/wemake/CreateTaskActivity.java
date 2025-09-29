package com.utp.wemake;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.ChecklistItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CreateTaskActivity extends AppCompatActivity {

    // --- Variables de la Interfaz (Views) ---
    private TextInputEditText inputTitle, inputDescription;
    private ChipGroup chipGroupMembers;
    private Chip chipTime, chipDate;
    private MaterialButtonToggleGroup togglePriority;
    private LinearLayout checklistContainer;
    private MaterialButton btnAddChecklistItem;

    // --- Variables para la Lógica ---
    private final Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task);

        // El método onCreate ahora es un resumen claro de lo que se configura.
        initializeViews();
        setupToolbar();
        setupInitialData();
        setupListeners();
    }

    /**
     * Infla el menú de opciones en la Toolbar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.save_menu, menu);
        return true;
    }

    /**
     * Maneja los clics en los ítems del menú de la Toolbar (ej. "Guardar").
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveTask(); // Llama a tu lógica de guardado
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.new_task);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Vincula las variables de la clase con las vistas del layout XML.
     */
    private void initializeViews() {
        // No se necesita btnBack aquí si es un MaterialToolbar, se maneja de otra forma.
        // btnSave se inicializará en setupListeners.
        inputTitle = findViewById(R.id.input_task_title);
        inputDescription = findViewById(R.id.input_task_description);
        chipGroupMembers = findViewById(R.id.chipGroupMembers);
        chipTime = findViewById(R.id.chip_time);
        chipDate = findViewById(R.id.chip_date);
        togglePriority = findViewById(R.id.toggle_priority);
        checklistContainer = findViewById(R.id.checklist_container);
        btnAddChecklistItem = findViewById(R.id.btn_add_checklist_item);
    }

    /**
     * Establece los valores iniciales en las vistas (ej. fecha y hora actual).
     */
    private void setupInitialData() {
        chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
        chipTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    /**
     * Configura todos los listeners para las interacciones del usuario.
     */
    private void setupListeners() {
        chipTime.setOnClickListener(v -> showTimePicker());
        chipDate.setOnClickListener(v -> showDatePicker());
        btnAddChecklistItem.setOnClickListener(v -> addChecklistItem(null, false));
    }

    /**
     * Añade dinámicamente una nueva fila de subtarea a la checklist.
     * @param text El texto a poner en el EditText (null si es nuevo).
     * @param isChecked El estado del CheckBox.
     */
    private void addChecklistItem(String text, boolean isChecked) {
        // Infla el layout de la fila de la checklist
        View itemView = getLayoutInflater().inflate(R.layout.list_item_check, checklistContainer, false);

        TextInputEditText etItem = itemView.findViewById(R.id.et_checklist_item);
        CheckBox checkBox = itemView.findViewById(R.id.checkbox_item);
        ImageButton btnRemove = itemView.findViewById(R.id.btn_remove_item);

        if (text != null) {
            etItem.setText(text);
        }
        checkBox.setChecked(isChecked);

        // Configura el listener para el botón de eliminar de ESTA fila
        btnRemove.setOnClickListener(v -> {
            // Elimina la vista (la fila completa) de su contenedor padre
            checklistContainer.removeView(itemView);
        });

        // Añade la nueva fila al contenedor
        checklistContainer.addView(itemView);
        etItem.requestFocus(); // Pone el foco en el nuevo campo de texto
    }


    // =====================================================================================
    // --- LÓGICA DE SELECCIÓN DE FECHA Y HORA ---
    // =====================================================================================

    /**
     * Muestra el DatePickerDialog para seleccionar una fecha.
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateChip();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Muestra el TimePickerDialog para seleccionar una hora.
     */
    private void showTimePicker() {
        new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateTimeChip();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true // 24-hour format
        ).show();
    }

    private void updateDateChip() {
        chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
    }

    private void updateTimeChip() {
        chipTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    // =====================================================================================
    // --- LÓGICA PARA GUARDAR LA TAREA ---
    // =====================================================================================

    /**
     * Recorre todas las filas de la checklist y extrae los datos.
     * @return Una lista de objetos ChecklistItem.
     */
    private List<ChecklistItem> getChecklistItems() {
        List<ChecklistItem> items = new ArrayList<>();
        for (int i = 0; i < checklistContainer.getChildCount(); i++) {
            View itemView = checklistContainer.getChildAt(i);
            TextInputEditText etItem = itemView.findViewById(R.id.et_checklist_item);
            CheckBox checkBox = itemView.findViewById(R.id.checkbox_item);

            String text = etItem.getText().toString().trim();
            if (!text.isEmpty()) { // Solo guardamos las subtareas que no estén vacías
                items.add(new ChecklistItem(text, checkBox.isChecked()));
            }
        }
        return items;
    }

    /**
     * Valida los campos y guarda la información de la tarea.
     */
    private void saveTask() {
        String title = inputTitle.getText().toString().trim();
        if (!isTitleValid(title)) return;

        String description = inputDescription.getText().toString().trim();
        // Puedes añadir validación para la descripción si es necesario.

        String priority = getSelectedPriority();
        List<String> selectedMembers = getSelectedMembers();
        String selectedDateStr = dateFormat.format(selectedDateTime.getTime());
        String selectedTimeStr = timeFormat.format(selectedDateTime.getTime());

        List<ChecklistItem> checklistItems = getChecklistItems();

        // TODO: Crear un objeto Task con estos datos y guardarlo en la base de datos o ViewModel.
        String taskInfo = String.format(
                "Tarea: %s\nFecha: %s\nHora: %s\nPrioridad: %s\nMiembros: %s",
                title, selectedDateStr, selectedTimeStr, priority, selectedMembers
        );

        Toast.makeText(this, taskInfo, Toast.LENGTH_LONG).show();
        finish(); // Cierra la actividad después de guardar.
    }

    private boolean isTitleValid(String title) {
        if (title.isEmpty()) {
            inputTitle.setError("El título es requerido");
            return false;
        }
        return true;
    }

    private String getSelectedPriority() {
        int selectedId = togglePriority.getCheckedButtonId();
        if (selectedId == R.id.btn_priority_medium) {
            return "Medio";
        } else if (selectedId == R.id.btn_priority_high) {
            return "Alto";
        }
        return "Bajo"; // Valor por defecto
    }

    private List<String> getSelectedMembers() {
        // Usando Java 8 Streams para un código más moderno y conciso
        return chipGroupMembers.getCheckedChipIds().stream()
                .map(id -> ((Chip) chipGroupMembers.findViewById(id)).getText().toString())
                .collect(Collectors.toList());
    }
}