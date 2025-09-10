package com.utp.wemake;

public class Task {
    private String titulo;
    private String descripcion;
    private String responsable;

    public Task(String titulo, String descripcion, String responsable) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.responsable = responsable;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getResponsable() {
        return responsable;
    }
}
