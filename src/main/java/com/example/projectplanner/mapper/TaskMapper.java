package com.example.projectplanner.mapper;

import com.example.projectplanner.dto.TaskDTO;
import com.example.projectplanner.model.Task;

public class TaskMapper {

    public static TaskDTO toDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStartDate(task.getStartDate());
        dto.setEndDate(task.getEndDate());

        if (task.getParent() != null) {
            dto.setParent(new TaskDTO.ParentSummary(
                    task.getParent().getId(),
                    task.getParent().getTitle()
            ));
        } else {
            dto.setParent(null);
        }

        dto.setProjectId(task.getProjectId());
        dto.setCreatedById(task.getCreatedById());
        dto.setTimeEstimate(task.getTimeEstimate());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setEffortLevel(task.getEffortLevel());

        return dto;
    }
}
