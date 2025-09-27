package com.utp.wemake;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {

    private RecyclerView kanbanBoardRecycler;
    private ColumnAdapter columnAdapter;
    private List<KanbanColumn> columns = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // El onViewCreated ahora es un resumen claro de lo que se configura.
        setupViews(view);
        setupListeners(view);
    }

    /**
     * Configura las vistas principales y carga los datos.
     */
    private void setupViews(View view) {
        setupUserName(view);
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

    /**
     * Configura el nombre del usuario en la interfaz.
     */
    private void setupUserName(View view) {
        TextView nameTextView = view.findViewById(R.id.name);
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            String userName = mainActivity.getUserName();
            if (userName != null && !userName.isEmpty()) {
                String firstName = userName.split(" ")[0];
                nameTextView.setText(firstName);
            } else {
                nameTextView.setText("Usuario");
            }
        } else {
            nameTextView.setText("Usuario");
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