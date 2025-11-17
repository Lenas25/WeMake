package com.utp.wemake.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.repository.TaskRepository;
import com.utp.wemake.utils.Event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ApproveTaskRequestsViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;

    private final MutableLiveData<List<TaskProposal>> _proposals = new MutableLiveData<>();
    public LiveData<List<TaskProposal>> proposals = _proposals;

    private final MutableLiveData<Event<String>> _operationStatus = new MutableLiveData<>();
    public LiveData<Event<String>> operationStatus = _operationStatus;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    public ApproveTaskRequestsViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        loadTaskProposals();
    }

    public void loadTaskProposals() {
        _isLoading.setValue(true);
        taskRepository.getPendingTaskProposals().addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                _proposals.setValue(task.getResult().toObjects(TaskProposal.class));
            } else {
                _operationStatus.setValue(new Event<>("Error al cargar las solicitudes."));
            }
        });
    }

    public void approveTaskProposal(TaskProposal proposal, int rewardPoints, int penaltyPoints) {
        _isLoading.setValue(true);
        String adminUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        TaskModel newTask = new TaskModel();
        newTask.setTitle(proposal.getTitle());
        newTask.setDescription(proposal.getDescription());
        newTask.setDeadline(proposal.getDeadline());
        newTask.setPriority(proposal.getPriority());
        newTask.setSubtasks(proposal.getSubtasks() != null ? proposal.getSubtasks() : new ArrayList<>());
        newTask.setBoardId(proposal.getBoardId());
        newTask.setCreatedBy(proposal.getProposedBy());
        newTask.setCreatedAt(proposal.getProposedAt());
        newTask.setRewardPoints(rewardPoints);
        newTask.setPenaltyPoints(penaltyPoints);
        newTask.setApprovedBy(adminUserId);
        newTask.setApprovedAt(new Date());
        newTask.setStatus("pending");
        newTask.setAssignedMembers(proposal.getAssignedMembers());
        newTask.setReviewerId(proposal.getReviewerId());

        taskRepository.approveProposal(proposal.getId(), newTask).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _operationStatus.setValue(new Event<>("Propuesta aprobada con éxito."));
                loadTaskProposals(); // Recargar la lista
            } else {
                _isLoading.setValue(false);
                _operationStatus.setValue(new Event<>("Error al aprobar la propuesta."));
            }
        });
    }

    public void denyTaskProposal(TaskProposal proposal) {
        _isLoading.setValue(true);
        taskRepository.denyProposal(proposal.getId()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _operationStatus.setValue(new Event<>("Propuesta rechazada."));
                loadTaskProposals(); // Recargar la lista
            } else {
                _isLoading.setValue(false);
                _operationStatus.setValue(new Event<>("Error al rechazar la propuesta."));
            }
        });
    }
}