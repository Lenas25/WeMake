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
import com.google.android.material.chip.Chip;
import com.utp.wemake.models.Coupon;

public class CouponsManagementAdapter extends ListAdapter<Coupon, CouponsManagementAdapter.CouponViewHolder> {

    private final OnCouponInteractionListener listener;

    public interface OnCouponInteractionListener {
        void onEditClicked(Coupon coupon);
        void onDeleteClicked(Coupon coupon);
    }

    public CouponsManagementAdapter(OnCouponInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coupon_management, parent, false);
        return new CouponViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CouponViewHolder holder, int position) {
        Coupon coupon = getItem(position);
        holder.bind(coupon, listener);
    }

    static class CouponViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDescription;
        private final Chip chipCost;
        private final MaterialButton btnEdit, btnDelete;

        public CouponViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_coupon_title);
            tvDescription = itemView.findViewById(R.id.tv_coupon_description);
            chipCost = itemView.findViewById(R.id.chip_cost);
            btnEdit = itemView.findViewById(R.id.btn_edit_coupon);
            btnDelete = itemView.findViewById(R.id.btn_delete_coupon);
        }

        public void bind(Coupon coupon, OnCouponInteractionListener listener) {
            tvTitle.setText(coupon.getTitle());
            tvDescription.setText(coupon.getDescription());
            chipCost.setText(String.valueOf(coupon.getCost()));

            btnEdit.setOnClickListener(v -> listener.onEditClicked(coupon));
            btnDelete.setOnClickListener(v -> listener.onDeleteClicked(coupon));
        }
    }

    private static final DiffUtil.ItemCallback<Coupon> DIFF_CALLBACK = new DiffUtil.ItemCallback<Coupon>() {
        @Override
        public boolean areItemsTheSame(@NonNull Coupon oldItem, @NonNull Coupon newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Coupon oldItem, @NonNull Coupon newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    oldItem.getDescription().equals(newItem.getDescription()) &&
                    oldItem.getCost() == newItem.getCost();
        }
    };
}