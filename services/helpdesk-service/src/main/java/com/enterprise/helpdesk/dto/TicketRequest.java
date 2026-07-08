package com.enterprise.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TicketRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotNull(message = "Raised-by employee id is required")
    private Long raisedByEmployeeId;

    @NotBlank(message = "Raised-by employee name is required")
    private String raisedByName;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getRaisedByEmployeeId() { return raisedByEmployeeId; }
    public void setRaisedByEmployeeId(Long raisedByEmployeeId) { this.raisedByEmployeeId = raisedByEmployeeId; }
    public String getRaisedByName() { return raisedByName; }
    public void setRaisedByName(String raisedByName) { this.raisedByName = raisedByName; }
}
