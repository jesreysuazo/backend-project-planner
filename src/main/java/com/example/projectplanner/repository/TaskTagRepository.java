package com.example.projectplanner.repository;

import com.example.projectplanner.model.TaskTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {
    List<TaskTag> findByTaskId(Long taskId);
}
