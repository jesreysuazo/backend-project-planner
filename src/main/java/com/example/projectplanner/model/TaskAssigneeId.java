package com.example.projectplanner.model;

import java.io.Serializable;
import java.util.Objects;

public class TaskAssigneeId implements Serializable {
    private Long taskId;
    private Long userId;

    public TaskAssigneeId() {}

    public TaskAssigneeId(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskAssigneeId)) return false;
        TaskAssigneeId that = (TaskAssigneeId) o;
        return taskId.equals(that.taskId) && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId);
    }
}
