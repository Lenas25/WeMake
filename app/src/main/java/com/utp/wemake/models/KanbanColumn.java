package com.utp.wemake.models;

import java.util.List;

public class KanbanColumn {

    private String title;
    private List<Task> tasks;

    public KanbanColumn(String title, List<Task> tasks) {
        this.title = title;
        this.tasks = tasks;
    }

    public String getTitle() { return title; }
    public List<Task> getTasks() { return tasks; }
}
