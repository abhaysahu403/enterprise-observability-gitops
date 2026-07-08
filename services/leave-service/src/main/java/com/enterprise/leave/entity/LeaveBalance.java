package com.enterprise.leave.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"})
})
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(name = "total_allocated", nullable = false)
    private int totalAllocated;

    @Column(name = "used", nullable = false)
    private int used = 0;

    @Version
    private Long version;

    public int available() {
        return totalAllocated - used;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(int totalAllocated) { this.totalAllocated = totalAllocated; }
    public int getUsed() { return used; }
    public void setUsed(int used) { this.used = used; }
}
