package com.example.projectplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Task parent;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    private Long timeEstimate; // in milliseconds


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EffortLevel effortLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== ENUMS =====
    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        DONE,
        DELETED,
        ON_HOLD
    }

    public enum EffortLevel {
        SIMPLE,
        MODERATE,
        COMPLEX
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Task getParent() { return parent; }
    public void setParent(Task parent) { this.parent = parent; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public Long getTimeEstimate() { return timeEstimate; }
    public void setTimeEstimate(Long timeEstimate) { this.timeEstimate = timeEstimate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public EffortLevel getEffortLevel() { return effortLevel; }
    public void setEffortLevel(EffortLevel effortLevel) { this.effortLevel = effortLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
