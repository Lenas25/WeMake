package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // No necesitas los parámetros de la plantilla, así que los hemos eliminado.
    // Este es un constructor vacío requerido.
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Esta línea infla (crea) tu layout XML y lo convierte en una vista de Java.
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // A partir de aquí, el layout ya existe.
        // Este es el lugar perfecto para encontrar tus vistas y configurarles datos.

        setupSummaryCards(view);
        setupRecyclerViews(view);
    }

    /**
     * Encuentra cada tarjeta de resumen y le asigna los datos correspondientes.
     * @param view La vista raíz del fragmento (pasada desde onViewCreated).
     */
    private void setupSummaryCards(View view) {
        // --- Tarjeta de Tareas ---
        MaterialCardView cardTareas = view.findViewById(R.id.card_tareas);
        ImageView iconTareas = cardTareas.findViewById(R.id.summary_icon);
        TextView valueTareas = cardTareas.findViewById(R.id.summary_value);
        TextView labelTareas = cardTareas.findViewById(R.id.summary_label);

        iconTareas.setImageResource(R.drawable.ic_tasks); // Asegúrate de tener este icono
        valueTareas.setText("12");
        labelTareas.setText("tareas");

        // --- Tarjeta de Pendientes ---
        MaterialCardView cardPendientes = view.findViewById(R.id.card_pendientes);
        ImageView iconPendientes = cardPendientes.findViewById(R.id.summary_icon);
        TextView valuePendientes = cardPendientes.findViewById(R.id.summary_value);
        TextView labelPendientes = cardPendientes.findViewById(R.id.summary_label);

        iconPendientes.setImageResource(R.drawable.ic_pending); // Crea este icono si no existe
        valuePendientes.setText("4");
        labelPendientes.setText("pendientes");

        // --- Tarjeta de Vencidos ---
        MaterialCardView cardVencidos = view.findViewById(R.id.card_vencidos);
        ImageView iconVencidos = cardVencidos.findViewById(R.id.summary_icon);
        TextView valueVencidos = cardVencidos.findViewById(R.id.summary_value);
        TextView labelVencidos = cardVencidos.findViewById(R.id.summary_label);

        iconVencidos.setImageResource(R.drawable.ic_expired); // Crea este icono si no existe
        valueVencidos.setText("5");
        labelVencidos.setText("vencidos");

        // --- Tarjeta de Puntos ---
        MaterialCardView cardPuntos = view.findViewById(R.id.card_puntos);
        ImageView iconPuntos = cardPuntos.findViewById(R.id.summary_icon);
        TextView valuePuntos = cardPuntos.findViewById(R.id.summary_value);
        TextView labelPuntos = cardPuntos.findViewById(R.id.summary_label);

        iconPuntos.setImageResource(R.drawable.ic_rocket); // Crea este icono si no existe
        valuePuntos.setText("150");
        labelPuntos.setText("puntos");
    }

    /**
     * Configura los RecyclerViews para las listas de tareas.
     * @param view La vista raíz del fragmento.
     */
    private void setupRecyclerViews(View view) {
        // Configuración para la lista de tareas pendientes
        RecyclerView recyclerViewPending = view.findViewById(R.id.recycler_pending);
        recyclerViewPending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // TODO: Crea y asigna un Adapter al recyclerViewPending con los datos de las tareas.
        recyclerViewPending.setAdapter(new TaskAdapter(getSampleTasks("Pendiente")));

        // Configuración para la lista de tareas en progreso
        RecyclerView recyclerViewInProgress = view.findViewById(R.id.recycler_in_progress);
        recyclerViewInProgress.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // TODO: Crea y asigna un Adapter al recyclerViewInProgress con los datos de las tareas.
        recyclerViewInProgress.setAdapter(new TaskAdapter(getSampleTasks("En Progreso")));

        // Configuración para la lista de tareas completadas
        RecyclerView recyclerViewDone = view.findViewById(R.id.recycler_done);
        recyclerViewDone.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewDone.setAdapter(new TaskAdapter(getSampleTasks("Completada")));
    }

    private List<Task> getSampleTasks(String estado) {
        List<Task> list = new ArrayList<>();
        list.add(new Task("Reunión semanal", "Definir pendientes", "Elena"));
        list.add(new Task("Revisión de código", "Pull request módulo test", "Mario"));
        list.add(new Task("Diseño UI", "Pantalla Estadisticas", "Carlos"));
        return list;
    }
}