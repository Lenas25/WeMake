package com.utp.wemake;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.constants.Roles;
import com.utp.wemake.models.Member;

import java.util.ArrayList;
import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<Member> members;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(Member member);
        void onMemberAdded(Member member);
        void onMemberRemoved(Member member);
    }

    public MembersAdapter(List<Member> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_members, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = members.get(position);
        holder.bind(member, listener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public List<Member> getSelectedMembers() {
        List<Member> selected = new ArrayList<>();
        for (Member member : members) {
            if (member.isAdded()) {
                selected.add(member);
            }
        }
        return selected;
    }

    public int getSelectedMembersCount() {
        int count = 0;
        for (Member member : members) {
            if (member.isAdded()) {
                count++;
            }
        }
        return count;
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private ShapeableImageView imgAvatar;
        private TextView tvName;
        private TextView tvEmail;
        private TextView tvAddedStatus;
        private Chip chipRole;
        private MaterialButton btnRemoveMember;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvAddedStatus = itemView.findViewById(R.id.tvAddedStatus);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
        }

        public void bind(Member member, OnMemberClickListener listener) {
            tvName.setText(member.getName());
            tvEmail.setText(member.getEmail());
            chipRole.setText(member.isAdmin() ? "Admin" : "Usuario");
            
            // Configurar colores del chip según el rol
            Context context = itemView.getContext();
            if (member.isAdmin()) {
                chipRole.setChipBackgroundColorResource(R.color.md_theme_primaryContainer);
                chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer));
            } else {
                chipRole.setChipBackgroundColorResource(R.color.md_theme_secondaryContainer);
                chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer));
            }

            // Mostrar/ocultar elementos según el estado
            if (member.isAdded()) {
                tvAddedStatus.setVisibility(View.VISIBLE);
                btnRemoveMember.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.7f);
            } else {
                tvAddedStatus.setVisibility(View.GONE);
                btnRemoveMember.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }

            // Listeners
            itemView.setOnClickListener(v -> {
                if (!member.isAdded()) {
                    // Si no está agregado, agregarlo
                    listener.onMemberAdded(member);
                } else {
                    // Si ya está agregado, mostrar opciones de rol
                    listener.onMemberClick(member);
                }
            });

            chipRole.setOnClickListener(v -> {
                if (member.isAdded()) {
                    // Solo permitir cambiar rol si ya está agregado
                    listener.onMemberClick(member);
                }
            });

            btnRemoveMember.setOnClickListener(v -> {
                listener.onMemberRemoved(member);
            });
        }
    }
}