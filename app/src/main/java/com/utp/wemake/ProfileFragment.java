    package com.utp.wemake;

    import android.content.Intent;
    import android.os.Bundle;
    import android.text.InputType;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.EditText;
    import android.widget.FrameLayout;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.fragment.app.Fragment;
    import androidx.lifecycle.ViewModelProvider;
    import com.bumptech.glide.Glide;
    import com.google.android.material.appbar.MaterialToolbar;
    import com.google.android.material.chip.Chip;
    import com.google.android.material.dialog.MaterialAlertDialogBuilder;
    import com.google.android.material.imageview.ShapeableImageView;
    import com.google.android.material.materialswitch.MaterialSwitch;
    import com.utp.wemake.auth.FirebaseAuthHelper;
    import com.utp.wemake.models.User;
    import com.utp.wemake.utils.NotificationPrefs;
    import com.utp.wemake.viewmodels.MainViewModel;
    import com.utp.wemake.viewmodels.ProfileViewModel;

    public class ProfileFragment extends Fragment {

        // --- ViewModels ---
        private ProfileViewModel profileViewModel; // Para lógica específica del perfil (puntos, subir foto)
        private MainViewModel mainViewModel;       // Para datos globales (usuario, tablero seleccionado)

        // --- Vistas de la UI ---
        private ShapeableImageView profileAvatar;
        private ProgressBar progressBar;
        private TextView profileName, profileEmail;
        private Chip profileCoinsChip;
        private MaterialSwitch notificationSwitch;

        private FirebaseAuthHelper auth;

        // --- Utilidades ---
        private NotificationPrefs notificationPrefs;

        // --- Launcher para seleccionar imagen ---
        private final ActivityResultLauncher<String> selectImageLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        User currentUser = mainViewModel.getCurrentUserData().getValue();
                        profileViewModel.updateUserProfilePicture(uri, currentUser);
                    }
                });

        public ProfileFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            notificationPrefs = new NotificationPrefs(requireContext());

            // Inicializa ambos ViewModels
            profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
            mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

            auth = new FirebaseAuthHelper(requireActivity(), new FirebaseAuthHelper.AuthCallback() {
                @Override
                public void onSuccess(String userName, String userEmail, boolean isRegistration) {
                }
                @Override
                public void onError(String errorMessage) {
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_profile, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            initializeViews(view);
            setupToolbar(view);
            setupOptions(view);
            setupListeners(view);
            setupObservers();
        }

        private void initializeViews(View view) {
            profileAvatar = view.findViewById(R.id.profile_avatar);
            profileName = view.findViewById(R.id.profile_name);
            profileEmail = view.findViewById(R.id.profile_email);
            profileCoinsChip = view.findViewById(R.id.profile_coins_chip);
            progressBar = view.findViewById(R.id.progress_bar);
            notificationSwitch = view.findViewById(R.id.option_notifications).findViewById(R.id.list_item_switch);

        }

        private void setupToolbar(View view) {
            MaterialToolbar toolbar = view.findViewById(R.id.top_app_bar);
            toolbar.setTitle(R.string.item_profile);
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        /**
         * Rellena los textos estáticos de las opciones.
         */
        private void setupOptions(View view) {
            // Configuración de "Editar Perfil"
            View editProfileView = view.findViewById(R.id.option_edit_profile);
            ((TextView) editProfileView.findViewById(R.id.list_item_title)).setText(R.string.option_edit_profile);
            ((TextView) editProfileView.findViewById(R.id.list_item_subtitle)).setText(R.string.detail_edit_profile);
            ((ImageView) editProfileView.findViewById(R.id.list_item_icon)).setImageResource(R.drawable.ic_profile);

            // Configuración de "Agregar Tablero"
            View addBoardView = view.findViewById(R.id.option_new_board);
            ((TextView) addBoardView.findViewById(R.id.list_item_title)).setText(R.string.option_new_board);
            ((TextView) addBoardView.findViewById(R.id.list_item_subtitle)).setText(R.string.detail_new_board);
            ((ImageView) addBoardView.findViewById(R.id.list_item_icon)).setImageResource(R.drawable.ic_board);

            // Configuración de "Unirte Tablero"
            View joinBoardView = view.findViewById(R.id.option_join_board);
            ((TextView) joinBoardView.findViewById(R.id.list_item_title)).setText(R.string.option_join_board);
            ((TextView) joinBoardView.findViewById(R.id.list_item_subtitle)).setText(R.string.detail_join_board);
            ((ImageView) joinBoardView.findViewById(R.id.list_item_icon)).setImageResource(R.drawable.ic_teamwork);

            // Configuración de "Notificaciones"
            View notificationsView = view.findViewById(R.id.option_notifications);
            ((TextView) notificationsView.findViewById(R.id.list_item_title)).setText(R.string.option_notifications);
            ((TextView) notificationsView.findViewById(R.id.list_item_subtitle)).setText(R.string.detail_notifications);
            ((ImageView) notificationsView.findViewById(R.id.list_item_icon)).setImageResource(R.drawable.ic_notifications);

            // Configuración de "Cerrar Sesión"
            View signoutView = view.findViewById(R.id.option_sign_out);
            ((TextView) signoutView.findViewById(R.id.list_item_title)).setText(R.string.option_sign_out);
            ((ImageView) signoutView.findViewById(R.id.list_item_icon)).setImageResource(R.drawable.ic_sign_out);
            signoutView.findViewById(R.id.list_item_subtitle).setVisibility(View.GONE);
        }

        private void setupListeners(View view) {
            view.findViewById(R.id.button_edit_avatar).setOnClickListener(v -> selectImageLauncher.launch("image/*"));
            view.findViewById(R.id.option_edit_profile).setOnClickListener(v -> startActivity(new Intent(getContext(), EditProfileActivity.class)));
            view.findViewById(R.id.option_new_board).setOnClickListener(v -> startActivity(new Intent(getContext(), EditBoardActivity.class)));
            view.findViewById(R.id.option_join_board).setOnClickListener(v -> {
                showJoinBoardDialog();
            });
            view.findViewById(R.id.option_sign_out).setOnClickListener(v -> showLogoutConfirmationDialog());

            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    notificationPrefs.setNotificationsEnabled(isChecked);
                    profileViewModel.updateNotificationPreference(isChecked);
                    Toast.makeText(getContext(), "Notificaciones " + (isChecked ? "Activadas" : "Desactivadas"), Toast.LENGTH_SHORT).show();
                }
            });

        }

        private void setupObservers() {
            mainViewModel.getCurrentUserData().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    profileName.setText(user.getName());
                    profileEmail.setText(user.getEmail());
                    Glide.with(this).load(user.getPhotoUrl())
                            .placeholder(R.drawable.ic_default_avatar).circleCrop().into(profileAvatar);

                    boolean isEnabledFromFirestore = user.isNotificationsEnabled();
                    notificationSwitch.setChecked(isEnabledFromFirestore);
                    notificationPrefs.setNotificationsEnabled(isEnabledFromFirestore);
                }
            });

            profileViewModel.getProfilePictureUrlUpdated().observe(getViewLifecycleOwner(), newUrl -> {
                if (newUrl != null) {
                    mainViewModel.updateUserPhotoLocally(newUrl);
                    Toast.makeText(getContext(), "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                }
            });

            mainViewModel.getSelectedBoard().observe(getViewLifecycleOwner(), selectedBoard -> {
                if (selectedBoard != null) {
                    profileViewModel.loadMemberDetailsForBoard(selectedBoard.getId());
                } else {
                    profileCoinsChip.setText("0 coins");
                }
            });

            profileViewModel.getMemberDetails().observe(getViewLifecycleOwner(), memberDetails -> {
                if (memberDetails != null) {
                    profileCoinsChip.setText(memberDetails.getPoints() + " coins");
                } else {
                    profileCoinsChip.setText("0 coins");
                }
            });

            mainViewModel.getJoinBoardSuccess().observe(getViewLifecycleOwner(), success -> {
                if (success != null) {
                    if (success) {
                        Toast.makeText(getContext(), "¡Te has unido al tablero con éxito!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "No se pudo unir al tablero. Verifica el código.", Toast.LENGTH_LONG).show();
                    }
                }
            });

            profileViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                progressBar.setVisibility(isLoading != null && isLoading ? View.VISIBLE : View.GONE);
            });

            profileViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
                if (error != null && !error.isEmpty()) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
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
                        auth.signOut();
                        Intent intent = new Intent(getContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .show();
        }

        /**
         * Muestra un diálogo para que el usuario ingrese un código de invitación.
         */
        private void showJoinBoardDialog() {
            if (getContext() == null) return;

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

            builder.setTitle("Unirse a un Tablero");
            builder.setMessage("Ingresa el código de invitación de 6 caracteres.");


            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            input.setHint("A7B2C9");

            FrameLayout container = new FrameLayout(requireContext());
            FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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