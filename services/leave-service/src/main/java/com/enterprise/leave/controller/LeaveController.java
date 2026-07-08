package com.enterprise.leave.controller;

import com.enterprise.leave.dto.*;
import com.enterprise.leave.service.LeaveService;
import com.enterprise.shared.dto.ApiResponse;
import com.enterprise.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@Tag(name = "Leave Management", description = "Apply, approve, reject, cancel leave requests; balances and history")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<ApiResponse<LeaveResponse>> apply(@Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request submitted", leaveService.apply(request)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    @Operation(summary = "Approve a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> approve(@PathVariable Long id,
                                                                @Valid @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Leave approved", leaveService.approve(id, request)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HR')")
    @Operation(summary = "Reject a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> reject(@PathVariable Long id,
                                                               @Valid @RequestBody LeaveDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Leave rejected", leaveService.reject(id, request)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel your own leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> cancel(@PathVariable Long id,
                                                               @RequestParam Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success("Leave cancelled", leaveService.cancel(id, employeeId)));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get leave balance for an employee, by leave type")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> balance(
            @RequestParam Long employeeId, @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.balances(employeeId, year)));
    }

    @GetMapping("/history")
    @Operation(summary = "Leave history / reports with optional employee and status filters")
    public ResponseEntity<ApiResponse<PageResponse<LeaveResponse>>> history(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.history(employeeId, status, pageable)));
    }
}
