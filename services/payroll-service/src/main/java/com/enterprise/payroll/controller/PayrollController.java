package com.enterprise.payroll.controller;

import com.enterprise.payroll.dto.PayrollGenerateRequest;
import com.enterprise.payroll.dto.PayrollResponse;
import com.enterprise.payroll.dto.PayrollRunSummary;
import com.enterprise.payroll.service.PayrollService;
import com.enterprise.shared.dto.ApiResponse;
import com.enterprise.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@Tag(name = "Payroll", description = "Monthly payroll generation, payslips, salary history, and status")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Manually trigger payroll generation for a given month/year (also runs automatically via the monthly job)")
    public ResponseEntity<ApiResponse<PayrollRunSummary>> generate(@Valid @RequestBody PayrollGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll generated",
                payrollService.generateForMonth(request.getMonth(), request.getYear())));
    }

    @GetMapping("/payslip")
    @Operation(summary = "Get a specific payslip for an employee and period")
    public ResponseEntity<ApiResponse<PayrollResponse>> payslip(
            @RequestParam Long employeeId, @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPayslip(employeeId, month, year)));
    }

    @GetMapping("/history")
    @Operation(summary = "Salary history for an employee")
    public ResponseEntity<ApiResponse<PageResponse<PayrollResponse>>> history(
            @RequestParam Long employeeId, @PageableDefault(size = 12, sort = "payYear") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.salaryHistory(employeeId, pageable)));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Payroll status for all employees in a given period")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> status(
            @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.payrollStatusForPeriod(month, year)));
    }

    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Mark a payroll record as paid")
    public ResponseEntity<ApiResponse<PayrollResponse>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked as paid", payrollService.markPaid(id)));
    }
}
