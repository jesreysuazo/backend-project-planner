package com.example.projectplanner.controller;

import com.example.projectplanner.dto.JoinProjectRequest;
import com.example.projectplanner.dto.CreateProjectRequest;
import com.example.projectplanner.dto.ProjectDTO;
import com.example.projectplanner.dto.TaskDTO;
import com.example.projectplanner.dto.ProjectScheduleResponse;
import com.example.projectplanner.dto.ProjectMember;
import com.example.projectplanner.repository.ProjectRepository;
import com.example.projectplanner.repository.TaskRepository;
import com.example.projectplanner.security.JwtProvider;
import com.example.projectplanner.service.ProjectService;
import com.example.projectplanner.service.TaskService;
import com.example.projectplanner.mapper.TaskMapper;
import com.example.projectplanner.model.Project;
import com.example.projectplanner.model.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Comparator;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final JwtProvider jwtProvider;
    private final TaskService taskService;
    private final ProjectRepository projectRepo;
    private final TaskRepository taskRepo;

    public ProjectController(ProjectService projectService,
                             JwtProvider jwtProvider,
                             TaskService taskService,
                             ProjectRepository projectRepo,
                             TaskRepository taskRepo) {
        this.projectService = projectService;
        this.jwtProvider = jwtProvider;
        this.taskService = taskService;
        this.projectRepo = projectRepo;
        this.taskRepo = taskRepo;
    }

    @PostMapping("/create")
    public ProjectDTO createProject(
            @RequestBody CreateProjectRequest req,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        return projectService.createProject(
                req.getName(),
                req.getDescription(),
                req.getProjectStartDate(),
                userId
        );
    }

    @PostMapping("/join")
    public ProjectDTO joinProject(
            @RequestBody JoinProjectRequest req,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        return projectService.joinProject(req.getInviteCode(), userId);
    }

    @PostMapping("/{projectId}/delete")
    public ResponseEntity<?> softDeleteProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        projectService.deleteProject(projectId, userId);

        return ResponseEntity.ok("Project marked as deleted");
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMember>> getProjectMembers(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);


        List<ProjectMember> members = projectService.getProjectMembers(projectId, userId);

        return ResponseEntity.ok(members);
    }


    @GetMapping("/my-projects")
    public List<ProjectDTO> getAllProjects(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        return projectService.getAllProjectsForUser(userId);
    }

    @PatchMapping("/{projectId}")
    public ProjectDTO updateProject(
            @PathVariable Long projectId,
            @RequestBody CreateProjectRequest req,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        return projectService.updateProject(projectId, req, userId);
    }

    @GetMapping("/{projectId}")
    public ProjectDTO getProjectById(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        return projectService.getProjectById(projectId, userId);
    }

    @PostMapping("/{projectId}/schedule")
    public ResponseEntity<ProjectScheduleResponse> generateProjectScheduleForProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token
    ) {

        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        LocalDateTime projectStart = project.getProjectStartDate();
        if (projectStart == null) {
            projectStart = LocalDateTime.now();
        }


        List<Task> tasks = taskRepo.findByProjectId(projectId).stream()
                .filter(t -> t.getStatus() != Task.Status.DELETED)
                .toList();


        Map<Long, List<Long>> dependencies = new HashMap<>();
        for (Task task : tasks) {
            if (task.getParent() != null && task.getParent().getStatus() != Task.Status.DELETED) {
                dependencies
                        .computeIfAbsent(task.getId(), k -> new ArrayList<>())
                        .add(task.getParent().getId());
            }
        }

        // generate schedule
        taskService.generateProjectSchedule(tasks, projectStart, dependencies, userId);


        List<TaskDTO> dtoList = tasks.stream()
                .filter(t -> t.getStatus() != Task.Status.DELETED)
                .sorted(Comparator.comparing(Task::getStartDate, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(task -> {
                    TaskDTO dto = TaskMapper.toDTO(task);

                    List<Task> subtasks = taskRepo.findByParentId(task.getId()).stream()
                            .filter(st -> st.getStatus() != Task.Status.DELETED)
                            .sorted(Comparator.comparing(Task::getStartDate, Comparator.nullsLast(LocalDateTime::compareTo)))
                            .toList();

                    dto.setSubtasks(
                            subtasks.stream()
                                    .map(st -> new TaskDTO.SubTaskSummary(
                                            st.getId(),
                                            st.getTitle(),
                                            st.getStatus()
                                    ))
                                    .toList()
                    );
                    return dto;
                })
                .toList();

        // Calculate project total duration (earliest start to latest end)
        LocalDateTime minStart = tasks.stream()
                .map(Task::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(projectStart);

        LocalDateTime maxEnd = tasks.stream()
                .map(Task::getEndDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(projectStart);

        long totalDays = ChronoUnit.DAYS.between(minStart.toLocalDate(), maxEnd.toLocalDate()) + 1;

        ProjectScheduleResponse response = new ProjectScheduleResponse(dtoList, totalDays);
        return ResponseEntity.ok(response);
    }
}
