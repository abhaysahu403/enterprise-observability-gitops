package com.enterprise.payroll.dto;

import java.util.List;

public class PayrollRunSummary {

    private int month;
    private int year;
    private int processedCount;
    private int failedCount;
    private List<PayrollResponse> records;

    public PayrollRunSummary(int month, int year, int processedCount, int failedCount, List<PayrollResponse> records) {
        this.month = month;
        this.year = year;
        this.processedCount = processedCount;
        this.failedCount = failedCount;
        this.records = records;
    }

    public int getMonth() { return month; }
    public int getYear() { return year; }
    public int getProcessedCount() { return processedCount; }
    public int getFailedCount() { return failedCount; }
    public List<PayrollResponse> getRecords() { return records; }
}
