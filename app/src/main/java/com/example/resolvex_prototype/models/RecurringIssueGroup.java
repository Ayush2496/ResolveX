package com.example.resolvex_prototype.models;

public class RecurringIssueGroup {
    private String category;
    private String location;
    private int reportCount;
    private String earliestReport;
    private String latestReport;

    public RecurringIssueGroup() {
    }

    public RecurringIssueGroup(String category, String location, int reportCount, String earliestReport, String latestReport) {
        this.category = category;
        this.location = location;
        this.reportCount = reportCount;
        this.earliestReport = earliestReport;
        this.latestReport = latestReport;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    public String getEarliestReport() {
        return earliestReport;
    }

    public void setEarliestReport(String earliestReport) {
        this.earliestReport = earliestReport;
    }

    public String getLatestReport() {
        return latestReport;
    }

    public void setLatestReport(String latestReport) {
        this.latestReport = latestReport;
    }
}