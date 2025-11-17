package com.utp.wemake.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.Ignore;

import java.util.Date;
import java.util.List;

@Entity(
        tableName = "tasks",
        indices = {
                @Index("boardId"),
                @Index("createdBy"),
                @Index("reviewerId"),
                @Index("status")
        }
)
public class TaskEntity {
    @PrimaryKey
    @NonNull
    private String id;

    private String title;
    private String description;
    private Long deadline; // Date convertido a Long
    private String priority;
    private String boardId;
    private String createdBy;
    private Long createdAt; // Date convertido a Long
    private String status;
    private int rewardPoints;
    private int penaltyPoints;
    private String approvedBy;
    private Long approvedAt; // Date convertido a Long
    private String reviewerId;
    private String assignedMembersJson; // List<String> serializado como JSON
    private Long completedAt; // Date convertido a Long
    private boolean penaltyApplied;

    // Campos para sincronización offline
    private boolean isPendingSync; // Indica si hay cambios pendientes de sincronizar
    private Long lastModified; // Timestamp de última modificación local

    @Ignore
    private List<SubtaskEntity> subtasks; // Se carga por separado

    // Constructor vacío
    public TaskEntity() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getDeadline() { return deadline; }
    public void setDeadline(Long deadline) { this.deadline = deadline; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public int getPenaltyPoints() { return penaltyPoints; }
    public void setPenaltyPoints(int penaltyPoints) { this.penaltyPoints = penaltyPoints; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Long approvedAt) { this.approvedAt = approvedAt; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getAssignedMembersJson() { return assignedMembersJson; }
    public void setAssignedMembersJson(String assignedMembersJson) {
        this.assignedMembersJson = assignedMembersJson;
    }

    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }

    public boolean isPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(boolean penaltyApplied) {
        this.penaltyApplied = penaltyApplied;
    }

    public boolean isPendingSync() { return isPendingSync; }
    public void setPendingSync(boolean pendingSync) { this.isPendingSync = pendingSync; }

    public Long getLastModified() { return lastModified; }
    public void setLastModified(Long lastModified) { this.lastModified = lastModified; }

    @Ignore
    public List<SubtaskEntity> getSubtasks() { return subtasks; }
    @Ignore
    public void setSubtasks(List<SubtaskEntity> subtasks) { this.subtasks = subtasks; }
}
