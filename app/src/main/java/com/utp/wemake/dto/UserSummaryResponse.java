package com.utp.wemake.dto;

import com.google.gson.annotations.SerializedName;

public class UserSummaryResponse {
    @SerializedName("tasks_involved")
    private int tasksInvolved;

    @SerializedName("tasks_completed")
    private int tasksCompleted;

    @SerializedName("on_time_rate")
    private double onTimeRate;

    @SerializedName("overdue_tasks")
    private int overdueTasks;

    // -- Getters --
    public int getTasksInvolved() {
        return tasksInvolved;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public double getOnTimeRate() {
        return onTimeRate;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }
}