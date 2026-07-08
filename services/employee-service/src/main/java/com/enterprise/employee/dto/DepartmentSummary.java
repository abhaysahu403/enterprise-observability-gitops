package com.enterprise.employee.dto;

public class DepartmentSummary {
    private String department;
    private long employeeCount;

    // Default constructor for Jackson/Redis deserialization
    public DepartmentSummary() {
    }

    public DepartmentSummary(String department, long employeeCount) {
        this.department = department;
        this.employeeCount = employeeCount;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public long getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(long employeeCount) { this.employeeCount = employeeCount; }
}
