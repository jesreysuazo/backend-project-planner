package com.example.projectplanner.service;

import com.example.projectplanner.model.Task;
import com.example.projectplanner.model.Task.Status;
import com.example.projectplanner.model.TaskActivity;
import com.example.projectplanner.model.TaskAssignee;
import com.example.projectplanner.model.TaskAssigneeId;
import com.example.projectplanner.model.TaskComment;
import com.example.projectplanner.model.TaskTag;
import com.example.projectplanner.model.User;
import com.example.projectplanner.repository.*;
import com.example.projectplanner.dto.AssigneeDTO;
import com.example.projectplanner.dto.TaskDTO;
import com.example.projectplanner.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepo;
    private final TaskAssigneeRepository assigneeRepo;
    private final TaskTagRepository tagRepo;
    private final TaskCommentRepository commentRepo;
    private final TaskActivityRepository activityRepo;
    private final UserRepository userRepo;

    public TaskService(TaskRepository taskRepo,
                       TaskAssigneeRepository assigneeRepo,
                       TaskTagRepository tagRepo,
                       TaskCommentRepository commentRepo,
                       TaskActivityRepository activityRepo, UserRepository userRepo) {
        this.taskRepo = taskRepo;
        this.assigneeRepo = assigneeRepo;
        this.tagRepo = tagRepo;
        this.commentRepo = commentRepo;
        this.activityRepo = activityRepo;
        this.userRepo = userRepo;
    }


    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        return taskRepo.findByProjectId(projectId)
                .stream()
                // ✅ updated: map subtasks as well
                .map(task -> {
                    TaskDTO dto = TaskMapper.toDTO(task);

                    // fetch subtasks for this task
                    List<Task> subtasks = taskRepo.findByParentId(task.getId());
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
    }

    public Task createTask(Task task, Long creatorId) {
        task.setCreatedById(creatorId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setStatus(Status.NOT_STARTED);

        if (task.getParent() != null && task.getParent().getId() != null) {
            Task parent = taskRepo.findById(task.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent task not found"));

            if (!Objects.equals(parent.getProjectId(), task.getProjectId())) {
                throw new IllegalArgumentException("Parent task must belong to the same project");
            }

            task.setParent(parent);
        }

        if (task.getEffortLevel() == null) {
            throw new IllegalArgumentException("Effort level is required");
        }


        task.setTimeEstimate(calculateTimeEstimate(task));

        Task saved = taskRepo.save(task);


        User creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
        String message = String.format("%s created task \"%s\" at %s with EffortLevel %s",
                creator.getName(), saved.getTitle(), timestamp, saved.getEffortLevel());
        logActivity(saved.getId(), creatorId, "created_task", message);

        // Update parent tasks
        if (saved.getParent() != null) {
            updateParentTimeEstimates(saved.getParent());
        }

        return saved;
    }




    public Task updateTask(Long taskId, Task updatedTask, Long updaterId) {
        Task existing = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        //cannot start or complete a task if subtasks not done
        if ((updatedTask.getStatus() == Status.IN_PROGRESS || updatedTask.getStatus() == Status.DONE)) {
            if (!canStartTask(existing.getId())) {
                // throw new IllegalStateException("Cannot start task until all subtasks are completed");
                throw new IllegalArgumentException("Cannot start task until all subtasks are completed");
            }
        }

        User updater = userRepo.findById(updaterId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StringBuilder changes = new StringBuilder();


        if (updatedTask.getParent() == null) {
            if (existing.getParent() != null) {
                changes.append(String.format("removed parent (was %s); ", existing.getParent().getTitle()));
                existing.setParent(null);
            }
        } else if (updatedTask.getParent().getId() != null) {
            Task newParent = taskRepo.findById(updatedTask.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent task not found"));

            if (Objects.equals(newParent.getId(), existing.getId())) {
                // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be its own parent");
                throw new IllegalArgumentException("Task cannot be its own parent");
            }
            if (!Objects.equals(newParent.getProjectId(), existing.getProjectId())) {
                // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent must belong to the same project");
                throw new IllegalArgumentException("Parent must belong to the same project");
            }
            if (isDescendant(existing, newParent)) {
                // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign a descendant as parent");
                throw new IllegalArgumentException("Cannot assign a descendant as parent");
            }

            if (existing.getParent() == null || !Objects.equals(existing.getParent().getId(), newParent.getId())) {
                changes.append(String.format("parent changed to \"%s\"; ", newParent.getTitle()));
                existing.setParent(newParent);
            }
        }


        if (!Objects.equals(existing.getTitle(), updatedTask.getTitle())) {
            changes.append(String.format("title from \"%s\" to \"%s\"; ", existing.getTitle(), updatedTask.getTitle()));
            existing.setTitle(updatedTask.getTitle());
        }
        if (!Objects.equals(existing.getDescription(), updatedTask.getDescription())) {
            changes.append("description updated; ");
            existing.setDescription(updatedTask.getDescription());
        }
        if (!Objects.equals(existing.getStartDate(), updatedTask.getStartDate())) {
            changes.append(String.format("start date from %s to %s; ", existing.getStartDate(), updatedTask.getStartDate()));
            existing.setStartDate(updatedTask.getStartDate());
        }
        if (!Objects.equals(existing.getEndDate(), updatedTask.getEndDate())) {
            changes.append(String.format("end date from %s to %s; ", existing.getEndDate(), updatedTask.getEndDate()));
            existing.setEndDate(updatedTask.getEndDate());
        }
        if (!Objects.equals(existing.getStatus(), updatedTask.getStatus())) {
            changes.append(String.format("status from %s to %s; ", existing.getStatus(), updatedTask.getStatus()));
            existing.setStatus(updatedTask.getStatus());
        }


        if (!Objects.equals(existing.getEffortLevel(), updatedTask.getEffortLevel())) {
            changes.append(String.format("effort level from %s to %s; ", existing.getEffortLevel(), updatedTask.getEffortLevel()));
            existing.setEffortLevel(updatedTask.getEffortLevel());
        }

        existing.setUpdatedAt(LocalDateTime.now());

        // Recalculate
        existing.setTimeEstimate(calculateTimeEstimate(existing));

        Task saved = taskRepo.save(existing);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
        String message = changes.length() == 0
                ? String.format("%s updated task \"%s\" at %s (no fields changed)", updater.getName(), saved.getTitle(), timestamp)
                : String.format("%s updated task \"%s\" at %s: %s", updater.getName(), saved.getTitle(), timestamp, changes.toString());

        logActivity(saved.getId(), updaterId, "updated_task", message);

        if (saved.getParent() != null) {
            updateParentTimeEstimates(saved.getParent());
        }

        return saved;
    }

    /**
     * Utility: checks if candidateParent is a descendant of task (to prevent cycles)
     */
    private boolean isDescendant(Task task, Task candidateParent) {
        Task current = candidateParent.getParent();
        while (current != null) {
            if (Objects.equals(current.getId(), task.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }



    public boolean canStartTask(Long taskId) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        List<Task> subtasks = taskRepo.findByParentId(task.getId());


        return subtasks.stream()
                .allMatch(t -> t.getStatus() == Status.DONE);
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));


        TaskDTO dto = TaskMapper.toDTO(task);


        List<Task> subtasks = taskRepo.findByParentId(task.getId());
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
    }


    public List<String> getTagsByTaskId(Long taskId) {
        return tagRepo.findByTaskId(taskId)
                .stream()
                .map(TaskTag::getTag)
                .toList();
    }


    public List<AssigneeDTO> getAssigneesByTaskId(Long taskId) {
        return assigneeRepo.findByTaskId(taskId)
                .stream()
                .map(a -> {
                    User user = userRepo.findById(a.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return new AssigneeDTO(user.getId(), user.getName());
                })
                .toList();
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        task.setStatus(Status.DELETED);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepo.save(task);


        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));


        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));


        String message = String.format(
                "%s deleted task \"%s\" at %s",
                user.getName(),
                task.getTitle(),
                timestamp
        );

        logActivity(taskId, userId, "deleted_task", message);

        if (task.getParent() != null) {
            updateParentTimeEstimates(task.getParent());
        }
    }



    public void assignUser(Long taskId, Long userId, Long actorId) {
        TaskAssigneeId pk = new TaskAssigneeId(taskId, userId);
        if (assigneeRepo.existsById(pk)) return;

        TaskAssignee assignee = new TaskAssignee();
        assignee.setTaskId(taskId);
        assignee.setUserId(userId);
        assigneeRepo.save(assignee);


        User actor = userRepo.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found"));
        User assignedUser = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Assigned user not found"));

        String message = String.format(
                "%s added %s as a task assignee",
                actor.getName(),
                assignedUser.getName()
        );

        logActivity(taskId, actorId, "assigned_user", message);
    }

    public void removeUser(Long taskId, Long userId, Long actorId) {
        TaskAssigneeId pk = new TaskAssigneeId(taskId, userId);
        if (!assigneeRepo.existsById(pk)) return;

        assigneeRepo.deleteById(pk);


        User actor = userRepo.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Actor not found"));
        User removedUser = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Removed user not found"));

        String message = String.format(
                "%s removed %s from task assignees",
                actor.getName(),
                removedUser.getName()
        );

        logActivity(taskId, actorId, "removed_user", message);
    }

    // ---------------- Tags ----------------
    public void addTag(Long taskId, String tag, Long actorId) {
        TaskTag t = new TaskTag();
        t.setTaskId(taskId);
        t.setTag(tag);
        tagRepo.save(t);


        User actor = userRepo.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));

        String message = String.format(
                "%s added tag \"%s\" to task \"%s\" at %s",
                actor.getName(), tag, task.getTitle(), timestamp
        );

        logActivity(taskId, actorId, "added_tag", message);
    }

    public void removeTag(Long taskId, String tag, Long actorId) {
        List<TaskTag> tags = tagRepo.findByTaskId(taskId);
        tags.stream()
                .filter(t -> t.getTag().equals(tag))
                .forEach(tagRepo::delete);

        User actor = userRepo.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));


        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));

        String message = String.format(
                "%s removed tag \"%s\" from task \"%s\" at %s",
                actor.getName(), tag, task.getTitle(), timestamp
        );

        logActivity(taskId, actorId, "removed_tag", message);
    }


    public TaskComment addComment(Long taskId, Long userId, String comment) {
        // Create new comment
        TaskComment c = new TaskComment();
        c.setTaskId(taskId);
        c.setUserId(userId);
        c.setComment(comment);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());

        TaskComment saved = commentRepo.save(c);


        userRepo.findById(userId).ifPresent(user -> {
            saved.setUserName(user.getName());


            String formattedDate = saved.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
            String activityLog = String.format(
                    "%s added a comment: \"%s\" at %s",
                    user.getName(),
                    saved.getComment(),
                    formattedDate
            );

            logActivity(taskId, userId, "added_comment", activityLog);
        });

        return saved;
    }

    public void deleteComment(Long commentId, Long userId) {
        TaskComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        commentRepo.delete(c);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));

        String message = String.format(
                "%s deleted a comment \"%s\" at %s",
                user.getName(),
                c.getComment(),
                timestamp
        );

        logActivity(c.getTaskId(), userId, "deleted_comment", message);
    }


    private void logActivity(Long taskId, Long userId, String action, String detailsJson) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setUserId(userId);
        activity.setAction(action);
        activity.setDetails(detailsJson);
        activity.setCreatedAt(LocalDateTime.now());
        activityRepo.save(activity);
    }

    public List<TaskComment> getCommentsByTaskId(Long taskId) {
        List<TaskComment> comments = commentRepo.findByTaskId(taskId);

        comments.forEach(c -> {
            userRepo.findById(c.getUserId()).ifPresent(user -> c.setUserName(user.getName()));
        });

        return comments;
    }


    public List<TaskActivity> getActivityLogsByTaskId(Long taskId) {
        return activityRepo.findByTaskId(taskId);
    }

    private Long calculateTimeEstimate(Task task) {
        if (task.getTimeEstimate() != null && task.getTimeEstimate() > 0) {
            return task.getTimeEstimate();
        }

        if (task.getEffortLevel() != null) {
            switch (task.getEffortLevel()) {
                case SIMPLE -> task.setTimeEstimate(Duration.ofDays(1).toMillis());
                case MODERATE -> task.setTimeEstimate(Duration.ofDays(3).toMillis());
                case COMPLEX -> task.setTimeEstimate(Duration.ofDays(5).toMillis());
            }
        }

        return task.getTimeEstimate() != null ? task.getTimeEstimate() : 0L;
    }

    private void updateParentTimeEstimates(Task parent) {
        if (parent == null) return;

        List<Task> subtasks = taskRepo.findByParentId(parent.getId()).stream()
                .filter(st -> st.getStatus() != Task.Status.DELETED)
                .toList();

        LocalDateTime maxSubtaskEnd = subtasks.stream()
                .map(Task::getEndDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        boolean startChanged = false;

        if (maxSubtaskEnd != null) {
            // Only update parent start if it is before the latest subtask end
            if (parent.getStartDate() == null || parent.getStartDate().isBefore(maxSubtaskEnd)) {
                parent.setStartDate(maxSubtaskEnd.plusDays(1));
                startChanged = true;
            }
        }

        // Recalculate end date
        if (parent.getEndDate() == null || startChanged) {
            long durationMs = calculateTimeEstimate(parent);
            LocalDateTime newEnd = parent.getStartDate().plus(Duration.ofMillis(durationMs));
            parent.setEndDate(newEnd.withHour(23).withMinute(59).withSecond(59).withNano(999_000_000));
        }

        taskRepo.save(parent);

        if (parent.getParent() != null) {
            updateParentTimeEstimates(parent.getParent());
        }
    }


    public void generateProjectSchedule(
            List<Task> tasks,
            LocalDateTime projectStartDate,
            Map<Long, List<Long>> dependencies,
            Long userId
    ) {
        Map<Long, Task> taskMap = tasks.stream()
                .filter(t -> t.getStatus() != Status.DONE && t.getStatus() != Status.DELETED)
                .collect(Collectors.toMap(Task::getId, t -> t));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Filter dependencies for non-deleted tasks
        Map<Long, List<Long>> filteredDependencies = dependencies.entrySet().stream()
                .filter(e -> taskMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(taskMap::containsKey)
                                .toList()
                ));

        List<Long> sortedIds = topologicalSort(filteredDependencies);

        for (Long taskId : sortedIds) {
            Task task = taskMap.get(taskId);
            if (task == null) continue;

            List<Long> deps = filteredDependencies.getOrDefault(taskId, Collections.emptyList());

            // Generate start date if null
            if (task.getStartDate() == null) {
                LocalDateTime start = projectStartDate;
                if (!deps.isEmpty()) {
                    start = deps.stream()
                            .map(depId -> {
                                Task depTask = taskMap.get(depId);
                                return depTask != null ? depTask.getEndDate() : null;
                            })
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(projectStartDate);
                }
                task.setStartDate(start);
            }

            // Generate end date if null
            if (task.getEndDate() == null) {
                long durationMs = calculateTimeEstimate(task);
                LocalDateTime end = task.getStartDate().plus(Duration.ofMillis(durationMs));
                task.setEndDate(end.withHour(23).withMinute(59).withSecond(59).withNano(999_000_000));
            }

            taskRepo.save(task);
        }

        // Update parent tasks if any
        for (Task task : tasks) {
            if (task.getParent() != null) {
                updateParentTimeEstimates(task.getParent());
            }
        }
    }




    private List<Long> topologicalSort(Map<Long, List<Long>> dependencies) {
        // Collect all task IDs
        Set<Long> allTasks = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allTasks::addAll);

        // Initialize in-degree map
        Map<Long, Integer> inDegree = new HashMap<>();
        allTasks.forEach(id -> inDegree.put(id, 0));

        // Build in-degree (increment dependents, not dependencies)
        for (Map.Entry<Long, List<Long>> entry : dependencies.entrySet()) {
            Long task = entry.getKey();
            for (Long dep : entry.getValue()) {
                inDegree.put(task, inDegree.get(task) + 1);
            }
        }

        // Queue for nodes with 0 in-degree
        Queue<Long> queue = new LinkedList<>();
        inDegree.forEach((k, v) -> {
            if (v == 0) queue.add(k);
        });

        // Perform Kahn’s algorithm
        List<Long> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long n = queue.poll();
            sorted.add(n);

            // For each task that depends on n, decrement in-degree
            for (Map.Entry<Long, List<Long>> entry : dependencies.entrySet()) {
                Long task = entry.getKey();
                List<Long> deps = entry.getValue();
                if (deps.contains(n)) {
                    inDegree.put(task, inDegree.get(task) - 1);
                    if (inDegree.get(task) == 0) queue.add(task);
                }
            }
        }

        if (sorted.size() != allTasks.size()) {
            throw new IllegalStateException("Cycle detected in dependencies");
        }

        return sorted;
    }

}
