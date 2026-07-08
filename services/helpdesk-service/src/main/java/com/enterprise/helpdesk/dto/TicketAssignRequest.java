package com.enterprise.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TicketAssignRequest {

    @NotNull(message = "Assignee employee id is required")
    private Long assigneeId;

    @NotBlank(message = "Assignee name is required")
    private String assigneeName;

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
}
