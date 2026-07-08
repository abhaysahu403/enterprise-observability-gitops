package com.enterprise.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LeaveDecisionRequest {

    @NotNull(message = "Approver id is required")
    private Long approverId;

    @NotBlank(message = "Approver name is required")
    private String approverName;

    private String comment;

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
