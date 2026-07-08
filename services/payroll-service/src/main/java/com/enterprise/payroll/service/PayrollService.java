package com.enterprise.payroll.service;

import com.enterprise.payroll.dto.PayrollResponse;
import com.enterprise.payroll.dto.PayrollRunSummary;
import com.enterprise.payroll.entity.EmployeeSalaryProfile;
import com.enterprise.payroll.entity.PayrollRecord;
import com.enterprise.payroll.entity.PayrollStatus;
import com.enterprise.payroll.repository.EmployeeSalaryProfileRepository;
import com.enterprise.payroll.repository.PayrollRecordRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    // Simple progressive annual tax slabs (illustrative, not a real tax regime)
    private static final BigDecimal SLAB_1_LIMIT = BigDecimal.valueOf(500_000);
    private static final BigDecimal SLAB_2_LIMIT = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal SLAB_2_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal SLAB_3_RATE = BigDecimal.valueOf(0.20);

    private static final BigDecimal MONTHLY_BONUS_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal YEAR_END_BONUS_RATE = BigDecimal.valueOf(0.15);

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeSalaryProfileRepository salaryProfileRepository;

    public PayrollService(PayrollRecordRepository payrollRecordRepository,
                           EmployeeSalaryProfileRepository salaryProfileRepository) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.salaryProfileRepository = salaryProfileRepository;
    }

    @Transactional
    public PayrollRunSummary generateForMonth(int month, int year) {
        if (payrollRecordRepository.existsByPayYearAndPayMonth(year, month)) {
            throw new BusinessException("ALREADY_GENERATED",
                    String.format("Payroll for %d-%02d has already been generated", year, month));
        }

        List<EmployeeSalaryProfile> profiles = salaryProfileRepository.findByActiveTrue();
        List<PayrollResponse> results = new ArrayList<>();
        int failed = 0;

        for (EmployeeSalaryProfile profile : profiles) {
            try {
                PayrollRecord record = buildRecord(profile, month, year);
                PayrollRecord saved = payrollRecordRepository.save(record);
                results.add(PayrollResponse.from(saved));
            } catch (Exception e) {
                failed++;
                log.error("Payroll generation failed for employeeId={} period={}-{}",
                        profile.getEmployeeId(), year, month, e);
            }
        }

        log.info("Payroll run completed: period={}-{} processed={} failed={}", year, month, results.size(), failed);
        return new PayrollRunSummary(month, year, results.size(), failed, results);
    }

    private PayrollRecord buildRecord(EmployeeSalaryProfile profile, int month, int year) {
        BigDecimal basic = profile.getBasicSalary();
        BigDecimal bonus = calculateBonus(basic, month);
        BigDecimal gross = basic.add(bonus);
        BigDecimal tax = calculateMonthlyTax(gross);
        BigDecimal net = gross.subtract(tax).setScale(2, RoundingMode.HALF_UP);

        PayrollRecord record = new PayrollRecord();
        record.setEmployeeId(profile.getEmployeeId());
        record.setEmployeeName(profile.getEmployeeName());
        record.setPayMonth(month);
        record.setPayYear(year);
        record.setBasicSalary(basic);
        record.setBonus(bonus.setScale(2, RoundingMode.HALF_UP));
        record.setGrossSalary(gross.setScale(2, RoundingMode.HALF_UP));
        record.setTaxDeducted(tax.setScale(2, RoundingMode.HALF_UP));
        record.setNetSalary(net);
        record.setStatus(PayrollStatus.GENERATED);
        record.setGeneratedAt(Instant.now());
        return record;
    }

    private BigDecimal calculateBonus(BigDecimal basic, int month) {
        BigDecimal bonus = basic.multiply(MONTHLY_BONUS_RATE);
        if (month == 12) {
            bonus = bonus.add(basic.multiply(YEAR_END_BONUS_RATE));
        }
        return bonus;
    }

    private BigDecimal calculateMonthlyTax(BigDecimal monthlyGross) {
        BigDecimal annualGross = monthlyGross.multiply(BigDecimal.valueOf(12));
        BigDecimal annualTax;

        if (annualGross.compareTo(SLAB_1_LIMIT) <= 0) {
            annualTax = BigDecimal.ZERO;
        } else if (annualGross.compareTo(SLAB_2_LIMIT) <= 0) {
            annualTax = annualGross.subtract(SLAB_1_LIMIT).multiply(SLAB_2_RATE);
        } else {
            BigDecimal slab2Tax = SLAB_2_LIMIT.subtract(SLAB_1_LIMIT).multiply(SLAB_2_RATE);
            BigDecimal slab3Tax = annualGross.subtract(SLAB_2_LIMIT).multiply(SLAB_3_RATE);
            annualTax = slab2Tax.add(slab3Tax);
        }

        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public PayrollResponse getPayslip(Long employeeId, int month, int year) {
        PayrollRecord record = payrollRecordRepository.findByEmployeeIdAndPayYearAndPayMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Payslip not found for employee %d, period %d-%02d", employeeId, year, month)));
        return PayrollResponse.from(record);
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollResponse> salaryHistory(Long employeeId, Pageable pageable) {
        Page<PayrollRecord> page = payrollRecordRepository.findByEmployeeId(employeeId, pageable);
        return PageResponse.from(page.map(PayrollResponse::from));
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> payrollStatusForPeriod(int month, int year) {
        return payrollRecordRepository.findByPayYearAndPayMonth(year, month).stream()
                .map(PayrollResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PayrollResponse markPaid(Long payrollRecordId) {
        PayrollRecord record = payrollRecordRepository.findById(payrollRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record", "id", payrollRecordId));
        record.setStatus(PayrollStatus.PAID);
        PayrollRecord saved = payrollRecordRepository.save(record);
        log.info("Payroll record marked PAID: id={}", payrollRecordId);
        return PayrollResponse.from(saved);
    }
}
