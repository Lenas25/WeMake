package com.utp.wemake.models;

// Clase modelo que representa una tarea
public class Task {
    // Atributos principales de la tarea
    private String id;
    private String titulo; // Título de la tarea
    private String descripcion; // Descripción breve de la tarea
    private String responsable; // Persona responsable de la tarea

    private String estado;

    // Constructor que inicializa los atributos de la tarea
    public Task( String titulo, String descripcion, String responsable) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.responsable = responsable;
    }

    // Getter para obtener el título de la tarea
    public String getTitulo() {
        return titulo;
    }

    // Getter para obtener la descripción de la tarea
    public String getDescripcion() {
        return descripcion;
    }

    // Getter para obtener el responsable de la tarea
    public String getResponsable() {
        return responsable;
    }

    public String getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }
}
