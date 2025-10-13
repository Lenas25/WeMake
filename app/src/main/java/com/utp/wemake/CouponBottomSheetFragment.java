package com.utp.wemake;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.Coupon;

public class CouponBottomSheetFragment extends BottomSheetDialogFragment {


    public interface OnCouponSaveListener {
        void onCouponSaved(Coupon coupon);
    }
    private OnCouponSaveListener listener;

    // --- Vistas de la UI ---
    private TextView tvFormTitle;
    private TextInputEditText etTitle, etDescription, etCost;
    private MaterialButton btnSaveCoupon;
    private Coupon existingCoupon;

    // Método de fábrica para CREAR
    public static CouponBottomSheetFragment newInstance() {
        return new CouponBottomSheetFragment();
    }

    // Método de fábrica para EDITAR
    public static CouponBottomSheetFragment newInstance(Coupon coupon) {
        CouponBottomSheetFragment fragment = new CouponBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable("coupon", coupon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperamos el cupón si se pasó como argumento (modo Editar)
        if (getArguments() != null && getArguments().containsKey("coupon")) {
            existingCoupon = (Coupon) getArguments().getSerializable("coupon");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_coupon_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupUI();
        setupListeners();
    }

    private void initializeViews(View view) {
        tvFormTitle = view.findViewById(R.id.tv_coupon_form_title);
        etTitle = view.findViewById(R.id.et_coupon_title);
        etDescription = view.findViewById(R.id.et_coupon_desc);
        etCost = view.findViewById(R.id.et_coupon_cost);
        btnSaveCoupon = view.findViewById(R.id.btn_save_coupon);
    }

    /**
     * Configura la UI inicial dependiendo de si es modo Crear o Editar.
     */
    private void setupUI() {
        if (existingCoupon != null) {
            // --- Modo Editar ---
            tvFormTitle.setText("Editar Cupón");
            // Rellena los campos con los datos del cupón existente
            etTitle.setText(existingCoupon.getTitle());
            etDescription.setText(existingCoupon.getDescription());
            etCost.setText(String.valueOf(existingCoupon.getCost()));
        } else {
            // --- Modo Crear ---
            tvFormTitle.setText("Crear Nuevo Cupón");
            // Los campos se quedan vacíos
        }
    }

    private void setupListeners() {
        btnSaveCoupon.setOnClickListener(v -> {
            saveCoupon();
        });
    }

    private void saveCoupon() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String costStr = etCost.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(costStr)) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int cost = Integer.parseInt(costStr);

        // 2. Determina si es un cupón nuevo o una actualización
        Coupon couponToSave;
        if (existingCoupon != null) {
            // Es una actualización, usa el objeto existente
            couponToSave = existingCoupon;
        } else {
            // Es uno nuevo, crea un nuevo objeto
            couponToSave = new Coupon();
        }

        // 3. Actualiza el objeto con los datos del formulario
        couponToSave.setTitle(title);
        couponToSave.setDescription(description);
        couponToSave.setCost(cost);

        // 4. Notifica a la Activity que lo llamó
        if (listener != null) {
            listener.onCouponSaved(couponToSave);
        }

        dismiss(); // Cierra el BottomSheet
    }

    /**
     * Permite que la Activity/Fragment que lo llama se registre como "oyente".
     */
    public void setOnCouponSaveListener(OnCouponSaveListener listener) {
        this.listener = listener;
    }
}