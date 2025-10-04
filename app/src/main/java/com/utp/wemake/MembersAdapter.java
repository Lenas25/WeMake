package com.utp.wemake;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    // --- CLASE WRAPPER INTERNA ---
    public static class MemberViewData {
        public final User user;
        public final Member memberDetails;
        public final boolean isMember;

        public MemberViewData(User user, Member memberDetails, boolean isMember) {
            this.user = user;
            this.memberDetails = memberDetails;
            this.isMember = isMember;
        }

        public String getName() { return user.getName(); }
        public String getEmail() { return user.getEmail(); }
        public String getAvatarUrl() { return user.getPhotoUrl(); }
        public boolean isAdmin() { return memberDetails != null && "admin".equalsIgnoreCase(memberDetails.getRole()); }
    }

    private List<MemberViewData> displayList = new ArrayList<>();
    private final OnMemberInteractionListener listener;

    // La interfaz con los métodos correctos
    public interface OnMemberInteractionListener {
        void onAddMember(User user);
        void onRemoveMember(User user);
        void onMemberRoleClick(User user, String currentRole);
    }

    public MembersAdapter(OnMemberInteractionListener listener) {
        this.listener = listener;
    }

    public void setData(List<User> users, Map<String, Member> memberDetailsMap) {
        displayList.clear();
        if (users == null || memberDetailsMap == null) {
            notifyDataSetChanged();
            return;
        }
        for (User user : users) {
            boolean isMember = memberDetailsMap.containsKey(user.getUserid());
            Member details = isMember ? memberDetailsMap.get(user.getUserid()) : null;
            displayList.add(new MemberViewData(user, details, isMember));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_members, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        MemberViewData data = displayList.get(position);
        holder.bind(data, listener);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView imgAvatar;
        private final TextView tvName, tvEmail, tvAddedStatus;
        private final Chip chipRole;
        private final MaterialButton btnRemoveMember;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvAddedStatus = itemView.findViewById(R.id.tvAddedStatus);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
        }

        // CORRECCIÓN: El parámetro ahora es OnMemberInteractionListener
        public void bind(final MemberViewData data, final OnMemberInteractionListener listener) {
            tvName.setText(data.getName());
            tvEmail.setText(data.getEmail());

            Glide.with(itemView.getContext()).load(data.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar).circleCrop().into(imgAvatar);

            if (data.isMember) {
                tvAddedStatus.setVisibility(View.VISIBLE);
                btnRemoveMember.setVisibility(View.VISIBLE);
                chipRole.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.7f);

                chipRole.setText(data.isAdmin() ? "Admin" : "Usuario");
                Context context = itemView.getContext();
                if (data.isAdmin()) {
                    chipRole.setChipBackgroundColorResource(R.color.md_theme_primaryContainer);
                    chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer));
                } else {
                    chipRole.setChipBackgroundColorResource(R.color.md_theme_secondaryContainer);
                    chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer));
                }

            } else {
                tvAddedStatus.setVisibility(View.GONE);
                btnRemoveMember.setVisibility(View.GONE);
                chipRole.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }

            itemView.setOnClickListener(v -> {
                if (!data.isMember) {
                    listener.onAddMember(data.user);
                } else {
                    String currentRole = (data.memberDetails != null) ? data.memberDetails.getRole() : "member";
                    listener.onMemberRoleClick(data.user, currentRole);
                }
            });

            chipRole.setOnClickListener(v -> {
                if (data.isMember) {
                    String currentRole = (data.memberDetails != null) ? data.memberDetails.getRole() : "member";
                    listener.onMemberRoleClick(data.user, currentRole);
                }
            });

            btnRemoveMember.setOnClickListener(v -> {
                listener.onRemoveMember(data.user);
            });
        }
    }
}