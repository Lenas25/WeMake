package com.utp.wemake;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.User;
import com.utp.wemake.viewmodels.BoardViewModel;

import java.util.List;

public class BoardSettingsActivity extends AppCompatActivity {

    private BoardViewModel boardViewModel;
    private String boardId;

    // Vistas que vamos a manipular
    private MaterialCardView boardCard;
    private TextView tvBoardName;
    private TextView tvBoardDescription;
    private TextView tvMembersLabel;
    private TextView tvCodeText;
    private LinearLayout membersAvatarContainer;
    private TextView membersOverflowCount;
    private View optionManageUsers, optionManageCoupons, optionApproveTasks, optionEditBoard, optionApproveRequests;
    private TextView tvInvitationCode;
    private MaterialButton btnCopyInviteCode;
    private ImageView icShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_board_settings);

        boardId = getIntent().getStringExtra("boardId");
        if (boardId == null) {
            Toast.makeText(this, "ID de tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupViewModel();

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

    private void initializeViews() {
        boardCard = findViewById(R.id.board_card);
        tvBoardName = findViewById(R.id.tvBoardName);
        tvBoardDescription = findViewById(R.id.tvBoardDescription);
        tvMembersLabel = findViewById(R.id.tvMembersLabel);
        membersAvatarContainer = findViewById(R.id.members_avatar_container);
        membersOverflowCount = findViewById(R.id.members_overflow_count);

        tvCodeText = findViewById(R.id.tvCodeText);
        tvInvitationCode = findViewById(R.id.tvInvitationCode);
        btnCopyInviteCode = findViewById(R.id.btnCopyInviteCode);
        icShare = findViewById(R.id.ic_share);

        optionEditBoard = findViewById(R.id.option_edit_board);
        optionManageUsers = findViewById(R.id.option_manage_users);
        optionManageCoupons = findViewById(R.id.option_manage_coupons);
        optionApproveTasks = findViewById(R.id.option_approve_tasks);
        optionApproveRequests = findViewById(R.id.option_approve_requests);
    }

    private void setupViewModel() {
        boardViewModel = new ViewModelProvider(this).get(BoardViewModel.class);

        // Observa los datos del tablero (nombre, color, etc.)
        boardViewModel.getBoardData().observe(this, this::updateBoardUI);

        // Observa la lista de miembros (los objetos User)
        boardViewModel.getBoardMembers().observe(this, this::updateMembersUI);

        // Observa el rol del usuario actual en este tablero
        boardViewModel.getCurrentUserMemberDetails().observe(this, memberDetails -> {
            Log.d("BoardSettingsRole", "Observador de rol activado.");
            boolean isAdmin = (memberDetails != null && memberDetails.isAdmin());
            Log.d("BoardSettingsRole", "El usuario es admin: " + isAdmin);
            updateOptionsVisibility(isAdmin);
        });

        // Inicia la escucha en tiempo real de todos los datos
        boardViewModel.listenToBoard(boardId);
    }

    /**
     * Muestra u oculta las opciones de administrador según el rol del usuario.
     */
    private void updateOptionsVisibility(boolean isAdmin) {
        if (isAdmin) {
            // Si es admin, muestra las opciones
            findViewById(R.id.divide_edit_board).setVisibility(View.VISIBLE);
            findViewById(R.id.divide_manage_users).setVisibility(View.VISIBLE);
            findViewById(R.id.divide_manage_coupons).setVisibility(View.VISIBLE);
            findViewById(R.id.divide_approve_tasks).setVisibility(View.VISIBLE);
            findViewById(R.id.divide_approve_requests).setVisibility(View.VISIBLE);
            optionEditBoard.setVisibility(View.VISIBLE);
            optionManageUsers.setVisibility(View.VISIBLE);
            optionManageCoupons.setVisibility(View.VISIBLE);
            optionApproveTasks.setVisibility(View.VISIBLE);
            optionApproveRequests.setVisibility(View.VISIBLE);
        } else {
            // Si no es admin, oculta las opciones
            findViewById(R.id.divide_edit_board).setVisibility(View.GONE);
            findViewById(R.id.divide_manage_users).setVisibility(View.GONE);
            findViewById(R.id.divide_manage_coupons).setVisibility(View.GONE);
            findViewById(R.id.divide_approve_tasks).setVisibility(View.GONE);
            findViewById(R.id.divide_approve_requests).setVisibility(View.GONE);
            optionEditBoard.setVisibility(View.GONE);
            optionManageUsers.setVisibility(View.GONE);
            optionManageCoupons.setVisibility(View.GONE);
            optionApproveTasks.setVisibility(View.GONE);
            optionApproveRequests.setVisibility(View.GONE);
        }
    }

    /**
     * Actualiza la UI con los datos del tablero.
     */
    private void updateBoardUI(Board board) {
        if (board == null) return;

        tvBoardName.setText(board.getName());

        if (board.getDescription() != null && !board.getDescription().isEmpty()) {
            tvBoardDescription.setText(board.getDescription());
            tvBoardDescription.setVisibility(View.VISIBLE);
        } else {
            tvBoardDescription.setVisibility(View.GONE);
        }

        // Actualizar el código de invitación
        if (board.getInvitationCode() != null && !board.getInvitationCode().isEmpty()) {
            tvInvitationCode.setText(board.getInvitationCode());
            tvInvitationCode.setVisibility(View.VISIBLE);
        } else {
            tvInvitationCode.setText("N/A");
            tvInvitationCode.setVisibility(View.VISIBLE);
        }

        // Cambiar color de la tarjeta
        try {
            int boardColor = Color.parseColor(board.getColor());
            boardCard.setCardBackgroundColor(ColorStateList.valueOf(boardColor));
            // Calcula la luminancia del color de fondo (0.0 para negro, 1.0 para blanco)
            double luminance = ColorUtils.calculateLuminance(boardColor);
            // Si el color es claro (luminancia > 0.5), usa texto oscuro.
            // Si no, usa texto claro.
            int contrastColor;
            if (luminance > 0.5) {
                contrastColor = Color.BLACK; // Fondo claro -> Texto negro
            } else {
                contrastColor = Color.WHITE; // Fondo oscuro -> Texto blanco
            }

            tvBoardName.setTextColor(contrastColor);
            tvBoardDescription.setTextColor(contrastColor);
            tvMembersLabel.setTextColor(contrastColor);
            tvInvitationCode.setTextColor(contrastColor);
            tvCodeText.setTextColor(contrastColor);
            icShare.setImageTintList(ColorStateList.valueOf(contrastColor));
            btnCopyInviteCode.setIconTint(ColorStateList.valueOf(contrastColor));
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Actualiza dinámicamente los avatares de los miembros.
     */
    private void updateMembersUI(List<User> members) {
        membersAvatarContainer.removeAllViews(); // Limpiar vistas anteriores
        membersOverflowCount.setVisibility(View.GONE);

        if (members == null || members.isEmpty()) {
            return; // No hacer nada si no hay miembros
        }

        int maxAvatars = 3;
        // Si hay más de 3, solo mostraremos 2 avatares + el contador
        int avatarsToShow = (members.size() > maxAvatars) ? maxAvatars - 1 : members.size();

        for (int i = 0; i < avatarsToShow; i++) {
            User member = members.get(i);
            ShapeableImageView avatar = createAvatarImageView();

            Glide.with(this)
                    .load(member.getPhotoUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(avatar);

            membersAvatarContainer.addView(avatar);
        }

        // Si hay más miembros de los que se muestran, configurar el contador
        if (members.size() > maxAvatars) {
            int overflow = members.size() - avatarsToShow;
            membersOverflowCount.setText("+" + overflow);
            membersAvatarContainer.addView(membersOverflowCount); // Añadir el contador al final
            membersOverflowCount.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Crea una vista de ImageView para un avatar con los estilos correctos.
     */
    private ShapeableImageView createAvatarImageView() {
        ShapeableImageView imageView = new ShapeableImageView(this);
        int sizeInDp = 32;
        int sizeInPx = (int) (sizeInDp * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
        // Margen negativo para que se superpongan
        params.setMarginEnd((int) (-10 * getResources().getDisplayMetrics().density));
        imageView.setLayoutParams(params);

        imageView.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        imageView.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
        imageView.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build());

        return imageView;
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

        setupOptionItem(
                findViewById(R.id.option_approve_requests),
                R.string.approve_requests,
                R.string.detail_approve_requests,
                R.drawable.ic_check_coupons
        );
    }

    /**
     * Configura los listeners para las opciones clicables.
     */
    private void setupListeners() {
        optionEditBoard.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditBoardActivity.class);
            intent.putExtra(EditBoardActivity.EXTRA_BOARD_ID, boardId);
            startActivity(intent);
        });

        optionManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMembersActivity.class);
            intent.putExtra("boardId", boardId);
            startActivity(intent);
        });

        optionManageCoupons.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageCouponsActivity.class);
            intent.putExtra("boardId", boardId);
            startActivity(intent);

        });

        optionApproveTasks.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApproveTaskRequestsActivity.class);
            intent.putExtra("boardId", boardId);
            startActivity(intent);
        });

        optionApproveRequests.setOnClickListener(v -> {
            Intent intent = new Intent(this, ApproveRequestsActivity.class);
            intent.putExtra("boardId", boardId);
            startActivity(intent);
        });

        findViewById(R.id.option_redeem_points).setOnClickListener(v -> {
            Intent intent = new Intent(this, RedeemPointsActivity.class);
            intent.putExtra("boardId", boardId);
            startActivity(intent);
        });

        // Listener para copiar el código de invitación
        btnCopyInviteCode.setOnClickListener(v -> {
            String inviteCode = tvInvitationCode.getText().toString();
            if (!inviteCode.equals("N/A") && !inviteCode.isEmpty()) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Código de invitación", inviteCode);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, getString(R.string.code_copied), Toast.LENGTH_SHORT).show();
            }
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