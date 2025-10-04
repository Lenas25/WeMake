package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.viewmodels.CreateBoardViewModel;

public class CreateBoardActivity extends AppCompatActivity {

    private TextInputEditText etBoardName;
    private MaterialButton btnConfirm;
    private CreateBoardViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_board);

        viewModel = new ViewModelProvider(this).get(CreateBoardViewModel.class);

        initializeViews();
        setupListeners();
        setupObservers();
    }

    private void initializeViews() {
        etBoardName = findViewById(R.id.et_board_name);
        btnConfirm = findViewById(R.id.btn_confirm_create_board);
    }

    private void setupListeners() {
        btnConfirm.setOnClickListener(v -> {
            String boardName = etBoardName.getText().toString().trim();
            if (boardName.isEmpty()) {
                etBoardName.setError("El nombre es requerido");
                return;
            }
            // Llama al ViewModel para crear el tablero
            viewModel.createNewBoard(boardName);
        });
    }

    private void setupObservers() {
        viewModel.getIsBoardCreated().observe(this, isCreated -> {
            if (isCreated) {
                Toast.makeText(this, "¡Tablero creado con éxito!", Toast.LENGTH_SHORT).show();
                // Navega a la pantalla principal
                navigateToMain();
            }
        });

        viewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(CreateBoardActivity.this, MainActivity.class);
        // Limpia el historial para que el usuario no pueda volver a esta pantalla ni a WelcomeActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}