package com.example.projectplanner.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "task_assignees")
@IdClass(TaskAssigneeId.class)
public class TaskAssignee {

    @Id
    private Long taskId;

    @Id
    private Long userId;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
