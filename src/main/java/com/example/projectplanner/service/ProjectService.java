package com.example.projectplanner.service;

import com.example.projectplanner.dto.CreateProjectRequest;
import com.example.projectplanner.dto.ProjectDTO;
import com.example.projectplanner.dto.ProjectMember;
import com.example.projectplanner.model.Project;
import com.example.projectplanner.model.ProjectUser;
import com.example.projectplanner.model.ProjectUserId;
import com.example.projectplanner.model.User;
import com.example.projectplanner.repository.ProjectRepository;
import com.example.projectplanner.repository.ProjectUserRepository;
import com.example.projectplanner.repository.UserRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectUserRepository projectUserRepo;
    private final UserRepository userRepo;

    public ProjectService(ProjectRepository projectRepo,
                          ProjectUserRepository projectUserRepo,
                          UserRepository userRepo) {
        this.projectRepo = projectRepo;
        this.projectUserRepo = projectUserRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public ProjectDTO createProject(String name, String description, LocalDateTime projectStartDate, Long userId) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setInviteCode(generateInviteCode());
        project.setCreatedById(userId);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setProjectStartDate(projectStartDate);
        project.setStatus(1);

        Project saved = projectRepo.save(project);

        ProjectUser pu = new ProjectUser();
        pu.setProjectId(saved.getId());
        pu.setUserId(userId);
        pu.setRole(ProjectUser.Role.OWNER);
        pu.setJoinedAt(LocalDateTime.now());
        projectUserRepo.save(pu);

        return toDTO(saved);
    }

    @Transactional
    public ProjectDTO joinProject(String inviteCode, Long userId) {
        Project project = projectRepo.findByInviteCode(inviteCode);
        if (project == null || project.getStatus() == 2) {
            throw new IllegalArgumentException("Invalid or deleted project");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ProjectUserId pk = new ProjectUserId(project.getId(), user.getId());
        if (projectUserRepo.existsById(pk)) {
            throw new IllegalStateException("User already in project");
        }

        ProjectUser projectUser = new ProjectUser();
        projectUser.setProjectId(project.getId());
        projectUser.setUserId(user.getId());
        projectUser.setRole(ProjectUser.Role.MEMBER);
        projectUser.setJoinedAt(LocalDateTime.now());
        projectUserRepo.save(projectUser);

        return toDTO(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getCreatedById().equals(userId)) {
            throw new SecurityException("Only the owner can delete this project");
        }

        project.setStatus(2);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepo.save(project);
    }

    @Transactional
    public ProjectDTO updateProject(Long projectId, CreateProjectRequest req, Long userId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        if (!project.getCreatedById().equals(userId)) {
            throw new SecurityException("Only the owner can update this project");
        }

        if (req.getName() != null) project.setName(req.getName());
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        if (req.getProjectStartDate() != null) project.setProjectStartDate(req.getProjectStartDate());

        project.setUpdatedAt(LocalDateTime.now());
        projectRepo.save(project);

        return toDTO(project);
    }

    public List<ProjectDTO> getAllProjectsForUser(Long userId) {
        List<ProjectUser> userProjects = projectUserRepo.findByUserId(userId);
        return userProjects.stream()
                .map(up -> projectRepo.findById(up.getProjectId()).orElse(null))
                .filter(p -> p != null && p.getStatus() == 1)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ProjectDTO toDTO(Project project) {
        List<ProjectUser> members = projectUserRepo.findByProjectId(project.getId());
        List<ProjectMember> memberDTOs = members.stream()
                .map(mu -> {
                    User u = userRepo.findById(mu.getUserId()).orElse(null);
                    String name = u != null ? u.getName() : "Unknown";
                    return new ProjectMember(mu.getUserId(), name, mu.getRole().name());
                })
                .collect(Collectors.toList());

        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getInviteCode(),
                project.getProjectStartDate(),
                memberDTOs
        );
    }

    public List<ProjectMember> getProjectMembers(Long projectId, Long userId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));


        boolean isMember = projectUserRepo.findByProjectId(projectId).stream()
                .anyMatch(mu -> mu.getUserId().equals(userId));

        if (!isMember) {
            throw new SecurityException("You are not a member of this project");
        }


        List<ProjectUser> members = projectUserRepo.findByProjectId(projectId);
        return members.stream()
                .map(mu -> {
                    User u = userRepo.findById(mu.getUserId()).orElse(null);
                    String name = u != null ? u.getName() : "Unknown";
                    return new ProjectMember(mu.getUserId(), name, mu.getRole().name());
                })
                .toList();
    }

    private String generateInviteCode() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    public ProjectDTO getProjectById(Long projectId, Long userId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));


        boolean isMember = projectUserRepo.findByProjectId(projectId).stream()
                .anyMatch(mu -> mu.getUserId().equals(userId));

        if (!isMember) {
            throw new SecurityException("You are not a member of this project");
        }


        return toDTO(project);
    }

}
