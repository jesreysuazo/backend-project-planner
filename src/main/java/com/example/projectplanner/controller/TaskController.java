package com.example.projectplanner.controller;

import com.example.projectplanner.model.Task;
import com.example.projectplanner.model.TaskActivity;
import com.example.projectplanner.model.TaskComment;
import com.example.projectplanner.security.JwtProvider;
import com.example.projectplanner.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.projectplanner.dto.AssigneeDTO;
import com.example.projectplanner.dto.TaskDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JwtProvider jwtProvider;

    public TaskController(TaskService taskService, JwtProvider jwtProvider) {
        this.taskService = taskService;
        this.jwtProvider = jwtProvider;
    }


    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Task task,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);
        return ResponseEntity.ok(taskService.createTask(task, userId));
    }

    @GetMapping("/by-project")
    public ResponseEntity<List<TaskDTO>> getTasksByProjectId(@RequestParam Long projectId) {
        List<TaskDTO> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Task task,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);
        return ResponseEntity.ok(taskService.updateTask(id, task, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtProvider.getIdFromToken(jwt);
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/assignees")
    public ResponseEntity<List<AssigneeDTO>> getAllAssigneesByTask(@PathVariable Long id) {
        List<AssigneeDTO> assignees = taskService.getAssigneesByTaskId(id);
        return ResponseEntity.ok(assignees);
    }


    @PostMapping("/{id}/assign/{userId}")
    public ResponseEntity<Void> assignUser(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        taskService.assignUser(id, userId, actorId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/assign/{userId}")
    public ResponseEntity<Void> removeUser(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        taskService.removeUser(id, userId, actorId);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/tags")
    public ResponseEntity<Void> addTag(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        String tag = body.get("tag");
        taskService.addTag(id, tag, actorId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/tags")
    public ResponseEntity<Void> removeTag(
            @PathVariable Long id,
            @RequestParam String tag,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        taskService.removeTag(id, tag, actorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/tags")
    public ResponseEntity<List<String>> getAllTagsByTask(@PathVariable Long id) {
        List<String> tags = taskService.getTagsByTaskId(id);
        return ResponseEntity.ok(tags);
    }


    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskComment> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        return ResponseEntity.ok(
                taskService.addComment(id, actorId, body.get("comment"))
        );
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long actorId = jwtProvider.getIdFromToken(jwt);
        taskService.deleteComment(commentId, actorId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TaskComment>> getAllCommentsByTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getCommentsByTaskId(id));
    }


    @GetMapping("/{id}/activity-logs")
    public ResponseEntity<List<TaskActivity>> getAllActivityLogsByTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getActivityLogsByTaskId(id));
    }

    @GetMapping("/effort-levels")
    public ResponseEntity<Task.EffortLevel[]> getEffortLevels() {
        return ResponseEntity.ok(Task.EffortLevel.values());
    }
}
