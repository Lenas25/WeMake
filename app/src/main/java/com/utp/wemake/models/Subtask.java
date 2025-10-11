package com.utp.wemake.models;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Subtask {
    @DocumentId
    private String id;
    
    private String text;
    private boolean completed;
    private Date completedAt;
    
    // Constructor vacío para Firebase
    public Subtask() {}
    
    // Constructor principal
    public Subtask(String text) {
        this.text = text;
        this.completed = false;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { 
        this.completed = completed;
        if (completed) {
            this.completedAt = new Date();
        } else {
            this.completedAt = null;
        }
    }
    
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}
