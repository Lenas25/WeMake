package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.utp.wemake.models.Member;
import com.utp.wemake.constants.Roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Log;

/**
 * Repositorio para manejar las operaciones de miembros en Firebase
 * Implementa el patrón Repository para separar la lógica de datos
 */
public class MemberRepository {
    
    private static final String COLLECTION_MEMBERS = "members";
    private static final String COLLECTION_BOARDS = "boards";
    private static final String COLLECTION_USERS = "users";
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    public MemberRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Busca usuarios por nombre o email en la colección de usuarios
     * @param query Texto de búsqueda
     * @param callback Callback para manejar el resultado
     */
    public void searchUsers(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        
        // Crear una lista para almacenar todos los resultados
        List<Member> allResults = new ArrayList<>();
        AtomicInteger completedQueries = new AtomicInteger(0);
        final int totalQueries = 2; // Búsqueda por nombre y por email
        
        // Función para verificar si todas las consultas han terminado
        Runnable checkCompletion = () -> {
            if (completedQueries.incrementAndGet() == totalQueries) {
                // Eliminar duplicados basándose en el ID
                List<Member> uniqueResults = new ArrayList<>();
                Set<String> seenIds = new HashSet<>();
                
                for (Member member : allResults) {
                    if (!seenIds.contains(member.getId())) {
                        uniqueResults.add(member);
                        seenIds.add(member.getId());
                    }
                }
                
                callback.onSuccess(uniqueResults);
            }
        };
        
        // Búsqueda por nombre
        db.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("name", lowerQuery)
                .whereLessThanOrEqualTo("name", lowerQuery + "\uf8ff")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            Member member = doc.toObject(Member.class);
                            if (member != null) {
                                // Asignar el ID del documento
                                member.setId(doc.getId());
                                allResults.add(member);
                            }
                        }
                    }
                    checkCompletion.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("MemberRepository", "Error searching by name: " + e.getMessage());
                    checkCompletion.run();
                });
        
        // Búsqueda por email
        db.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("email", lowerQuery)
                .whereLessThanOrEqualTo("email", lowerQuery + "\uf8ff")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            Member member = doc.toObject(Member.class);
                            if (member != null) {
                                // Asignar el ID del documento
                                member.setId(doc.getId());
                                allResults.add(member);
                            }
                        }
                    }
                    checkCompletion.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("MemberRepository", "Error searching by email: " + e.getMessage());
                    checkCompletion.run();
                });
    }

    /**
     * Crea o actualiza un usuario en la colección de usuarios
     * @param user Usuario de Firebase Auth
     * @param callback Callback para manejar el resultado
     */
    public void createOrUpdateUser(FirebaseUser user, MemberCallback callback) {
        if (user == null) {
            callback.onError(new Exception("Usuario no válido"));
            return;
        }
        
        Member member = new Member(
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "Usuario",
                user.getEmail(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                Roles.USER
        );
        
        db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(member.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(member))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Obtiene todos los miembros de un tablero
     * @param boardId ID del tablero
     * @param callback Callback para manejar el resultado
     */
    public void getBoardMembers(String boardId, MembersCallback callback) {
        db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Member> members = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            Member member = doc.toObject(Member.class);
                            if (member != null) {
                                // Asignar el ID del documento
                                member.setId(doc.getId());
                                members.add(member);
                            }
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Agrega un miembro a un tablero
     * @param boardId ID del tablero
     * @param member Miembro a agregar
     * @param callback Callback para manejar el resultado
     */
    public void addMemberToBoard(String boardId, Member member, MemberCallback callback) {
        // Validar que el miembro tenga un ID válido
        if (member.getId() == null || member.getId().trim().isEmpty()) {
            callback.onError(new Exception("El miembro no tiene un ID válido"));
            return;
        }
        
        // Validar que el boardId sea válido
        if (boardId == null || boardId.trim().isEmpty()) {
            callback.onError(new Exception("El ID del tablero no es válido"));
            return;
        }
        
        member.setBoardId(boardId);
        member.setAdded(true);
        
        db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS)
                .document(member.getId())
                .set(member.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(member))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Actualiza el rol de un miembro
     * @param boardId ID del tablero
     * @param memberId ID del miembro
     * @param newRole Nuevo rol
     * @param callback Callback para manejar el resultado
     */
    public void updateMemberRole(String boardId, String memberId, String newRole, MemberCallback callback) {
        db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS)
                .document(memberId)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    // Obtener el miembro actualizado
                    db.collection(COLLECTION_BOARDS)
                            .document(boardId)
                            .collection(COLLECTION_MEMBERS)
                            .document(memberId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Member member = documentSnapshot.toObject(Member.class);
                                    callback.onSuccess(member);
                                } else {
                                    callback.onError(new Exception("Miembro no encontrado"));
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Elimina un miembro de un tablero
     * @param boardId ID del tablero
     * @param memberId ID del miembro
     * @param callback Callback para manejar el resultado
     */
    public void removeMemberFromBoard(String boardId, String memberId, VoidCallback callback) {
        db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS)
                .document(memberId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Obtiene el usuario actual
     * @return Member con los datos del usuario actual
     */
    public Member getCurrentUser() {
        if (auth.getCurrentUser() != null) {
            return new Member(
                    auth.getCurrentUser().getUid(),
                    auth.getCurrentUser().getDisplayName() != null ? 
                            auth.getCurrentUser().getDisplayName() : "Usuario",
                    auth.getCurrentUser().getEmail(),
                    auth.getCurrentUser().getPhotoUrl() != null ? 
                            auth.getCurrentUser().getPhotoUrl().toString() : "",
                    Roles.USER
            );
        }
        return null;
    }

    // Interfaces para callbacks
    public interface SearchCallback {
        void onSuccess(List<Member> members);
        void onError(Exception e);
    }

    public interface MembersCallback {
        void onSuccess(List<Member> members);
        void onError(Exception e);
    }

    public interface MemberCallback {
        void onSuccess(Member member);
        void onError(Exception e);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
