package com.example.projectplanner.model;

import jakarta.persistence.*;

@Entity
@Table(name = "task_tags")
@IdClass(TaskTagId.class)
public class TaskTag {

    @Id
    private Long taskId;

    @Id
    private String tag;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
