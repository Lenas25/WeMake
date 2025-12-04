package com.utp.wemake.repository;

import android.app.Application;

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
import com.utp.wemake.db.AppDatabase;
import com.utp.wemake.db.TaskDao;
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
import java.util.concurrent.Executors;

public class TaskRepository {
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_TASK_PROPOSALS = "task_proposals";
    private final FirebaseFirestore db;
    private final MemberRepository memberRepository;
    private final CollectionReference proposalsCollection;
    private final CollectionReference tasksCollection;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    private final TaskDao taskDao;

    public TaskRepository(Application application) {
        this.db = FirebaseFirestore.getInstance();
        this.proposalsCollection = db.collection(COLLECTION_TASK_PROPOSALS);
        tasksCollection = db.collection(COLLECTION_TASKS);
        this.memberRepository = new MemberRepository();
        AppDatabase database = AppDatabase.getDatabase(application);
        this.taskDao = database.taskDao();
    }

    public interface OnTasksUpdatedListener {
        void onTasksUpdated(List<TaskModel> tasks);
        void onError(Exception e);
    }

    /**
     * Guarda una PROPUESTA de tarea localmente y activa la sincronización.
     */
    public void createTaskProposalOffline(TaskProposal proposal) {
        // 1. Crear un objeto TaskModel vacío.
        TaskModel taskToSave = new TaskModel();

        // 2. Copiar todos los datos relevantes del objeto 'proposal' al 'taskToSave'.
        taskToSave.setTitle(proposal.getTitle());
        taskToSave.setDescription(proposal.getDescription());
        taskToSave.setDeadline(proposal.getDeadline());
        taskToSave.setPriority(proposal.getPriority());
        taskToSave.setSubtasks(proposal.getSubtasks());
        taskToSave.setBoardId(proposal.getBoardId());
        taskToSave.setCreatedBy(proposal.getProposedBy());
        taskToSave.setCreatedAt(proposal.getProposedAt());

        // Copiar los campos sugeridos por el usuario, si existen en el modelo TaskProposal
        taskToSave.setAssignedMembers(proposal.getAssignedMembers());
        taskToSave.setReviewerId(proposal.getReviewerId());

        // 3. Establecer los "flags" especiales para la sincronización.
        taskToSave.setProposal(true);
        taskToSave.setSynced(false);

        // 4. Ejecutar la operación de guardado en la base de datos en un hilo de fondo.
        new Thread(() -> {
            taskDao.insertTask(taskToSave);
        }).start();
    }

    public void saveTaskLocally(TaskModel task) {
        // Ejecutar en un hilo de fondo para no bloquear la UI.
        Executors.newSingleThreadExecutor().execute(() -> {
            taskDao.insertTask(task);
        });
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
     * Obtiene las propuestas de tareas pendientes de aprobación para un tablero específico.
     * @param boardId El ID del tablero para filtrar las propuestas.
     */
    public Task<QuerySnapshot> getPendingTaskProposalsForBoard(String boardId) {
        return proposalsCollection
                .whereEqualTo("status", "awaiting_approval")
                .whereEqualTo("boardId", boardId)
                .get();
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

    public void listenToTasksForUserInBoards(List<String> boardIds, String userId, final OnTasksUpdatedListener listener) {
        detachListeners(); // resetea listeners previos de cualquier pantalla

        // Map combinado por taskId
        Map<String, TaskModel> combined = new ConcurrentHashMap<>();

        for (String boardId : boardIds) {
            // 1) Tareas asignadas al usuario en el board
            Query assignedQuery = tasksCollection
                    .whereEqualTo("boardId", boardId)
                    .whereArrayContains("assignedMembers", userId);

            ListenerRegistration assignedListener = assignedQuery.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    listener.onError(e);
                    return;
                }
                if (snapshots != null) {
                    // Limpiar solo las tareas de este board para este “scope” y reponer
                    // En lugar de limpiar, actualizamos entradas específicas
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        if (task != null) {
                            task.setId(doc.getId());
                            if (task.getBoardId() == null) task.setBoardId(boardId);
                            combined.put(doc.getId(), task);
                        }
                    }
                    listener.onTasksUpdated(new ArrayList<>(combined.values()));
                }
            });
            activeListeners.add(assignedListener);

