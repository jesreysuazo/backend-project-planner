package com.example.projectplanner.dto;

import java.time.LocalDateTime;

public class CreateProjectRequest {
    private String name;
    private String description;
    private LocalDateTime projectStartDate;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getProjectStartDate() { return projectStartDate; }
    public void setProjectStartDate(LocalDateTime projectStartDate) { this.projectStartDate = projectStartDate; }
}
