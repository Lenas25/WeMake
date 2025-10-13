package com.utp.wemake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.utp.wemake.models.Coupon;
import java.util.ArrayList;
import java.util.List;

public class CouponsAdapter extends RecyclerView.Adapter<CouponsAdapter.CouponViewHolder> {

    private List<Coupon> couponList = new ArrayList<>();
    private final OnRedeemClickListener listener;
    private int userPoints = 0;

    public interface OnRedeemClickListener {
        void onRedeemClicked(Coupon coupon);
    }

    public CouponsAdapter(OnRedeemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Coupon> coupons) {
        this.couponList = coupons;
        notifyDataSetChanged();
    }

    public void setUserPoints(int points) {
        this.userPoints = points;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coupon, parent, false);
        return new CouponViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CouponViewHolder holder, int position) {
        Coupon coupon = couponList.get(position);
        holder.bind(coupon, userPoints, listener);
    }

    @Override
    public int getItemCount() {
        return couponList.size();
    }

    static class CouponViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDescription;
        private final MaterialButton btnRedeem;

        public CouponViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_coupon_title);
            tvDescription = itemView.findViewById(R.id.tv_coupon_description);
            btnRedeem = itemView.findViewById(R.id.btn_redeem);
        }

        public void bind(Coupon coupon, int userPoints, OnRedeemClickListener listener) {
            tvTitle.setText(coupon.getTitle());
            tvDescription.setText(coupon.getDescription());
            btnRedeem.setText(String.valueOf(coupon.getCost()));

            // Lógica para habilitar/deshabilitar el botón
            if (userPoints >= coupon.getCost()) {
                btnRedeem.setEnabled(true);
                btnRedeem.setOnClickListener(v -> listener.onRedeemClicked(coupon));
            } else {
                btnRedeem.setEnabled(false);
                btnRedeem.setOnClickListener(null);
            }
        }
    }
}