package com.utp.wemake;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.utp.wemake.models.Coupon;
import com.utp.wemake.models.User;
import com.utp.wemake.viewmodels.MainViewModel;
import com.utp.wemake.viewmodels.RedeemPointsViewModel;

public class RedeemPointsActivity extends AppCompatActivity implements CouponsAdapter.OnRedeemClickListener {

    private RedeemPointsViewModel viewModel;
    private MainViewModel mainViewModel;
    private TextView tvUserPoints;
    private RecyclerView rvAvailableCoupons;
    private RecyclerView rvRedeemedCoupons;
    private CouponsAdapter availableCouponsAdapter;
    private RedeemedCouponsAdapter redeemedCouponsAdapter;
    private View emptyStateLayout;
    private CouponsAdapter adapter;
    private String boardId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_redeem_points);

        boardId = getIntent().getStringExtra("boardId");
        if (boardId == null) {
            Toast.makeText(this, "Error: ID de tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(RedeemPointsViewModel.class);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupObservers();
        createNotificationChannel();
        requestNotificationPermission();
    }

    private void initializeViews() {
        tvUserPoints = findViewById(R.id.tv_user_points);
        rvAvailableCoupons = findViewById(R.id.rv_coupons);
        rvRedeemedCoupons = findViewById(R.id.rv_redeemed_coupons);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.redeem_points);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        availableCouponsAdapter = new CouponsAdapter(this);
        rvAvailableCoupons.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAvailableCoupons.setAdapter(availableCouponsAdapter);

        redeemedCouponsAdapter = new RedeemedCouponsAdapter();
        rvRedeemedCoupons.setLayoutManager(new LinearLayoutManager(this));
        rvRedeemedCoupons.setAdapter(redeemedCouponsAdapter);
    }

    private void setupObservers() {
        // Observa los datos del usuario. Cuando llegan, inicia la escucha de los datos del tablero.
        mainViewModel.getCurrentUserData().observe(this, user -> {
            if (user != null) {
                this.currentUser = user;
                viewModel.startListening(boardId, user.getUserid());
            }
        });

        viewModel.getMemberDetails().observe(this, member -> {
            int pointsToShow = (member != null) ? member.getPoints() : 0;
            tvUserPoints.setText(String.valueOf(pointsToShow));
            availableCouponsAdapter.setUserPoints(pointsToShow);
        });

        viewModel.getAvailableCoupons().observe(this, coupons -> {
            boolean isEmpty = (coupons == null || coupons.isEmpty());
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvAvailableCoupons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (!isEmpty) {
                availableCouponsAdapter.submitList(coupons);
            }
        });

        viewModel.getRedeemedCoupons().observe(this, redeemedList -> {
            if (redeemedList != null) {
                redeemedCouponsAdapter.submitList(redeemedList);
            }
        });

        viewModel.getRedemptionRequestSuccess().observe(this, success -> {
            if (success != null) {
                if (success) {
                    showLocalNotification("Solicitud Enviada", "Tu solicitud de canje está siendo revisada por un administrador.");
                }
                viewModel.doneShowingSuccess();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.doneShowingError();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
        });
    }

    @Override
    public void onRedeemClicked(Coupon coupon) {
        if (currentUser == null) {
            Toast.makeText(this, "Aún no se han cargado los datos del usuario.", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar Canje")
                .setMessage("¿Estás seguro de que quieres canjear '" + coupon.getTitle() + "' por " + coupon.getCost() + " puntos?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    viewModel.redeemCoupon(boardId, currentUser, coupon);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void createNotificationChannel() {
        // Los canales solo son necesarios para API 26 (Android 8.0) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "wemake_local_notifications";
            CharSequence name = "Solicitud de Cupones";
            String description = "Notificaciones locales para acciones en la app";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            // Registra el canal en el sistema
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Dentro de tu Activity
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU es Android 13
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no ha sido concedido, lo pedimos
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    /**
     * Muestra una notificación local simple.
     * @param title El título de la notificación.
     * @param message El cuerpo del mensaje.
     */
    private void showLocalNotification(String title, String message) {
        // Primero, comprueba si tienes permiso (solo para Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Si no tienes permiso, no puedes mostrar la notificación.
            // Podrías mostrar un Toast pidiendo al usuario que active los permisos.
            Toast.makeText(this, "Por favor, activa los permisos de notificación.", Toast.LENGTH_SHORT).show();
            return;
        }

        String channelId = "wemake_local_notifications"; // Debe ser el MISMO ID que el del canal que creaste

        // Construye la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Muestra la notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // El 'notificationId' debe ser único si quieres mostrar múltiples notificaciones a la vez.
        // Si usas el mismo ID, la nueva notificación reemplazará a la anterior.
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}