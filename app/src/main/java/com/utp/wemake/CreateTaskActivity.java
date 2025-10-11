package com.utp.wemake;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.viewmodels.CreateTaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private CreateTaskViewModel viewModel;
    private String boardId;
    private List<Map<String, Object>> boardMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task);

        // Obtener boardId
        BoardSelectionPrefs prefs = new BoardSelectionPrefs(getApplicationContext());
        boardId = prefs.getSelectedBoardId();

        if (boardId == null || boardId.isEmpty()) {
            Toast.makeText(this, "Error: ID del tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(CreateTaskViewModel.class);

        // El método onCreate ahora es un resumen claro de lo que se configura.
        initializeViews();
        setupToolbar();
        setupInitialData();
        setupListeners();
        observeViewModel();

        // Cargar miembros del tablero
        viewModel.loadBoardMembers(boardId);
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
        Toast.makeText(this, "Botón presionado: " + item.getItemId(), Toast.LENGTH_SHORT).show();

        if (item.getItemId() == R.id.action_save) {
            Toast.makeText(this, "Guardando tarea...", Toast.LENGTH_SHORT).show();
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

        // Seleccionar prioridad media por defecto
        togglePriority.check(R.id.btn_priority_medium);
    }

    /**
     * Configura todos los listeners para las interacciones del usuario.
     */
    private void setupListeners() {
        // Listeners para fecha y hora
        chipTime.setOnClickListener(v -> showTimePicker());
        chipDate.setOnClickListener(v -> showDatePicker());

        // Listener para agregar subtareas
        btnAddChecklistItem.setOnClickListener(v -> addChecklistItem());
    }

    private void observeViewModel() {
        viewModel.boardMembers.observe(this, members -> {
            if (members != null) {
                this.boardMembers = members;
                populateMembersChips();
            }
        });

        viewModel.taskCreated.observe(this, isCreated -> {
            if (isCreated) {
                Toast.makeText(this, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                Toast.makeText(this, "Creando tarea...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateMembersChips() {
        chipGroupMembers.removeAllViews();

        if (boardMembers != null) {
            for (Map<String, Object> memberData : boardMembers) {
                User user = (User) memberData.get("user");
                if (user != null) {
                    Chip chip = new Chip(this);
                    chip.setText(user.getName());
                    chip.setTag(user.getUserid());
                    chip.setCheckable(true);
                    chipGroupMembers.addView(chip);
                }
            }
        }
    }

    /**
     * Añade dinámicamente una nueva fila de subtarea a la checklist.
     */
    private void addChecklistItem() {
        // Infla el layout de la fila de la checklist
        View itemView = getLayoutInflater().inflate(R.layout.list_item_check, checklistContainer, false);

        // Listener para eliminar item
        itemView.findViewById(R.id.btn_remove_item).setOnClickListener(v -> {
            checklistContainer.removeView(itemView);
        });

        // Añade la nueva fila al contenedor
        checklistContainer.addView(itemView);
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
                    chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Muestra el TimePickerDialog para seleccionar una hora.
     */
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    chipTime.setText(timeFormat.format(selectedDateTime.getTime()));
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    // =====================================================================================
    // --- LÓGICA PARA GUARDAR LA TAREA ---
    // =====================================================================================

    /**
     * Recorre todas las filas de la checklist y extrae los datos.
     * @return Una lista de objetos ChecklistItem.
     */
    private List<Subtask> getChecklistItems() {
        List<Subtask> items = new ArrayList<>();
        for (int i = 0; i < checklistContainer.getChildCount(); i++) {
            View itemView = checklistContainer.getChildAt(i);
            TextInputEditText etItem = itemView.findViewById(R.id.et_checklist_item);
            CheckBox checkBox = itemView.findViewById(R.id.checkbox_item);

            String text = etItem.getText().toString().trim();
            if (!text.isEmpty()) { // Solo guardamos las subtareas que no estén vacías
                Subtask subtask = new Subtask(text);
                subtask.setCompleted(checkBox.isChecked());
                items.add(subtask);
            }
        }
        return items;
    }

    /**
     * Valida los campos y guarda la información de la tarea.
     */
    private void saveTask() {
        // Agregar logs para debug
        Toast.makeText(this, "Iniciando guardado...", Toast.LENGTH_SHORT).show();

        String title = inputTitle.getText().toString().trim();
        if (!isTitleValid(title)) return;

        String description = inputDescription.getText().toString().trim();
        // Puedes añadir validación para la descripción si es necesario.

        String priority = getSelectedPriority();
        List<String> assignedMemberIds = getSelectedMemberIds();
        List<Subtask> subtasks = getChecklistItems();

        // Log de datos para debug
        Toast.makeText(this, "Datos: " + title + " - " + priority, Toast.LENGTH_SHORT).show();

        viewModel.createTask(boardId, title, description, priority, assignedMemberIds,
                selectedDateTime.getTime(), subtasks);
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
        if (selectedId == R.id.btn_priority_high) {
            return TaskConstants.PRIORITY_HIGH;
        } else if (selectedId == R.id.btn_priority_medium) {
            return TaskConstants.PRIORITY_MEDIUM;
        }
        return TaskConstants.PRIORITY_LOW;
    }

    private List<String> getSelectedMemberIds() {
        List<String> memberIds = new ArrayList<>();
        for (int i = 0; i < chipGroupMembers.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupMembers.getChildAt(i);
            if (chip.isChecked()) {
                memberIds.add((String) chip.getTag());
            }
        }
        return memberIds;
    }
}