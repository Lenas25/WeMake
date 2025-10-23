package com.utp.wemake.repository;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.utp.wemake.models.Member;
import com.utp.wemake.models.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemberRepository {

    private static final String COLLECTION_MEMBERS_DETAILS = "members_details";
    private static final String COLLECTION_BOARDS = "boards";
    private static final String COLLECTION_USERS = "users";
    private static final String TAG = "MemberRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public MemberRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<List<Map<String, Object>>> getBoardMembers(String boardId) {
        return db.collection(COLLECTION_BOARDS).document(boardId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw task.getException();
            }

            DocumentSnapshot boardSnapshot = task.getResult();
            List<String> memberIds = (List<String>) boardSnapshot.get("members");

            if (memberIds == null || memberIds.isEmpty()) {
                return Tasks.forResult(new ArrayList<>());
            }

            List<Task<Map<String, Object>>> tasks = new ArrayList<>();
            for (String memberId : memberIds) {
                tasks.add(getMemberDetails(boardId, memberId));
            }

            return Tasks.whenAllSuccess(tasks).continueWith(allTasks -> {
                List<Object> resultsWithNulls = allTasks.getResult();
                List<Map<String, Object>> filteredResults = new ArrayList<>();
                for (Object result : resultsWithNulls) {
                    if (result != null) {
                        filteredResults.add((Map<String, Object>) result);
                    }
                }

                return filteredResults;
            });
        });
    }

    private Task<Map<String, Object>> getMemberDetails(String boardId, String memberId) {
        Task<DocumentSnapshot> userTask = db.collection(COLLECTION_USERS).document(memberId).get();
        Task<DocumentSnapshot> memberDetailsTask = db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS_DETAILS)
                .document(memberId)
                .get();

        return Tasks.whenAll(userTask, memberDetailsTask).continueWith(task -> {
            DocumentSnapshot userSnapshot = userTask.getResult();
            DocumentSnapshot memberDetailsSnapshot = memberDetailsTask.getResult();

            if (!userSnapshot.exists() || !memberDetailsSnapshot.exists()) {
                Log.w(TAG, "No se encontró el usuario o los detalles del miembro para el ID: " + memberId);
                return null;
            }

            User user = userSnapshot.toObject(User.class);
            Member member = memberDetailsSnapshot.toObject(Member.class);

            Map<String, Object> memberData = new HashMap<>();
            memberData.put("user", user);
            memberData.put("member", member);

            return memberData;
        });
    }

    public Task<Void> updateMemberRole(String boardId, String userId, String newRole) {
        DocumentReference memberRef = db.collection(COLLECTION_BOARDS)
                .document(boardId)
                .collection(COLLECTION_MEMBERS_DETAILS)
                .document(userId);
        return memberRef.update("role", newRole);
    }

    public Task<Void> deleteMember(String boardId, String userId) {
        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);
        DocumentReference memberDetailsRef = boardRef.collection(COLLECTION_MEMBERS_DETAILS).document(userId);

        WriteBatch batch = db.batch();

        batch.delete(memberDetailsRef);

        batch.update(boardRef, "members", FieldValue.arrayRemove(userId));

        return batch.commit();
    }

    public Task<Void> addMember(String boardId, String userId) {
        Member newMember = new Member("user", 0);

        DocumentReference boardRef = db.collection(COLLECTION_BOARDS).document(boardId);
        DocumentReference memberDetailsRef = boardRef.collection(COLLECTION_MEMBERS_DETAILS).document(userId);

        WriteBatch batch = db.batch();
        batch.set(memberDetailsRef, newMember);

        batch.update(boardRef, "members", FieldValue.arrayUnion(userId));

        return batch.commit();
    }

    public Task<List<User>> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }

        String lowerCaseQuery = query.toLowerCase().trim();

        Query nameQuery = db.collection(COLLECTION_USERS)
                .orderBy("name")
                .startAt(lowerCaseQuery)
                .endAt(lowerCaseQuery + "\uf8ff")
                .limit(10);

        Query emailQuery = db.collection(COLLECTION_USERS)
                .orderBy("email")
                .startAt(lowerCaseQuery)
                .endAt(lowerCaseQuery + "\uf8ff")
                .limit(10);

        Task<QuerySnapshot> nameTask = nameQuery.get();
        Task<QuerySnapshot> emailTask = emailQuery.get();

        return Tasks.whenAll(nameTask, emailTask).continueWith(task -> {
            HashMap<String, User> userMap = new HashMap<>();

            if (nameTask.isSuccessful()) {
                QuerySnapshot nameResults = nameTask.getResult();
                if (nameResults != null) {
                    for (DocumentSnapshot doc : nameResults) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserid(doc.getId());
                            userMap.put(doc.getId(), user);
                        }
                    }
                }
            } else {
                Log.e(TAG, "La búsqueda por nombre falló", nameTask.getException());
            }

            if (emailTask.isSuccessful()) {
                QuerySnapshot emailResults = emailTask.getResult();
                if (emailResults != null) {
                    for (DocumentSnapshot doc : emailResults) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserid(doc.getId());
                            userMap.put(doc.getId(), user);
                        }
                    }
                }
            } else {
                Log.e(TAG, "La búsqueda por email falló", emailTask.getException());
            }

            if (!nameTask.isSuccessful() && !emailTask.isSuccessful()) {
                throw new Exception("Ambas búsquedas fallaron.", task.getException());
            }

            return new ArrayList<>(userMap.values());
        });
    }

    /**
     * NUEVO MÉTODO: Verifica si el usuario actual tiene el rol de "admin"
     * en un tablero específico.
     *
     * @param boardId El ID del tablero a verificar.
     * @return Una Tarea que resultará en 'true' si es admin, 'false' en caso contrario.
     */
    public Task<Boolean> isCurrentUserAdminOfBoard(String boardId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forResult(false);
        }
        String currentUserId = currentUser.getUid();

        return db.collection("boards").document(boardId)
                .collection("members_details").document(currentUserId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d("RepoAdminCheck", "Documento de miembro encontrado en members_details. Datos: " + document.getData());
                            String role = document.getString("role");
                            Log.d("RepoAdminCheck", "Valor del campo 'role': " + role);
                            return "admin".equals(role);
                        } else {
                            Log.w("RepoAdminCheck", "Documento de miembro NO encontrado en la ruta.");
                            return false;
                        }
                    }
                    Log.e("RepoAdminCheck", "Error en la tarea al obtener documento de miembro.", task.getException());
                    return false;
                });
    }

}