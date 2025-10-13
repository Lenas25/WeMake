package com.utp.wemake;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.model.ColorShape;
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
    private String selectedColorHex = "#DDEA96";
    private View colorPreview;

    private BoardViewModel viewModel;
    private String currentBoardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_board);

        currentBoardId = getIntent().getStringExtra(EXTRA_BOARD_ID);
        viewModel = new ViewModelProvider(this).get(BoardViewModel.class);

        initializeViews();
        setupToolbar();
        setupListeners();
        setupObservers();

        // Determina el modo (Crear o Editar)
        if (currentBoardId != null) {
            MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
            toolbar.setTitle("Editar Tablero");
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
        updateColorPreview(selectedColorHex);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etBoardName = findViewById(R.id.et_board_name);
        etBoardDescription = findViewById(R.id.et_board_description);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        colorPreview = findViewById(R.id.color_preview);
    }

    private void setupObservers() {
        // Observador para cuando se cargan los datos de un tablero existente
        viewModel.getBoardData().observe(this, board -> {
            if (board != null) {
                etBoardName.setText(board.getName());
                etBoardDescription.setText(board.getDescription());
                selectedColorHex = board.getColor();
                updateColorPreview(selectedColorHex);
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

        findViewById(R.id.color_selector_layout).setOnClickListener(v -> showColorPickerDialog());
    }

    /**
     * Guarda los cambios del tablero.
     */
    private void saveBoardChanges() {
        String boardName = etBoardName.getText().toString().trim();
        String boardDescription = etBoardDescription.getText().toString().trim();
        String selectedColor = this.selectedColorHex;

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
    }

        /**
         * Muestra el diálogo del selector de color.
         */
        private void showColorPickerDialog() {
            new ColorPickerDialog
                    .Builder(this)
                    .setTitle("Elige un color para el tablero")
                    .setColorShape(ColorShape.SQAURE) // O ColorShape.CIRCLE
                    .setDefaultColor(selectedColorHex) // El color actualmente seleccionado
                    .setColorListener((color, colorHex) -> {
                        // Este código se ejecuta cuando el usuario selecciona un color y pulsa "OK"
                        selectedColorHex = colorHex;
                        updateColorPreview(colorHex);
                    })
                    .show();
        }

        /**
         * Actualiza la previsualización del color.
         * @param colorHex El color en formato hexadecimal (ej: "#FF5733").
         */
        private void updateColorPreview(String colorHex) {
            try {
                int color = Color.parseColor(colorHex);
                colorPreview.getBackground().setTint(color);
            } catch (IllegalArgumentException e) {
                colorPreview.getBackground().setTint(Color.LTGRAY);
            }
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