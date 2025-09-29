package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.utp.wemake.utils.NotificationPrefs;
import com.utp.wemake.viewmodels.ProfileViewModel;

public class ProfileFragment extends Fragment {

    private NotificationPrefs notificationPrefs;
    private ProfileViewModel viewModel;
    private ShapeableImageView profileAvatar;
    private ProgressBar progressBar;
    private TextView profileName, profileEmail;

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.updateUserProfilePicture(uri);
                }
            });


    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializamos el helper aquí.
        notificationPrefs = new NotificationPrefs(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        progressBar = view.findViewById(R.id.progress_bar);

        // El método onViewCreated ahora es un resumen claro de lo que se configura.

        setupObservers();
        setupToolbar(view);
        setupOptions(view);
        setupListeners(view);
    }

    private void setupObservers() {
        // Observa los datos del usuario
        viewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                profileName.setText(user.getName());
                profileEmail.setText(user.getEmail());

                // Carga la imagen de perfil con una librería como Glide o Coil
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(profileAvatar);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observa los mensajes de error
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Configura la barra de navegación superior (Toolbar).
     */
    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.item_profile);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    /**
     * Rellena los datos de las opciones de la lista (texto e iconos).
     */
    private void setupOptions(View view) {
        // Contenedores de las opciones
        View editProfileView = view.findViewById(R.id.option_edit_profile);
        View notificationsView = view.findViewById(R.id.option_notifications);
        View signoutView = view.findViewById(R.id.option_sign_out);

        // Configuración de "Editar Perfil"
        TextView editProfileTitle = editProfileView.findViewById(R.id.list_item_title);
        TextView editProfileSubtitle = editProfileView.findViewById(R.id.list_item_subtitle);
        ImageView editProfileIcon = editProfileView.findViewById(R.id.list_item_icon);
        editProfileTitle.setText(R.string.option_edit_profile);
        editProfileSubtitle.setText(R.string.detail_edit_profile);
        editProfileIcon.setImageResource(R.drawable.ic_profile);

        // Configuración de "Notificaciones"
        TextView notificationsTitle = notificationsView.findViewById(R.id.list_item_title);
        TextView notificationsSubtitle = notificationsView.findViewById(R.id.list_item_subtitle);
        ImageView notificationsIcon = notificationsView.findViewById(R.id.list_item_icon);
        notificationsTitle.setText(R.string.option_notifications);
        notificationsSubtitle.setText(R.string.detail_notifications);
        notificationsIcon.setImageResource(R.drawable.ic_notifications);

        // Configuración de "Cerrar Sesión"
        TextView signoutTitle = signoutView.findViewById(R.id.list_item_title);
        TextView signoutSubtitle = signoutView.findViewById(R.id.list_item_subtitle);
        ImageView signoutIcon = signoutView.findViewById(R.id.list_item_icon);
        signoutTitle.setText(R.string.option_sign_out);
        signoutIcon.setImageResource(R.drawable.ic_sign_out);
        signoutSubtitle.setVisibility(View.GONE);
    }

    /**
     * Configura los listeners para las interacciones del usuario.
     */
    private void setupListeners(View view) {
        View editProfileView = view.findViewById(R.id.option_edit_profile);
        View notificationsView = view.findViewById(R.id.option_notifications);
        View signoutView = view.findViewById(R.id.option_sign_out);

        // Listener para el botón de editar avatar
        ImageButton editAvatarButton = view.findViewById(R.id.button_edit_avatar);
        editAvatarButton.setOnClickListener(v -> {
            // Lanza el selector de imágenes
            selectImageLauncher.launch("image/*");
        });

        // Listener para "Editar Perfil"
        editProfileView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        // Listener para el Switch de "Notificaciones"
        MaterialSwitch notificationSwitch = notificationsView.findViewById(R.id.list_item_switch);
        boolean isEnabled = notificationPrefs.areNotificationsEnabled();
        notificationSwitch.setChecked(isEnabled);

        // GUARDAR el nuevo valor cada vez que el usuario cambie el Switch.
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    notificationPrefs.setNotificationsEnabled(isChecked); // Guardamos la nueva preferencia

                    // Mostramos un mensaje de confirmación al usuario.
                    Toast.makeText(getContext(), "Notificaciones " + (isChecked ? "Activadas" : "Desactivadas"), Toast.LENGTH_SHORT).show();
        });

        // Listener para "Cerrar Sesión"
        signoutView.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    /**
     * Muestra un diálogo de confirmación para cerrar sesión.
     */
    private void showLogoutConfirmationDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle(getString(R.string.option_sign_out))
                .setMessage(getString(R.string.logout_dialog_message))
                .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.dialog_accept), (dialog, which) -> {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }
}