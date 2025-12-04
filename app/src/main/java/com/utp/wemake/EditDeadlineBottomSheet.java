package com.utp.wemake;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.utp.wemake.viewmodels.CreateTaskViewModel; // O HomeViewModel según uses

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditDeadlineBottomSheet extends BottomSheetDialogFragment {

    private CreateTaskViewModel viewModel;
    private Calendar calendar = Calendar.getInstance();
    private TextView tvSelectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'de' MMM, yyyy", Locale.getDefault());

    public static EditDeadlineBottomSheet newInstance(String taskId, long currentDeadlineMillis) {
        EditDeadlineBottomSheet fragment = new EditDeadlineBottomSheet();
        Bundle args = new Bundle();
        args.putString("TASK_ID", taskId);
        args.putLong("CURRENT_DATE", currentDeadlineMillis);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateTaskViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_edit_deadline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCardView cardDateSelector = view.findViewById(R.id.card_date_selector);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        MaterialButton btnSave = view.findViewById(R.id.button_save_date);

        String taskId = getArguments().getString("TASK_ID");
        long currentMillis = getArguments().getLong("CURRENT_DATE", 0);

        if (currentMillis > 0) {
            calendar.setTimeInMillis(currentMillis);
            updateDateLabel();
        }

        cardDateSelector.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> {
            if (taskId != null) {
                // Llamamos al método del ViewModel para guardar
                viewModel.updateTaskDeadline(taskId, calendar.getTime());
                Toast.makeText(getContext(), "Fecha actualizada", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void updateDateLabel() {
        tvSelectedDate.setText(dateFormat.format(calendar.getTime()));
    }
}