package com.enterprise.payroll.service;

import com.enterprise.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Background job that automatically generates payroll on the 1st of every
 * month for the previous month, per the spec's "monthly payroll job"
 * requirement. Generation is idempotent (PayrollService rejects duplicate
 * runs for the same period), so this is safe to retry.
 */
@Component
public class PayrollScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(PayrollScheduledJob.class);

    private final PayrollService payrollService;

    public PayrollScheduledJob(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // Runs at 02:00 AM on the 1st day of every month
    @Scheduled(cron = "${app.payroll.monthly-cron:0 0 2 1 * *}")
    public void runMonthlyPayroll() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        int month = previousMonth.getMonthValue();
        int year = previousMonth.getYear();

        log.info("Scheduled monthly payroll job starting for period {}-{}", year, month);
        try {
            var summary = payrollService.generateForMonth(month, year);
            log.info("Scheduled monthly payroll job completed: processed={} failed={}",
                    summary.getProcessedCount(), summary.getFailedCount());
        } catch (BusinessException e) {
            // Already generated for this period - not an error, just a no-op
            log.info("Scheduled monthly payroll job skipped: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Scheduled monthly payroll job failed for period {}-{}", year, month, e);
        }
    }
}
