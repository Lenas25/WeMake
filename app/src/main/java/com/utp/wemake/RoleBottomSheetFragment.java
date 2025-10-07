package com.utp.wemake;

import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.annotation.AttrRes;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.constants.Roles;

public class RoleBottomSheetFragment extends BottomSheetDialogFragment {

    // Argumentos que recibiremos
    private String userId;
    private String userName;
    private String userEmail;;
    private String photoUrl;
    private String currentRole;


    private RadioButton rbAdmin;
    private RadioButton rbUser;
    private MaterialCardView cardAdmin;
    private MaterialCardView cardUser;
    private ImageView iconAdmin;
    private ImageView iconUser;

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
    public static RoleBottomSheetFragment newInstance(String userId, String userName, String userEmail, String photoUrl, String currentRole) {
        RoleBottomSheetFragment fragment = new RoleBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("userName", userName);
        args.putString("userEmail", userEmail);
        args.putString("photoUrl", photoUrl);
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
            photoUrl = getArguments().getString("photoUrl");
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

        ShapeableImageView imgAvatar = view.findViewById(R.id.imgMemberAvatar);
        TextView tvMemberName = view.findViewById(R.id.tvMemberName);
        TextView tvMemberEmail = view.findViewById(R.id.tvMemberEmail);
        cardAdmin = view.findViewById(R.id.cardAdmin);
        cardUser = view.findViewById(R.id.cardUser);
        iconAdmin = view.findViewById(R.id.iconAdmin);
        iconUser = view.findViewById(R.id.iconUser);
        rbAdmin = view.findViewById(R.id.rbAdmin);
        rbUser = view.findViewById(R.id.rbUser);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(view.getContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        tvMemberName.setText(userName);
        tvMemberEmail.setText(userEmail);

        updateSelectionUI(Roles.ADMIN.equals(currentRole));
    }

    private void setupListeners(View view) {
        MaterialButton btnSaveRole = view.findViewById(R.id.btnSaveRole);
        MaterialButton btnDeleteMember = view.findViewById(R.id.btnDeleteMember);

        cardAdmin.setOnClickListener(v -> updateSelectionUI(true));
        rbAdmin.setOnClickListener(v -> updateSelectionUI(true));
        iconAdmin.setOnClickListener(v -> updateSelectionUI(true));
        cardUser.setOnClickListener(v -> updateSelectionUI(false));
        rbUser.setOnClickListener(v -> updateSelectionUI(false));
        iconUser.setOnClickListener(v -> updateSelectionUI(false));

        // Listeners de los botones de acción (no cambian)
        btnSaveRole.setOnClickListener(v -> {
            String newRole = rbAdmin.isChecked() ? Roles.ADMIN : Roles.USER;
            if (listener != null) {
                listener.onRoleChanged(userId, newRole);
            }
            dismiss();
        });

        btnDeleteMember.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberDeleted(userId);
            }
            dismiss();
        });
    }

    private void updateSelectionUI(boolean isAdminSelected) {
        rbAdmin.setChecked(isAdminSelected);
        rbUser.setChecked(!isAdminSelected);

        int selectedStrokeWidth = (int) (2 * getResources().getDisplayMetrics().density);
        int unselectedStrokeWidth = (int) (1 * getResources().getDisplayMetrics().density);

        int colorPrimary = getThemeColor(R.attr.colorPrimary);
        int colorOutline = getThemeColor(R.attr.colorOutlineVariant);

        if (isAdminSelected) {
            cardAdmin.setStrokeWidth(selectedStrokeWidth);
            cardAdmin.setStrokeColor(colorPrimary);
            iconAdmin.setImageTintList(ColorStateList.valueOf(colorPrimary));
            cardUser.setStrokeWidth(unselectedStrokeWidth);
            cardUser.setStrokeColor(colorOutline);
            iconUser.setImageTintList(ColorStateList.valueOf(colorOutline));
        } else {
            cardAdmin.setStrokeWidth(unselectedStrokeWidth);
            cardAdmin.setStrokeColor(colorOutline);
            iconAdmin.setImageTintList(ColorStateList.valueOf(colorOutline));
            cardUser.setStrokeWidth(selectedStrokeWidth);
            cardUser.setStrokeColor(colorPrimary);
            iconUser.setImageTintList(ColorStateList.valueOf(colorPrimary));
        }
    }

    /**
     * Método auxiliar para resolver un atributo de color del tema actual.
     * @param colorAttr El atributo a resolver, por ejemplo, R.attr.colorPrimary
     * @return El valor del color entero.
     */
    private int getThemeColor(@AttrRes int colorAttr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(colorAttr, typedValue, true);
        return typedValue.data;
    }


    public void setOnRoleChangeListener(OnRoleChangeListener listener) {
    this.listener = listener;
}
}