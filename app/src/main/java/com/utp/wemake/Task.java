package com.utp.wemake;

public class Task {
    private String titulo;
    private String descripcion;
    private String revisor;

    public Task(String titulo, String descripcion, String revisor) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.revisor = revisor;
    }

    public String getTitulo() {return titulo;}
    public String getDescripcion() {return descripcion;}
    public String getRevisor() {return revisor;}
}
