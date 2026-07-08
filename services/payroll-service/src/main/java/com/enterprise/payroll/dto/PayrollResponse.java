package com.enterprise.payroll.dto;

import com.enterprise.payroll.entity.PayrollRecord;

import java.math.BigDecimal;
import java.time.Instant;

public class PayrollResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private int payMonth;
    private int payYear;
    private BigDecimal basicSalary;
    private BigDecimal bonus;
    private BigDecimal grossSalary;
    private BigDecimal taxDeducted;
    private BigDecimal netSalary;
    private String status;
    private Instant generatedAt;

    public static PayrollResponse from(PayrollRecord r) {
        PayrollResponse dto = new PayrollResponse();
        dto.id = r.getId();
        dto.employeeId = r.getEmployeeId();
        dto.employeeName = r.getEmployeeName();
        dto.payMonth = r.getPayMonth();
        dto.payYear = r.getPayYear();
        dto.basicSalary = r.getBasicSalary();
        dto.bonus = r.getBonus();
        dto.grossSalary = r.getGrossSalary();
        dto.taxDeducted = r.getTaxDeducted();
        dto.netSalary = r.getNetSalary();
        dto.status = r.getStatus().name();
        dto.generatedAt = r.getGeneratedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public int getPayMonth() { return payMonth; }
    public int getPayYear() { return payYear; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getBonus() { return bonus; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public BigDecimal getTaxDeducted() { return taxDeducted; }
    public BigDecimal getNetSalary() { return netSalary; }
    public String getStatus() { return status; }
    public Instant getGeneratedAt() { return generatedAt; }
}
