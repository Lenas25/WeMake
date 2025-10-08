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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utp.wemake.MembersAdapter;
import com.utp.wemake.databinding.ActivityAddMembersBinding;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import com.utp.wemake.utils.BoardSelectionPrefs;
import com.utp.wemake.viewmodels.AddMembersViewModel;
import java.util.ArrayList;
import java.util.Map;

public class AddMembersActivity extends AppCompatActivity
        implements MembersAdapter.OnMemberClickListener, RoleBottomSheetFragment.OnRoleChangeListener {
    private TextInputEditText etSearch;
    private RecyclerView rvMembers;
    private TextView tvMemberCount;
    private View layoutEmptyState;
    private MaterialButton btnSaveMembers;

    private AddMembersViewModel viewModel;
    private MembersAdapter adapter;
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

        BoardSelectionPrefs prefs = new BoardSelectionPrefs(getApplicationContext());
        boardId = prefs.getSelectedBoardId();

        viewModel = new ViewModelProvider(this).get(AddMembersViewModel.class);

        setupToolbar();
        setupViews();
        setupRecyclerView();
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
        tvMemberCount = findViewById(R.id.tvMemberCount);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        btnSaveMembers = findViewById(R.id.btnSaveMembers);

    }

    private void setupRecyclerView() {
        adapter = new MembersAdapter(new ArrayList<>(), currentUserId, this);
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.members.observe(this, members -> {
            if (members != null && !members.isEmpty()) {
                adapter.updateMembers(members);
                layoutEmptyState.setVisibility(View.GONE);
                rvMembers.setVisibility(View.VISIBLE);
            } else {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvMembers.setVisibility(View.GONE);
            }
            tvMemberCount.setText(String.valueOf(members != null ? members.size() : 0));
        });

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

    }

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

        RoleBottomSheetFragment bottomSheet = RoleBottomSheetFragment.newInstance(user.getUserid(), user.getName(), user.getEmail(), user.getPhotoUrl(), member.getRole());
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
}