package com.example.projectplanner.dto;

import java.util.List;

public class ProjectScheduleResponse {
    private List<TaskDTO> tasks;
    private long totalDays;

    public ProjectScheduleResponse(List<TaskDTO> tasks, long totalDays) {
        this.tasks = tasks;
        this.totalDays = totalDays;
    }

    public List<TaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDTO> tasks) {
        this.tasks = tasks;
    }

    public long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }
}
