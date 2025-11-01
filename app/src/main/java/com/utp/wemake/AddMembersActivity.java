package com.utp.wemake;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.viewmodels.AddMembersViewModel;

import java.util.ArrayList;
import java.util.Map;

public class AddMembersActivity extends AppCompatActivity
        implements MembersAdapter.OnMemberClickListener, 
                   RoleBottomSheetFragment.OnRoleChangeListener,
                   SearchUsersAdapter.OnUserClickListener {
    
    private TextInputEditText etSearch;
    private RecyclerView rvMembers;
    private RecyclerView rvSearchResults;
    private TextView tvMemberCount;
    private View layoutEmptyState;
    private View layoutSearchResults;
    private MaterialButton btnSaveMembers;

    private AddMembersViewModel viewModel;
    private MembersAdapter membersAdapter;
    private SearchUsersAdapter searchAdapter;
    private String boardId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_members);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Si por alguna razón no hay usuario, cerramos la actividad.
            Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        this.currentUserId = currentUser.getUid();

        // Primero intentar obtener el boardId del Intent, luego de las preferencias como fallback
        boardId = getIntent().getStringExtra("boardId");
        if (boardId == null || boardId.isEmpty()) {
            // Solo usar preferencias como fallback si no viene en el Intent
            BoardSelectionPrefs prefs = new BoardSelectionPrefs(getApplicationContext());
            boardId = prefs.getSelectedBoardId();
        }

        viewModel = new ViewModelProvider(this).get(AddMembersViewModel.class);

        setupToolbar();
        setupViews();
        setupRecyclerViews();
        setupSearch();
        observeViewModel();

        if (boardId != null && !boardId.isEmpty()) {
            viewModel.loadMembers(boardId);
        } else {
            Toast.makeText(this, "ID de tablero no válido", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.title_add_members);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupViews() {
        etSearch = findViewById(R.id.etSearch);
        rvMembers = findViewById(R.id.rvMembers);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutSearchResults = findViewById(R.id.layoutSearchResults);
    }

    private void setupRecyclerViews() {
        // Adapter para miembros del tablero
        membersAdapter = new MembersAdapter(new ArrayList<>(), currentUserId, this);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);

        // Adapter para resultados de búsqueda
        searchAdapter = new SearchUsersAdapter(new ArrayList<>(), this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showMembersList();
                } else {
                    viewModel.searchUsers(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        // Observar miembros del tablero
        viewModel.members.observe(this, members -> {
            if (members != null && !members.isEmpty()) {
                membersAdapter.updateMembers(members);
                showMembersList();
            } else {
                showEmptyState();
            }
            tvMemberCount.setText(String.valueOf(members != null ? members.size() : 0));
        });

        // Observar resultados de búsqueda
        viewModel.searchResults.observe(this, users -> {
            String q = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            if (q.isEmpty()) {
                // Si se limpió la búsqueda, volvemos a la lista de miembros del tablero
                showMembersList();
                return;
            }

            if (users != null && !users.isEmpty()) {
                searchAdapter.updateUsers(users);
                showSearchResults();
            } else {
                // Búsqueda activa pero sin resultados → mostrar estado vacío
                showEmptyState();
            }
        });

        // Observar estados de carga y errores
        viewModel.updateSuccess.observe(this, isSuccess -> {
            if (isSuccess) {
                Toast.makeText(this, "Operación completada con éxito", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            // Aquí puedes mostrar/ocultar un indicador de carga si lo deseas
        });
    }

    private void showMembersList() {
        rvMembers.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutSearchResults.setVisibility(View.GONE);
    }

    private void showSearchResults() {
        rvMembers.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutSearchResults.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        rvMembers.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutSearchResults.setVisibility(View.GONE);

        // Personalizar mensaje cuando hay búsqueda sin resultados
        String q = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        TextView title = layoutEmptyState.findViewById(R.id.empty_title);
        TextView subtitle = layoutEmptyState.findViewById(R.id.empty_subtitle);
        if (title != null && subtitle != null) {
            if (!q.isEmpty()) {
                title.setText("Sin coincidencias");
                subtitle.setText("No encontramos usuarios para: " + q);
            } else {
                title.setText("No hay miembros agregados");
                subtitle.setText("Busca y agrega miembros al tablero");
            }
        }
    }

    // Implementación de MembersAdapter.OnMemberClickListener
    @Override
    public void onMemberClick(Map<String, Object> memberData) {
        User user = (User) memberData.get("user");
        Member member = (Member) memberData.get("member");

        if (user == null || member == null) {
            Toast.makeText(this, "Error: Datos de miembro incompletos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId != null && currentUserId.equals(user.getUserid())) {
            Toast.makeText(this, "No puedes editar tu propio rol.", Toast.LENGTH_SHORT).show();
            return;
        }

        RoleBottomSheetFragment bottomSheet = RoleBottomSheetFragment.newInstance(
            user.getUserid(), 
            user.getName(), 
            user.getEmail(), 
            user.getPhotoUrl(), 
            member.getRole()
        );
        bottomSheet.setOnRoleChangeListener(this);
        bottomSheet.show(getSupportFragmentManager(), "RoleBottomSheetFragment");
    }

    @Override
    public void onRoleChanged(String userId, String newRole) {
        viewModel.updateMemberRole(boardId, userId, newRole);
    }

    @Override
    public void onMemberDeleted(String userId) {
        viewModel.deleteMember(boardId, userId);
    }

    @Override
    public void onMemberAdded(String userId) {
        viewModel.addMemberToBoard(boardId, userId);
    }

    @Override
    public void onUserClick(User user) {
        viewModel.addMemberToBoard(boardId, user.getUserid());
        etSearch.setText(""); // Limpiar búsqueda
    }
}