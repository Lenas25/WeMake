package com.utp.wemake;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    private SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", Locale.US);
    private SimpleDateFormat dayNumberFormat = new SimpleDateFormat("dd", Locale.getDefault());

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public DateAdapter(List<Date> dates, OnDateClickListener listener) {
        this.dates = dates;
        this.listener = listener;

        // Buscar el día de hoy para seleccionarlo por defecto
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < dates.size(); i++) {
            if (isSameDay(dates.get(i), today.getTime())) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        Date date = dates.get(position);

        holder.tvDayName.setText(dayNameFormat.format(date).toUpperCase());
        holder.tvDayNumber.setText(dayNumberFormat.format(date));

        if (selectedPosition == position) {
            // Estilo Seleccionado: Fondo blanco, texto azul
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_date_selected);
            holder.tvDayNumber.setTextColor(Color.parseColor("#7986CB")); // Mismo azul que el fondo
            holder.tvDayName.setAlpha(1.0f);
        } else {
            // Estilo No Seleccionado: Sin fondo, texto blanco
            holder.tvDayNumber.setBackground(null);
            holder.tvDayNumber.setTextColor(Color.WHITE);
            holder.tvDayName.setAlpha(0.6f);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousItem = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousItem);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onDateClick(date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance(); c1.setTime(d1);
        Calendar c2 = Calendar.getInstance(); c2.setTime(d2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDayNumber;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
        }
    }
}