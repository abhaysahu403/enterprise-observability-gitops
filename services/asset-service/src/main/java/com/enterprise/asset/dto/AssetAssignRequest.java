package com.enterprise.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssetAssignRequest {

    @NotNull(message = "Employee id is required")
    private Long employeeId;

    @NotBlank(message = "Employee name is required")
    private String employeeName;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
