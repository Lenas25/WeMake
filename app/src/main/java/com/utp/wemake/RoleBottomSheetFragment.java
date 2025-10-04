package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.utp.wemake.constants.Roles;
import com.utp.wemake.models.User;

public class RoleBottomSheetFragment extends BottomSheetDialogFragment {

    // Argumentos que recibiremos
    private String userId;
    private String userName;
    private String userEmail;
    private String currentRole;

    // Listener para notificar a la Activity/Fragment que lo llamó
    private OnRoleChangeListener listener;

    // La interfaz de comunicación
    public interface OnRoleChangeListener {
        void onRoleChanged(String userId, String newRole);
        void onMemberDeleted(String userId);
    }

    /**
     * Método de fábrica para crear el BottomSheet y pasarle los datos necesarios.
     */
    public static RoleBottomSheetFragment newInstance(User user, String boardId, String currentRole) {
        RoleBottomSheetFragment fragment = new RoleBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("userId", user.getUserid());
        args.putString("userName", user.getName());
        args.putString("userEmail", user.getEmail());
        args.putString("boardId", boardId); // El boardId se usará en el futuro si se necesita
        args.putString("currentRole", currentRole);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            userName = getArguments().getString("userName");
            userEmail = getArguments().getString("userEmail");
            currentRole = getArguments().getString("currentRole");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_change_role, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupListeners(view);
    }

    private void setupViews(View view) {
        TextView tvMemberName = view.findViewById(R.id.tvMemberName);
        TextView tvMemberEmail = view.findViewById(R.id.tvMemberEmail);
        RadioButton rbAdmin = view.findViewById(R.id.rbAdmin);
        RadioButton rbUser = view.findViewById(R.id.rbUser);

        tvMemberName.setText(userName);
        tvMemberEmail.setText(userEmail);

        if (Roles.ADMIN.equals(currentRole)) {
            rbAdmin.setChecked(true);
        } else {
            rbUser.setChecked(true);
        }
    }

    private void setupListeners(View view) {
        MaterialCardView cardAdmin = view.findViewById(R.id.cardAdmin);
        MaterialCardView cardUser = view.findViewById(R.id.cardUser);
        RadioButton rbAdmin = view.findViewById(R.id.rbAdmin);
        RadioButton rbUser = view.findViewById(R.id.rbUser);
        MaterialButton btnSaveRole = view.findViewById(R.id.btnSaveRole);
        MaterialButton btnDeleteMember = view.findViewById(R.id.btnDeleteMember);

        cardAdmin.setOnClickListener(v -> rbAdmin.setChecked(true));
        cardUser.setOnClickListener(v -> rbUser.setChecked(true));


        btnSaveRole.setOnClickListener(v -> {
            String newRole = rbAdmin.isChecked() ? Roles.ADMIN : Roles.USER;
            if (listener != null) {
                // Notifica a la Activity que el rol debe cambiar
                listener.onRoleChanged(userId, newRole);
            }
            dismiss(); // Cierra el BottomSheet
        });

        btnDeleteMember.setOnClickListener(v -> {
            if (listener != null) {
                // Notifica a la Activity que el miembro debe ser eliminado
                listener.onMemberDeleted(userId);
            }
            dismiss(); // Cierra el BottomSheet
        });
    }

    /**
     * Permite que la Activity/Fragment que lo llama se registre como "oyente".
     */
    public void setOnRoleChangeListener(OnRoleChangeListener listener) {
        this.listener = listener;
    }
}