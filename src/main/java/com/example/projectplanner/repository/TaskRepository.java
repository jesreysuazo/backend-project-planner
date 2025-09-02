package com.example.projectplanner.repository;

import com.example.projectplanner.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByParentId(Long parentId);
    List<Task> findByProjectId(Long boardId);
    List<Task> findByStatus(String status);
}
