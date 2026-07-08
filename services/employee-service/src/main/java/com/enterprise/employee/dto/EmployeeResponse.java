package com.enterprise.employee.dto;

import com.enterprise.employee.entity.Employee;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public class EmployeeResponse implements Serializable {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String department;
    private String designation;
    private Long managerId;
    private String managerName;
    private LocalDate joiningDate;
    private String status;
    private String salaryGrade;
    private Instant createdAt;

    public static EmployeeResponse from(Employee e) {
        EmployeeResponse dto = new EmployeeResponse();
        dto.id = e.getId();
        dto.employeeCode = e.getEmployeeCode();
        dto.firstName = e.getFirstName();
        dto.lastName = e.getLastName();
        dto.email = e.getEmail();
        dto.phone = e.getPhone();
        dto.address = e.getAddress();
        dto.department = e.getDepartment() != null ? e.getDepartment().name() : null;
        dto.designation = e.getDesignation();
        if (e.getManager() != null) {
            dto.managerId = e.getManager().getId();
            dto.managerName = e.getManager().getFirstName() + " " + e.getManager().getLastName();
        }
        dto.joiningDate = e.getJoiningDate();
        dto.status = e.getStatus() != null ? e.getStatus().name() : null;
        dto.salaryGrade = e.getSalaryGrade() != null ? e.getSalaryGrade().name() : null;
        dto.createdAt = e.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public Long getManagerId() { return managerId; }
    public String getManagerName() { return managerName; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public String getStatus() { return status; }
    public String getSalaryGrade() { return salaryGrade; }
    public Instant getCreatedAt() { return createdAt; }
}
