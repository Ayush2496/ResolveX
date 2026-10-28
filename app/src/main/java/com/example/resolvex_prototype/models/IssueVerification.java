package com.example.resolvex_prototype.models;

public class IssueVerification {
    private int verificationId;
    private int issueId;
    private int studentId;
    private int resolutionCycle;
    private String result; // VERIFIED or REOPENED
    private String reason;
    private String comment;
    private String timestamp;

    public IssueVerification() {
    }

    public int getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(int verificationId) {
        this.verificationId = verificationId;
    }

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getResolutionCycle() {
        return resolutionCycle;
    }

    public void setResolutionCycle(int resolutionCycle) {
        this.resolutionCycle = resolutionCycle;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}