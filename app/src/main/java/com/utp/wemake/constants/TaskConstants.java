package com.utp.wemake.constants;

public class TaskConstants {
    // Estados de tareas
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_IN_REVIEW = "in_review";
    public static final String STATUS_COMPLETED = "completed";

    // Prioridades
    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";
    
    private TaskConstants() {
        // Constructor privado para evitar instanciación
    }
}
