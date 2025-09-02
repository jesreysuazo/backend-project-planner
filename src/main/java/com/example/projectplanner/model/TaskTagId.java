package com.example.projectplanner.model;

import java.io.Serializable;
import java.util.Objects;

public class TaskTagId implements Serializable {
    private Long taskId;
    private String tag;

    public TaskTagId() {}
    public TaskTagId(Long taskId, String tag) {
        this.taskId = taskId;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskTagId)) return false;
        TaskTagId that = (TaskTagId) o;
        return taskId.equals(that.taskId) && tag.equals(that.tag);
    }

    @Override
    public int hashCode() { return Objects.hash(taskId, tag); }
}
