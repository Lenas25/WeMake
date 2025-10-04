package com.utp.wemake;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.User;
import com.utp.wemake.viewmodels.AddMembersViewModel;

public class AddMembersActivity extends AppCompatActivity implements MembersAdapter.OnMemberInteractionListener {

    private TextInputEditText etSearch;
    private RecyclerView rvMembers;
    private TextView tvMemberCount;
    private View layoutEmptyState;
    private MembersAdapter membersAdapter;
    private AddMembersViewModel viewModel;
    private String currentBoardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members);

        currentBoardId = getIntent().getStringExtra("boardId");
        if (currentBoardId == null) {
            Toast.makeText(this, "Error: ID del tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(AddMembersViewModel.class);
        viewModel.init(currentBoardId);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchListener();
        setupObservers();
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        rvMembers = findViewById(R.id.rvMembers);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        findViewById(R.id.btnSaveMembers).setOnClickListener(v -> viewModel.saveAndClose());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.title_add_members);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        membersAdapter = new MembersAdapter(this); // Solo necesita el listener
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Añade un pequeño retraso para no buscar en cada tecla
                // handler.removeCallbacks(searchRunnable);
                // handler.postDelayed(() -> viewModel.searchUsers(s.toString()), 300);
                viewModel.searchUsers(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupObservers() {
        // ¡UN ÚNICO OBSERVADOR PARA LA LISTA!
        viewModel.getDisplayList().observe(this, displayList -> {
            // El adaptador ahora espera un objeto MemberViewData,
            // pero tu método setData espera User y Map. Debemos ajustar el adaptador.
            // Por ahora, asumimos que el adaptador recibe la lista combinada.
            // membersAdapter.submitList(displayList); // Si usas ListAdapter

            // Si el método setData no existe en tu adaptador actual:
            // Por favor, usa la versión del adaptador que te di en la respuesta anterior,
            // que sí tiene el método setData(List<User>, Map<String, Member>).

            updateEmptyState(displayList == null || displayList.isEmpty());
            if (displayList != null) {
                tvMemberCount.setText(displayList.stream().filter(d -> d.isMember).count() + " miembros");
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> { /* Muestra/oculta ProgressBar */ });
        viewModel.getError().observe(this, error -> { if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show(); });
        viewModel.shouldCloseScreen().observe(this, close -> { if (close) finish(); });
    }

    private void updateEmptyState(boolean isEmpty) {
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvMembers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onAddMember(User user) { viewModel.addMember(user); }

    @Override
    public void onRemoveMember(User user) { viewModel.removeMember(user); }

    @Override
    public void onMemberRoleClick(User user, String currentRole) {
        RoleBottomSheetFragment bottomSheet = RoleBottomSheetFragment.newInstance(user, currentBoardId, currentRole);

        bottomSheet.setOnRoleChangeListener(new RoleBottomSheetFragment.OnRoleChangeListener() {
            @Override
            public void onRoleChanged(String userId, String newRole) {
                viewModel.updateMemberRole(userId, newRole);
                Toast.makeText(AddMembersActivity.this, "Rol actualizado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMemberDeleted(String userId) {
                viewModel.removeMemberById(userId);
                Toast.makeText(AddMembersActivity.this, "Miembro eliminado", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show(getSupportFragmentManager(), "RoleBottomSheet");
    }
}