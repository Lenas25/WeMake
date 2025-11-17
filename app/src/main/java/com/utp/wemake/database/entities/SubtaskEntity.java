package com.utp.wemake.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Date;

@Entity(
        tableName = "subtasks",
        foreignKeys = @ForeignKey(
                entity = TaskEntity.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("taskId")}
)
public class SubtaskEntity {
    @PrimaryKey
    @NonNull
    private String id;

    private String taskId; // FK a TaskEntity
    private String text;
    private boolean completed;
    private Long completedAt; // Date convertido a Long

    // Constructor vacío
    public SubtaskEntity() {}

    // Constructor completo
    public SubtaskEntity(String id, String taskId, String text, boolean completed, Long completedAt) {
        this.id = id;
        this.taskId = taskId;
        this.text = text;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
}
