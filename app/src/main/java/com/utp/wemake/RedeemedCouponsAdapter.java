package com.utp.wemake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.wemake.models.RedemptionRequest;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RedeemedCouponsAdapter extends ListAdapter<RedemptionRequest, RedeemedCouponsAdapter.RedeemedViewHolder> {

    protected RedeemedCouponsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public RedeemedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_redeemed_coupon, parent, false);
        return new RedeemedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RedeemedViewHolder holder, int position) {
        RedemptionRequest request = getItem(position);
        holder.bind(request);
    }

    static class RedeemedViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate;
        // Formateador para mostrar la fecha de forma amigable
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'de' MMMM, yyyy", new Locale("es", "ES"));

        public RedeemedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_redeemed_title);
            tvDate = itemView.findViewById(R.id.tv_redeemed_date);
        }

        public void bind(RedemptionRequest request) {
            tvTitle.setText(request.getCouponTitle());

            if (request.getRequestedAt() != null) {
                tvDate.setText("Canjeado el " + dateFormat.format(request.getRequestedAt()));
            } else {
                tvDate.setText("");
            }
        }
    }

    // DiffUtil para que el ListAdapter sea eficiente
    private static final DiffUtil.ItemCallback<RedemptionRequest> DIFF_CALLBACK = new DiffUtil.ItemCallback<RedemptionRequest>() {
        @Override
        public boolean areItemsTheSame(@NonNull RedemptionRequest oldItem, @NonNull RedemptionRequest newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull RedemptionRequest oldItem, @NonNull RedemptionRequest newItem) {
            return oldItem.getId().equals(newItem.getId()); // Inmutable, con el ID es suficiente
        }
    };
}