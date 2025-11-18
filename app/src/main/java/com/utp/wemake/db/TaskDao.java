package com.utp.wemake.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.utp.wemake.models.TaskModel;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(TaskModel task);

    // Obtiene todas las tareas que están marcadas como no sincronizadas
    @Query("SELECT * FROM tasks_offline WHERE isSynced = 0")
    List<TaskModel> getUnsyncedTasks();

    // Marca una tarea como sincronizada
    @Query("UPDATE tasks_offline SET isSynced = 1 WHERE id = :localId")
    void markTaskAsSynced(String localId);

    // Elimina una tarea después de sincronizarla
    @Query("DELETE FROM tasks_offline WHERE id = :localId")
    void deleteTaskById(String localId);
}