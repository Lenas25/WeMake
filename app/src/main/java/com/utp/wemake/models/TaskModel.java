package com.utp.wemake.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Clase modelo que representa una tarea
public class TaskModel {
    @DocumentId
    // Atributos principales de la tarea
    private String id;
    private String title; // Título de la tarea
    private String description; // Descripción breve de la tarea
    private String status; // "pendiente", "en progreso", "completado"
    private String priority; // "Baja", "Media", "Alta"
    private List<String> assignedMembers; // IDs de usuarios asignados
    private String boardId; // ID del tablero al que pertenece
    private String createdBy; // ID del usuario que creó la tarea

    @ServerTimestamp
    private Date createdAt;

    private Date dueDate;
    private List<Subtask> subtasks;

    // Constructor vacío para Firebase
    public TaskModel() {}

    // Constructor que inicializa los atributos de la tarea
    public TaskModel(String title, String description, String priority,
                     List<String> assignedMembers, String boardId, String createdBy) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignedMembers = assignedMembers;
        this.boardId = boardId;
        this.createdBy = createdBy;
        this.status = "pending"; // Estado inicial
        this.subtasks = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Getter para obtener el título de la tarea
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) { this.title = title; }

    // Getter para obtener la descripción de la tarea
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    // Getter para obtener el responsable de la tarea
    public List<String> getAssignedMembers() {
        return assignedMembers;
    }
    public void setAssignedMembers(List<String> assignedMembers) { this.assignedMembers = assignedMembers; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    // Métodos de utilidad
    public boolean isPending() { return "pending".equals(status); }
    public boolean isInProgress() { return "in_progress".equals(status); }
    public boolean isCompleted() { return "completed".equals(status); }

    public boolean isHighPriority() { return "high".equals(priority); }
    public boolean isMediumPriority() { return "medium".equals(priority); }
    public boolean isLowPriority() { return "low".equals(priority); }

    public int getCompletedSubtasksCount() {
        if (subtasks == null) return 0;
        return (int) subtasks.stream().filter(Subtask::isCompleted).count();
    }

    public int getTotalSubtasksCount() {
        return subtasks != null ? subtasks.size() : 0;
    }
}
