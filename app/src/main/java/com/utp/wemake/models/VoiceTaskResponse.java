package com.utp.wemake.models;

import java.util.Date;
import java.util.List;

public class VoiceTaskResponse {
    private String title;
    private String description;
    private String priority;
    private Date deadline;
    private List<Subtask> subtasks;
    private List<String> assignedMembers;
    private String reviewerId;
    private boolean success;
    private String error;

    // Constructors, getters y setters...
    public VoiceTaskResponse() {}

    // Getters y setters para todos los campos
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) { this.subtasks = subtasks; }

    public List<String> getAssignedMembers() { return assignedMembers; }
    public void setAssignedMembers(List<String> assignedMembers) { this.assignedMembers = assignedMembers; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
