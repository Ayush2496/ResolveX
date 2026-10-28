package com.example.resolvex_prototype.utils;

import java.util.Locale;

public class IssueStatusManager {

    public static final String SUBMITTED = "SUBMITTED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String RESOLVED = "RESOLVED";
    public static final String VERIFIED = "VERIFIED";
    public static final String REOPENED = "REOPENED";
    public static final String CLOSED = "CLOSED";

    /**
     * Not a real issue status. It is only written to the history timeline when an admin
     * changes an issue's priority, so that change is also traceable.
     */
    public static final String PRIORITY_CHANGED = "PRIORITY_CHANGED";

    /** Like PRIORITY_CHANGED: a history-only marker written when an admin hands an issue to another staff member. */
    public static final String REASSIGNED = "REASSIGNED";

    public static boolean isValidTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;

        String cur = currentStatus.trim().toUpperCase(Locale.ROOT);
        String next = newStatus.trim().toUpperCase(Locale.ROOT);

        if (cur.equals(next)) return false;

        switch (cur) {
            case SUBMITTED:
                return next.equals(ASSIGNED);
            case ASSIGNED:
                return next.equals(IN_PROGRESS);
            case IN_PROGRESS:
                return next.equals(RESOLVED);
            case RESOLVED:
                return next.equals(VERIFIED) || next.equals(REOPENED);
            case REOPENED:
                return next.equals(IN_PROGRESS);
            case VERIFIED:
                return next.equals(CLOSED);
            case CLOSED:
                return false;
            default:
                return false;
        }
    }

    public static String getHumanReadableStatus(String status) {
        if (status == null) return "Reported";
        switch (status.trim().toUpperCase(Locale.ROOT)) {
            case SUBMITTED:
                return "Reported";
            case ASSIGNED:
                return "Assigned";
            case IN_PROGRESS:
                return "In Progress";
            case RESOLVED:
                return "Resolution Submitted";
            case VERIFIED:
                return "Verified";
            case REOPENED:
                return "Reopened";
            case CLOSED:
                return "Closed";
            case PRIORITY_CHANGED:
                return "Priority Changed";
            case REASSIGNED:
                return "Reassigned";
            default:
                return status;
        }
    }

    public static String formatIssueId(int issueId) {
        return String.format(Locale.US, "RX-%04d", issueId);
    }
}
