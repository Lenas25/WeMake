package com.utp.wemake.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.utp.wemake.database.entities.SubtaskEntity;

import java.util.List;

@Dao
public interface SubtaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubtask(SubtaskEntity subtask);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSubtasks(List<SubtaskEntity> subtasks);

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    List<SubtaskEntity> getSubtasksForTask(String taskId);

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    void deleteSubtasksForTask(String taskId);

    @Delete
    void deleteSubtask(SubtaskEntity subtask);

    @Query("UPDATE subtasks SET completed = :completed, completedAt = :completedAt WHERE id = :subtaskId")
    void updateSubtaskStatus(String subtaskId, boolean completed, Long completedAt);
}
