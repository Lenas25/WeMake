package com.utp.wemake.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

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
import com.utp.wemake.database.AppDatabase;
import com.utp.wemake.database.TaskConverter;
import com.utp.wemake.database.dao.SubtaskDao;
import com.utp.wemake.database.dao.TaskDao;
import com.utp.wemake.database.entities.SubtaskEntity;
import com.utp.wemake.database.entities.TaskEntity;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_TASK_PROPOSALS = "task_proposals";

    private final FirebaseFirestore db;
    private final MemberRepository memberRepository;
    private final CollectionReference proposalsCollection;
    private final CollectionReference tasksCollection;
    private final List<ListenerRegistration> activeListeners = new ArrayList<>();

    // Room Database
    private final AppDatabase database;
    private final TaskDao taskDao;
    private final SubtaskDao subtaskDao;
    private final ExecutorService executorService;
    private final Context context;
    private final ConnectivityManager connectivityManager;

    // Map para almacenar los MediatorLiveData por boardId+userId
    private final Map<String, MediatorLiveData<List<TaskModel>>> activeMediators = new ConcurrentHashMap<>();

    // NetworkCallback para detectar cambios de conexión
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean wasOffline = false;

    public TaskRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.proposalsCollection = db.collection(COLLECTION_TASK_PROPOSALS);
        this.tasksCollection = db.collection(COLLECTION_TASKS);
        this.memberRepository = new MemberRepository();

        // Inicializar Room
        this.database = AppDatabase.getDatabase(this.context);
        this.taskDao = database.taskDao();
        this.subtaskDao = database.subtaskDao();
        this.executorService = Executors.newFixedThreadPool(4);

        // Inicializar ConnectivityManager
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Registrar NetworkCallback para detectar cambios de conexión
        registerNetworkCallback();

        // Verificar estado inicial y sincronizar si hay conexión
        if (isOnline()) {
            syncPendingChangesToFirebase();
        } else {
            wasOffline = true;
        }
    }

    /**
     * Registra un NetworkCallback para detectar cuando se recupera la conexión.
     */
    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    // Se recuperó la conexión
                    if (wasOffline) {
                        wasOffline = false;
                        // Sincronizar cambios pendientes
                        syncPendingChangesToFirebase();
                    }
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    // Se perdió la conexión
                    wasOffline = true;
                }
            };

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    /**
     * Desregistra el NetworkCallback.
     * Debe llamarse cuando el repositorio ya no se necesita.
     */
    public void unregisterNetworkCallback() {
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                // Ignorar errores al desregistrar
            }
        }
    }

    // Método para verificar conexión a internet
    public boolean isOnline() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // Para versiones anteriores
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    public interface OnTasksUpdatedListener {
        void onTasksUpdated(List<TaskModel> tasks);
        void onError(Exception e);
    }

    // ========== MÉTODOS DE CREACIÓN ==========

    /**
     * Crea una nueva propuesta de tarea en la colección 'task_proposals'.
     * Usado por usuarios no-administradores.
     * Las propuestas solo se guardan en Firebase (no en SQLite).
     */
    public Task<DocumentReference> createTaskProposal(TaskProposal proposal) {
        return db.collection(COLLECTION_TASK_PROPOSALS).add(proposal);
    }

    /**
     * Crea una nueva tarea. Se guarda primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<DocumentReference> createTask(TaskModel task) {
        // Generar ID si no existe
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(db.collection(COLLECTION_TASKS).document().getId());
        }

        // Guardar en SQLite primero (siempre)
        executorService.execute(() -> {
            TaskEntity entity = TaskConverter.toEntity(task);
            entity.setPendingSync(true); // Marcar como pendiente de sincronización
            taskDao.insertTask(entity);

            // Guardar subtareas
            if (entity.getSubtasks() != null && !entity.getSubtasks().isEmpty()) {
                subtaskDao.insertSubtasks(entity.getSubtasks());
            }
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
            Task<Void> firebaseTask = tasksCollection.document(task.getId()).set(task);
            firebaseTask.addOnSuccessListener(aVoid -> {
                // Marcar como sincronizado
                executorService.execute(() -> {
                    TaskEntity entity = taskDao.getTaskById(task.getId());
                    if (entity != null) {
                        entity.setPendingSync(false);
                        taskDao.updateTask(entity);
                    }
                });
            });
            // Convertir Task<Void> a Task<DocumentReference>
            return firebaseTask.continueWith(task1 -> tasksCollection.document(task.getId()));
        } else {
            // Retornar una tarea completada inmediatamente (offline)
            return Tasks.forResult(tasksCollection.document(task.getId()));
        }
    }

    // ========== MÉTODOS DE LECTURA ==========

    /**
     * Obtiene una tarea específica por su ID desde SQLite (rápido).
     * Si no existe localmente y hay conexión, intenta obtenerla de Firebase.
     */
    public LiveData<TaskModel> getTaskById(String taskId) {
        MediatorLiveData<TaskModel> result = new MediatorLiveData<>();

        LiveData<TaskEntity> entityLiveData = taskDao.getTaskByIdLiveData(taskId);
        result.addSource(entityLiveData, entity -> {
            if (entity != null) {
                // Ejecutar en hilo de fondo
                executorService.execute(() -> {
                    List<SubtaskEntity> subtasks = subtaskDao.getSubtasksForTask(entity.getId());
                    TaskModel taskModel = TaskConverter.toModel(entity, subtasks);
                    result.postValue(taskModel); // postValue es thread-safe
                });
            } else if (isOnline()) {
                // Si no existe localmente y hay conexión, obtener de Firebase
                tasksCollection.document(taskId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TaskModel task = documentSnapshot.toObject(TaskModel.class);
                        if (task != null) {
                            task.setId(documentSnapshot.getId());
                            executorService.execute(() -> saveTaskToLocal(task, false));
                            result.postValue(task);
                        }
                    }
                });
            }
        });

        return result;
    }

    /**
     * Obtiene tareas desde SQLite (rápido) y luego sincroniza con Firebase en segundo plano.
     * Para un solo board.
     */
    public LiveData<List<TaskModel>> getTasksForUserInBoard(String boardId, String userId) {
        String key = boardId + "_" + userId;

        // Si ya existe un MediatorLiveData para este board+user, devolverlo
        MediatorLiveData<List<TaskModel>> existing = activeMediators.get(key);
        if (existing != null) {
            return existing;
        }

        // Crear nuevo MediatorLiveData
        MediatorLiveData<List<TaskModel>> result = new MediatorLiveData<>();

        // Primero establecer un valor vacío para limpiar cualquier dato anterior
        result.setValue(new ArrayList<>());

        // Combinar tareas asignadas y de revisor
        LiveData<List<TaskEntity>> assigned = taskDao.getAssignedTasksForUserLiveData(boardId, userId);
        LiveData<List<TaskEntity>> reviewer = taskDao.getReviewerTasksForUserLiveData(boardId, userId);

        // Variables para almacenar los valores actuales
        final List<TaskEntity>[] currentAssigned = new List[]{null};
        final List<TaskEntity>[] currentReviewer = new List[]{null};

        // Variable para rastrear el boardId actual (para evitar emisiones de otros tableros)
        final String[] currentBoardId = new String[]{boardId};

        // Función helper para combinar y emitir
        Runnable combineAndEmit = () -> {
            // Verificar que el boardId sigue siendo el mismo
            if (!boardId.equals(currentBoardId[0])) {
                return; // Ignorar si el boardId cambió
            }

            if (currentAssigned[0] != null && currentReviewer[0] != null) {
                executorService.execute(() -> {
                    List<TaskModel> converted = combineAndConvert(currentAssigned[0], currentReviewer[0]);
                    // Filtrar estrictamente por boardId
                    List<TaskModel> filtered = new ArrayList<>();
                    for (TaskModel task : converted) {
                        if (boardId.equals(task.getBoardId())) {
                            filtered.add(task);
                        }
                    }
                    result.postValue(filtered);
                });
            }
        };

        result.addSource(assigned, assignedTasks -> {
            if (assignedTasks != null) {
                currentAssigned[0] = assignedTasks;
                combineAndEmit.run();
            }
        });

        result.addSource(reviewer, reviewerTasks -> {
            if (reviewerTasks != null) {
                currentReviewer[0] = reviewerTasks;
                combineAndEmit.run();
            }
        });

        // Guardar referencia para poder limpiarlo después
        activeMediators.put(key, result);

        // Sincronizar con Firebase en segundo plano (solo una vez, dentro del repo)
        if (isOnline()) {
            syncTasksFromFirebase(boardId, userId);
        }

        return result;
    }

    /**
     * Obtiene tareas desde SQLite para múltiples boards.
     */
    public LiveData<List<TaskModel>> getTasksForUserInBoards(List<String> boardIds, String userId) {
        MediatorLiveData<List<TaskModel>> result = new MediatorLiveData<>();

        if (boardIds == null || boardIds.isEmpty()) {
            result.setValue(new ArrayList<>());
            return result;
        }

        LiveData<List<TaskEntity>> tasksLiveData = taskDao.getTasksByBoardIdsLiveData(boardIds);
        result.addSource(tasksLiveData, entities -> {
            if (entities != null) {
                // Ejecutar en hilo de fondo
                executorService.execute(() -> {
                    // Filtrar solo las tareas del usuario (asignadas o como revisor)
                    List<TaskModel> userTasks = new ArrayList<>();
                    for (TaskEntity entity : entities) {
                        boolean isAssigned = entity.getAssignedMembersJson() != null &&
                                entity.getAssignedMembersJson().contains(userId);
                        boolean isReviewer = userId.equals(entity.getReviewerId());

                        if (isAssigned || isReviewer) {
                            List<SubtaskEntity> subtasks = subtaskDao.getSubtasksForTask(entity.getId());
                            userTasks.add(TaskConverter.toModel(entity, subtasks));
                        }
                    }
                    result.postValue(userTasks); // postValue es thread-safe
                });
            }
        });

        // Sincronizar con Firebase en segundo plano
        if (isOnline()) {
            for (String boardId : boardIds) {
                syncTasksFromFirebase(boardId, userId);
            }
        }

        return result;
    }

    /**
     * Limpia los MediatorLiveData activos para un board específico
     */
    public void clearMediatorForBoard(String boardId, String userId) {
        String key = boardId + "_" + userId;
        MediatorLiveData<List<TaskModel>> mediator = activeMediators.remove(key);
        if (mediator != null) {
            // Limpiar las fuentes
            mediator.removeSource(taskDao.getAssignedTasksForUserLiveData(boardId, userId));
            mediator.removeSource(taskDao.getReviewerTasksForUserLiveData(boardId, userId));
        }
    }

    /**
     * Obtiene todas las propuestas de tareas que están pendientes de aprobación.
     * Las propuestas solo están en Firebase.
     */
    public Task<QuerySnapshot> getPendingTaskProposals() {
        return proposalsCollection.whereEqualTo("status", "awaiting_approval").get();
    }

    // ========== MÉTODOS DE ACTUALIZACIÓN ==========

    /**
     * Actualiza una tarea existente.
     * Se guarda primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> updateTask(TaskModel task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Task ID must not be null for update."));
        }

        // Actualizar en SQLite primero
        executorService.execute(() -> {
            TaskEntity entity = TaskConverter.toEntity(task);
            entity.setPendingSync(true);
            taskDao.updateTask(entity);

            // Actualizar subtareas
            if (entity.getSubtasks() != null) {
                subtaskDao.deleteSubtasksForTask(task.getId());
                if (!entity.getSubtasks().isEmpty()) {
                    subtaskDao.insertSubtasks(entity.getSubtasks());
                }
            }
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
            Task<Void> firebaseTask = tasksCollection.document(task.getId()).set(task);
            firebaseTask.addOnSuccessListener(aVoid -> {
                executorService.execute(() -> {
                    TaskEntity entity = taskDao.getTaskById(task.getId());
                    if (entity != null) {
                        entity.setPendingSync(false);
                        taskDao.updateTask(entity);
                    }
                });
            });
            return firebaseTask;
        } else {
            // Retornar una tarea completada inmediatamente (offline)
            return Tasks.forResult(null);
        }
    }

    /**
     * Actualiza el estado de una subtarea específica dentro de una tarea.
     * Se actualiza primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> updateSubtaskStatus(String taskId, String subtaskId, boolean isCompleted) {
        // Actualizar en SQLite primero
        executorService.execute(() -> {
            Long completedAt = isCompleted ? System.currentTimeMillis() : null;
            subtaskDao.updateSubtaskStatus(subtaskId, isCompleted, completedAt);

            // Marcar la tarea como pendiente de sincronización
            TaskEntity taskEntity = taskDao.getTaskById(taskId);
            if (taskEntity != null) {
                taskEntity.setPendingSync(true);
                taskDao.updateTask(taskEntity);
            }
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
            DocumentReference taskRef = tasksCollection.document(taskId);

            Task<Void> firebaseTask = db.runTransaction(transaction -> {
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

            firebaseTask.addOnSuccessListener(aVoid -> {
                executorService.execute(() -> {
                    TaskEntity taskEntity = taskDao.getTaskById(taskId);
                    if (taskEntity != null) {
                        taskEntity.setPendingSync(false);
                        taskDao.updateTask(taskEntity);
                    }
                });
            });

            return firebaseTask;
        } else {
            return Tasks.forResult(null);
        }
    }

    /**
     * Actualiza el estado de una tarea y, si se completa, aplica los puntos de recompensa.
     * Se actualiza primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> updateStatusAndApplyPoints(String taskId, String newStatus) {
        // Actualizar en SQLite primero
        executorService.execute(() -> {
            TaskEntity entity = taskDao.getTaskById(taskId);
            if (entity != null) {
                entity.setStatus(newStatus);
                if (TaskConstants.STATUS_COMPLETED.equals(newStatus)) {
                    entity.setCompletedAt(System.currentTimeMillis());
                }
                entity.setPendingSync(true);
                taskDao.updateTask(entity);
            }
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
            DocumentReference taskRef = tasksCollection.document(taskId);

            Task<Void> firebaseTask = taskRef.get().onSuccessTask(documentSnapshot -> {
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

                return batch.commit();
            });

            firebaseTask.addOnSuccessListener(aVoid -> {
                executorService.execute(() -> {
                    TaskEntity entity = taskDao.getTaskById(taskId);
                    if (entity != null) {
                        entity.setPendingSync(false);
                        taskDao.updateTask(entity);
                    }
                });
            });

            return firebaseTask;
        } else {
            return Tasks.forResult(null);
        }
    }

    /**
     * Actualiza únicamente el campo de prioridad de una tarea.
     * Se actualiza primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> updatePriority(String taskId, String newPriority) {
        if (taskId == null || taskId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Task ID cannot be null for priority update."));
        }

        // Actualizar en SQLite primero
        executorService.execute(() -> {
            TaskEntity entity = taskDao.getTaskById(taskId);
            if (entity != null) {
                entity.setPriority(newPriority);
                entity.setPendingSync(true);
                taskDao.updateTask(entity);
            }
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
            DocumentReference taskRef = tasksCollection.document(taskId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("priority", newPriority);

            Task<Void> firebaseTask = taskRef.update(updates);
            firebaseTask.addOnSuccessListener(aVoid -> {
                executorService.execute(() -> {
                    TaskEntity entity = taskDao.getTaskById(taskId);
                    if (entity != null) {
                        entity.setPendingSync(false);
                        taskDao.updateTask(entity);
                    }
                });
            });

            return firebaseTask;
        } else {
            return Tasks.forResult(null);
        }
    }

    /**
     * Procesa una tarea vencida aplicando penalizaciones.
     * Se actualiza primero en SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> processOverdueTask(TaskModel task) {
        // Actualizar en SQLite primero
        executorService.execute(() -> {
            TaskEntity entity = TaskConverter.toEntity(task);
            entity.setStatus(TaskConstants.STATUS_PENDING);
            entity.setAssignedMembersJson("[]");
            entity.setReviewerId(null);
            entity.setPenaltyApplied(true);
            entity.setPendingSync(true);
            taskDao.updateTask(entity);
        });

        // Si hay conexión, sincronizar con Firebase
        if (isOnline()) {
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

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", TaskConstants.STATUS_PENDING);
            updates.put("assignedMembers", new ArrayList<>());
            updates.put("reviewerId", null);
            updates.put("penaltyApplied", true);

            batch.update(taskRef, updates);

            Task<Void> firebaseTask = batch.commit();
            firebaseTask.addOnSuccessListener(aVoid -> {
                executorService.execute(() -> {
                    TaskEntity entity = taskDao.getTaskById(task.getId());
                    if (entity != null) {
                        entity.setPendingSync(false);
                        taskDao.updateTask(entity);
                    }
                });
            });

            return firebaseTask;
        } else {
            return Tasks.forResult(null);
        }
    }

    // ========== MÉTODOS DE ELIMINACIÓN ==========

    /**
     * Elimina una tarea de la base de datos.
     * Se elimina primero de SQLite y luego se sincroniza con Firebase.
     */
    public Task<Void> deleteTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Task ID cannot be null."));
        }

        // Eliminar de SQLite primero
        executorService.execute(() -> {
            subtaskDao.deleteSubtasksForTask(taskId);
            taskDao.deleteTaskById(taskId);
        });

        // Si hay conexión, eliminar de Firebase
        if (isOnline()) {
            return tasksCollection.document(taskId).delete();
        } else {
            return Tasks.forResult(null);
        }
    }

    // ========== MÉTODOS DE PROPUESTAS ==========

    /**
     * Aprueba una propuesta, moviéndola a la colección de tareas y eliminando la original.
     * Se guarda en SQLite después de aprobar.
     */
    public Task<Void> approveProposal(String proposalId, TaskModel newTask) {
        DocumentReference proposalRef = proposalsCollection.document(proposalId);
        DocumentReference taskRef = tasksCollection.document(); // ID automático
        newTask.setId(taskRef.getId());

        WriteBatch batch = db.batch();
        batch.set(taskRef, newTask);
        batch.delete(proposalRef);

        Task<Void> firebaseTask = batch.commit();
        firebaseTask.addOnSuccessListener(aVoid -> {
            // Guardar en SQLite después de aprobar
            executorService.execute(() -> saveTaskToLocal(newTask, false));
        });

        return firebaseTask;
    }

    /**
     * Rechaza (elimina) una propuesta de tarea.
     */
    public Task<Void> denyProposal(String proposalId) {
        return proposalsCollection.document(proposalId).delete();
    }

    // ========== MÉTODOS DE SINCRONIZACIÓN ==========

    /**
     * Sincroniza las tareas desde Firebase a SQLite.
     */
    public void syncTasksFromFirebase(String boardId, String userId) {
        if (!isOnline()) return;

        // Obtener tareas asignadas
        Query assignedQuery = tasksCollection
                .whereEqualTo("boardId", boardId)
                .whereArrayContains("assignedMembers", userId);

        assignedQuery.get().addOnSuccessListener(querySnapshot -> {
            executorService.execute(() -> {
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    TaskModel task = doc.toObject(TaskModel.class);
                    if (task != null) {
                        task.setId(doc.getId());
                        // Verificar que el boardId coincida antes de guardar
                        if (boardId.equals(task.getBoardId())) {
                            saveTaskToLocal(task, false); // false = ya está sincronizado
                            count++;
                        } else {
                            Log.w("TaskRepository", "Tarea con boardId incorrecto ignorada: " + task.getId() + " (esperado: " + boardId + ", recibido: " + task.getBoardId() + ")");
                        }
                    }
                }
            });
        });

        // Obtener tareas de revisor
        Query reviewerQuery = tasksCollection
                .whereEqualTo("boardId", boardId)
                .whereEqualTo("reviewerId", userId);

        reviewerQuery.get().addOnSuccessListener(querySnapshot -> {
            executorService.execute(() -> {
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    TaskModel task = doc.toObject(TaskModel.class);
                    if (task != null) {
                        task.setId(doc.getId());
                        // Verificar que el boardId coincida antes de guardar
                        if (boardId.equals(task.getBoardId())) {
                            saveTaskToLocal(task, false);
                            count++;
                        } else {
                            Log.w("TaskRepository", "Tarea con boardId incorrecto ignorada: " + task.getId() + " (esperado: " + boardId + ", recibido: " + task.getBoardId() + ")");
                        }
                    }
                }
            });
        });
    }

    /**
     * Sincroniza los cambios pendientes con Firebase.
     */
    public void syncPendingChangesToFirebase() {
        if (!isOnline()) {
            wasOffline = true;
            return;
        }

        executorService.execute(() -> {
            List<TaskEntity> pendingTasks = taskDao.getPendingSyncTasks();

            if (pendingTasks.isEmpty()) {
                return; // No hay nada que sincronizar
            }

            for (TaskEntity entity : pendingTasks) {
                TaskModel task = TaskConverter.toModel(entity, subtaskDao.getSubtasksForTask(entity.getId()));

                tasksCollection.document(task.getId()).set(task)
                        .addOnSuccessListener(aVoid -> {
                            executorService.execute(() -> {
                                entity.setPendingSync(false);
                                taskDao.updateTask(entity);
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Error de sincronización, se mantendrá como pendiente
                            // Se podría agregar un contador de reintentos aquí
                        });
            }
        });
    }

    // ========== MÉTODOS PRIVADOS ==========

    /**
     * Guarda una tarea en SQLite local.
     */
    private void saveTaskToLocal(TaskModel task, boolean pendingSync) {
        TaskEntity entity = TaskConverter.toEntity(task);
        entity.setPendingSync(pendingSync);
        taskDao.insertTask(entity);

        if (entity.getSubtasks() != null && !entity.getSubtasks().isEmpty()) {
            subtaskDao.deleteSubtasksForTask(task.getId());
            subtaskDao.insertSubtasks(entity.getSubtasks());
        }
    }

    /**
     * Combina y convierte entidades de Room a modelos.
     * Este método debe ejecutarse en un hilo de fondo.
     */
    private List<TaskModel> combineAndConvert(List<TaskEntity> assigned, List<TaskEntity> reviewer) {
        Map<String, TaskEntity> combined = new HashMap<>();

        if (assigned != null) {
            for (TaskEntity entity : assigned) {
                combined.put(entity.getId(), entity);
            }
        }
        if (reviewer != null) {
            for (TaskEntity entity : reviewer) {
                combined.put(entity.getId(), entity);
            }
        }

        List<TaskModel> result = new ArrayList<>();
        for (TaskEntity entity : combined.values()) {
            List<SubtaskEntity> subtasks = subtaskDao.getSubtasksForTask(entity.getId());
            result.add(TaskConverter.toModel(entity, subtasks));
        }
        return result;
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
        unregisterNetworkCallback();
    }
}