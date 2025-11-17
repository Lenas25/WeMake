package com.utp.wemake;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.User;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.viewmodels.CreateTaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";

    // --- Vistas ---
    private TextInputLayout rewardPointsLayout, penaltyPointsLayout, reviewerLayout;
    private TextInputEditText inputTitle, inputDescription, inputRewardPoints, inputPenaltyPoints;
    private AutoCompleteTextView inputReviewer;
    private ChipGroup chipGroupMembers;
    private Chip chipTime, chipDate;
    private MaterialButtonToggleGroup togglePriority;
    private LinearLayout checklistContainer;
    private MaterialToolbar toolbar;

    // --- Lógica ---
    private CreateTaskViewModel viewModel;
    private String boardId;
    private String taskId; // Null si es creación, con valor si es edición
    private final Calendar selectedDateTime = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Mapa para relacionar el nombre del revisor con su ID
    private final Map<String, String> reviewerNameToIdMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_task);

        boardId = new BoardSelectionPrefs(this).getSelectedBoardId();
        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);

        if (boardId == null || boardId.isEmpty()) {
            Toast.makeText(this, "Error: ID del tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupInitialData();
        setupToolbar();
        setupListeners();

        viewModel = new ViewModelProvider(this,
                new CreateTaskViewModel.Factory(getApplication()))
                .get(CreateTaskViewModel.class);
        observeViewModel();

        // Cargar datos iniciales (miembros y la tarea si se está editando)
        viewModel.loadInitialData(boardId, taskId);
    }

    private void setupInitialData() {
        // Establecer fecha y hora actual en los chips
        chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
        chipTime.setText(timeFormat.format(selectedDateTime.getTime()));

        // Seleccionar "Prioridad Media" por defecto
        togglePriority.check(R.id.btn_priority_medium);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.top_app_bar);
        rewardPointsLayout = findViewById(R.id.reward_points_layout);
        penaltyPointsLayout = findViewById(R.id.penalty_points_layout);
        reviewerLayout = findViewById(R.id.reviewer_layout);
        inputTitle = findViewById(R.id.input_task_title);
        inputDescription = findViewById(R.id.input_task_description);
        inputRewardPoints = findViewById(R.id.input_reward_points);
        inputPenaltyPoints = findViewById(R.id.input_penalty_points);
        inputReviewer = findViewById(R.id.input_reviewer);
        chipGroupMembers = findViewById(R.id.chipGroupMembers);
        chipTime = findViewById(R.id.chip_time);
        chipDate = findViewById(R.id.chip_date);
        togglePriority = findViewById(R.id.toggle_priority);
        checklistContainer = findViewById(R.id.checklist_container);
    }

    private void setupToolbar() {
        if (taskId != null) {
            toolbar.setTitle(R.string.edit_task);
        } else {
            toolbar.setTitle(R.string.new_task);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                collectDataAndSave();
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        chipTime.setOnClickListener(v -> showTimePicker());
        chipDate.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btn_add_checklist_item).setOnClickListener(v -> addChecklistItem("", false));
    }

    private void observeViewModel() {
        viewModel.isUserAdmin.observe(this, isAdmin -> configureUiState());

        viewModel.taskToEdit.observe(this, task -> {
            populateFormForEditing(task);
            configureUiState(); // También reconfiguramos la UI después de cargar los datos de edición
        });

        viewModel.boardMembers.observe(this, members -> {
            populateMembersChips(members);
            populateReviewerDropdown(members);
        });

        viewModel.taskSaved.observe(this, saved -> {
            if (saved) {
                Toast.makeText(this, "Tarea guardada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * NUEVO: Método central para controlar la visibilidad y el estado de los campos
     * basándose en el rol del usuario y si está en modo de edición.
     */
    private void configureUiState() {
        Boolean isAdmin = viewModel.isUserAdmin.getValue();
        boolean isEditing = taskId != null;
        Log.d("Is admin", String.valueOf(isAdmin));
        // Si el rol aún no se ha determinado, no hacemos nada.
        if (isAdmin == null) {
            return;
        }

        if (isAdmin) {
            rewardPointsLayout.setVisibility(View.VISIBLE);
            penaltyPointsLayout.setVisibility(View.VISIBLE);
        } else {
            rewardPointsLayout.setVisibility(View.GONE);
            penaltyPointsLayout.setVisibility(View.GONE);
        }

        if (!isAdmin && isEditing) {
            // Si un usuario normal está editando, NO PUEDE cambiar la fecha/hora.
            chipDate.setEnabled(false);
            chipTime.setEnabled(false);
            chipDate.setClickable(false);
            chipTime.setClickable(false);
            // cambiar la apariencia para que se vea deshabilitado
            chipDate.setAlpha(0.6f);
            chipTime.setAlpha(0.6f);
        } else {
            // Si un usuario normal está creando, SÍ PUEDE elegir la fecha/hora.
            chipDate.setEnabled(true);
            chipTime.setEnabled(true);
            chipDate.setClickable(true);
            chipTime.setClickable(true);
            chipDate.setAlpha(1.0f);
            chipTime.setAlpha(1.0f);
        }
    }

    private void populateMembersChips(List<Map<String, Object>> members) {
        chipGroupMembers.removeAllViews();
        if (members == null) return;

        for (Map<String, Object> memberData : members) {
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

    private void populateReviewerDropdown(List<Map<String, Object>> members) {
        if (members == null) return;

        List<String> memberNames = new ArrayList<>();
        reviewerNameToIdMap.clear();

        for (Map<String, Object> memberData : members) {
            User user = (User) memberData.get("user");
            if (user != null) {
                memberNames.add(user.getName());
                reviewerNameToIdMap.put(user.getName(), user.getUserid());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, memberNames);
        inputReviewer.setAdapter(adapter);
    }

    /**
     * Rellena todo el formulario con los datos de una tarea existente.
     */
    private void populateFormForEditing(TaskModel task) {
        if (task == null) return;

        inputTitle.setText(task.getTitle());
        inputDescription.setText(task.getDescription());

        // Rellenar datos de admin si es visible
        if (rewardPointsLayout.getVisibility() == View.VISIBLE) {
            inputRewardPoints.setText(String.valueOf(task.getRewardPoints()));
            inputPenaltyPoints.setText(String.valueOf(task.getPenaltyPoints()));
            // Buscar y setear el nombre del revisor a partir del ID
            for (Map.Entry<String, String> entry : reviewerNameToIdMap.entrySet()) {
                if (entry.getValue().equals(task.getReviewerId())) {
                    inputReviewer.setText(entry.getKey(), false);
                    break;
                }
            }
        }

        // Seleccionar prioridad
        switch (task.getPriority()) {
            case TaskConstants.PRIORITY_HIGH:
                togglePriority.check(R.id.btn_priority_high);
                break;
            case TaskConstants.PRIORITY_LOW:
                togglePriority.check(R.id.btn_priority_low);
                break;
            default:
                togglePriority.check(R.id.btn_priority_medium);
                break;
        }

        // Marcar miembros asignados
        if (task.getAssignedMembers() != null) {
            for (int i = 0; i < chipGroupMembers.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupMembers.getChildAt(i);
                if (task.getAssignedMembers().contains((String) chip.getTag())) {
                    chip.setChecked(true);
                }
            }
        }

        // Establecer fecha y hora
        if (task.getDeadline() != null) {
            selectedDateTime.setTime(task.getDeadline());
            chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
            chipTime.setText(timeFormat.format(selectedDateTime.getTime()));
        }

        // Rellenar checklist
        checklistContainer.removeAllViews();
        if (task.getSubtasks() != null) {
            for (Subtask subtask : task.getSubtasks()) {
                addChecklistItem(subtask.getText(), subtask.isCompleted());
            }
        }
    }

    /**
     * Recolecta todos los datos del formulario y los envía al ViewModel para guardarlos.
     */
    private void collectDataAndSave() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            inputTitle.setError("El título es requerido");
            inputTitle.requestFocus();
            return;
        } else {
            inputTitle.setError(null);
        }

        String description = inputDescription.getText().toString().trim();
        String priority = getSelectedPriority();
        List<Subtask> subtasks = getChecklistItems();
        Date deadline = selectedDateTime.getTime();

        List<String> assignedMemberIds = getSelectedMemberIds();
        if (assignedMemberIds.isEmpty()) {
            Toast.makeText(this, "Debes asignar la tarea al menos a un miembro", Toast.LENGTH_LONG).show();
            return;
        }

        // --- La recolección y validación del revisor AHORA ESTÁ FUERA del if(isAdmin) ---
        String reviewerName = inputReviewer.getText().toString().trim();
        String reviewerId = reviewerNameToIdMap.get(reviewerName);

        // Validación 1: El revisor debe ser seleccionado (para todos)
        if (reviewerId == null || reviewerId.isEmpty()) {
            reviewerLayout.setError("Debes seleccionar un revisor para la tarea");
            reviewerLayout.requestFocus();
            return;
        } else {
            reviewerLayout.setError(null);
        }

        // Validación 2: El revisor no puede ser un miembro asignado (para todos)
        if (assignedMemberIds.contains(reviewerId)) {
            reviewerLayout.setError("El revisor no puede ser un miembro asignado");
            reviewerLayout.requestFocus();
            Toast.makeText(this, "El revisor no puede ser uno de los miembros asignados", Toast.LENGTH_LONG).show();
            return;
        } else {
            reviewerLayout.setError(null);
        }

        // --- Recolección de puntos (SOLO PARA ADMINS) ---
        int reward = 0;
        int penalty = 0;
        if (Boolean.TRUE.equals(viewModel.isUserAdmin.getValue())) {
            try {
                reward = Integer.parseInt(inputRewardPoints.getText().toString());
                penalty = Integer.parseInt(inputPenaltyPoints.getText().toString());
            } catch (NumberFormatException e) {
            }
        }

        // --- Si todas las validaciones pasan, llamamos al ViewModel ---
        Log.d("CreateTaskActivity", "Todas las validaciones pasaron. Guardando tarea...");
        viewModel.saveTask(boardId, title, description, priority, assignedMemberIds, deadline,
                subtasks, reward, penalty, reviewerId);
    }

    // --- Métodos de utilidad ---

    private List<Subtask> getChecklistItems() {
        List<Subtask> items = new ArrayList<>();
        for (int i = 0; i < checklistContainer.getChildCount(); i++) {
            View itemView = checklistContainer.getChildAt(i);
            TextInputEditText etItem = itemView.findViewById(R.id.et_checklist_item);
            CheckBox checkBox = itemView.findViewById(R.id.checkbox_item);
            String text = etItem.getText().toString().trim();
            if (!text.isEmpty()) {
                Subtask subtask = new Subtask(text);
                subtask.setCompleted(checkBox.isChecked());
                items.add(subtask);
            }
        }
        return items;
    }

    private void addChecklistItem(String text, boolean isChecked) {
        View itemView = getLayoutInflater().inflate(R.layout.list_item_check, checklistContainer, false);
        TextInputEditText etItem = itemView.findViewById(R.id.et_checklist_item);
        CheckBox checkBox = itemView.findViewById(R.id.checkbox_item);
        etItem.setText(text);
        checkBox.setChecked(isChecked);
        itemView.findViewById(R.id.btn_remove_item).setOnClickListener(v -> checklistContainer.removeView(itemView));
        checklistContainer.addView(itemView);
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDateTime.set(year, month, day);
            chipDate.setText(dateFormat.format(selectedDateTime.getTime()));
        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hour, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
            selectedDateTime.set(Calendar.MINUTE, minute);
            chipTime.setText(timeFormat.format(selectedDateTime.getTime()));
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private String getSelectedPriority() {
        int selectedId = togglePriority.getCheckedButtonId();
        if (selectedId == R.id.btn_priority_high) return TaskConstants.PRIORITY_HIGH;
        if (selectedId == R.id.btn_priority_low) return TaskConstants.PRIORITY_LOW;
        return TaskConstants.PRIORITY_MEDIUM;
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