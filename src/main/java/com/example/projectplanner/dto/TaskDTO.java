package com.example.projectplanner.dto;

import com.example.projectplanner.model.Task;
import com.example.projectplanner.model.Task.Status;

import java.time.LocalDateTime;
import java.util.List;

public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ParentSummary parent;
    private Long projectId;
    private Long createdById;
    private long timeEstimate;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Task.EffortLevel effortLevel;


    private List<SubTaskSummary> subtasks;


    public static class ParentSummary {
        private Long id;
        private String title;

        public ParentSummary(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
    }


    public static class SubTaskSummary {
        private Long id;
        private String title;
        private Status status;

        public SubTaskSummary(Long id, String title, Status status) {
            this.id = id;
            this.title = title;
            this.status = status;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public Status getStatus() { return status; }
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public ParentSummary getParent() { return parent; }
    public void setParent(ParentSummary parent) { this.parent = parent; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public long getTimeEstimate() { return timeEstimate; }
    public void setTimeEstimate(long timeEstimate) { this.timeEstimate = timeEstimate; }

    public Task.Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Task.EffortLevel getEffortLevel() { return effortLevel; }
    public void setEffortLevel(Task.EffortLevel effortLevel) { this.effortLevel = effortLevel; }


    public List<SubTaskSummary> getSubtasks() { return subtasks; }
    public void setSubtasks(List<SubTaskSummary> subtasks) { this.subtasks = subtasks; }
}
