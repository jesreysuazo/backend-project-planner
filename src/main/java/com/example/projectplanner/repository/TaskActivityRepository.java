package com.example.projectplanner.repository;

import com.example.projectplanner.model.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    List<TaskActivity> findByTaskId(Long taskId);
}
