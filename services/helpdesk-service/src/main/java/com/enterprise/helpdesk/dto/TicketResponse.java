package com.enterprise.helpdesk.dto;

import com.enterprise.helpdesk.entity.Ticket;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Long raisedByEmployeeId;
    private String raisedByName;
    private Long assignedToEmployeeId;
    private String assignedToName;
    private Instant slaDueAt;
    private boolean slaBreached;
    private String resolution;
    private Instant resolvedAt;
    private List<TicketCommentResponse> comments;
    private Instant createdAt;

    public static TicketResponse from(Ticket t) {
        TicketResponse dto = new TicketResponse();
        dto.id = t.getId();
        dto.ticketNumber = t.getTicketNumber();
        dto.title = t.getTitle();
        dto.description = t.getDescription();
        dto.category = t.getCategory().name();
        dto.priority = t.getPriority().name();
        dto.status = t.getStatus().name();
        dto.raisedByEmployeeId = t.getRaisedByEmployeeId();
        dto.raisedByName = t.getRaisedByName();
        dto.assignedToEmployeeId = t.getAssignedToEmployeeId();
        dto.assignedToName = t.getAssignedToName();
        dto.slaDueAt = t.getSlaDueAt();
        dto.slaBreached = t.getResolvedAt() == null
                ? Instant.now().isAfter(t.getSlaDueAt())
                : t.getResolvedAt().isAfter(t.getSlaDueAt());
        dto.resolution = t.getResolution();
        dto.resolvedAt = t.getResolvedAt();
        dto.createdAt = t.getCreatedAt();
        if (t.getComments() != null) {
            dto.comments = t.getComments().stream().map(TicketCommentResponse::from).collect(Collectors.toList());
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public Long getRaisedByEmployeeId() { return raisedByEmployeeId; }
    public String getRaisedByName() { return raisedByName; }
    public Long getAssignedToEmployeeId() { return assignedToEmployeeId; }
    public String getAssignedToName() { return assignedToName; }
    public Instant getSlaDueAt() { return slaDueAt; }
    public boolean isSlaBreached() { return slaBreached; }
    public String getResolution() { return resolution; }
    public Instant getResolvedAt() { return resolvedAt; }
    public List<TicketCommentResponse> getComments() { return comments; }
    public Instant getCreatedAt() { return createdAt; }
}
