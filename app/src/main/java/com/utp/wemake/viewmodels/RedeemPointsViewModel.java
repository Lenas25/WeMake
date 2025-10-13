package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.utp.wemake.models.Coupon;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.RedemptionRequest;
import com.utp.wemake.models.User;
import com.utp.wemake.repository.BoardRepository;

import java.util.List;

public class RedeemPointsViewModel extends ViewModel {
    private final BoardRepository boardRepository = new BoardRepository();
    private ListenerRegistration couponsListener;
    private ListenerRegistration memberDetailsListener;
    private final MutableLiveData<List<RedemptionRequest>> redeemedCoupons = new MutableLiveData<>();
    private ListenerRegistration redeemedCouponsListener;

    // --- LiveData para la UI ---
    private final MutableLiveData<List<Coupon>> availableCoupons = new MutableLiveData<>();
    private final MutableLiveData<Member> memberDetails = new MutableLiveData<>();
    private final MutableLiveData<Boolean> redemptionRequestSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // --- Getters ---
    public LiveData<List<Coupon>> getAvailableCoupons() { return availableCoupons; }
    public LiveData<List<RedemptionRequest>> getRedeemedCoupons() { return redeemedCoupons; }
    public LiveData<Member> getMemberDetails() { return memberDetails; }
    public LiveData<Boolean> getRedemptionRequestSuccess() { return redemptionRequestSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * Inicia la escucha en tiempo real para los cupones y los puntos del usuario.
     */
    public void startListening(String boardId, String userId) {
        isLoading.setValue(true);
        removeListeners();

        couponsListener = boardRepository.listenToCouponsForBoard(boardId, (snapshot, e) -> {
            if (e != null) {
                error.setValue("Error al cargar las recompensas.");
                isLoading.setValue(false); // Detiene la carga si hay error aquí
                return;
            }
            if (snapshot != null) {
                availableCoupons.setValue(snapshot.toObjects(Coupon.class));
            }
        });

        memberDetailsListener = boardRepository.listenToMemberDetails(boardId, userId, (snapshot, e) -> {
            isLoading.setValue(false); // La carga inicial termina cuando llegan los datos del usuario
            if (e != null) {
                error.setValue("Error al cargar tus puntos.");
                memberDetails.setValue(null);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                memberDetails.setValue(snapshot.toObject(Member.class));
            } else {
                memberDetails.setValue(null); // El usuario ya no es miembro
            }
        });

        redeemedCouponsListener = boardRepository.listenToRedeemedCoupons(boardId, userId, (snapshot, e) -> {
            if (e != null) {
                error.setValue("Error al cargar tus recompensas canjeadas.");
                return;
            }
            if (snapshot != null) {
                redeemedCoupons.setValue(snapshot.toObjects(RedemptionRequest.class));
            }
        });
    }

    /**
     * Inicia la transacción para solicitar el canje de un cupón.
     * La lógica de este método no cambia.
     */
    public void redeemCoupon(String boardId, User user, Coupon coupon) {
        isLoading.setValue(true);
        boardRepository.requestCouponRedemption(boardId, user.getUserid(), user.getName(), coupon)
                .addOnSuccessListener(aVoid -> {
                    redemptionRequestSuccess.setValue(true);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    redemptionRequestSuccess.setValue(false);
                    error.setValue(e.getMessage());
                    isLoading.setValue(false);
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }

    /**
     * Método de ayuda para cancelar y limpiar todos los listeners activos.
     */
    private void removeListeners() {
        if (couponsListener != null) {
            couponsListener.remove();
            couponsListener = null;
        }
        if (memberDetailsListener != null) {
            memberDetailsListener.remove();
            memberDetailsListener = null;
        }
        if (redeemedCouponsListener != null) {
            redeemedCouponsListener.remove();
            redeemedCouponsListener = null;
        }
    }

    /**
     * Resetea el estado del LiveData de éxito para que el evento no se dispare de nuevo.
     */
    public void doneShowingSuccess() {
        redemptionRequestSuccess.setValue(null);
    }

    /**
     * Resetea el estado del LiveData de error.
     */
    public void doneShowingError() {
        error.setValue(null);
    }

}