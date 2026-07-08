package com.enterprise.payroll.repository;

import com.enterprise.payroll.entity.EmployeeSalaryProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeSalaryProfileRepository extends JpaRepository<EmployeeSalaryProfile, Long> {
    List<EmployeeSalaryProfile> findByActiveTrue();
}
