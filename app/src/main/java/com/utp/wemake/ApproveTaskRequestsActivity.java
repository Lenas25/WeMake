package com.utp.wemake;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.viewmodels.ApproveTaskRequestsViewModel;

import java.util.ArrayList;

public class ApproveTaskRequestsActivity extends AppCompatActivity implements ProposalAdapter.OnProposalActionListener {

    private ApproveTaskRequestsViewModel viewModel;
    private RecyclerView rvRequests;
    private TextView tvEmptyState;
    private ProposalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_approve_requests);

        viewModel = new ViewModelProvider(this).get(ApproveTaskRequestsViewModel.class);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        observeViewModel();

    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.approve_tasks); // Asegúrate de tener esta string
        toolbar.setNavigationOnClickListener(v -> finish());

        // Maneja los insets para el modo EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        rvRequests = findViewById(R.id.rv_requests);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProposalAdapter(new ArrayList<>(), this);
        rvRequests.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                rvRequests.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.GONE);
            } else {
                updateDataView(viewModel.proposals.getValue());
            }
        });

        viewModel.proposals.observe(this, proposals -> {
            if (!Boolean.TRUE.equals(viewModel.isLoading.getValue())) {
                updateDataView(proposals);
            }
        });

        viewModel.operationStatus.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDataView(java.util.List<TaskProposal> proposals) {
        if (proposals == null || proposals.isEmpty()) {
            // ESTADO: Carga exitosa (sin datos)
            tvEmptyState.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            // ESTADO: Carga exitosa (con datos)
            tvEmptyState.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
            // Actualizamos el adaptador con los nuevos datos
            adapter = new ProposalAdapter(proposals, this);
            rvRequests.setAdapter(adapter);
        }
    }


    @Override
    public void onApproveClicked(TaskProposal proposal, int reward, int penalty) {
        if (reward < 0 || penalty < 0) {
            Toast.makeText(this, "Los puntos no pueden ser negativos.", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.approveTaskProposal(proposal, reward, penalty);
    }

    @Override
    public void onDenyClicked(TaskProposal proposal) {
        viewModel.denyTaskProposal(proposal);
    }
}