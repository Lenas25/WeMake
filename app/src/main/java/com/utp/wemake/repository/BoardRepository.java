package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.utp.wemake.models.Board;
import com.utp.wemake.models.Member;

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


}