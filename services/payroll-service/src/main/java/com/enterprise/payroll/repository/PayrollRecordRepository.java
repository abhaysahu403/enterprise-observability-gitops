package com.enterprise.payroll.repository;

import com.enterprise.payroll.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    Page<PayrollRecord> findByEmployeeId(Long employeeId, Pageable pageable);

    List<PayrollRecord> findByPayYearAndPayMonth(int payYear, int payMonth);

    Optional<PayrollRecord> findByEmployeeIdAndPayYearAndPayMonth(Long employeeId, int payYear, int payMonth);

    boolean existsByPayYearAndPayMonth(int payYear, int payMonth);
}
