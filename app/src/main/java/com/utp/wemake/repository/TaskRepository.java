package com.utp.wemake.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.utp.wemake.constants.TaskConstants;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private final FirebaseFirestore db;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Crea una nueva tarea en Firebase
     */
    public Task<String> createTask(TaskModel task) {
        DocumentReference taskRef = db.collection(TaskConstants.COLLECTION_TASKS).document();
        task.setId(taskRef.getId());

        WriteBatch batch = db.batch();
        batch.set(taskRef, task);

        // Crear subtareas si existen
        if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            for (Subtask subtask : task.getSubtasks()) {
                DocumentReference subtaskRef = taskRef
                        .collection(TaskConstants.COLLECTION_SUBTASKS)
                        .document();
                subtask.setId(subtaskRef.getId());
                batch.set(subtaskRef, subtask);
            }
        }

        return batch.commit().continueWith(task1 -> task.getId());
    }

    /**
     * Obtiene todas las tareas de un tablero
     */
    public Task<List<TaskModel>> getTasksByBoard(String boardId) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .whereEqualTo("boardId", boardId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<TaskModel> tasks = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        for (DocumentSnapshot doc : snapshot) {
                            TaskModel taskObj = doc.toObject(TaskModel.class);
                            if (taskObj != null) {
                                taskObj.setId(doc.getId());
                                tasks.add(taskObj);
                            }
                        }
                    }
                    return tasks;
                });
    }

    /**
     * Obtiene las subtareas de una tarea específica
     */
    public Task<List<Subtask>> getSubtasksByTask(String taskId) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(taskId)
                .collection(TaskConstants.COLLECTION_SUBTASKS)
                .get()
                .continueWith(task -> {
                    List<Subtask> subtasks = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        for (DocumentSnapshot doc : snapshot) {
                            Subtask subtask = doc.toObject(Subtask.class);
                            if (subtask != null) {
                                subtask.setId(doc.getId());
                                subtasks.add(subtask);
                            }
                        }
                    }
                    return subtasks;
                });
    }

    /**
     * Actualiza el estado de una tarea
     */
    public Task<Void> updateTaskStatus(String taskId, String newStatus) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(taskId)
                .update("status", newStatus);
    }

    /**
     * Actualiza una subtarea
     */
    public Task<Void> updateSubtask(String taskId, String subtaskId, boolean completed) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(taskId)
                .collection(TaskConstants.COLLECTION_SUBTASKS)
                .document(subtaskId)
                .update("completed", completed, "completedAt", completed ? new java.util.Date() : null);
    }

    /**
     * Elimina una tarea
     */
    public Task<Void> deleteTask(String taskId) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(taskId)
                .delete();
    }

    /**
     * Obtiene una tarea específica por su ID
     */
    public Task<TaskModel> getTaskById(String taskId) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(taskId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            TaskModel taskObj = doc.toObject(TaskModel.class);
                            if (taskObj != null) {
                                taskObj.setId(doc.getId());
                            }
                            return taskObj;
                        }
                    }
                    return null;
                });
    }

    /**
     * Actualiza una tarea completa
     */
    public Task<Void> updateTask(TaskModel task) {
        return db.collection(TaskConstants.COLLECTION_TASKS)
                .document(task.getId())
                .set(task);
    }
}
