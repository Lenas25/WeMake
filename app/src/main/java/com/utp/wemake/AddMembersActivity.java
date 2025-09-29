package com.utp.wemake;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.Member;
import com.utp.wemake.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;

public class AddMembersActivity extends AppCompatActivity implements MembersAdapter.OnMemberClickListener {

    private TextInputEditText etSearch;
    private RecyclerView rvMembers;
    private TextView tvMemberCount;
    private View layoutEmptyState;
    private MaterialButton btnSaveMembers;
    
    private MembersAdapter membersAdapter;
    private List<Member> membersList;
    private List<Member> searchResults;
    private MemberRepository memberRepository;
    private String currentBoardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_members);

        // Obtener el ID del tablero desde el Intent
        currentBoardId = getIntent().getStringExtra("boardId");
        if (currentBoardId == null) {
            Toast.makeText(this, "Error: ID del tablero no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        memberRepository = new MemberRepository();
        setupToolbar();
        setupViews();
        setupRecyclerView();
        setupSearch();
        loadInitialData();
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
        tvMemberCount = findViewById(R.id.tvMemberCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnSaveMembers = findViewById(R.id.btnSaveMembers);
        
        btnSaveMembers.setOnClickListener(v -> saveMembers());
    }

    private void setupRecyclerView() {
        membersList = new ArrayList<>();
        searchResults = new ArrayList<>();
        
        membersAdapter = new MembersAdapter(searchResults, this);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(membersAdapter);
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchMembers(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadInitialData() {
        // Cargar miembros existentes del tablero
        memberRepository.getBoardMembers(currentBoardId, new MemberRepository.MembersCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                membersList.clear();
                membersList.addAll(members);
                searchResults.clear();
                searchResults.addAll(members);
                membersAdapter.notifyDataSetChanged();
                updateMemberCount();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddMembersActivity.this, "Error al cargar miembros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchMembers(String query) {
        if (query.trim().isEmpty()) {
            searchResults.clear();
            searchResults.addAll(membersList);
            membersAdapter.notifyDataSetChanged();
            updateEmptyState();
            return;
        }

        // Mostrar indicador de carga
        showLoadingIndicator(true);

        // Buscar en Firebase
        memberRepository.searchUsers(query, new MemberRepository.SearchCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                showLoadingIndicator(false);
                searchResults.clear();
                searchResults.addAll(members);
                membersAdapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                showLoadingIndicator(false);
                Toast.makeText(AddMembersActivity.this, "Error en la búsqueda: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Log.e("AddMembersActivity", "Search error: " + e.getMessage()); // Original code had this line commented out
            }
        });
    }

    private void updateMemberCount() {
        int count = membersAdapter.getSelectedMembersCount();
        tvMemberCount.setText(count + " miembros");
        btnSaveMembers.setEnabled(count > 0);
    }

    private void updateEmptyState() {
        if (searchResults.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvMembers.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvMembers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMemberClick(Member member) {
        // Solo mostrar el bottom sheet si el miembro ya está agregado al tablero
        if (member.isAdded()) {
            showRoleBottomSheet(member);
        } else {
            // Si no está agregado, agregarlo primero
            onMemberAdded(member);
        }
    }

    @Override
    public void onMemberAdded(Member member) {
        // Validar que el miembro tenga un ID válido
        if (member.getId() == null || member.getId().trim().isEmpty()) {
            Toast.makeText(this, "Error: El miembro no tiene un ID válido", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validar que el boardId sea válido
        if (currentBoardId == null || currentBoardId.trim().isEmpty()) {
            Toast.makeText(this, "Error: ID del tablero no válido", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Agregar miembro al tablero en Firebase
        memberRepository.addMemberToBoard(currentBoardId, member, new MemberRepository.MemberCallback() {
            @Override
            public void onSuccess(Member addedMember) {
                // Marcar como agregado
                member.setAdded(true);
                member.setBoardId(currentBoardId);
                
                // Agregar a la lista de miembros del tablero
                membersList.add(addedMember);
                
                // Actualizar la lista de búsqueda
                int index = searchResults.indexOf(member);
                if (index != -1) {
                    searchResults.set(index, addedMember);
                }
                
                // Notificar cambios
                membersAdapter.notifyItemChanged(index);
                updateMemberCount();
                updateEmptyState();
                
                Toast.makeText(AddMembersActivity.this, member.getName() + " agregado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddMembersActivity.this, "Error al agregar miembro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AddMembersActivity", "Error adding member: " + e.getMessage());
            }
        });
    }

    @Override
    public void onMemberRemoved(Member member) {
        // Remover miembro del tablero en Firebase
        memberRepository.removeMemberFromBoard(currentBoardId, member.getId(), new MemberRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                member.setAdded(false);
                membersAdapter.notifyItemChanged(searchResults.indexOf(member));
                updateMemberCount();
                Toast.makeText(AddMembersActivity.this, member.getName() + " removido", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddMembersActivity.this, "Error al remover miembro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRoleBottomSheet(Member member) {
        RoleBottomSheetFragment bottomSheet = RoleBottomSheetFragment.newInstance(member);
        
        // Configurar el listener para manejar cambios de rol
        bottomSheet.setOnRoleChangeListener(new RoleBottomSheetFragment.OnRoleChangeListener() {
            @Override
            public void onRoleChanged(Member updatedMember, String newRole) {
                // Actualizar el miembro en la lista local
                int index = membersList.indexOf(updatedMember);
                if (index != -1) {
                    membersList.set(index, updatedMember);
                }
                
                // Actualizar en la lista de búsqueda
                int searchIndex = searchResults.indexOf(updatedMember);
                if (searchIndex != -1) {
                    searchResults.set(searchIndex, updatedMember);
                }
                
                // Notificar cambios
                membersAdapter.notifyItemChanged(searchIndex);
                updateMemberCount();
                
                Toast.makeText(AddMembersActivity.this, "Rol actualizado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMemberDeleted(Member deletedMember) {
                // Remover de la lista local
                membersList.remove(deletedMember);
                searchResults.remove(deletedMember);
                
                // Notificar cambios
                membersAdapter.notifyDataSetChanged();
                updateMemberCount();
                updateEmptyState();
                
                Toast.makeText(AddMembersActivity.this, deletedMember.getName() + " eliminado", Toast.LENGTH_SHORT).show();
            }
        });
        
        bottomSheet.show(getSupportFragmentManager(), "RoleBottomSheet");
    }

    private void saveMembers() {
        List<Member> selectedMembers = membersAdapter.getSelectedMembers();
        Toast.makeText(this, "Miembros guardados: " + selectedMembers.size(), Toast.LENGTH_SHORT).show();
        onBackPressed();
    }

    private void showLoadingIndicator(boolean show) {
        // Implementar indicador de carga si es necesario
        if (show) {
            // Mostrar progress bar o similar
        } else {
            // Ocultar progress bar
        }
    }
}