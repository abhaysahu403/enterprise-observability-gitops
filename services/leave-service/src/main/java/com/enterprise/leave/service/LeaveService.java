package com.enterprise.leave.service;

import com.enterprise.leave.dto.*;
import com.enterprise.leave.entity.*;
import com.enterprise.leave.repository.LeaveBalanceRepository;
import com.enterprise.leave.repository.LeaveRequestRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    // Default annual allocation per leave type, used when a balance row
    // doesn't exist yet for an employee/year (first-time application).
    private static final int DEFAULT_ANNUAL = 18;
    private static final int DEFAULT_CASUAL = 12;
    private static final int DEFAULT_MEDICAL = 10;
    private static final int DEFAULT_WFH = 24;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    public LeaveService(LeaveRequestRepository leaveRequestRepository, LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional
    public LeaveResponse apply(LeaveApplyRequest request) {
        LeaveType type = parseType(request.getLeaveType());

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("INVALID_DATE_RANGE", "End date cannot be before start date");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int year = request.getStartDate().getYear();

        LeaveBalance balance = getOrCreateBalance(request.getEmployeeId(), type, year);
        if (balance.available() < totalDays) {
            throw new BusinessException("INSUFFICIENT_BALANCE",
                    String.format("Insufficient %s leave balance: requested %d day(s), %d available",
                            type.name(), totalDays, balance.available()));
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(request.getEmployeeId());
        leave.setEmployeeName(request.getEmployeeName());
        leave.setLeaveType(type);
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setTotalDays(totalDays);
        leave.setReason(request.getReason());
        leave.setStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(leave);
        log.info("Leave applied: id={} employeeId={} type={} days={}",
                saved.getId(), saved.getEmployeeId(), type, totalDays);
        return LeaveResponse.from(saved);
    }

    @Transactional
    public LeaveResponse approve(Long id, LeaveDecisionRequest request) {
        LeaveRequest leave = findOrThrow(id);
        requirePending(leave);

        LeaveBalance balance = getOrCreateBalance(leave.getEmployeeId(), leave.getLeaveType(), leave.getStartDate().getYear());
        if (balance.available() < leave.getTotalDays()) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "Employee no longer has sufficient balance for this request");
        }
        balance.setUsed(balance.getUsed() + leave.getTotalDays());
        leaveBalanceRepository.save(balance);

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApproverId(request.getApproverId());
        leave.setApproverName(request.getApproverName());
        leave.setApproverComment(request.getComment());
        leave.setDecidedAt(Instant.now());

        LeaveRequest saved = leaveRequestRepository.save(leave);
        log.info("Leave approved: id={} approver={}", saved.getId(), request.getApproverName());
        return LeaveResponse.from(saved);
    }

    @Transactional
    public LeaveResponse reject(Long id, LeaveDecisionRequest request) {
        LeaveRequest leave = findOrThrow(id);
        requirePending(leave);

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApproverId(request.getApproverId());
        leave.setApproverName(request.getApproverName());
        leave.setApproverComment(request.getComment());
        leave.setDecidedAt(Instant.now());

        LeaveRequest saved = leaveRequestRepository.save(leave);
        log.info("Leave rejected: id={} approver={}", saved.getId(), request.getApproverName());
        return LeaveResponse.from(saved);
    }

    @Transactional
    public LeaveResponse cancel(Long id, Long requestingEmployeeId) {
        LeaveRequest leave = findOrThrow(id);

        if (!leave.getEmployeeId().equals(requestingEmployeeId)) {
            throw new BusinessException("NOT_OWNER", "You can only cancel your own leave requests");
        }
        if (leave.getStatus() == LeaveStatus.CANCELLED || leave.getStatus() == LeaveStatus.REJECTED) {
            throw new BusinessException("INVALID_STATE", "Leave request is already " + leave.getStatus());
        }

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            LeaveBalance balance = getOrCreateBalance(leave.getEmployeeId(), leave.getLeaveType(), leave.getStartDate().getYear());
            balance.setUsed(Math.max(0, balance.getUsed() - leave.getTotalDays()));
            leaveBalanceRepository.save(balance);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(leave);
        log.info("Leave cancelled: id={}", saved.getId());
        return LeaveResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeaveResponse> history(Long employeeId, String status, Pageable pageable) {
        Page<LeaveRequest> page;
        if (employeeId != null && status != null) {
            page = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, parseStatus(status), pageable);
        } else if (employeeId != null) {
            page = leaveRequestRepository.findByEmployeeId(employeeId, pageable);
        } else if (status != null) {
            page = leaveRequestRepository.findByStatus(parseStatus(status), pageable);
        } else {
            page = leaveRequestRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(LeaveResponse::from));
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> balances(Long employeeId, Integer year) {
        int y = year != null ? year : Year.now().getValue();
        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, y);

        // Ensure every leave type has a visible balance row, even if unused so far
        List<LeaveType> allTypes = List.of(LeaveType.values());
        List<LeaveBalanceResponse> result = new java.util.ArrayList<>();
        for (LeaveType type : allTypes) {
            LeaveBalance existing = balances.stream().filter(b -> b.getLeaveType() == type).findFirst()
                    .orElseGet(() -> getOrCreateBalance(employeeId, type, y));
            result.add(LeaveBalanceResponse.from(existing));
        }
        return result;
    }

    private LeaveBalance getOrCreateBalance(Long employeeId, LeaveType type, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(employeeId, type, year)
                .orElseGet(() -> {
                    LeaveBalance balance = new LeaveBalance();
                    balance.setEmployeeId(employeeId);
                    balance.setLeaveType(type);
                    balance.setYear(year);
                    balance.setTotalAllocated(defaultAllocation(type));
                    balance.setUsed(0);
                    return leaveBalanceRepository.save(balance);
                });
    }

    private int defaultAllocation(LeaveType type) {
        return switch (type) {
            case ANNUAL -> DEFAULT_ANNUAL;
            case CASUAL -> DEFAULT_CASUAL;
            case MEDICAL -> DEFAULT_MEDICAL;
            case WORK_FROM_HOME -> DEFAULT_WFH;
        };
    }

    private void requirePending(LeaveRequest leave) {
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("INVALID_STATE", "Leave request is not in PENDING state (current: " + leave.getStatus() + ")");
        }
    }

    private LeaveRequest findOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request", "id", id));
    }

    private LeaveType parseType(String value) {
        try {
            return LeaveType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_LEAVE_TYPE", "Unknown leave type: " + value);
        }
    }

    private LeaveStatus parseStatus(String value) {
        try {
            return LeaveStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown leave status: " + value);
        }
    }
}
