package com.utp.wemake; // Asegúrate de que el paquete sea el correcto

import android.content.Context;
import android.util.Log;
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
import com.utp.wemake.models.User; // <-- Asegúrate de importar tu modelo User

import java.util.List;
import java.util.Map;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private List<Map<String, Object>> membersDataList;
    private OnMemberClickListener listener;
    private String currentUserId;

    public interface OnMemberClickListener {
        void onMemberClick(Map<String, Object> memberData);
        void onMemberDeleted(String userId);
        void onMemberAdded(User user);
    }

    public MembersAdapter(List<Map<String, Object>> membersDataList, String currentUserId, OnMemberClickListener listener) {
        this.membersDataList = membersDataList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_members, parent, false);
        return new MemberViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Map<String, Object> memberData = membersDataList.get(position);
        holder.bind(memberData);
    }

    @Override
    public int getItemCount() {
        return membersDataList.size();
    }

    public void updateMembers(List<Map<String, Object>> newMembers) {
        this.membersDataList.clear();
        this.membersDataList.addAll(newMembers);
        notifyDataSetChanged();
    }


    // --- ViewHolder ---
    class MemberViewHolder extends RecyclerView.ViewHolder {
        private ShapeableImageView imgAvatar;
        private TextView tvName;
        private TextView tvEmail;
        private TextView tvAddedStatus;
        private Chip chipRole;
        private MaterialButton btnRemoveMember;
        private final OnMemberClickListener listener;

        public MemberViewHolder(@NonNull View itemView, OnMemberClickListener listener) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvAddedStatus = itemView.findViewById(R.id.tvAddedStatus);
            chipRole = itemView.findViewById(R.id.chipRole);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
            this.listener = listener;
        }

        /**
         * Este es el método clave que ha sido modificado.
         * Ahora acepta un Map en lugar de un solo objeto Member.
         */
        public void bind(Map<String, Object> memberData) {
            User user = (User) memberData.get("user");
            Member member = (Member) memberData.get("member");

            if (user == null || member == null) {
                Log.e("MembersAdapter", "Datos de miembro incompletos en la posición: " + getAdapterPosition());
                itemView.setVisibility(View.GONE);
                return;
            }
            itemView.setVisibility(View.VISIBLE);


            String imageUrl = user.getPhotoUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_default_avatar);
            }


            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            String role = member.getRole();
            chipRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));

            Context context = itemView.getContext();
            if (member.isAdmin()) {
                chipRole.setChipBackgroundColorResource(R.color.md_theme_primaryContainer);
                chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer));
            } else {
                chipRole.setChipBackgroundColorResource(R.color.md_theme_secondaryContainer);
                chipRole.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer));
            }

            String memberUserId = user.getUserid();

            // Comparamos el ID del miembro de esta fila con el ID del usuario actual
            if (currentUserId != null && currentUserId.equals(memberUserId)) {
                // ES EL USUARIO ACTUAL
                tvName.setText(user.getName() + " (Tú)"); // Añadimos un indicador visual
                btnRemoveMember.setVisibility(View.GONE); // Ocultamos el botón de eliminar
                chipRole.setClickable(false); // Deshabilitamos el clic en el rol
            } else {
                // ES OTRO USUARIO
                tvName.setText(user.getName()); // Nos aseguramos de que no tenga el "(Tú)"
                btnRemoveMember.setVisibility(View.VISIBLE); // Mostramos el botón de eliminar
                chipRole.setClickable(true); // Habilitamos el clic en el rol
            }

            chipRole.setOnClickListener(v -> {
                    listener.onMemberClick(memberData);
            });

            btnRemoveMember.setOnClickListener(v -> {
                listener.onMemberDeleted(user.getUserid());
            });
        }
    }
}