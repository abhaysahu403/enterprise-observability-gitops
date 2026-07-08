package com.enterprise.employee.service;

import com.enterprise.employee.dto.DepartmentSummary;
import com.enterprise.employee.dto.EmployeeRequest;
import com.enterprise.employee.dto.EmployeeResponse;
import com.enterprise.employee.entity.Department;
import com.enterprise.employee.entity.Employee;
import com.enterprise.employee.entity.EmployeeStatus;
import com.enterprise.employee.entity.SalaryGrade;
import com.enterprise.employee.repository.EmployeeRepository;
import com.enterprise.employee.repository.EmployeeSpecifications;
import com.enterprise.shared.constant.AppConstants;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true)
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Employee with email '" + request.getEmail() + "' already exists");
        }

        Employee employee = new Employee();
        applyRequest(employee, request);
        employee.setEmployeeCode(generateEmployeeCode());

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created: id={} code={}", saved.getId(), saved.getEmployeeCode());
        return EmployeeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_EMPLOYEE, key = "#id")
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String search, String department, String status,
                                                   Long managerId, Pageable pageable) {
        Department dept = parseEnum(Department.class, department, "department");
        EmployeeStatus st = parseEnum(EmployeeStatus.class, status, "status");

        Page<Employee> page = employeeRepository.findAll(
                EmployeeSpecifications.withFilters(search, dept, st, managerId), pageable);
        return PageResponse.from(page.map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> listByDepartment(String department, Pageable pageable) {
        Department dept = parseEnum(Department.class, department, "department");
        Page<Employee> page = employeeRepository.findByDepartment(dept, pageable);
        return PageResponse.from(page.map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listByManager(Long managerId) {
        return employeeRepository.findByManagerId(managerId).stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = AppConstants.CACHE_EMPLOYEE, key = "#id")
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);

        if (!employee.getEmail().equalsIgnoreCase(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already in use by another employee");
        }

        applyRequest(employee, request);
        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: id={}", saved.getId());
        return EmployeeResponse.from(saved);
    }

    @Transactional
    @CacheEvict(value = AppConstants.CACHE_EMPLOYEE, key = "#id")
    public void delete(Long id) {
        Employee employee = findOrThrow(id);
        employeeRepository.delete(employee);
        log.info("Employee deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_DASHBOARD, key = "'department-summary'")
    public List<DepartmentSummary> departmentSummary() {
        return List.of(Department.values()).stream()
                .map(d -> new DepartmentSummary(d.name(), employeeRepository.countByDepartment(d)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_DASHBOARD, key = "'status-summary'")
    public long activeEmployeeCount() {
        return employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
    }

    private void applyRequest(Employee employee, EmployeeRequest request) {
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setAddress(request.getAddress());
        employee.setDepartment(parseEnum(Department.class, request.getDepartment(), "department"));
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setSalaryGrade(parseEnum(SalaryGrade.class, request.getSalaryGrade(), "salaryGrade"));
        employee.setAuthUserId(request.getAuthUserId());

        if (request.getStatus() != null) {
            employee.setStatus(parseEnum(EmployeeStatus.class, request.getStatus(), "status"));
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id (manager)", request.getManagerId()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_" + fieldName.toUpperCase(), "Invalid " + fieldName + ": " + value);
        }
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private String generateEmployeeCode() {
        long nextId = employeeRepository.count() + 1;
        return String.format("EMP%05d", nextId);
    }
}
