package com.example.resolvex_prototype.utils;

/**
 * The fixed lists used by the forms and filters. Keeping them in one place means the
 * report form, the filters and the database always agree on the exact spelling.
 */
public class IssueOptions {

    public static final String[] CATEGORIES = {
            "Classroom Equipment", "Internet / Wi-Fi", "Water / Plumbing", "Furniture",
            "Electrical", "Cleanliness / Sanitation", "Other"
    };

    public static final String[] PRIORITIES = {"Low", "Medium", "High"};

    /** Campus areas. Students pick one of these instead of typing free text. */
    public static final String[] BUILDINGS = {
            "Block A", "Block B", "Block C", "Central Library", "Cafeteria",
            "Auditorium", "Sports Complex", "Hostel", "Parking Area", "Other"
    };

    /** Builds the stored location, e.g. "Block A - Room 204" (or just "Central Library"). */
    public static String buildLocation(String building, String room) {
        String cleanRoom = capitalizeWords(normalizeSpaces(room));
        if (cleanRoom.isEmpty()) {
            return building;
        }
        return building + " - " + cleanRoom;
    }

    /** Trims the text and collapses repeated spaces. */
    public static String normalizeSpaces(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }

    /** "room 204" becomes "Room 204". */
    public static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean startOfWord = true;
        for (char ch : text.toCharArray()) {
            if (startOfWord && Character.isLetter(ch)) {
                sb.append(Character.toUpperCase(ch));
            } else {
                sb.append(ch);
            }
            startOfWord = (ch == ' ');
        }
        return sb.toString();
    }

    public static boolean isValidPriority(String priority) {
        for (String p : PRIORITIES) {
            if (p.equalsIgnoreCase(priority)) return true;
        }
        return false;
    }
}
