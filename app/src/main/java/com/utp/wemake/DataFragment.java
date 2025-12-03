package com.utp.wemake;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.utp.wemake.dto.DashboardResponse;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.viewmodels.DashboardViewModel;
import com.utp.wemake.viewmodels.MainViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataFragment extends Fragment {

    // ViewModels
    private DashboardViewModel dashboardViewModel;
    private MainViewModel mainViewModel; // Para obtener el boardId seleccionado

    // Vistas
    private ProgressBar progressBar;
    private View contentScrollView;
    private TextView tvMessage;
    private TextView tvTotalTasks, tvOnTimeRate;
    private BarChart barChart;
    private PieChart pieChart;
    private RecyclerView rvAtRiskTasks, rvLeaderboard;

    public DataFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupToolbar(view);
        observeViewModels();
    }

    private void initializeViews(@NonNull View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        contentScrollView = view.findViewById(R.id.content_scroll_view);
        tvMessage = view.findViewById(R.id.tv_message);
        tvTotalTasks = view.findViewById(R.id.tv_total_tasks);
        tvOnTimeRate = view.findViewById(R.id.tv_on_time_rate);
        barChart = view.findViewById(R.id.bar_chart_tasks_per_week);
        pieChart = view.findViewById(R.id.pie_chart_priority);
        rvAtRiskTasks = view.findViewById(R.id.rv_at_risk_tasks);
        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.item_stats);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });
    }

    private void observeViewModels() {
        dashboardViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                contentScrollView.setVisibility(View.GONE);
                tvMessage.setVisibility(View.GONE);
            }
        });

        dashboardViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                contentScrollView.setVisibility(View.GONE);
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(error);
            } else {
                tvMessage.setVisibility(View.GONE);
            }
        });

        dashboardViewModel.getDashboardData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                contentScrollView.setVisibility(View.VISIBLE);
                populateUI(data);
            }
        });

        // --- OBSERVADOR DEL MAIN VIEWMODEL ---
        // Escucha cambios en el tablero seleccionado para recargar los datos
        mainViewModel.getSelectedBoard().observe(getViewLifecycleOwner(), selectedBoard -> {
            if (selectedBoard != null && selectedBoard.getId() != null) {
                // Cuando el tablero cambia, le pedimos al DashboardViewModel que cargue los nuevos datos
                dashboardViewModel.loadDashboardData(selectedBoard.getId());
            } else {
                // Manejar el caso donde no hay ningún tablero seleccionado
                tvMessage.setText("Por favor, selecciona un tablero para ver los datos.");
                tvMessage.setVisibility(View.VISIBLE);
                contentScrollView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }
        });

        dashboardViewModel.getLeaderboard().observe(getViewLifecycleOwner(), leaderboard -> {
            if (leaderboard != null) {
                rvLeaderboard.setAdapter(new LeaderboardAdapter(leaderboard));
            }
        });
    }

    /**
     * MÉTODO CORREGIDO: populateUI()
     * Ahora incluye validaciones null-safe para evitar crashes
     */
    private void populateUI(DashboardResponse data) {
        // Validar que data no sea null
        if (data == null) {
            showEmptyState("No hay datos disponibles");
            return;
        }

        // Validar summary
        if (data.summary == null) {
            showEmptyState("No hay resumen disponible");
            return;
        }

        // Validar productivity
        if (data.productivity == null) {
            showEmptyState("No hay datos de productividad");
            return;
        }

        // Popular tarjetas de resumen de forma segura
        tvTotalTasks.setText(String.valueOf(data.summary.totalTasks));

        // Evitar división por cero y valores inválidos
        double onTimeRate = data.productivity.onTimeCompletionRate;
        if (Double.isNaN(onTimeRate) || Double.isInfinite(onTimeRate)) {
            onTimeRate = 0.0;
        }
        tvOnTimeRate.setText(String.format(Locale.US, "%.1f%%", onTimeRate));

        // Popular gráficos con validación
        setupBarChart(data.productivity.tasksCompletedPerWeek);
        setupPieChart(data.productivity.priorityDistribution);

        // Popular lista de tareas en riesgo
        if (data.predictions != null && data.predictions.atRiskTasks != null && !data.predictions.atRiskTasks.isEmpty()) {
            rvAtRiskTasks.setAdapter(new AtRiskTasksAdapter(data.predictions.atRiskTasks));
            rvAtRiskTasks.setVisibility(View.VISIBLE);
        } else {
            rvAtRiskTasks.setVisibility(View.GONE);
        }
    }

    /**
     * Muestra un mensaje cuando no hay datos disponibles
     */
    private void showEmptyState(String message) {
        contentScrollView.setVisibility(View.GONE);
        tvMessage.setVisibility(View.VISIBLE);
        tvMessage.setText(message);
        progressBar.setVisibility(View.GONE);
    }

    /**
     * MÉTODO CORREGIDO: setupBarChart()
     * Ahora maneja correctamente datos vacíos o nulos
     */
    private void setupBarChart(Map<String, Integer> data) {
        if (data == null || data.isEmpty()) {
            barChart.setVisibility(View.GONE);
            return;
        }
        barChart.setVisibility(View.VISIBLE);

        int textColor = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(data.keySet());
        Collections.sort(labels);

        List<String> sortedWeeks = new ArrayList<>(data.keySet());
        Collections.sort(sortedWeeks);
 
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            Integer value = data.get(label);
            // Validar que el valor no sea null
            entries.add(new BarEntry(i, value != null ? value : 0));
        }

        // Validar que tengamos al menos una entrada
        if (entries.isEmpty()) {
            barChart.setVisibility(View.GONE);
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tareas Completadas");
        dataSet.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.chart_color_1),
                ContextCompat.getColor(requireContext(), R.color.chart_color_2),
                ContextCompat.getColor(requireContext(), R.color.chart_color_3),
                ContextCompat.getColor(requireContext(), R.color.chart_color_4)
        });
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);

        // Eje X (inferior)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColor);
        xAxis.setGranularity(1f);

        // Eje Y (izquierdo)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true); // Líneas de guía horizontales
        leftAxis.setGridColor(Color.parseColor("#40FFFFFF")); // Guías semitransparentes
        leftAxis.setTextColor(textColor);
        leftAxis.setAxisMinimum(0f); // Empezar siempre en 0

        // Eje Y (derecho)
        barChart.getAxisRight().setEnabled(false);

        barChart.animateY(1000);
        barChart.invalidate(); // Refrescar
    }

    private void setupPieChart(Map<String, Integer> data) {
        if (data == null || data.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            return;
        }
        pieChart.setVisibility(View.VISIBLE);

        int textColor = ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface);
        int backgroundColor = ContextCompat.getColor(requireContext(), R.color.md_theme_surface);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        // Validar y agregar entradas solo si tienen valores mayores a 0
        Integer highValue = data.get("high");
        if (highValue != null && highValue > 0) {
            entries.add(new PieEntry(highValue, "Alta"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.priority_high_border));
        }

        Integer mediumValue = data.get("medium");
        if (mediumValue != null && mediumValue > 0) {
            entries.add(new PieEntry(mediumValue, "Media"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.priority_medium_border));
        }

        Integer lowValue = data.get("low");
        if (lowValue != null && lowValue > 0) {
            entries.add(new PieEntry(lowValue, "Baja"));
            colors.add(ContextCompat.getColor(requireContext(), R.color.priority_low_border));
        }

        // Si no hay entradas, ocultar el gráfico
        if (entries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(textColor);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f); // Pequeño espacio entre las rebanadas

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart)); // Mostrar como porcentajes

        pieChart.setData(pieData);

        // Personalización de Estilo
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextColor(textColor); // Color de la leyenda
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(backgroundColor); // Color del agujero igual al fondo
        pieChart.setCenterText("Prioridad");
        pieChart.setCenterTextColor(textColor);
        pieChart.setEntryLabelColor(textColor);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}