package com.utp.wemake;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private List<Date> dates;
    private int selectedPosition = 0;
    private OnDateClickListener listener;
    private List<Date> datesWithTasks; // Fechas que tienen tareas

    private SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", Locale.US);
    private SimpleDateFormat dayNumberFormat = new SimpleDateFormat("dd", Locale.getDefault());

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public DateAdapter(List<Date> dates, OnDateClickListener listener) {
        this.dates = dates;
        this.listener = listener;
        this.datesWithTasks = new java.util.ArrayList<>();

        // Buscar el día de hoy para seleccionarlo por defecto
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < dates.size(); i++) {
            if (isSameDay(dates.get(i), today.getTime())) {
                selectedPosition = i;
                break;
            }
        }
    }

    public void setDatesWithTasks(List<Date> datesWithTasks) {
        this.datesWithTasks = datesWithTasks != null ? datesWithTasks : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        Date date = dates.get(position);
        boolean hasTasks = datesWithTasks.contains(date) ||
                datesWithTasks.stream().anyMatch(d -> isSameDay(d, date));

        holder.bind(date, position == selectedPosition, hasTasks);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public void setSelectedDate(Date date) {
        int previousPosition = selectedPosition;
        for (int i = 0; i < dates.size(); i++) {
            if (isSameDay(dates.get(i), date)) {
                selectedPosition = i;
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDayNumber;
        View viewHasTasks;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            viewHasTasks = itemView.findViewById(R.id.view_has_tasks);
        }

        void bind(Date date, boolean isSelected, boolean hasTasks) {
            tvDayName.setText(dayNameFormat.format(date).toUpperCase());
            tvDayNumber.setText(dayNumberFormat.format(date));

            // Indicador de tareas
            viewHasTasks.setVisibility(hasTasks ? View.VISIBLE : View.GONE);

            if (isSelected) {
                // Estilo Seleccionado: Fondo blanco, texto azul
                tvDayNumber.setBackgroundResource(R.drawable.bg_date_selected);
                tvDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_primary));
                tvDayName.setAlpha(1.0f);
                tvDayName.setTextColor(Color.WHITE);
            } else {
                // Estilo No Seleccionado: Sin fondo, texto blanco
                tvDayNumber.setBackground(null);
                tvDayNumber.setTextColor(Color.WHITE);
                tvDayName.setAlpha(0.6f);
                tvDayName.setTextColor(Color.WHITE);
            }

            itemView.setOnClickListener(v -> {
                int previousItem = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(previousItem);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onDateClick(date);
                }
            });
        }
    }
}