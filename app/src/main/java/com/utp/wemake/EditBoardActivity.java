package com.utp.wemake;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.viewmodels.BoardViewModel;

import java.util.HashMap;
import java.util.Map;

public class EditBoardActivity extends AppCompatActivity {
    public static final String EXTRA_BOARD_ID = "board_id";

    private TextInputEditText etBoardName, etBoardDescription;
    private MaterialButton btnSave, btnDelete;
    private View selectedColorView;
    private Map<Integer, String> colorMap = new HashMap<>();

    private BoardViewModel viewModel;
    private String currentBoardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_board);

        currentBoardId = getIntent().getStringExtra(EXTRA_BOARD_ID);
        viewModel = new ViewModelProvider(this).get(BoardViewModel.class);

        initializeViews();
        setupToolbar();
        setupListeners();
        setupObservers();

        // Determina el modo (Crear o Editar)
        if (currentBoardId != null) {
            // Modo Editar: carga los datos del tablero
            viewModel.loadBoard(currentBoardId);
        } else {
            // Modo Crear: ajusta la UI
            configureUiForCreateMode();
        }
    }

    private void configureUiForCreateMode() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle("Crear Nuevo Tablero");
        btnDelete.setVisibility(View.GONE);
        // Selecciona el primer color por defecto
        updateColorSelection(findViewById(R.id.color_option_1));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        setupViews();

        // Mapea los IDs de las vistas de color a sus valores hexadecimales
        colorMap.put(R.id.color_option_1, "#E6EE9C");
        colorMap.put(R.id.color_option_2, "#FFCDD2");
        colorMap.put(R.id.color_option_3, "#C8E6C9");
        colorMap.put(R.id.color_option_4, "#BBDEFB");
    }

    private void setupObservers() {
        // Observador para cuando se cargan los datos de un tablero existente
        viewModel.getBoardData().observe(this, board -> {
            if (board != null) {
                etBoardName.setText(board.getName());
                etBoardDescription.setText(board.getDescription());

                // Encuentra la vista de color que coincide y la selecciona
                for (Map.Entry<Integer, String> entry : colorMap.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(board.getColor())) {
                        updateColorSelection(findViewById(entry.getKey()));
                        break;
                    }
                }
            }
        });

        // Observador para el resultado de las operaciones (crear, editar, eliminar)
        viewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Operación exitosa", Toast.LENGTH_SHORT).show();
                finish(); // Cierra la actividad
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }


    /**
     * Configuración de la barra de navegación superior (Toolbar) y el modo EdgeToEdge.
     */
    private void setupToolBar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.title_edit_board);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Inicializa las vistas del layout.
     */
    private void setupViews() {
        etBoardName = findViewById(R.id.et_board_name);
        etBoardDescription = findViewById(R.id.et_board_description);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        
        // Seleccionar el primer color por defecto
        selectedColorView = findViewById(R.id.color_option_1);
        updateColorSelection(selectedColorView);
    }

    /**
     * Configura los listeners para los botones y selectores de color.
     */
    private void setupListeners() {
        // Botón Guardar
        btnSave.setOnClickListener(v -> {
            saveBoardChanges();
        });

        // Botón Eliminar
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation();
        });

        // Selectores de color
        findViewById(R.id.color_option_1).setOnClickListener(v -> updateColorSelection(v));
        findViewById(R.id.color_option_2).setOnClickListener(v -> updateColorSelection(v));
        findViewById(R.id.color_option_3).setOnClickListener(v -> updateColorSelection(v));
        findViewById(R.id.color_option_4).setOnClickListener(v -> updateColorSelection(v));
    }

    /**
     * Actualiza la selección visual del color.
     */
    private void updateColorSelection(View colorView) {
        // Remover selección anterior
        if (selectedColorView != null) {
            selectedColorView.setScaleX(1.0f);
            selectedColorView.setScaleY(1.0f);
            selectedColorView.setAlpha(0.7f);
        }
        
        // Aplicar nueva selección
        selectedColorView = colorView;
        selectedColorView.setScaleX(1.2f);
        selectedColorView.setScaleY(1.2f);
        selectedColorView.setAlpha(1.0f);
    }

    /**
     * Guarda los cambios del tablero.
     */
    private void saveBoardChanges() {
        String boardName = etBoardName.getText().toString().trim();
        String boardDescription = etBoardDescription.getText().toString().trim();
        String selectedColor = colorMap.get(selectedColorView.getId());

        if (boardName.isEmpty()) {
            etBoardName.setError("El nombre del tablero es requerido");
            etBoardName.requestFocus();
            return;
        }

        if (boardDescription.isEmpty()) {
            etBoardDescription.setError("La descripción es requerida");
            etBoardDescription.requestFocus();
            return;
        }

        viewModel.saveBoard(currentBoardId, boardName, boardDescription, selectedColor);

        // Aquí implementarías la lógica para guardar los cambios
        Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show();
        
        // Regresar a la pantalla anterior
        onBackPressed();
    }



    /**
     * Muestra un diálogo de confirmación para eliminar el tablero.
     */
    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Tablero")
                .setMessage("¿Estás seguro de que quieres eliminar este tablero? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteBoard(currentBoardId);
                    Toast.makeText(this, "Tablero eliminado", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}