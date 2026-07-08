package com.enterprise.employee.controller;

import com.enterprise.employee.dto.DepartmentSummary;
import com.enterprise.employee.dto.EmployeeRequest;
import com.enterprise.employee.dto.EmployeeResponse;
import com.enterprise.employee.service.EmployeeService;
import com.enterprise.shared.constant.Roles;
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
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee record management: CRUD, search, department and manager listings")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Create a new employee record")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created", employeeService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Search employees with optional filters, pagination and sorting")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long managerId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                employeeService.search(search, department, status, managerId, pageable)));
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "List employees in a given department")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> byDepartment(
            @PathVariable String department, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.listByDepartment(department, pageable)));
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "List direct reports of a manager")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.listByManager(managerId)));
    }

    @GetMapping("/reports/department-summary")
    @Operation(summary = "Employee count per department (cached)")
    public ResponseEntity<ApiResponse<List<DepartmentSummary>>> departmentSummary() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.departmentSummary()));
    }

    @GetMapping("/reports/active-count")
    @Operation(summary = "Total active employee count (cached)")
    public ResponseEntity<ApiResponse<Long>> activeCount() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.activeEmployeeCount()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update an employee record")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", employeeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an employee record")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted", null));
    }
}
