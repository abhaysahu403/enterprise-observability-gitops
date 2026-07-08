package com.enterprise.leave.dto;

import com.enterprise.leave.entity.LeaveRequest;

import java.time.Instant;
import java.time.LocalDate;

public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private String reason;
    private String status;
    private Long approverId;
    private String approverName;
    private String approverComment;
    private Instant decidedAt;
    private Instant createdAt;

    public static LeaveResponse from(LeaveRequest r) {
        LeaveResponse dto = new LeaveResponse();
        dto.id = r.getId();
        dto.employeeId = r.getEmployeeId();
        dto.employeeName = r.getEmployeeName();
        dto.leaveType = r.getLeaveType().name();
        dto.startDate = r.getStartDate();
        dto.endDate = r.getEndDate();
        dto.totalDays = r.getTotalDays();
        dto.reason = r.getReason();
        dto.status = r.getStatus().name();
        dto.approverId = r.getApproverId();
        dto.approverName = r.getApproverName();
        dto.approverComment = r.getApproverComment();
        dto.decidedAt = r.getDecidedAt();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getTotalDays() { return totalDays; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public Long getApproverId() { return approverId; }
    public String getApproverName() { return approverName; }
    public String getApproverComment() { return approverComment; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
