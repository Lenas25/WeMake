package com.utp.wemake.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.utp.wemake.database.entities.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(TaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTasks(List<TaskEntity> tasks);

    @Update
    void updateTask(TaskEntity task);

    @Delete
    void deleteTask(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<TaskEntity> getTaskByIdLiveData(String taskId);

    // Obtener tareas por boardId
    @Query("SELECT * FROM tasks WHERE boardId = :boardId")
    List<TaskEntity> getTasksByBoardId(String boardId);

    @Query("SELECT * FROM tasks WHERE boardId = :boardId")
    LiveData<List<TaskEntity>> getTasksByBoardIdLiveData(String boardId);

    // Obtener tareas asignadas a un usuario en un board
    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND assignedMembersJson LIKE '%' || :userId || '%'")
    List<TaskEntity> getAssignedTasksForUser(String boardId, String userId);

    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND assignedMembersJson LIKE '%' || :userId || '%'")
    LiveData<List<TaskEntity>> getAssignedTasksForUserLiveData(String boardId, String userId);

    // Obtener tareas donde el usuario es revisor
    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND reviewerId = :userId")
    List<TaskEntity> getReviewerTasksForUser(String boardId, String userId);

    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND reviewerId = :userId")
    LiveData<List<TaskEntity>> getReviewerTasksForUserLiveData(String boardId, String userId);

    // Obtener tareas pendientes de sincronización
    @Query("SELECT * FROM tasks WHERE isPendingSync = 1")
    List<TaskEntity> getPendingSyncTasks();

    // Obtener todas las tareas de múltiples boards
    @Query("SELECT * FROM tasks WHERE boardId IN (:boardIds)")
    List<TaskEntity> getTasksByBoardIds(List<String> boardIds);

    @Query("SELECT * FROM tasks WHERE boardId IN (:boardIds)")
    LiveData<List<TaskEntity>> getTasksByBoardIdsLiveData(List<String> boardIds);

    // Eliminar todas las tareas de un board
    @Query("DELETE FROM tasks WHERE boardId = :boardId")
    void deleteTasksByBoardId(String boardId);

    // Eliminar una tarea por ID
    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteTaskById(String taskId);
}
