package com.example.resolvex_prototype.models;

public class ResolutionAnalytics {
    private int totalResolved;
    private int totalVerified;
    private int totalReopened;
    private double verificationRate;
    private double reopenRate;
    private double avgResolutionTimeHours;

    public ResolutionAnalytics() {
    }

    public int getTotalResolved() {
        return totalResolved;
    }

    public void setTotalResolved(int totalResolved) {
        this.totalResolved = totalResolved;
    }

    public int getTotalVerified() {
        return totalVerified;
    }

    public void setTotalVerified(int totalVerified) {
        this.totalVerified = totalVerified;
    }

    public int getTotalReopened() {
        return totalReopened;
    }

    public void setTotalReopened(int totalReopened) {
        this.totalReopened = totalReopened;
    }

    public double getVerificationRate() {
        return verificationRate;
    }

    public void setVerificationRate(double verificationRate) {
        this.verificationRate = verificationRate;
    }

    public double getReopenRate() {
        return reopenRate;
    }

    public void setReopenRate(double reopenRate) {
        this.reopenRate = reopenRate;
    }

    public double getAvgResolutionTimeHours() {
        return avgResolutionTimeHours;
    }

    public void setAvgResolutionTimeHours(double avgResolutionTimeHours) {
        this.avgResolutionTimeHours = avgResolutionTimeHours;
    }
}