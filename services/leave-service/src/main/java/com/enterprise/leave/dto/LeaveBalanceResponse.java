package com.enterprise.leave.dto;

import com.enterprise.leave.entity.LeaveBalance;

public class LeaveBalanceResponse {

    private String leaveType;
    private int totalAllocated;
    private int used;
    private int available;
    private int year;

    public static LeaveBalanceResponse from(LeaveBalance b) {
        LeaveBalanceResponse dto = new LeaveBalanceResponse();
        dto.leaveType = b.getLeaveType().name();
        dto.totalAllocated = b.getTotalAllocated();
        dto.used = b.getUsed();
        dto.available = b.available();
        dto.year = b.getYear();
        return dto;
    }

    public String getLeaveType() { return leaveType; }
    public int getTotalAllocated() { return totalAllocated; }
    public int getUsed() { return used; }
    public int getAvailable() { return available; }
    public int getYear() { return year; }
}
