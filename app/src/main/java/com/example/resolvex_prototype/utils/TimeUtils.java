package com.example.resolvex_prototype.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * All timestamps in the database are stored as "yyyy-MM-dd HH:mm:ss" in the device's local time.
 * Because every timestamp is created here (never by SQLite's datetime('now')), comparisons such as
 * "created in the last 30 days" are always local-vs-local and cannot drift by the time-zone offset.
 * Locale.US keeps the digits ASCII on every device language.
 */
public class TimeUtils {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static String format(Date date) {
        return new SimpleDateFormat(PATTERN, Locale.US).format(date);
    }

    public static String now() {
        return format(new Date());
    }

    public static String hoursAgo(int hours) {
        return format(new Date(System.currentTimeMillis() - hours * 3600000L));
    }

    public static String daysAgo(int days) {
        return hoursAgo(days * 24);
    }

    private static Date parse(String timestamp) {
        try {
            return new SimpleDateFormat(PATTERN, Locale.US).parse(timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    /** Short "how long ago": now, 12m, 3h, 2d, 3w. Returns "" when the text cannot be read. */
    public static String ago(String timestamp) {
        Date then = parse(timestamp);
        if (then == null) return "";
        long minutes = (System.currentTimeMillis() - then.getTime()) / 60000L;
        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        if (days < 7) return days + "d";
        return (days / 7) + "w";
    }

    /** "21 Sep" */
    public static String shortDate(String timestamp) {
        Date then = parse(timestamp);
        if (then == null) return "";
        return new SimpleDateFormat("d MMM", Locale.US).format(then);
    }

    /** Midnight at the start of today (local time). */
    public static String startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return format(c.getTime());
    }
}
