package com.utp.wemake; // O tu paquete de adaptadores

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utp.wemake.models.TaskProposal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProposalAdapter extends RecyclerView.Adapter<ProposalAdapter.ProposalViewHolder> {

    private final List<TaskProposal> proposals;
    private final OnProposalActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnProposalActionListener {
        void onApproveClicked(TaskProposal proposal, int reward, int penalty);
        void onDenyClicked(TaskProposal proposal);
    }

    public ProposalAdapter(List<TaskProposal> proposals, OnProposalActionListener listener) {
        this.proposals = proposals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProposalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_proposal, parent, false);
        return new ProposalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProposalViewHolder holder, int position) {
        TaskProposal proposal = proposals.get(position);
        holder.bind(proposal);
    }

    @Override
    public int getItemCount() {
        return proposals.size();
    }


    class ProposalViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvProposalInfo;
        TextInputEditText inputRewardPoints, inputPenaltyPoints;
        MaterialButton btnApprove, btnDeny;

        ProposalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvProposalInfo = itemView.findViewById(R.id.tv_proposal_info);
            inputRewardPoints = itemView.findViewById(R.id.input_reward_points);
            inputPenaltyPoints = itemView.findViewById(R.id.input_penalty_points);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnDeny = itemView.findViewById(R.id.btn_deny);
        }

        void bind(final TaskProposal proposal) {
            tvTaskTitle.setText(proposal.getTitle());

            // Obtener el nombre directamente del objeto proposal
            String proposerName = proposal.getProposerName();
            if (proposerName == null || proposerName.isEmpty()) {
                proposerName = "Usuario desconocido"; // Medida de seguridad
            }

            String info = "Propuesto por " + proposerName + " el " + dateFormat.format(proposal.getProposedAt());
            tvProposalInfo.setText(info);

            btnApprove.setOnClickListener(v -> {
                int reward = Integer.parseInt(inputRewardPoints.getText().toString());
                int penalty = Integer.parseInt(inputPenaltyPoints.getText().toString());
                listener.onApproveClicked(proposal, reward, penalty);
            });

            btnDeny.setOnClickListener(v -> listener.onDenyClicked(proposal));
        }
    }
}