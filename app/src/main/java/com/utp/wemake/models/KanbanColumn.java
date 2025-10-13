package com.utp.wemake.models;

import java.util.List;

public class KanbanColumn {

    private String title;
    private List<TaskModel> tasks;

    public KanbanColumn(String title, List<TaskModel> tasks) {
        this.title = title;
        this.tasks = tasks;
    }

    public String getTitle() { return title; }
    public List<TaskModel> getTasks() { return tasks; }
}
