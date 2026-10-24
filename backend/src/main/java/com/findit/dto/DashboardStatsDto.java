package com.findit.dto;

public class DashboardStatsDto {

    private long totalReports;
    private long lostCount;
    private long foundCount;
    private long openLostCount;
    private long openFoundCount;
    private long pendingClaimsCount;
    private long resolvedCount;
    private int recoveryRate;

    public DashboardStatsDto() {
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getLostCount() {
        return lostCount;
    }

    public void setLostCount(long lostCount) {
        this.lostCount = lostCount;
    }

    public long getFoundCount() {
        return foundCount;
    }

    public void setFoundCount(long foundCount) {
        this.foundCount = foundCount;
    }

    public long getOpenLostCount() {
        return openLostCount;
    }

    public void setOpenLostCount(long openLostCount) {
        this.openLostCount = openLostCount;
    }

    public long getOpenFoundCount() {
        return openFoundCount;
    }

    public void setOpenFoundCount(long openFoundCount) {
        this.openFoundCount = openFoundCount;
    }

    public long getPendingClaimsCount() {
        return pendingClaimsCount;
    }

    public void setPendingClaimsCount(long pendingClaimsCount) {
        this.pendingClaimsCount = pendingClaimsCount;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public int getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(int recoveryRate) {
        this.recoveryRate = recoveryRate;
    }
}
