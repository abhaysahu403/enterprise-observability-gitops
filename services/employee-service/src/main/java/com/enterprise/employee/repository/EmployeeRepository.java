package com.enterprise.employee.repository;

import com.enterprise.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    Page<Employee> findByDepartment(com.enterprise.employee.entity.Department department, Pageable pageable);

    List<Employee> findByManagerId(Long managerId);

    long countByStatus(com.enterprise.employee.entity.EmployeeStatus status);

    long countByDepartment(com.enterprise.employee.entity.Department department);
}
