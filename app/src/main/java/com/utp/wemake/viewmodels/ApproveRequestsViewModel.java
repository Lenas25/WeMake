package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.utp.wemake.models.RedemptionRequest;
import com.utp.wemake.repository.BoardRepository;
import java.util.List;

public class ApproveRequestsViewModel extends ViewModel {
    private final BoardRepository boardRepository = new BoardRepository();

    private final MutableLiveData<List<RedemptionRequest>> pendingRequests = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // Variable para guardar la referencia al listener
    private ListenerRegistration requestsListener;
    private String currentBoardId;
    public LiveData<List<RedemptionRequest>> getPendingRequests() { return pendingRequests; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void startListening(String boardId) {
        if (boardId == null) return;
        this.currentBoardId = boardId;

        isLoading.setValue(true);
        // Cancela cualquier escucha anterior
        if (requestsListener != null) {
            requestsListener.remove();
        }

        requestsListener = boardRepository.listenToPendingRedemptions(boardId, (snapshot, e) -> {
            isLoading.setValue(false);
            if (e != null) {
                error.setValue("Error al escuchar las solicitudes.");
                return;
            }
            if (snapshot != null) {
                pendingRequests.setValue(snapshot.toObjects(RedemptionRequest.class));
            }
        });
    }

    /**
     * Llama al repositorio para aprobar una solicitud.
     * NO necesita recargar la lista, el listener lo hará automáticamente.
     */
    public void approveRequest(String requestId, String adminId) {
        isLoading.setValue(true);
        boardRepository.approveRedemptionRequest(currentBoardId, requestId, adminId)
                .addOnFailureListener(e -> {
                    error.setValue("Error al aprobar la solicitud.");
                    isLoading.setValue(false);
                });
    }

    /**
     * Llama al repositorio para denegar una solicitud.
     * NO necesita recargar la lista, el listener lo hará automáticamente.
     */
    public void denyRequest(RedemptionRequest request, String adminId) {
        isLoading.setValue(true);
        boardRepository.denyRedemptionRequest(currentBoardId, request, adminId)
                .addOnFailureListener(e -> {
                    error.setValue("Error al denegar la solicitud.");
                    isLoading.setValue(false);
                });
    }

    /**
     * Cancela el listener cuando el ViewModel se destruye para evitar fugas de memoria.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}