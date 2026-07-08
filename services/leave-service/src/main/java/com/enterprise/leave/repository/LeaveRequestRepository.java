package com.enterprise.leave.repository;

import com.enterprise.leave.entity.LeaveRequest;
import com.enterprise.leave.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status, Pageable pageable);

    long countByStatus(LeaveStatus status);
}
