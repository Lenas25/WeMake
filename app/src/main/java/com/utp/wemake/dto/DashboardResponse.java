package com.utp.wemake.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class DashboardResponse {

    @SerializedName("summary")
    public SummaryMetrics summary;

    @SerializedName("productivity")
    public ProductivityMetrics productivity;

    @SerializedName("predictions")
    public Predictions predictions;

    public static class SummaryMetrics {
        @SerializedName("total_tasks")
        public int totalTasks;

        @SerializedName("pending_tasks")
        public int pendingTasks;
    }

    public static class ProductivityMetrics {
        @SerializedName("tasks_completed_per_week")
        public Map<String, Integer> tasksCompletedPerWeek;

        @SerializedName("priority_distribution")
        public Map<String, Integer> priorityDistribution;

        @SerializedName("avg_completion_time_days")
        public double avgCompletionTimeDays;

        @SerializedName("on_time_completion_rate")
        public double onTimeCompletionRate;
    }

    public static class Predictions {
        @SerializedName("at_risk_tasks")
        public List<AtRiskTask> atRiskTasks;
    }

    public static class AtRiskTask {
        @SerializedName("title")
        public String title;

        @SerializedName("priority")
        public String priority;
    }
}