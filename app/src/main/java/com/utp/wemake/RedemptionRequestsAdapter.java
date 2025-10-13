package com.utp.wemake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.utp.wemake.models.RedemptionRequest;

public class RedemptionRequestsAdapter extends ListAdapter<RedemptionRequest, RedemptionRequestsAdapter.RequestViewHolder> {

    private final OnRequestInteractionListener listener;

    // Interfaz para notificar a la Activity cuando se hace clic en un botón
    public interface OnRequestInteractionListener {
        void onApproveClicked(RedemptionRequest request);
        void onDenyClicked(RedemptionRequest request);
    }

    public RedemptionRequestsAdapter(OnRequestInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_redemption_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RedemptionRequest request = getItem(position);
        holder.bind(request, listener);
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInfo, tvCost;
        private final MaterialButton btnApprove, btnDeny;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInfo = itemView.findViewById(R.id.tv_request_info);
            tvCost = itemView.findViewById(R.id.tv_request_cost);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnDeny = itemView.findViewById(R.id.btn_deny);
        }

        public void bind(final RedemptionRequest request, final OnRequestInteractionListener listener) {
            // Construye los textos a mostrar
            String infoText = request.getUserName() + " solicitó '" + request.getCouponTitle() + "'";
            String costText = "Costo: " + request.getCost() + " puntos";

            tvInfo.setText(infoText);
            tvCost.setText(costText);

            // Asigna los listeners a los botones
            btnApprove.setOnClickListener(v -> listener.onApproveClicked(request));
            btnDeny.setOnClickListener(v -> listener.onDenyClicked(request));
        }
    }

    // DiffUtil ayuda al ListAdapter a calcular las diferencias en la lista de forma eficiente
    private static final DiffUtil.ItemCallback<RedemptionRequest> DIFF_CALLBACK = new DiffUtil.ItemCallback<RedemptionRequest>() {
        @Override
        public boolean areItemsTheSame(@NonNull RedemptionRequest oldItem, @NonNull RedemptionRequest newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull RedemptionRequest oldItem, @NonNull RedemptionRequest newItem) {
            return oldItem.getStatus().equals(newItem.getStatus());
        }
    };
}