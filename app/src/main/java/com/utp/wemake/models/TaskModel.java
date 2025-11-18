package com.utp.wemake.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.utp.wemake.db.Converters;

@Entity(tableName = "tasks_offline") // Room que esta es una tabla
@TypeConverters(Converters.class)     // Manejar tipos de datos complejos
public class TaskModel {

    public TaskModel() {
        this.id = UUID.randomUUID().toString();
    }

    private boolean penaltyApplied = false;

    @PrimaryKey // El ID es la clave única en la tabla local
    @NonNull
    @DocumentId
    private String id;
    private String title; // Título de la tarea
    private String description; // Descripción breve de la tarea

    private Date deadline;
    private String priority;
    private List<Subtask> subtasks;
    private String boardId;
    private String createdBy; // ID del usuario que la propuso originalmente
    private Date createdAt;


    private boolean isSynced = false;   // 'false' si necesita ser subida a Firebase
    private boolean isProposal = false; // 'true' si debe ir a 'task_proposals'


    private String status; // "pending", "in_progress", "in_review", "completed"
    private int rewardPoints;
    private int penaltyPoints;
    private String approvedBy; // ID del admin que la aprobó
    private Date approvedAt;
    private String reviewerId; // ID del admin/usuario que debe revisarla
    private List<String> assignedMembers;
    private Date completedAt;


    public boolean isPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(boolean penaltyApplied) { this.penaltyApplied = penaltyApplied; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public List<String> getAssignedMembers() {
        return assignedMembers;
    }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
    public boolean isProposal() { return isProposal; }
    public void setProposal(boolean proposal) { isProposal = proposal; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public void setAssignedMembers(List<String> assignedMembers) {
        this.assignedMembers = assignedMembers;
    }
}
