package com.utp.wemake; // O tu paquete de adaptadores

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.utp.wemake.dto.LeaderboardResponse;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardResponse> userList;

    /**
     * Constructor del adaptador.
     * @param userList La lista de entradas del leaderboard a mostrar.
     */
    public LeaderboardAdapter(List<LeaderboardResponse> userList) {
        this.userList = userList;
    }

    /**
     * Se llama cuando el RecyclerView necesita un nuevo ViewHolder.
     * Infla el layout del item (item_leaderboard_user.xml).
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Se llama para mostrar los datos en una posición específica.
     * Vincula los datos del objeto LeaderboardEntry con las vistas del ViewHolder.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardResponse userEntry = userList.get(position);
        holder.bind(userEntry);
    }

    /**
     * Devuelve el número total de items en la lista.
     */
    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    /**
     * Clase interna que representa cada fila (item) en el RecyclerView.
     * Contiene las referencias a las vistas del layout del item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvRank;
        final TextView tvUserName;
        final TextView tvPoints;
        final ShapeableImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vincular las vistas del layout con las variables
            tvRank = itemView.findViewById(R.id.tv_rank);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvPoints = itemView.findViewById(R.id.tv_points);
        }

        /**
         * Método que rellena las vistas con los datos de un objeto LeaderboardEntry.
         * @param user La entrada del leaderboard para esta fila.
         */
        void bind(LeaderboardResponse user) {
            // Establecer el ranking
            tvRank.setText(String.valueOf(user.rank));

            // Establecer el nombre del usuario
            tvUserName.setText(user.name);

            // Establecer los puntos, formateando el texto
            tvPoints.setText(String.format(Locale.getDefault(), "%d pts", user.points));

            // Cargar la imagen del avatar usando la librería Glide
            Glide.with(itemView.getContext())
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar) // Imagen de reserva mientras carga
                    .error(R.drawable.ic_default_avatar)      // Imagen de reserva si la URL falla
                    .circleCrop() // Para que la imagen sea redonda
                    .into(ivAvatar);
        }
    }
}