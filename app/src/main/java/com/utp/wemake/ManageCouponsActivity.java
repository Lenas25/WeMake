package com.utp.wemake;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.utp.wemake.models.Coupon;
import com.utp.wemake.viewmodels.ManageCouponsViewModel;

public class ManageCouponsActivity extends AppCompatActivity implements CouponsManagementAdapter.OnCouponInteractionListener, CouponBottomSheetFragment.OnCouponSaveListener {

    private ManageCouponsViewModel viewModel;
    private RecyclerView rvCoupons;
    private CouponsManagementAdapter adapter;
    private View emptyStateView;
    private String boardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_coupons);

        boardId = getIntent().getStringExtra("boardId");

        viewModel = new ViewModelProvider(this).get(ManageCouponsViewModel.class);
        viewModel.init(boardId);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        setupObservers();
    }

    private void initializeViews() {
        rvCoupons = findViewById(R.id.rv_coupons_management);
        emptyStateView = findViewById(R.id.tv_empty_state_coupons);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.manage_coupons);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        adapter = new CouponsManagementAdapter(this);
        rvCoupons.setLayoutManager(new LinearLayoutManager(this));
        rvCoupons.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.fab_add_coupon).setOnClickListener(v -> {
            showCouponForm(null); // Pasa null para el modo "Crear"
        });
    }


    private void setupObservers() {
        viewModel.getCoupons().observe(this, coupons -> {
            boolean isEmpty = coupons == null || coupons.isEmpty();
            emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvCoupons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            adapter.submitList(coupons);
        });
        viewModel.getError().observe(this, error -> { /* Muestra Toast de error */ });
    }

    private void showCouponForm(Coupon coupon) {
        CouponBottomSheetFragment bottomSheet;
        if (coupon == null) {
            bottomSheet = CouponBottomSheetFragment.newInstance();
        } else {
            bottomSheet = CouponBottomSheetFragment.newInstance(coupon);
        }
        bottomSheet.setOnCouponSaveListener(this);
        bottomSheet.show(getSupportFragmentManager(), "CouponFormBottomSheet");
    }

    @Override
    public void onEditClicked(Coupon coupon) {
        showCouponForm(coupon); // Pasa el objeto cupón para el modo "Editar"
    }

    @Override
    public void onDeleteClicked(Coupon coupon) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Cupón")
                .setMessage("¿Estás seguro de que quieres eliminar '" + coupon.getTitle() + "'? Esta acción es permanente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteCoupon(coupon);
                })
                .show();
    }

    // --- Callback de la Interfaz del BottomSheet ---
    @Override
    public void onCouponSaved(Coupon coupon) {
        if (coupon.getId() == null || coupon.getId().isEmpty()) {
            viewModel.createCoupon(coupon);
        } else {
            viewModel.updateCoupon(coupon);
        }
    }
}