            // 2) Tareas donde el usuario es revisor en el board
            Query reviewerQuery = tasksCollection
                    .whereEqualTo("boardId", boardId)
                    .whereEqualTo("reviewerId", userId);

            ListenerRegistration reviewerListener = reviewerQuery.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    listener.onError(e);
                    return;
                }
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        TaskModel task = doc.toObject(TaskModel.class);
                        if (task != null) {
                            task.setId(doc.getId());
                            if (task.getBoardId() == null) task.setBoardId(boardId);
                            combined.put(doc.getId(), task);
                        }
                    }
                    listener.onTasksUpdated(new ArrayList<>(combined.values()));
                }
            });
            activeListeners.add(reviewerListener);
        }
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
     * @param taskId La tarea que se está actualizando.
     * @param newStatus El nuevo estado.
     * @return Una Tarea que se completa cuando la operación termina.
     */
    public Task<Void> updateStatusAndApplyPoints(String taskId, String newStatus) {
        DocumentReference taskRef = tasksCollection.document(taskId);

        return taskRef.get().onSuccessTask(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                throw new Exception("La tarea con ID " + taskId + " no existe.");
            }
            TaskModel task = documentSnapshot.toObject(TaskModel.class);
            if (task == null) {
                throw new Exception("No se pudo deserializar la tarea.");
            }

            WriteBatch batch = db.batch();

            batch.update(taskRef, "status", newStatus);

            if (TaskConstants.STATUS_COMPLETED.equals(newStatus)) {
                batch.update(taskRef, "completedAt", new Date());

                if (task.getRewardPoints() > 0 && task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
                    batch = memberRepository.addPointsToMembersBatch(
                            batch,
                            task.getBoardId(),
                            task.getAssignedMembers(),
                            task.getRewardPoints()
                    );
                }
            }

            // 3. Ejecutar todas las operaciones en el batch
            return batch.commit();
        });
    }

    /**
     * Actualiza únicamente el campo de prioridad de una tarea.
     * @param taskId La tarea que se está actualizando.
     * @param newPriority El nuevo valor de prioridad como TaskConstants.PRIORITY_HIGH
     * @return Una Tarea que se completa cuando la operación termina.
     */
    public Task<Void> updatePriority(String taskId, String newPriority) {
        if (taskId == null || taskId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Task ID cannot be null for priority update."));
        }

        DocumentReference taskRef = tasksCollection.document(taskId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("priority", newPriority);

        // Actualizar la base de datos
        return taskRef.update(updates);
    }

    public Task<Void> processOverdueTask(TaskModel task) {
        DocumentReference taskRef = tasksCollection.document(task.getId());
        WriteBatch batch = db.batch();

        if (task.getPenaltyPoints() > 0 && task.getAssignedMembers() != null && !task.getAssignedMembers().isEmpty()) {
            batch = memberRepository.addPointsToMembersBatch(
                    batch,
                    task.getBoardId(),
                    task.getAssignedMembers(),
                    -task.getPenaltyPoints()
            );
        }

        // 2. Actualizar la tarea en la base de datos
        Map<String, Object> updates = new HashMap<>();

        updates.put("status", TaskConstants.STATUS_PENDING);

        updates.put("assignedMembers", new ArrayList<>());
        updates.put("reviewerId", null);
        updates.put("penaltyApplied", true);

        batch.update(taskRef, updates);

        return batch.commit();
    }

    public Task<Void> updateTaskField(String taskId, String fieldName, Object value) {
        return db.collection("tasks").document(taskId)
                .update(fieldName, value);
    }
}