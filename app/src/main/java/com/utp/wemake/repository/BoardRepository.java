package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.Coupon;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.RedemptionRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final String COLLECTION_BOARDS = "boards";

    /**
     * Obtener los tableros a los que pertence un usuario.
     */
    public Task<QuerySnapshot> getBoardsForCurrentUser() {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection(COLLECTION_BOARDS).whereArrayContains("members", uid).get();
    }

    /**
     * Crea un nuevo documento de tablero en la colección 'boards'.
     */
    public Task<Void> createBoard(Board board) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            return Tasks.forException(new Exception("Usuario no autenticado"));
        }

        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document();
        DocumentReference memberDetailsRef = boardRef.collection("members_details").document(currentUserId);

        Member firstMember = new Member("admin", 0); // Rol de admin, 0 puntos al inicio
        WriteBatch batch = db.batch();
        batch.set(boardRef, board);
        batch.set(memberDetailsRef, firstMember);

        return batch.commit();
    }

    /**
     * Busca un tablero por su código de invitación.
     *
     * @return Una Tarea que contendrá el QuerySnapshot con el tablero encontrado (si existe).
     */
    public Task<QuerySnapshot> findBoardByInviteCode(String code) {
        return db.collection(COLLECTION_BOARDS)
                .whereEqualTo("invitationCode", code)
                .limit(1) // Solo esperamos un resultado
                .get();
    }

    public Task<DocumentSnapshot> getBoardById(String boardId) {
        return db.collection(COLLECTION_BOARDS).document(boardId).get();
    }

    public Task<Void> updateBoard(String boardId, Board board) {
        // Usamos un Map para actualizar solo los campos que queremos cambiar
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", board.getName());
        updates.put("description", board.getDescription());
        updates.put("color", board.getColor());

        return db.collection(COLLECTION_BOARDS).document(boardId).update(updates);
    }

    public Task<Void> deleteBoard(String boardId) {
        // TODO: En una app real, también deberías eliminar todas las tareas y miembros con cloud functions
        return db.collection(COLLECTION_BOARDS).document(boardId).delete();
    }

    /**
     * Obtiene los detalles de un miembro específico (rol, puntos) dentro de un tablero.
     */
    public Task<Member> getMemberDetails(String boardId, String userId) {
        return db.collection(COLLECTION_BOARDS).document(boardId)
                .collection("members_details").document(userId)
                .get().continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(Member.class);
                    }
                    return null;
                });
    }

    /**
     * Inicia una escucha en tiempo real para la colección de cupones de un tablero.
     * @param boardId El ID del tablero.
     * @param listener El callback que se activará con cada cambio.
     * @return El registro del listener para poder cancelarlo después.
     */
    public ListenerRegistration listenToCouponsForBoard(String boardId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_BOARDS).document(boardId).collection("coupons")
                .addSnapshotListener(listener);
    }

    public Task<Void> createCoupon(String boardId, Coupon coupon) {
        return db.collection(COLLECTION_BOARDS).document(boardId).collection("coupons").add(coupon).continueWith(task -> null);
    }

    public Task<Void> updateCoupon(String boardId, Coupon coupon) {
        return db.collection(COLLECTION_BOARDS).document(boardId)
                .collection("coupons").document(coupon.getId())
                .set(coupon, SetOptions.merge()); // Merge para no borrar otros campos
    }

    public Task<Void> deleteCoupon(String boardId, String couponId) {
        return db.collection(COLLECTION_BOARDS).document(boardId)
                .collection("coupons").document(couponId)
                .delete();
    }

    public Task<Void> requestCouponRedemption(String boardId, String userId, String userName, Coupon coupon) {
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);

        DocumentReference memberRef = boardRef.collection("members_details").document(userId);

        DocumentReference requestRef = boardRef.collection("redemption_requests").document();

        return db.runTransaction(transaction -> {
            DocumentSnapshot memberSnap = transaction.get(memberRef);

            Member member = memberSnap.toObject(Member.class);

            if (member == null || member.getPoints() < coupon.getCost()) {
                throw new FirebaseFirestoreException("No tienes suficientes puntos.", FirebaseFirestoreException.Code.ABORTED);
            }

            long newPoints = member.getPoints() - coupon.getCost();
            transaction.update(memberRef, "points", newPoints);

            RedemptionRequest request = new RedemptionRequest();
            request.setUserId(userId);
            request.setUserName(userName);
            request.setCouponId(coupon.getId());
            request.setCouponTitle(coupon.getTitle());
            request.setCost(coupon.getCost());
            request.setStatus("pendiente");

            transaction.set(requestRef, request);

            return null; // La transacción fue exitosa
        });
    }

    public ListenerRegistration listenToPendingRedemptions(String boardId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_BOARDS).document(boardId).collection("redemption_requests")
                .whereEqualTo("status", "pendiente")
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public Task<Void> approveRedemptionRequest(String boardId, String requestId, String adminId) {
        DocumentReference requestRef = db.collection(COLLECTION_BOARDS).document(boardId).collection("redemption_requests").document(requestId);
        return requestRef.update("status", "aprobado", "reviewedBy", adminId, "reviewedAt", FieldValue.serverTimestamp());
    }

    public Task<Void> denyRedemptionRequest(String boardId, RedemptionRequest request, String adminId) {
        DocumentReference requestRef = db.collection(COLLECTION_BOARDS).document(boardId).collection("redemption_requests").document(request.getId());
        DocumentReference memberRef = db.collection(COLLECTION_BOARDS).document(boardId).collection("members_details").document(request.getUserId());

        return db.runTransaction(transaction -> {
            transaction.update(requestRef, "status", "denegado", "reviewedBy", adminId, "reviewedAt", FieldValue.serverTimestamp());
            transaction.update(memberRef, "points", FieldValue.increment(request.getCost()));
            return null;
        });
    }

    /**
     * Escucha en tiempo real las solicitudes de canje APROBADAS de un usuario específico en un tablero.
     */
    public ListenerRegistration listenToRedeemedCoupons(String boardId, String userId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_BOARDS).document(boardId).collection("redemption_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "aprobado")
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    /**
     * Escucha los cambios de un tablero en tiempo real.
     * @param boardId El ID del tablero.
     * @param listener El callback para manejar los cambios.
     * @return El registro del listener para poder cancelarlo después.
     */
    public ListenerRegistration listenToBoardById(String boardId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_BOARDS).document(boardId).addSnapshotListener(listener);
    }

    /**
     * Escucha los cambios de los detalles de un miembro en tiempo real.
     * @param boardId El ID del tablero.
     * @param userId El ID del usuario.
     * @param listener El callback para manejar los cambios.
     * @return El registro del listener para poder cancelarlo después.
     */
    public ListenerRegistration listenToMemberDetails(String boardId, String userId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_BOARDS).document(boardId)
                .collection("members_details").document(userId)
                .addSnapshotListener(listener);
    }
    /**
     * Une un usuario a un tablero existente. Realiza dos operaciones en un batch:
     * Añade el ID del usuario al array 'members' del tablero.
     * Crea un nuevo documento para el miembro en la subcolección 'members_details'.
     */
    public Task<Void> joinBoard(String boardId, String userId) {
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);
        DocumentReference memberDetailsRef = boardRef.collection("members_details").document(userId);

        Member newMember = new Member("member", 0);

        WriteBatch batch = db.batch();

        batch.update(boardRef, "members", FieldValue.arrayUnion(userId));

        batch.set(memberDetailsRef, newMember);

        return batch.commit();
    }

}