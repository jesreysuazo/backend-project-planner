package com.example.projectplanner.repository;

import com.example.projectplanner.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Project findByInviteCode(String inviteCode);

    List<Project> findByCreatedByIdAndStatus(Long createdById, Integer status);
}