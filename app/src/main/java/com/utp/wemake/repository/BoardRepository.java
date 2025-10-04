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

        // 1. Crea una referencia para el nuevo documento de tablero
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document();

        // 2. Crea una referencia para el documento del miembro DENTRO de la subcolección
        DocumentReference memberDetailsRef = boardRef.collection("members_details").document(currentUserId);

        Member firstMember = new Member("admin", 0); // Rol de admin, 0 puntos al inicio

        // 4. Crea la escritura por lotes (batch)
        WriteBatch batch = db.batch();

        // 5. Añade las dos operaciones al lote
        batch.set(boardRef, board);       // Escribe los datos del tablero
        batch.set(memberDetailsRef, firstMember); // Escribe los datos del miembro en la subcolección

        // 6. Ejecuta el lote. O ambas tienen éxito, o ambas fallan.
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

    /**
     * Añade un usuario a un tablero usando una escritura por lotes.
     */
    public Task<Void> addMemberToBoard(String boardId, String userId) {
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);
        DocumentReference memberDetailsRef = boardRef.collection("members_details").document(userId);

        WriteBatch batch = db.batch();
        batch.update(boardRef, "members", FieldValue.arrayUnion(userId));
        batch.set(memberDetailsRef, new Member("member", 0)); // Rol por defecto 'member', 0 puntos

        return batch.commit();
    }

    /**
     * Actualiza el rol de un miembro en la subcolección members_details.
     */
    public Task<Void> updateMemberRole(String boardId, String userId, String newRole) {
        return db.collection(COLLECTION_BOARDS).document(boardId)
                .collection("members_details").document(userId)
                .update("role", newRole);
    }

    /**
     * Elimina un usuario de un tablero usando una escritura por lotes.
     * (Ya lo tenías, solo asegúrate de que esté correcto).
     */
    public Task<Void> removeMemberFromBoard(String boardId, String userId) {
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);
        DocumentReference memberDetailsRef = boardRef.collection("members_details").document(userId);

        WriteBatch batch = db.batch();
        batch.update(boardRef, "members", FieldValue.arrayRemove(userId));
        batch.delete(memberDetailsRef);

        return batch.commit();
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
     * Obtiene los detalles (rol, puntos) de todos los miembros de un tablero.
     * @param boardId El ID del tablero.
     * @return Una Tarea que, al completarse, contiene un Map<UserId, Member>.
     */
    public Task<Map<String, Member>> getMembersDetailsForBoard(String boardId) {
        if (boardId == null) return Tasks.forException(new IllegalArgumentException("Board ID cannot be null"));

        return db.collection(COLLECTION_BOARDS).document(boardId).collection("members_details")
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Member> detailsMap = new HashMap<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Member member = doc.toObject(Member.class);
                            if (member != null) {
                                // La clave del mapa es el ID del documento (que es el UserId)
                                detailsMap.put(doc.getId(), member);
                            }
                        }
                        return detailsMap;
                    }
                    throw task.getException();
                });
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
        // TODO: En una app real, también deberías eliminar todas las tareas y miembros
        // de este tablero, posiblemente usando una Cloud Function.
        return db.collection(COLLECTION_BOARDS).document(boardId).delete();
    }
}