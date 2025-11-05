package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;
import com.utp.wemake.models.TaskProposal;
import com.utp.wemake.constants.TaskConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskRepository {
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_TASK_PROPOSALS = "task_proposals";
    private final FirebaseFirestore db;
    private final MemberRepository memberRepository;
    private final CollectionReference proposalsCollection;
    private final CollectionReference tasksCollection;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.proposalsCollection = db.collection(COLLECTION_TASK_PROPOSALS);
        tasksCollection = db.collection(COLLECTION_TASKS);
        this.memberRepository = new MemberRepository();
    }

    public interface OnTasksUpdatedListener {
        void onTasksUpdated(List<TaskModel> tasks);
        void onError(Exception e);
    }

    /**
     * Crea una nueva propuesta de tarea en la colección 'task_proposals'.
     * Usado por usuarios no-administradores.
     */
    public Task<DocumentReference> createTaskProposal(TaskProposal proposal) {
        return db.collection(COLLECTION_TASK_PROPOSALS).add(proposal);
    }

    /**
     * Crea una nueva tarea directamente en la colección 'tasks'.
     * Usado por administradores.
     */
    public Task<DocumentReference> createTask(TaskModel task) {
        return db.collection(COLLECTION_TASKS).add(task);
    }

    /**
     * Obtiene una tarea específica por su ID.
     */
    public Task<DocumentSnapshot> getTaskById(String taskId) {
        return db.collection(COLLECTION_TASKS).document(taskId).get();
    }

    /**
     * Actualiza una tarea existente.
     * El objeto TaskModel debe tener su ID ya establecido.
     */
    public Task<Void> updateTask(TaskModel task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Task ID must not be null for update."));
        }
        return db.collection(COLLECTION_TASKS).document(task.getId()).set(task);
    }

    /**
     * Obtiene todas las propuestas de tareas que están pendientes de aprobación.
     */
    public Task<QuerySnapshot> getPendingTaskProposals() {
        return proposalsCollection.whereEqualTo("status", "awaiting_approval").get();
    }

    /**
     * Aprueba una propuesta, moviéndola a la colección de tareas y eliminando la original.
     * @param proposalId El ID de la propuesta a aprobar.
     * @param newTask El objeto TaskModel completo que se creará.
     * @return Una Tarea que se completa cuando el batch termina.
     */
    public Task<Void> approveProposal(String proposalId, TaskModel newTask) {
        DocumentReference proposalRef = proposalsCollection.document(proposalId);
        DocumentReference taskRef = db.collection(COLLECTION_TASKS).document(); // ID automático

        WriteBatch batch = db.batch();
        batch.set(taskRef, newTask); // Crear la nueva tarea
        batch.delete(proposalRef); // Eliminar la propuesta
        return batch.commit();
    }

    /**
     * Rechaza (elimina) una propuesta de tarea.
     * @param proposalId El ID de la propuesta a rechazar.
     */
    public Task<Void> denyProposal(String proposalId) {
        return proposalsCollection.document(proposalId).delete();
    }

    /**
     * Inicia la escucha en tiempo real de las tareas del usuario.
     */
    public void listenToTasksForUserInBoard(String boardId, String userId, final OnTasksUpdatedListener listener) {
        detachListeners();

        // Mapas para almacenar los resultados de cada consulta por separado
        Map<String, TaskModel> assignedTasksMap = new ConcurrentHashMap<>();
        Map<String, TaskModel> reviewerTasksMap = new ConcurrentHashMap<>();

        // --- Listener 1: Para tareas donde el usuario es miembro asignado ---
        Query assignedQuery = tasksCollection
                .whereEqualTo("boardId", boardId)
                .whereArrayContains("assignedMembers", userId);

        ListenerRegistration assignedListener = assignedQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                listener.onError(e);
                return;
            }
            // Actualizar nuestro mapa con los nuevos datos
            assignedTasksMap.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                TaskModel task = doc.toObject(TaskModel.class);
                if (task != null) {
                    task.setId(doc.getId());
                    assignedTasksMap.put(doc.getId(), task);
                }
            }
            // Combinar y notificar
            listener.onTasksUpdated(combineResults(assignedTasksMap, reviewerTasksMap));
        });
        activeListeners.add(assignedListener);

        // --- Listener 2: Para tareas donde el usuario es el revisor ---
        Query reviewerQuery = tasksCollection
                .whereEqualTo("boardId", boardId)
                .whereEqualTo("reviewerId", userId);

        ListenerRegistration reviewerListener = reviewerQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                listener.onError(e);
                return;
            }
            // Actualizar nuestro mapa con los nuevos datos
            reviewerTasksMap.clear();
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                TaskModel task = doc.toObject(TaskModel.class);
                if (task != null) {
                    task.setId(doc.getId());
                    reviewerTasksMap.put(doc.getId(), task);
                }
            }
            // Combinar y notificar
            listener.onTasksUpdated(combineResults(assignedTasksMap, reviewerTasksMap));
        });
        activeListeners.add(reviewerListener);
    }

    /**
     * Método auxiliar para combinar los resultados de ambos listeners y eliminar duplicados.
     */
    private List<TaskModel> combineResults(Map<String, TaskModel> map1, Map<String, TaskModel> map2) {
        Map<String, TaskModel> combinedMap = new HashMap<>(map1);
        combinedMap.putAll(map2);
        return new ArrayList<>(combinedMap.values());
    }

    /**
     * Detiene todas las escuchas activas para prevenir memory leaks.
     */
    public void detachListeners() {
        for (ListenerRegistration listener : activeListeners) {
            listener.remove();
        }
        activeListeners.clear();
    }

    /**
     * Elimina una tarea de la base de datos.
     */
    public Task<Void> deleteTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Task ID cannot be null."));
        }
        return tasksCollection.document(taskId).delete();
    }

    /**
     * Actualiza el estado de una subtarea específica dentro de una tarea.
     * Usa un WriteBatch para asegurar la atomicidad.
     */
    public Task<Void> updateSubtaskStatus(String taskId, String subtaskId, boolean isCompleted) {

        DocumentReference taskRef = tasksCollection.document(taskId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(taskRef);
            TaskModel task = snapshot.toObject(TaskModel.class);

            if (task != null && task.getSubtasks() != null) {
                for (Subtask subtask : task.getSubtasks()) {
                    if (subtaskId.equals(subtask.getId())) {
                        subtask.setCompleted(isCompleted);
                        break;
                    }
                }
                transaction.update(taskRef, "subtasks", task.getSubtasks());
            }
            return null;
        });
    }

    /**
     * Actualiza el estado de una tarea y, si se completa, aplica los puntos de recompensa.
     * @param task La tarea que se está actualizando.
     * @param newStatus El nuevo estado.
     * @return Una Tarea que se completa cuando la operación termina.
     */
    public Task<Void> updateStatusAndApplyPoints(TaskModel task, String newStatus) {
        DocumentReference taskRef = tasksCollection.document(task.getId());

        WriteBatch batch = db.batch();

        batch.update(taskRef, "status", newStatus);

        if (TaskConstants.STATUS_COMPLETED.equals(newStatus)) {
            batch.update(taskRef, "completedAt", new Date());

            batch = memberRepository.addPointsToMembersBatch(
                    batch,
                    task.getBoardId(),
                    task.getAssignedMembers(),
                    task.getRewardPoints()
            );
        }

        return batch.commit();
    }

    public Task<Void> applyPenaltyAndMarkTask(TaskModel task) {
        DocumentReference taskRef = tasksCollection.document(task.getId());
        WriteBatch batch = db.batch();

        batch.update(taskRef, "penaltyApplied", true);

        batch = memberRepository.addPointsToMembersBatch(
                batch,
                task.getBoardId(),
                task.getAssignedMembers(),
                -task.getPenaltyPoints()
        );

        return batch.commit();
    }
}