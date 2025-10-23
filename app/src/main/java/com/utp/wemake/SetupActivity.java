package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.utp.wemake.viewmodels.MainViewModel;

public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "user_name";
    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        TextView tvWelcome = findViewById(R.id.tv_welcome_title);
        MaterialButton btnCreateBoard = findViewById(R.id.btn_create_board);
        MaterialButton btnJoinBoard = findViewById(R.id.btn_join_board);

        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        if (userName != null && !userName.isEmpty()) {
            String firstName = userName.split(" ")[0];
            tvWelcome.setText("¡Bienvenido, " + firstName + "!");
        }

        btnCreateBoard.setOnClickListener(v -> {
            Intent intent = new Intent(SetupActivity.this, CreateBoardActivity.class);
            startActivity(intent);
            finish();
        });

        btnJoinBoard.setOnClickListener(v -> {
            showJoinBoardDialog();
        });

        mainViewModel.getJoinBoardResult().observe(this, event -> {
            MainViewModel.JoinBoardResult result = event.getContentIfNotHandled();

            if (result != null) {
                switch (result) {
                    case SUCCESS:
                        Toast.makeText(this, "¡Te has unido al tablero con éxito!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        break;

                    case ALREADY_MEMBER:
                        Toast.makeText(this, "Ya eres miembro de este tablero.", Toast.LENGTH_SHORT).show();
                        break;

                    case BOARD_NOT_FOUND:
                        Toast.makeText(this, "No se encontró ningún tablero con ese código.", Toast.LENGTH_LONG).show();
                        break;

                    case ERROR:
                        // Un error genérico
                        Toast.makeText(this, "Ocurrió un error. Inténtalo de nuevo.", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }

    /**
     * Muestra un diálogo para que el usuario ingrese un código de invitación.
     */
    private void showJoinBoardDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Unirse a un Tablero");
        builder.setMessage("Ingresa el código de invitación de 6 caracteres.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("A7B2C9");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Unirse", (dialog, which) -> {
            String code = input.getText().toString().trim();
            mainViewModel.joinBoardWithCode(code);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}