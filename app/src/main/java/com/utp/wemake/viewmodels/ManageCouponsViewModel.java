package com.utp.wemake.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.ListenerRegistration; // Importar
import com.utp.wemake.models.Coupon;
import com.utp.wemake.repository.BoardRepository;
import java.util.List;

public class ManageCouponsViewModel extends ViewModel {
    private final BoardRepository boardRepository = new BoardRepository();
    private final MutableLiveData<List<Coupon>> coupons = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private String currentBoardId;

    private ListenerRegistration couponsListener;

    public LiveData<List<Coupon>> getCoupons() { return coupons; }
    public LiveData<String> getError() { return error; }

    public void init(String boardId) {
        if (this.currentBoardId == null) {
            this.currentBoardId = boardId;
            listenToCoupons();
        }
    }

    /**
     * Se suscribe a los cambios en la colección de cupones.
     */
    private void listenToCoupons() {
        if (couponsListener != null) {
            couponsListener.remove(); // Cancela cualquier escucha anterior
        }

        couponsListener = boardRepository.listenToCouponsForBoard(currentBoardId, (querySnapshot, e) -> {
            if (e != null) {
                error.setValue("Error al escuchar los cupones.");
                return;
            }

            if (querySnapshot != null) {
                // Convierte el resultado a una lista de objetos Coupon y la publica
                List<Coupon> couponList = querySnapshot.toObjects(Coupon.class);
                coupons.setValue(couponList);
            }
        });
    }

    public void createCoupon(Coupon coupon) {
        boardRepository.createCoupon(currentBoardId, coupon)
                .addOnFailureListener(e -> error.setValue("Error al crear el cupón."));
    }

    public void updateCoupon(Coupon coupon) {
        boardRepository.updateCoupon(currentBoardId, coupon)
                .addOnFailureListener(e -> error.setValue("Error al actualizar el cupón."));
    }

    public void deleteCoupon(Coupon coupon) {
        boardRepository.deleteCoupon(currentBoardId, coupon.getId())
                .addOnFailureListener(e -> error.setValue("Error al eliminar el cupón."));
    }

    /**
     * Se llama automáticamente cuando el ViewModel se destruye.
     * Es CRUCIAL cancelar el listener para evitar fugas de memoria.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (couponsListener != null) {
            couponsListener.remove();
        }
    }
}