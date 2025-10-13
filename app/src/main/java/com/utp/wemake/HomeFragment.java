package com.utp.wemake;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.utp.wemake.models.Board;
import com.utp.wemake.models.KanbanColumn;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.viewmodels.HomeViewModel;
import com.utp.wemake.viewmodels.MainViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {

    private HomeViewModel homeViewModel;
    private RecyclerView kanbanBoardRecycler;
    private ColumnAdapter columnAdapter;
    private List<KanbanColumn> columns = new ArrayList<>();
    private ShapeableImageView profileAvatar;
    private TextView profileName;
    private MainViewModel mainViewModel;
    private AutoCompleteTextView dropdownText;
    private View emptyStateLayout;

    // Flag para evitar múltiples cargas
    private boolean isLoadingData = false;
    private String currentBoardId = null;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners(view);
        setupObservers();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Solo recargar si hay un tablero seleccionado y no estamos cargando
        Board selectedBoard = mainViewModel.getSelectedBoard().getValue();
        if (selectedBoard != null && !isLoadingData) {
            String boardId = selectedBoard.getId();
            // Siempre recargar datos en onResume para capturar nuevas tareas
            Log.d("HomeFragment", "Recargando datos para tablero: " + selectedBoard.getName());
            loadBoardDataSafely(boardId);
        }
    }

    /**
     * Carga datos de forma segura evitando múltiples llamadas simultáneas
     */
    private void loadBoardDataSafely(String boardId) {
        if (isLoadingData) {
            Log.d("HomeFragment", "Ya se está cargando datos, ignorando llamada duplicada");
            return;
        }

        isLoadingData = true;
        currentBoardId = boardId;
        homeViewModel.loadBoardData(boardId);
    }

    /**
     * Vincula las variables de la clase con las vistas del layout.
     */
    private void initializeViews(View view) {
        profileAvatar = view.findViewById(R.id.avatar);
        profileName = view.findViewById(R.id.name);
        dropdownText = view.findViewById(R.id.dropdown_text);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);

        // Inicializar el RecyclerView del Kanban
        kanbanBoardRecycler = view.findViewById(R.id.recycler_kanban_board);
        kanbanBoardRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        setupSummaryCards(view);
    }

    /**
     * Configura los listeners para las interacciones del usuario.
     */
    private void setupListeners(View view) {
        ImageButton settingsButton = view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            // Pasamos el ID del tablero seleccionado a la pantalla de configuración
            Board currentBoard = mainViewModel.getSelectedBoard().getValue();
            if (currentBoard != null) {
                Intent intent = new Intent(requireContext(), BoardSettingsActivity.class);
                intent.putExtra("boardId", currentBoard.getId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Por favor, selecciona un tablero primero.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupObservers() {
        // Observador para los datos del USUARIO (nombre y foto)
        mainViewModel.getCurrentUserData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                if (user.getName() != null) {
                    profileName.setText(user.getName().split(" ")[0]);
                }
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(profileAvatar);
            }
        });

        // Observador para la LISTA DE TABLEROS (puebla el dropdown)
        mainViewModel.getUserBoards().observe(getViewLifecycleOwner(), boards -> {
            if (boards != null && !boards.isEmpty()) {
                // Creamos una lista de solo los nombres de los tableros
                List<String> boardNames = boards.stream().map(Board::getName).collect(Collectors.toList());

                // Creamos un adaptador para el dropdown
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, boardNames);
                dropdownText.setAdapter(adapter);

                // Configuramos el listener para cuando el usuario hace clic en un ítem
                dropdownText.setOnItemClickListener((parent, view, position, id) -> {
                    Board selected = boards.get(position);
                    // Notificamos al ViewModel que el usuario ha seleccionado un nuevo tablero
                    mainViewModel.selectBoard(selected);
                });
            }
        });

        // Observador para el TABLERO SELECCIONADO (reacciona al cambio)
        mainViewModel.getSelectedBoard().observe(getViewLifecycleOwner(), selectedBoard -> {
            if (selectedBoard != null) {
                // 1. Actualiza el texto del dropdown para que muestre el tablero activo
                dropdownText.setText(selectedBoard.getName(), false); // 'false' para no disparar el listener de nuevo

                // 2. MUESTRA EN CONSOLA EL CAMBIO
                Log.d("HomeFragment", "Tablero cambiado a: " + selectedBoard.getName() + " (ID: " + selectedBoard.getId() + ")");

                // Llamando al HomeViewModel para cargar los datos del tablero seleccionado
                //homeViewModel.loadBoardData(selectedBoard.getId());

                // Solo cargar si es un tablero diferente
                if (!selectedBoard.getId().equals(currentBoardId)) {
                    loadBoardDataSafely(selectedBoard.getId());
                }
            }
        });

        // Observadores del HomeViewModel
        homeViewModel.getKanbanColumns().observe(getViewLifecycleOwner(), columns -> {
            if (columns != null && kanbanBoardRecycler != null) {
                columnAdapter = new ColumnAdapter(columns, this);
                kanbanBoardRecycler.setAdapter(columnAdapter);
            }
        });

        homeViewModel.getTotalTasks().observe(getViewLifecycleOwner(), total -> {
            if (total != null && getView() != null) {
                TextView valueTareas = getView().findViewById(R.id.card_tareas).findViewById(R.id.summary_value);
                valueTareas.setText(String.valueOf(total));
                if (total == 0) {
                    // Si no hay tareas, oculta el RecyclerView y muestra el layout de estado vacío.
                    kanbanBoardRecycler.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                } else {
                    // Si hay una o más tareas, muestra el RecyclerView y oculta el estado vacío.
                    kanbanBoardRecycler.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                }
            }
        });

        homeViewModel.getPendingTasks().observe(getViewLifecycleOwner(), pending -> {
            if (pending != null && getView() != null) {
                TextView valuePendientes = getView().findViewById(R.id.card_pendientes).findViewById(R.id.summary_value);
                valuePendientes.setText(String.valueOf(pending));
            }
        });

        homeViewModel.getExpiredTasks().observe(getViewLifecycleOwner(), expired -> {
            if (expired != null && getView() != null) {
                TextView valueVencidos = getView().findViewById(R.id.card_vencidos).findViewById(R.id.summary_value);
                valueVencidos.setText(String.valueOf(expired));
            }
        });

        homeViewModel.getTotalPoints().observe(getViewLifecycleOwner(), points -> {
            if (points != null && getView() != null) {
                TextView valuePuntos = getView().findViewById(R.id.card_puntos).findViewById(R.id.summary_value);
                valuePuntos.setText(String.valueOf(points));
            }
        });

        homeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Observador para loading
        homeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                Log.d("HomeFragment", "Cargando datos del tablero...");
            } else {
                // Cuando termina de cargar, resetear el flag
                isLoadingData = false;
                Log.d("HomeFragment", "Carga de datos completada");
            }
        });
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
        valueTareas.setText("0"); // Valor inicial
        labelTareas.setText("tareas");

        // --- Tarjeta de Pendientes ---
        MaterialCardView cardPendientes = view.findViewById(R.id.card_pendientes);
        ImageView iconPendientes = cardPendientes.findViewById(R.id.summary_icon);
        TextView valuePendientes = cardPendientes.findViewById(R.id.summary_value);
        TextView labelPendientes = cardPendientes.findViewById(R.id.summary_label);
        iconPendientes.setImageResource(R.drawable.ic_pending);
        valuePendientes.setText("0"); // Valor inicial
        labelPendientes.setText("pendientes");

        // --- Tarjeta de Vencidos ---
        MaterialCardView cardVencidos = view.findViewById(R.id.card_vencidos);
        ImageView iconVencidos = cardVencidos.findViewById(R.id.summary_icon);
        TextView valueVencidos = cardVencidos.findViewById(R.id.summary_value);
        TextView labelVencidos = cardVencidos.findViewById(R.id.summary_label);
        iconVencidos.setImageResource(R.drawable.ic_expired);
        valueVencidos.setText("0"); // Valor inicial
        labelVencidos.setText("vencidos");

        // --- Tarjeta de Puntos ---
        MaterialCardView cardPuntos = view.findViewById(R.id.card_puntos);
        ImageView iconPuntos = cardPuntos.findViewById(R.id.summary_icon);
        TextView valuePuntos = cardPuntos.findViewById(R.id.summary_value);
        TextView labelPuntos = cardPuntos.findViewById(R.id.summary_label);
        iconPuntos.setImageResource(R.drawable.ic_rocket);
        valuePuntos.setText("0"); // Valor inicial
        labelPuntos.setText("puntos");
    }

    /**
     * Listener que se activa cuando se hace clic en el botón de cambiar estado de una tarea.
     */
    @Override
    public void onChangeStatusClicked(TaskModel task) {
        ChangeStatusBottomSheet bottomSheet = ChangeStatusBottomSheet.newInstance(
                task.getId(), task.getStatus()
        );
        bottomSheet.show(getChildFragmentManager(), "ChangeStatusBottomSheetTag");
    }
}