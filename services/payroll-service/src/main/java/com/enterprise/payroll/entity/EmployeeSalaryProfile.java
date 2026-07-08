package com.enterprise.payroll.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Local, denormalized reference to an employee's base salary for payroll
 * purposes. In a real system this would be synced from the Employee Service
 * via an event or scheduled reconciliation job; here it is seeded directly
 * to keep the demo self-contained.
 */
@Entity
@Table(name = "employee_salary_profiles")
public class EmployeeSalaryProfile {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_name", nullable = false, length = 200)
    private String employeeName;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(nullable = false)
    private boolean active = true;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
