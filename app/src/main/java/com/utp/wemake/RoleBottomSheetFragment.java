package com.utp.wemake;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.constants.Roles;
import com.utp.wemake.models.Member;
import com.utp.wemake.repository.MemberRepository;

public class RoleBottomSheetFragment extends BottomSheetDialogFragment {

    private Member member;
    private OnRoleChangeListener listener;
    private MemberRepository memberRepository;

    public interface OnRoleChangeListener {
        void onRoleChanged(Member member, String newRole);
        void onMemberDeleted(Member member);
    }

    public static RoleBottomSheetFragment newInstance(Member member) {
        RoleBottomSheetFragment fragment = new RoleBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("memberId", member.getId());
        args.putString("memberName", member.getName());
        args.putString("memberEmail", member.getEmail());
        args.putString("memberRole", member.getRole());
        args.putString("boardId", member.getBoardId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        memberRepository = new MemberRepository();
        
        if (getArguments() != null) {
            String id = getArguments().getString("memberId");
            String name = getArguments().getString("memberName");
            String email = getArguments().getString("memberEmail");
            String role = getArguments().getString("memberRole");
            String boardId = getArguments().getString("boardId");
            member = new Member(id, name, email, "", role);
            member.setBoardId(boardId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_change_role, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViews(view);
        setupListeners(view);
    }

    private void setupViews(View view) {
        TextView tvMemberName = view.findViewById(R.id.tvMemberName);
        TextView tvMemberEmail = view.findViewById(R.id.tvMemberEmail);
        RadioButton rbAdmin = view.findViewById(R.id.rbAdmin);
        RadioButton rbUser = view.findViewById(R.id.rbUser);

        tvMemberName.setText(member.getName());
        tvMemberEmail.setText(member.getEmail());

        // Seleccionar el rol actual
        if (member.isAdmin()) {
            rbAdmin.setChecked(true);
        } else {
            rbUser.setChecked(true);
        }
    }

    private void setupListeners(View view) {
        MaterialCardView cardAdmin = view.findViewById(R.id.cardAdmin);
        MaterialCardView cardUser = view.findViewById(R.id.cardUser);
        RadioButton rbAdmin = view.findViewById(R.id.rbAdmin);
        RadioButton rbUser = view.findViewById(R.id.rbUser);
        MaterialButton btnSaveRole = view.findViewById(R.id.btnSaveRole);
        MaterialButton btnDeleteMember = view.findViewById(R.id.btnDeleteMember);

        cardAdmin.setOnClickListener(v -> rbAdmin.setChecked(true));
        cardUser.setOnClickListener(v -> rbUser.setChecked(true));

        btnSaveRole.setOnClickListener(v -> {
            String newRole = rbAdmin.isChecked() ? Roles.ADMIN : Roles.USER;
            
            // Actualizar el rol localmente primero
            member.setRole(newRole);
            
            // Si tiene boardId, actualizar en Firebase
            if (member.getBoardId() != null && !member.getBoardId().isEmpty()) {
                memberRepository.updateMemberRole(member.getBoardId(), member.getId(), newRole, 
                    new MemberRepository.MemberCallback() {
                        @Override
                        public void onSuccess(Member updatedMember) {
                            if (listener != null) {
                                listener.onRoleChanged(updatedMember, newRole);
                            }
                            dismiss();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Manejar error
                            dismiss();
                        }
                    });
            } else {
                // Si no tiene boardId, solo notificar el cambio local
                if (listener != null) {
                    listener.onRoleChanged(member, newRole);
                }
                dismiss();
            }
        });

        btnDeleteMember.setOnClickListener(v -> {
            // Si tiene boardId, eliminar de Firebase
            if (member.getBoardId() != null && !member.getBoardId().isEmpty()) {
                memberRepository.removeMemberFromBoard(member.getBoardId(), member.getId(), 
                    new MemberRepository.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            if (listener != null) {
                                listener.onMemberDeleted(member);
                            }
                            dismiss();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Manejar error
                            dismiss();
                        }
                    });
            } else {
                // Si no tiene boardId, solo notificar la eliminación local
                if (listener != null) {
                    listener.onMemberDeleted(member);
                }
                dismiss();
            }
        });
    }

    public void setOnRoleChangeListener(OnRoleChangeListener listener) {
        this.listener = listener;
    }
}