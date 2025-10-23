package com.utp.wemake.models;

import java.util.Date;
import java.util.List;
import com.google.firebase.firestore.DocumentId;

public class TaskProposal {
    @DocumentId
    private String id;
    private String title;
    private String description;
    private Date deadline;
    private String priority;
    private List<Subtask> subtasks;
    private String boardId;
    private String proposedBy;
    private String proposerName;
    private Date proposedAt;
    private String status = "awaiting_approval";
    private List<String> assignedMembers;
    private String reviewerId;

    public List<String> getAssignedMembers() {
        return assignedMembers;
    }

    public void setAssignedMembers(List<String> assignedMembers) {
        this.assignedMembers = assignedMembers;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

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

    public String getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(String proposedBy) {
        this.proposedBy = proposedBy;
    }

    public String getProposerName() {
        return proposerName;
    }

    public void setProposerName(String proposerName) {
        this.proposerName = proposerName;
    }

    public Date getProposedAt() {
        return proposedAt;
    }

    public void setProposedAt(Date proposedAt) {
        this.proposedAt = proposedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
