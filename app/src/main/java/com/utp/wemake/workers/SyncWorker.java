package com.utp.wemake.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.utp.wemake.db.AppDatabase;
import com.utp.wemake.db.TaskDao;
import com.utp.wemake.models.TaskModel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SyncWorker extends Worker {

    private final TaskDao taskDao;
    private final FirebaseFirestore firestore;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        taskDao = AppDatabase.getDatabase(context).taskDao();
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 1. Obtener todas las tareas no sincronizadas
            List<TaskModel> unsyncedTasks = taskDao.getUnsyncedTasks();
            if (unsyncedTasks.isEmpty()) {
                return Result.success(); // No hay nada que hacer
            }

            for (TaskModel task : unsyncedTasks) {
                // 2. Decidir a qué colección subirla
                String collectionPath = task.isProposal() ? "task_proposals" : "tasks";

                // 3. Subir a Firebase
                Tasks.await(firestore.collection(collectionPath).add(task));

                // 4. Marcar la tarea como sincronizada en la base de datos local
                taskDao.markTaskAsSynced(task.getId());
            }

            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            return Result.retry(); // Si algo falla, WorkManager lo reintentará más tarde
        }
    }
}