package com.utp.wemake;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditBoardActivity extends AppCompatActivity {

    private TextInputEditText etBoardName;
    private TextInputEditText etBoardDescription;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;
    private View selectedColorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_board);

        setupToolBar();
        setupViews();
        setupListeners();
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
                    // Aquí implementarías la lógica para eliminar el tablero
                    Toast.makeText(this, "Tablero eliminado", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}