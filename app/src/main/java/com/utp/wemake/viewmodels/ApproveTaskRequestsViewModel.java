package com.utp.wemake.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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
    private final Application application;

    private String boardId; // ID del tablero para filtrar propuestas

    public ApproveTaskRequestsViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.taskRepository = new TaskRepository(application);
    }

    /**
     * Establece el ID del tablero y carga las propuestas correspondientes.
     * @param boardId El ID del tablero.
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        loadTaskProposals();
    }

    /**
     * Filtra por boardId
     */
    public void loadTaskProposals() {
        if (boardId == null) {
            _operationStatus.setValue(new Event<>("Error: No se especificó el tablero."));
            return;
        }

        _isLoading.setValue(true);
        taskRepository.getPendingTaskProposalsForBoard(boardId).addOnCompleteListener(task -> {
            _isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                List<TaskProposal> proposals = task.getResult().toObjects(TaskProposal.class);
                // Asignar IDs a las propuestas
                for (int i = 0; i < proposals.size(); i++) {
                    proposals.get(i).setId(task.getResult().getDocuments().get(i).getId());
                }
                _proposals.setValue(proposals);
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