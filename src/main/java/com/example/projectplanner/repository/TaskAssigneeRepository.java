package com.example.projectplanner.repository;

import com.example.projectplanner.model.TaskAssignee;
import com.example.projectplanner.model.TaskAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, TaskAssigneeId> {
    List<TaskAssignee> findByTaskId(Long taskId);
    List<TaskAssignee> findByUserId(Long userId);
}
