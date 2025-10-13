package com.utp.wemake;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.appbar.MaterialToolbar;
import com.utp.wemake.models.RedemptionRequest;
import com.utp.wemake.viewmodels.ApproveRequestsViewModel;

public class ApproveRequestsActivity extends AppCompatActivity implements RedemptionRequestsAdapter.OnRequestInteractionListener {

    private ApproveRequestsViewModel viewModel;
    private RedemptionRequestsAdapter adapter;
    private RecyclerView rvRequests;
    private TextView tvEmptyState;
    private String boardId;
    private String adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_approve_requests);

        boardId = getIntent().getStringExtra("boardId");
        adminId = FirebaseAuth.getInstance().getUid();

        if (boardId == null || adminId == null) {
            Toast.makeText(this, "Error: Faltan datos necesarios", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ApproveRequestsViewModel.class);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupObservers();

        viewModel.startListening(boardId);
    }

    private void initializeViews() {
        rvRequests = findViewById(R.id.rv_requests);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.approve_requests);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        adapter = new RedemptionRequestsAdapter(this);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getPendingRequests().observe(this, requests -> {
            boolean isEmpty = (requests == null || requests.isEmpty());
            tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvRequests.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (!isEmpty) {
                adapter.submitList(requests);
            }
        });
    }
    @Override
    public void onApproveClicked(RedemptionRequest request) {
        viewModel.approveRequest(request.getId(), adminId);

    }

    @Override
    public void onDenyClicked(RedemptionRequest request) {
        viewModel.denyRequest(request, adminId);
    }
}