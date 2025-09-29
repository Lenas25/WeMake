package com.utp.wemake;

import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.Task;
import com.utp.wemake.viewmodels.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {

    private RecyclerView kanbanBoardRecycler;
    private ColumnAdapter columnAdapter;
    private List<KanbanColumn> columns = new ArrayList<>();
    private ProfileViewModel viewModel;
    private ShapeableImageView profileAvatar;
    private TextView profileName;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        profileAvatar = view.findViewById(R.id.avatar);
        profileName = view.findViewById(R.id.name);

        // El onViewCreated ahora es un resumen claro de lo que se configura.
        setupViews(view);
        setupListeners(view);
    }

    /**
     * Configura las vistas principales y carga los datos.
     */
    private void setupViews(View view) {
        setupObservers();
        setupSummaryCards(view);
        setupKanbanBoard(view);
    }

    /**
     * Configura los listeners para las interacciones del usuario.
     */
    private void setupListeners(View view) {
        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BoardSettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupObservers() {
        // Observa los datos del usuario
        viewModel.getUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String firstName = user.getName().split(" ")[0];
                profileName.setText(firstName);

                // Carga la imagen de perfil con una librería como Glide o Coil
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(profileAvatar);
            }
        });
        // Observa los mensajes de error
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            String userName = mainActivity.getUserName();
            if (userName != null && !userName.isEmpty()) {
                String firstName = userName.split(" ")[0];
                profileName.setText(firstName);
            } else {
                profileName.setText("Usuario");
            }
        } else {
            profileName.setText("Usuario");
        }
    }

    /**
     * Asigna los datos a las tarjetas de resumen.
     */
    private void setupSummaryCards(View view) {
        // --- Tarjeta de Tareas ---
        MaterialCardView cardTareas = view.findViewById(R.id.card_tareas);
        ImageView iconTareas = cardTareas.findViewById(R.id.summary_icon);
        TextView valueTareas = cardTareas.findViewById(R.id.summary_value);
        TextView labelTareas = cardTareas.findViewById(R.id.summary_label);
        iconTareas.setImageResource(R.drawable.ic_tasks);
        valueTareas.setText("12");
        labelTareas.setText("tareas");

        // --- Tarjeta de Pendientes ---
        MaterialCardView cardPendientes = view.findViewById(R.id.card_pendientes);
        ImageView iconPendientes = cardPendientes.findViewById(R.id.summary_icon);
        TextView valuePendientes = cardPendientes.findViewById(R.id.summary_value);
        TextView labelPendientes = cardPendientes.findViewById(R.id.summary_label);
        iconPendientes.setImageResource(R.drawable.ic_pending);
        valuePendientes.setText("4");
        labelPendientes.setText("pendientes");

        // --- Tarjeta de Vencidos ---
        MaterialCardView cardVencidos = view.findViewById(R.id.card_vencidos);
        ImageView iconVencidos = cardVencidos.findViewById(R.id.summary_icon);
        TextView valueVencidos = cardVencidos.findViewById(R.id.summary_value);
        TextView labelVencidos = cardVencidos.findViewById(R.id.summary_label);
        iconVencidos.setImageResource(R.drawable.ic_expired);
        valueVencidos.setText("5");
        labelVencidos.setText("vencidos");

        // --- Tarjeta de Puntos ---
        MaterialCardView cardPuntos = view.findViewById(R.id.card_puntos);
        ImageView iconPuntos = cardPuntos.findViewById(R.id.summary_icon);
        TextView valuePuntos = cardPuntos.findViewById(R.id.summary_value);
        TextView labelPuntos = cardPuntos.findViewById(R.id.summary_label);
        iconPuntos.setImageResource(R.drawable.ic_rocket);
        valuePuntos.setText("150");
        labelPuntos.setText("puntos");
    }

    /**
     * Configura el RecyclerView principal del tablero Kanban.
     */
    private void setupKanbanBoard(View view) {
        kanbanBoardRecycler = view.findViewById(R.id.recycler_kanban_board);
        kanbanBoardRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        columns.clear();

        List<Task> pendingTasks = getSampleTasks("Pendiente");
        List<Task> inProgressTasks = getSampleTasks("En Progreso");
        List<Task> doneTasks = getSampleTasks("Completado");

        columns.add(new KanbanColumn("Pendiente", pendingTasks));
        columns.add(new KanbanColumn("En Progreso", inProgressTasks));
        columns.add(new KanbanColumn("Completado", doneTasks));

        columnAdapter = new ColumnAdapter(columns, this);
        kanbanBoardRecycler.setAdapter(columnAdapter);
    }

    /**
     * Genera una lista de tareas de ejemplo.
     */
    private List<Task> getSampleTasks(String estado) {
        List<Task> list = new ArrayList<>();
        list.add(new Task("Reunión semanal", "Definir pendientes", "Elena"));
        list.add(new Task("Revisión de código", "Pull request módulo test", "Mario"));
        list.add(new Task("Diseño UI", "Pantalla Estadisticas", "Carlos"));
        return list;
    }

    /**
     * Listener que se activa cuando se hace clic en el botón de cambiar estado de una tarea.
     */
    @Override
    public void onChangeStatusClicked(Task task) {
        ChangeStatusBottomSheet bottomSheet = ChangeStatusBottomSheet.newInstance(
                task.getId(), task.getEstado()
        );
        bottomSheet.show(getChildFragmentManager(), "ChangeStatusBottomSheetTag");
    }
}