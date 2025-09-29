package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class BoardSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_board_settings);

        setupToolbar();
        setupOptions();
        setupListeners();
    }

    /**
     * Configura la barra de navegación superior (Toolbar) y el modo EdgeToEdge.
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.settings);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Configura el contenido de todas las opciones de la lista.
     */
    private void setupOptions() {
        setupOptionItem(
                findViewById(R.id.option_edit_board),
                R.string.edit_board,
                R.string.detail_edit_board,
                R.drawable.ic_edit_board
        );

        setupOptionItem(
                findViewById(R.id.option_manage_users),
                R.string.manage_users,
                R.string.detail_manage_users,
                R.drawable.ic_group_add
        );

        setupOptionItem(
                findViewById(R.id.option_manage_coupons),
                R.string.manage_coupons,
                R.string.detail_manage_coupons,
                R.drawable.ic_manage_coupons
        );

        setupOptionItem(
                findViewById(R.id.option_redeem_points),
                R.string.redeem_points,
                R.string.detail_redeem_points,
                R.drawable.ic_redeem
        );

        setupOptionItem(
                findViewById(R.id.option_approve_tasks),
                R.string.approve_tasks,
                R.string.detail_approve_tasks,
                R.drawable.ic_approve_tasks
        );
    }

    /**
     * Configura los listeners para las opciones clicables.
     */
    private void setupListeners() {
        findViewById(R.id.option_edit_board).setOnClickListener(v -> {
            // Navegar a la pantalla de editar tablero
            Intent intent = new Intent(this, EditBoardActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.option_manage_users).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMembersActivity.class);
            intent.putExtra("boardId", "current_board_id"); // Pasar el ID del tablero actual
            startActivity(intent);
        });
    }

    /**
     * Método de ayuda para rellenar los datos de un item de la lista reutilizable.
     */
    private void setupOptionItem(View optionView, int titleResId, int subtitleResId, int iconResId) {
        TextView title = optionView.findViewById(R.id.list_item_title);
        TextView subtitle = optionView.findViewById(R.id.list_item_subtitle);
        ImageView icon = optionView.findViewById(R.id.list_item_icon);

        title.setText(titleResId);
        subtitle.setText(subtitleResId);
        icon.setImageResource(iconResId);
    }
}