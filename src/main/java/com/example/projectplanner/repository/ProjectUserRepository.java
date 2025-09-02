package com.example.projectplanner.repository;

import com.example.projectplanner.model.ProjectUser;
import com.example.projectplanner.model.ProjectUserId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {
    List<ProjectUser> findByUserId(Long userId);
    List<ProjectUser> findByProjectId(Long projectId);
}
