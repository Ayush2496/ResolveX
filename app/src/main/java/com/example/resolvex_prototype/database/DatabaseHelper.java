package com.example.resolvex_prototype.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.resolvex_prototype.models.AppNotification;
import com.example.resolvex_prototype.models.Feedback;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.models.IssueHistory;
import com.example.resolvex_prototype.models.IssueVerification;
import com.example.resolvex_prototype.models.RecurringIssueGroup;
import com.example.resolvex_prototype.models.ResolutionAnalytics;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.IssueOptions;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.PasswordUtils;
import com.example.resolvex_prototype.utils.TimeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "resolvex.db";
    private static final int DATABASE_VERSION = 7;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_ISSUES = "issues";
    public static final String TABLE_ISSUE_HISTORY = "issue_history";
    public static final String TABLE_ISSUE_VERIFICATION = "issue_verification";
    public static final String TABLE_FEEDBACK = "feedback";
    public static final String TABLE_NOTIFICATIONS = "notifications";

    /** A category + location with at least this many reports inside the window counts as "recurring". */
    public static final int RECURRING_THRESHOLD = 3;
    public static final int RECURRING_WINDOW_DAYS = 30;

    private static final String TITLE_RECURRING = "\u26A0 Recurring Issue Detected";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // =====================================================================
    //  SCHEMA, UPGRADES AND DEMO DATA
    // =====================================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "department TEXT, " +
                "year INTEGER DEFAULT 1, " +
                "phone TEXT, " +
                "created_at TEXT NOT NULL)";

        String createIssues = "CREATE TABLE " + TABLE_ISSUES + " (" +
                "issue_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "priority TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "image_path TEXT, " +
                "status TEXT NOT NULL, " +
                "assigned_staff_id INTEGER DEFAULT NULL, " +
                "resolution_notes TEXT, " +
                "resolution_image_path TEXT, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id), " +
                "FOREIGN KEY(assigned_staff_id) REFERENCES " + TABLE_USERS + "(user_id))";

        String createHistory = "CREATE TABLE " + TABLE_ISSUE_HISTORY + " (" +
                "history_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "issue_id INTEGER NOT NULL, " +
                "old_status TEXT, " +
                "new_status TEXT NOT NULL, " +
                "updated_by INTEGER NOT NULL, " +
                "note TEXT, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY(issue_id) REFERENCES " + TABLE_ISSUES + "(issue_id), " +
                "FOREIGN KEY(updated_by) REFERENCES " + TABLE_USERS + "(user_id))";

        String createFeedback = "CREATE TABLE " + TABLE_FEEDBACK + " (" +
                "feedback_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "issue_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL, " +
                "rating INTEGER NOT NULL, " +
                "comment TEXT, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(issue_id) REFERENCES " + TABLE_ISSUES + "(issue_id), " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id))";

        db.execSQL(createUsers);
        db.execSQL(createIssues);
        db.execSQL(createHistory);
        db.execSQL(verificationTableSql(TABLE_ISSUE_VERIFICATION));
        db.execSQL(createFeedback);
        db.execSQL(notificationsTableSql());
        createIndexes(db);

        seedInitialData(db);
    }

    private String verificationTableSql(String tableName) {
        return "CREATE TABLE " + tableName + " (" +
                "verification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "issue_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "resolution_cycle INTEGER NOT NULL DEFAULT 1, " +
                "result TEXT NOT NULL, " +
                "reason TEXT, " +
                "comment TEXT, " +
                "timestamp TEXT NOT NULL, " +
                "FOREIGN KEY(issue_id) REFERENCES " + TABLE_ISSUES + "(issue_id), " +
                "FOREIGN KEY(student_id) REFERENCES " + TABLE_USERS + "(user_id))";
    }

    private String notificationsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (" +
                "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "issue_id INTEGER, " +
                "title TEXT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "is_read INTEGER DEFAULT 0, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES " + TABLE_USERS + "(user_id))";
    }

    /** Indexes make the filters and dashboards fast when there are many issues. */
    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_issues_user ON " + TABLE_ISSUES + "(user_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_issues_staff ON " + TABLE_ISSUES + "(assigned_staff_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_issues_category_location ON " + TABLE_ISSUES + "(category, location)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_issue ON " + TABLE_ISSUE_HISTORY + "(issue_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_user ON " + TABLE_NOTIFICATIONS + "(user_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(verificationTableSql(TABLE_ISSUE_VERIFICATION).replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS "));
        }

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ISSUE_VERIFICATION + " ADD COLUMN resolution_cycle INTEGER NOT NULL DEFAULT 1");
            } catch (Exception ignored) {}

            try {
                db.execSQL("ALTER TABLE " + TABLE_FEEDBACK + " ADD COLUMN user_id INTEGER");
                db.execSQL("UPDATE " + TABLE_FEEDBACK + " SET user_id = (SELECT user_id FROM " + TABLE_ISSUES + " WHERE " + TABLE_ISSUES + ".issue_id = " + TABLE_FEEDBACK + ".issue_id) WHERE user_id IS NULL");
            } catch (Exception ignored) {}

            try {
                db.execSQL("UPDATE " + TABLE_ISSUES + " SET assigned_staff_id = NULL WHERE assigned_staff_id = 0");
            } catch (Exception ignored) {}
        }

        if (oldVersion < 4) {
            db.execSQL(notificationsTableSql());

            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN year INTEGER DEFAULT 1");
            } catch (Exception ignored) {}
        }

        if (oldVersion < 5) {
            // Safety pass so very old installations end up with the same columns as a new install
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN year INTEGER DEFAULT 1");
            } catch (Exception ignored) {}

            try {
                db.execSQL("ALTER TABLE " + TABLE_FEEDBACK + " ADD COLUMN user_id INTEGER");
                db.execSQL("UPDATE " + TABLE_FEEDBACK + " SET user_id = (SELECT user_id FROM " + TABLE_ISSUES + " WHERE " + TABLE_ISSUES + ".issue_id = " + TABLE_FEEDBACK + ".issue_id) WHERE user_id IS NULL");
            } catch (Exception ignored) {}

            try {
                db.execSQL("UPDATE " + TABLE_ISSUES + " SET assigned_staff_id = NULL WHERE assigned_staff_id = 0");
            } catch (Exception ignored) {}

            db.execSQL(notificationsTableSql());
        }

        if (oldVersion < 6) {
            // Version 2 created issue_verification with a wrong foreign key (issue_id pointed at the users table).
            rebuildVerificationTable(db);
            // Passwords used to be stored as plain text; hash them now.
            hashExistingPasswords(db);
            createIndexes(db);
        }

        if (oldVersion < 7) {
            try {
                db.execSQL("UPDATE " + TABLE_USERS + " SET email = REPLACE(email, '@resolvex.com', '@nmims.in') WHERE email LIKE '%@resolvex.com'");
            } catch (Exception ignored) {}
        }
    }

    /** SQLite cannot change a foreign key in place, so: create a correct table, copy the rows, swap the names. */
    private void rebuildVerificationTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS issue_verification_new");
        db.execSQL(verificationTableSql("issue_verification_new"));

        Cursor exists = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                new String[]{TABLE_ISSUE_VERIFICATION});
        boolean oldTableExists = exists.moveToFirst();
        exists.close();

        if (oldTableExists) {
            try {
                db.execSQL("INSERT INTO issue_verification_new " +
                        "(verification_id, issue_id, student_id, resolution_cycle, result, reason, comment, timestamp) " +
                        "SELECT verification_id, issue_id, student_id, resolution_cycle, result, reason, comment, timestamp " +
                        "FROM " + TABLE_ISSUE_VERIFICATION + " " +
                        "WHERE issue_id IN (SELECT issue_id FROM " + TABLE_ISSUES + ") " +
                        "AND student_id IN (SELECT user_id FROM " + TABLE_USERS + ")");
            } catch (Exception ignored) {
                // The old table had unexpected columns; the new one simply starts empty.
            }
            db.execSQL("DROP TABLE " + TABLE_ISSUE_VERIFICATION);
        }

        db.execSQL("ALTER TABLE issue_verification_new RENAME TO " + TABLE_ISSUE_VERIFICATION);
    }

    private void hashExistingPasswords(SQLiteDatabase db) {
        List<Integer> ids = new ArrayList<>();
        List<String> passwords = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT user_id, password FROM " + TABLE_USERS, null);
        while (cursor.moveToNext()) {
            ids.add(cursor.getInt(0));
            passwords.add(cursor.getString(1));
        }
        cursor.close();

        for (int i = 0; i < ids.size(); i++) {
            if (!PasswordUtils.isHashed(passwords.get(i))) {
                ContentValues cv = new ContentValues();
                cv.put("password", PasswordUtils.hash(passwords.get(i)));
                db.update(TABLE_USERS, cv, "user_id = ?", new String[]{String.valueOf(ids.get(i))});
            }
        }
    }

    /**
     * Demo data for a fresh install. It tells a complete story so every screen has something to show:
     * a closed issue, a resolved one waiting for the student, a reopened one, work in progress,
     * and three reports for the same classroom (the recurring-issue alert).
     */
    private void seedInitialData(SQLiteDatabase db) {
        String monthAgo = TimeUtils.daysAgo(30);

        seedUser(db, 1, "Aarav Sharma", "student@nmims.in", "student123", "STUDENT", "SOC", 2, "9876543210", monthAgo);
        seedUser(db, 2, "Rahul Verma", "staff@nmims.in", "staff123", "STAFF", "IT Support", 0, "9876543211", monthAgo);
        seedUser(db, 3, "Suresh Kumar", "elec@nmims.in", "staff123", "STAFF", "Electrical Maintenance", 0, "9876543212", monthAgo);
        seedUser(db, 4, "Amit Patel", "maint@nmims.in", "staff123", "STAFF", "General Maintenance", 0, "9876543213", monthAgo);
        seedUser(db, 5, "System Admin", "admin@nmims.in", "admin123", "ADMIN", "Administration", 0, "9876543214", monthAgo);
        seedUser(db, 6, "Priya Nair", "student2@nmims.in", "student123", "STUDENT", "STME", 3, "9876543215", monthAgo);

        // 1: work in progress
        seedIssue(db, 1, 1, "Broken projector in Lab 204", "The ceiling projector HDMI port is broken and displays flickering screen during lectures.",
                "Classroom Equipment", "High", "Block A - Room 204", "IN_PROGRESS", 2, "", 72, 48);
        seedHistory(db, 1, "", "SUBMITTED", 1, "Issue reported by student", 72);
        seedHistory(db, 1, "SUBMITTED", "ASSIGNED", 5, "Assigned to staff: Rahul Verma", 70);
        seedHistory(db, 1, "ASSIGNED", "IN_PROGRESS", 2, "Staff started work on issue", 48);

        // 2: assigned, not started
        seedIssue(db, 2, 1, "Wi-Fi not working in library", "Campus Wi-Fi drops connection repeatedly on the 2nd floor study area.",
                "Internet / Wi-Fi", "Medium", "Central Library", "ASSIGNED", 2, "", 50, 46);
        seedHistory(db, 2, "", "SUBMITTED", 1, "Issue reported by student", 50);
        seedHistory(db, 2, "SUBMITTED", "ASSIGNED", 5, "Assigned to staff: Rahul Verma", 46);

        // 3: resolved, waiting for the student to verify
        seedIssue(db, 3, 1, "Water leakage near cafeteria", "Pipe leakage causing wet floor near the main cafeteria entrance.",
                "Water / Plumbing", "High", "Cafeteria - Ground Floor", "RESOLVED", 4, "Replaced sealing washer and fixed pipe joint.", 120, 30);
        seedHistory(db, 3, "", "SUBMITTED", 1, "Issue reported by student", 120);
        seedHistory(db, 3, "SUBMITTED", "ASSIGNED", 5, "Assigned to staff: Amit Patel", 118);
        seedHistory(db, 3, "ASSIGNED", "IN_PROGRESS", 4, "Staff started work on issue", 100);
        seedHistory(db, 3, "IN_PROGRESS", "RESOLVED", 4, "Resolution submitted: Replaced sealing washer and fixed pipe joint.", 30);

        // 4: fully closed, with verification and feedback
        seedIssue(db, 4, 1, "Damaged chair in Room 301", "One of the desk chairs has a broken leg and needs replacement.",
                "Furniture", "Low", "Block B - Room 301", "CLOSED", 4, "Replaced damaged chair with new unit.", 240, 189);
        seedHistory(db, 4, "", "SUBMITTED", 1, "Issue reported by student", 240);
        seedHistory(db, 4, "SUBMITTED", "ASSIGNED", 5, "Assigned to staff: Amit Patel", 238);
        seedHistory(db, 4, "ASSIGNED", "IN_PROGRESS", 4, "Staff started work on issue", 220);
        seedHistory(db, 4, "IN_PROGRESS", "RESOLVED", 4, "Resolution submitted: Replaced damaged chair with new unit.", 200);
        seedHistory(db, 4, "RESOLVED", "VERIFIED", 1, "Student verified issue as fixed (Cycle 1)", 190);
        seedHistory(db, 4, "VERIFIED", "CLOSED", 1, "Issue closed after verification and feedback (4 stars)", 189);
        seedVerification(db, 4, 1, 1, "VERIFIED", "Issue confirmed fixed by student", "", 190);
        seedFeedback(db, 4, 1, 4, "Quick fix, thanks.", 189);

        // 5, 6, 7: new reports (6 and 7 are in the same room as issue 1, which makes it a recurring issue)
        seedIssue(db, 5, 1, "Flickering lights in hallway", "Tube light flickering continuously near Room 102.",
                "Electrical", "Low", "Block A - Ground Floor", "SUBMITTED", 0, "", 3, 3);
        seedHistory(db, 5, "", "SUBMITTED", 1, "Issue reported by student", 3);

        seedIssue(db, 6, 1, "Audio distorted on Lab 204 speakers", "Speakers crackle continuously during video presentations.",
                "Classroom Equipment", "High", "Block A - Room 204", "SUBMITTED", 0, "", 26, 26);
        seedHistory(db, 6, "", "SUBMITTED", 1, "Issue reported by student", 26);

        seedIssue(db, 7, 1, "Projector remote missing in Lab 204", "Cannot turn on projector without manual power button.",
                "Classroom Equipment", "Medium", "Block A - Room 204", "SUBMITTED", 0, "", 10, 10);
        seedHistory(db, 7, "", "SUBMITTED", 1, "Issue reported by student", 10);

        // 8: a second student's issue that the student reopened
        seedIssue(db, 8, 6, "Water cooler leaking near cafeteria counter", "The cooler drips onto the floor all day.",
                "Water / Plumbing", "Medium", "Cafeteria - Ground Floor", "REOPENED", 4, "Tightened the connection and cleaned the drain.", 100, 20);
        seedHistory(db, 8, "", "SUBMITTED", 6, "Issue reported by student", 100);
        seedHistory(db, 8, "SUBMITTED", "ASSIGNED", 5, "Assigned to staff: Amit Patel", 98);
        seedHistory(db, 8, "ASSIGNED", "IN_PROGRESS", 4, "Staff started work on issue", 90);
        seedHistory(db, 8, "IN_PROGRESS", "RESOLVED", 4, "Resolution submitted: Tightened the connection and cleaned the drain.", 60);
        seedHistory(db, 8, "RESOLVED", "REOPENED", 6, "Issue reopened by student (Cycle 1). Reason: Problem returned - Water is leaking again after two days.", 20);
        seedVerification(db, 8, 6, 1, "REOPENED", "Problem returned", "Water is leaking again after two days.", 20);

        // Notifications
        seedNotification(db, 1, 3, "Resolution Submitted - Action Required", "Staff marked issue RX-0003 as resolved. Please verify if it is fixed.", false, 30);
        seedNotification(db, 1, 4, "Issue Closed", "Your issue RX-0004 (Damaged chair in Room 301) is now closed. Thank you for your feedback!", true, 189);
        seedNotification(db, 2, 1, "New Issue Assigned", "You have been assigned to issue RX-0001: Broken projector in Lab 204", true, 70);
        seedNotification(db, 2, 2, "New Issue Assigned", "You have been assigned to issue RX-0002: Wi-Fi not working in library", false, 46);
        seedNotification(db, 4, 8, "Issue Reopened", "An issue you resolved (RX-0008) has been reopened by the student.", false, 20);
        seedNotification(db, 5, 5, "New Issue Submitted", "New issue RX-0005 (Flickering lights in hallway) reported at Block A - Ground Floor", false, 3);
        seedNotification(db, 5, 7, TITLE_RECURRING, "\u26A0 Recurring Condition: Classroom Equipment at Block A - Room 204 has 3 reports in last 30 days.", false, 10);
        seedNotification(db, 5, 8, "Issue Reopened Monitoring", "Issue RX-0008 has been reopened by student.", false, 20);
        seedNotification(db, 6, 8, "Issue Status Updated: Reopened", "Your issue RX-0008 (Water cooler leaking near cafeteria counter) has been sent back to staff.", true, 20);
    }

    private void seedUser(SQLiteDatabase db, int id, String name, String email, String password, String role,
                          String department, int year, String phone, String createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", id);
        cv.put("name", name);
        cv.put("email", email);
        cv.put("password", PasswordUtils.hash(password));
        cv.put("role", role);
        cv.put("department", department);
        cv.put("year", year);
        cv.put("phone", phone);
        cv.put("created_at", createdAt);
        db.insertOrThrow(TABLE_USERS, null, cv);
    }

    private void seedIssue(SQLiteDatabase db, int id, int userId, String title, String description, String category,
                           String priority, String location, String status, int staffId, String resolutionNotes,
                           int createdHoursAgo, int updatedHoursAgo) {
        ContentValues cv = new ContentValues();
        cv.put("issue_id", id);
        cv.put("user_id", userId);
        cv.put("title", title);
        cv.put("description", description);
        cv.put("category", category);
        cv.put("priority", priority);
        cv.put("location", location);
        cv.put("image_path", "");
        cv.put("status", status);
        if (staffId > 0) {
            cv.put("assigned_staff_id", staffId);
        } else {
            cv.putNull("assigned_staff_id");
        }
        cv.put("resolution_notes", resolutionNotes);
        cv.put("resolution_image_path", "");
        cv.put("created_at", TimeUtils.hoursAgo(createdHoursAgo));
        cv.put("updated_at", TimeUtils.hoursAgo(updatedHoursAgo));
        db.insertOrThrow(TABLE_ISSUES, null, cv);
    }

    private void seedHistory(SQLiteDatabase db, int issueId, String oldStatus, String newStatus, int updatedBy,
                             String note, int hoursAgo) {
        ContentValues cv = new ContentValues();
        cv.put("issue_id", issueId);
        cv.put("old_status", oldStatus);
        cv.put("new_status", newStatus);
        cv.put("updated_by", updatedBy);
        cv.put("note", note);
        cv.put("timestamp", TimeUtils.hoursAgo(hoursAgo));
        db.insertOrThrow(TABLE_ISSUE_HISTORY, null, cv);
    }

    private void seedVerification(SQLiteDatabase db, int issueId, int studentId, int cycle, String result,
                                  String reason, String comment, int hoursAgo) {
        ContentValues cv = new ContentValues();
        cv.put("issue_id", issueId);
        cv.put("student_id", studentId);
        cv.put("resolution_cycle", cycle);
        cv.put("result", result);
        cv.put("reason", reason);
        cv.put("comment", comment);
        cv.put("timestamp", TimeUtils.hoursAgo(hoursAgo));
        db.insertOrThrow(TABLE_ISSUE_VERIFICATION, null, cv);
    }

    private void seedFeedback(SQLiteDatabase db, int issueId, int userId, int rating, String comment, int hoursAgo) {
        ContentValues cv = new ContentValues();
        cv.put("issue_id", issueId);
        cv.put("user_id", userId);
        cv.put("rating", rating);
        cv.put("comment", comment);
        cv.put("created_at", TimeUtils.hoursAgo(hoursAgo));
        db.insertOrThrow(TABLE_FEEDBACK, null, cv);
    }

    private void seedNotification(SQLiteDatabase db, int userId, int issueId, String title, String message,
                                  boolean isRead, int hoursAgo) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("issue_id", issueId);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("is_read", isRead ? 1 : 0);
        cv.put("created_at", TimeUtils.hoursAgo(hoursAgo));
        db.insertOrThrow(TABLE_NOTIFICATIONS, null, cv);
    }

    // =====================================================================
    //  SMALL SHARED HELPERS
    // =====================================================================

    /** Adds one notification row. Call it inside the same transaction as the change it describes. */
    private void addNotification(SQLiteDatabase db, int userId, int issueId, String title, String message, String now) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("issue_id", issueId);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("is_read", 0);
        cv.put("created_at", now);
        db.insert(TABLE_NOTIFICATIONS, null, cv);
    }

    private void notifyAdmins(SQLiteDatabase db, int issueId, String title, String message, String now) {
        Cursor cursor = db.rawQuery("SELECT user_id FROM " + TABLE_USERS + " WHERE role = 'ADMIN'", null);
        List<Integer> adminIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            adminIds.add(cursor.getInt(0));
        }
        cursor.close();

        for (int adminId : adminIds) {
            addNotification(db, adminId, issueId, title, message, now);
        }
    }

    private void addHistory(SQLiteDatabase db, int issueId, String oldStatus, String newStatus, int updatedBy, String note, String now) {
        ContentValues cv = new ContentValues();
        cv.put("issue_id", issueId);
        cv.put("old_status", oldStatus);
        cv.put("new_status", newStatus);
        cv.put("updated_by", updatedBy);
        cv.put("note", note);
        cv.put("timestamp", now);
        db.insert(TABLE_ISSUE_HISTORY, null, cv);
    }

    private boolean isAdmin(int userId) {
        User user = getUserById(userId);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // =====================================================================
    //  USERS
    // =====================================================================

    public boolean insertUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("email", user.getEmail().toLowerCase(Locale.ROOT).trim());
        cv.put("password", PasswordUtils.hash(user.getPassword()));
        cv.put("role", user.getRole());
        cv.put("department", user.getDepartment());
        cv.put("year", user.getYear());
        cv.put("phone", user.getPhone());
        cv.put("created_at", TimeUtils.now());

        long result = db.insert(TABLE_USERS, null, cv);
        return result != -1;
    }

    /** Finds the user by email and role, then checks the password against the stored hash. */
    public User checkLogin(String email, String password, String role) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE LOWER(email) = ? AND role = ?",
                new String[]{email.toLowerCase(Locale.ROOT).trim(), role});

        User found = null;
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            if (PasswordUtils.verify(password, storedHash)) {
                found = cursorToUser(cursor);
            }
        }
        cursor.close();
        return found;
    }

    /** Admin only: sets a new password for any user (there is no e-mail based recovery in this project). */
    public boolean resetPassword(int targetUserId, String newPassword, int adminUserId) {
        if (!isAdmin(adminUserId)) return false;
        if (newPassword == null || newPassword.length() < 6) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hash(newPassword));
        return db.update(TABLE_USERS, cv, "user_id = ?", new String[]{String.valueOf(targetUserId)}) > 0;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE user_id = ?",
                new String[]{String.valueOf(userId)});

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    public List<User> getAllStaff() {
        return getUsersByRole("STAFF");
    }

    public List<User> getUsersByRole(String role) {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE role = ? ORDER BY name ASC",
                new String[]{role});

        while (cursor.moveToNext()) {
            list.add(cursorToUser(cursor));
        }
        cursor.close();
        return list;
    }

    public List<User> getAllUsers() {
        List<User> usersList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " ORDER BY user_id DESC", null);

        while (cursor.moveToNext()) {
            usersList.add(cursorToUser(cursor));
        }
        cursor.close();
        return usersList;
    }

    /** The password hash is deliberately not copied into the User object. */
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        user.setRole(cursor.getString(cursor.getColumnIndexOrThrow("role")));
        user.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow("department")));

        int yearIdx = cursor.getColumnIndex("year");
        if (yearIdx != -1 && !cursor.isNull(yearIdx)) {
            user.setYear(cursor.getInt(yearIdx));
        } else {
            user.setYear(1);
        }

        user.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        return user;
    }

    // =====================================================================
    //  ISSUES: CREATE
    // =====================================================================

    public long insertIssue(Issue issue) {
        SQLiteDatabase db = this.getWritableDatabase();
        String now = TimeUtils.now();
        String location = IssueOptions.normalizeSpaces(issue.getLocation());

        db.beginTransaction();
        long issueId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put("user_id", issue.getUserId());
            cv.put("title", issue.getTitle());
            cv.put("description", issue.getDescription());
            cv.put("category", issue.getCategory());
            cv.put("priority", issue.getPriority());
            cv.put("location", location);
            cv.put("image_path", issue.getImagePath() != null ? issue.getImagePath() : "");
            cv.put("status", IssueStatusManager.SUBMITTED);
            cv.putNull("assigned_staff_id");
            cv.put("resolution_notes", "");
            cv.put("resolution_image_path", "");
            cv.put("created_at", now);
            cv.put("updated_at", now);

            issueId = db.insert(TABLE_ISSUES, null, cv);

            if (issueId != -1) {
                int id = (int) issueId;
                addHistory(db, id, "", IssueStatusManager.SUBMITTED, issue.getUserId(), "Issue reported by student", now);

                // Notify admins of the new issue
                notifyAdmins(db, id, "New Issue Submitted",
                        "New issue " + IssueStatusManager.formatIssueId(id) + " (" + issue.getTitle() + ") reported at " + location, now);

                checkForRecurringIssue(db, id, issue.getCategory(), location, now);

                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }

        return issueId;
    }

    /**
     * Rule-based detection (not AI): when the same category is reported at the same location
     * RECURRING_THRESHOLD times within RECURRING_WINDOW_DAYS, admins get one alert.
     * Locations are compared ignoring capital letters and extra spaces.
     */
    private void checkForRecurringIssue(SQLiteDatabase db, int issueId, String category, String location, String now) {
        String cutoff = TimeUtils.daysAgo(RECURRING_WINDOW_DAYS);

        Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ISSUES + " WHERE category = ? AND LOWER(TRIM(location)) = LOWER(TRIM(?)) AND created_at >= ?",
                new String[]{category, location, cutoff});
        int reportCount = 0;
        if (countCursor.moveToFirst()) {
            reportCount = countCursor.getInt(0);
        }
        countCursor.close();

        if (reportCount < RECURRING_THRESHOLD) return;

        // Only alert once per window for the same category + location
        Cursor alertCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS + " n " +
                        "JOIN " + TABLE_ISSUES + " i ON n.issue_id = i.issue_id " +
                        "WHERE n.title = ? AND i.category = ? AND LOWER(TRIM(i.location)) = LOWER(TRIM(?)) AND n.created_at >= ?",
                new String[]{TITLE_RECURRING, category, location, cutoff});
        boolean alreadyAlerted = alertCursor.moveToFirst() && alertCursor.getInt(0) > 0;
        alertCursor.close();

        if (!alreadyAlerted) {
            notifyAdmins(db, issueId, TITLE_RECURRING,
                    "\u26A0 Recurring Condition: " + category + " at " + location + " has " + reportCount
                            + " reports in last " + RECURRING_WINDOW_DAYS + " days.", now);
        }
    }

    // =====================================================================
    //  ISSUES: READ (one query builder shared by the student, staff and admin lists)
    // =====================================================================

    public Issue getIssueById(int issueId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT i.*, u.name as student_name, s.name as staff_name FROM " + TABLE_ISSUES + " i " +
                "LEFT JOIN " + TABLE_USERS + " u ON i.user_id = u.user_id " +
                "LEFT JOIN " + TABLE_USERS + " s ON i.assigned_staff_id = s.user_id " +
                "WHERE i.issue_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(issueId)});
        Issue issue = null;
        if (cursor.moveToFirst()) {
            issue = cursorToIssue(cursor);
        }
        cursor.close();
        return issue;
    }

    /** Student: my own issues. */
    public List<Issue> getStudentIssues(int userId, String search, String category, String status) {
        return getStudentIssues(userId, search, category, status, null, null);
    }

    public List<Issue> getStudentIssues(int userId, String search, String category, String status,
                                        String priority, String since) {
        return queryIssues(userId, null, search, category, status, priority, since,
                "ORDER BY i.issue_id DESC");
    }

    public List<Issue> getPendingVerificationIssuesForStudent(int studentId) {
        return getStudentIssues(studentId, null, null, IssueStatusManager.RESOLVED);
    }

    /** Staff: only the issues assigned to this staff member. */
    public List<Issue> getAssignedIssues(int staffId, String search, String status) {
        return getAssignedIssues(staffId, search, status, null, null, null);
    }

    public List<Issue> getAssignedIssues(int staffId, String search, String status,
                                         String category, String priority, String since) {
        // Reopened and newly assigned work comes first in the staff queue
        return queryIssues(null, staffId, search, category, status, priority, since,
                "ORDER BY CASE WHEN i.status = 'REOPENED' THEN 1 WHEN i.status = 'ASSIGNED' THEN 2 " +
                        "WHEN i.status = 'IN_PROGRESS' THEN 3 ELSE 4 END ASC, i.issue_id DESC");
    }

    /** Admin: every issue. */
    public List<Issue> getAllIssues(String search, String category, String status, String priority) {
        return getAllIssues(search, category, status, priority, null);
    }

    public List<Issue> getAllIssues(String search, String category, String status, String priority, String since) {
        return queryIssues(null, null, search, category, status, priority, since,
                "ORDER BY i.issue_id DESC");
    }

    /**
     * Builds "SELECT ... WHERE ..." from whichever filters are set. Values are never pasted into the
     * SQL text; they are passed as ? arguments, which also protects against SQL injection.
     *
     * @param reporterId only issues reported by this user (null = any)
     * @param staffId    only issues assigned to this staff member (null = any)
     * @param since      only issues created at or after this timestamp (null = any)
     */
    private List<Issue> queryIssues(Integer reporterId, Integer staffId, String search, String category,
                                    String status, String priority, String since, String orderBy) {
        List<Issue> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT i.*, u.name as student_name, s.name as staff_name FROM ").append(TABLE_ISSUES).append(" i ")
                .append("LEFT JOIN ").append(TABLE_USERS).append(" u ON i.user_id = u.user_id ")
                .append("LEFT JOIN ").append(TABLE_USERS).append(" s ON i.assigned_staff_id = s.user_id ")
                .append("WHERE 1=1 ");

        List<String> args = new ArrayList<>();

        if (reporterId != null) {
            sb.append("AND i.user_id = ? ");
            args.add(String.valueOf(reporterId));
        }

        if (staffId != null) {
            sb.append("AND i.assigned_staff_id = ? ");
            args.add(String.valueOf(staffId));
        }

        if (search != null && !search.trim().isEmpty()) {
            String text = search.trim();
            sb.append("AND (i.title LIKE ? OR i.location LIKE ?");
            args.add("%" + text + "%");
            args.add("%" + text + "%");

            Integer idSearch = parseIssueId(text);
            if (idSearch != null) {
                sb.append(" OR i.issue_id = ?");
                args.add(String.valueOf(idSearch));
            }
            sb.append(") ");
        }

        if (!isAllOption(category)) {
            sb.append("AND i.category = ? ");
            args.add(category);
        }

        if (!isAllOption(status)) {
            sb.append("AND i.status = ? ");
            args.add(status);
        }

        if (!isAllOption(priority)) {
            sb.append("AND i.priority = ? ");
            args.add(priority);
        }

        if (since != null && !since.isEmpty()) {
            sb.append("AND i.created_at >= ? ");
            args.add(since);
        }

        sb.append(orderBy);

        Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            list.add(cursorToIssue(cursor));
        }
        cursor.close();
        return list;
    }

    private boolean isAllOption(String value) {
        return value == null || value.isEmpty() || value.equals("All")
                || value.equals("All Status") || value.equals("All Categories");
    }

    /** Lets people search by id: "RX-0007", "rx-7" or "7". */
    private Integer parseIssueId(String text) {
        String digits = text.toUpperCase(Locale.ROOT).startsWith("RX-") ? text.substring(3) : text;
        if (digits.matches("\\d{1,9}")) {
            return Integer.parseInt(digits);
        }
        return null;
    }

    private Issue cursorToIssue(Cursor cursor) {
        Issue issue = new Issue();
        issue.setIssueId(cursor.getInt(cursor.getColumnIndexOrThrow("issue_id")));
        issue.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
        issue.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        issue.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        issue.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
        issue.setPriority(cursor.getString(cursor.getColumnIndexOrThrow("priority")));
        issue.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
        issue.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow("image_path")));
        issue.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));

        int staffIdx = cursor.getColumnIndex("assigned_staff_id");
        if (staffIdx != -1 && !cursor.isNull(staffIdx)) {
            issue.setAssignedStaffId(cursor.getInt(staffIdx));
        } else {
            issue.setAssignedStaffId(0);
        }

        issue.setResolutionNotes(cursor.getString(cursor.getColumnIndexOrThrow("resolution_notes")));
        issue.setResolutionImagePath(cursor.getString(cursor.getColumnIndexOrThrow("resolution_image_path")));
        issue.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        issue.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow("updated_at")));

        int studentNameIdx = cursor.getColumnIndex("student_name");
        if (studentNameIdx != -1) {
            issue.setStudentName(cursor.getString(studentNameIdx));
        }

        int staffNameIdx = cursor.getColumnIndex("staff_name");
        if (staffNameIdx != -1) {
            issue.setAssignedStaffName(cursor.getString(staffNameIdx));
        }

        return issue;
    }

    // =====================================================================
    //  RECURRING ISSUES AND ANALYTICS (admin)
    // =====================================================================

    public List<RecurringIssueGroup> getRecurringIssues() {
        List<RecurringIssueGroup> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT category, MIN(location) AS location, COUNT(*) AS report_count, " +
                "MIN(created_at) AS earliest_report, MAX(created_at) AS latest_report " +
                "FROM " + TABLE_ISSUES + " " +
                "WHERE created_at >= ? " +
                "GROUP BY category, LOWER(TRIM(location)) " +
                // The threshold is a constant written into the SQL on purpose: rawQuery sends ? values as text,
                // and SQLite treats a number as smaller than any text, so "COUNT(*) >= ?" would never match.
                "HAVING COUNT(*) >= " + RECURRING_THRESHOLD + " " +
                "ORDER BY report_count DESC";

        Cursor cursor = db.rawQuery(query, new String[]{TimeUtils.daysAgo(RECURRING_WINDOW_DAYS)});
        while (cursor.moveToNext()) {
            RecurringIssueGroup group = new RecurringIssueGroup();
            group.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category")));
            group.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
            group.setReportCount(cursor.getInt(cursor.getColumnIndexOrThrow("report_count")));
            group.setEarliestReport(cursor.getString(cursor.getColumnIndexOrThrow("earliest_report")));
            group.setLatestReport(cursor.getString(cursor.getColumnIndexOrThrow("latest_report")));
            list.add(group);
        }
        cursor.close();
        return list;
    }

    public List<Issue> getRecurringIssueReports(String category, String location) {
        List<Issue> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT i.*, u.name as student_name, s.name as staff_name FROM " + TABLE_ISSUES + " i " +
                "LEFT JOIN " + TABLE_USERS + " u ON i.user_id = u.user_id " +
                "LEFT JOIN " + TABLE_USERS + " s ON i.assigned_staff_id = s.user_id " +
                "WHERE i.category = ? AND LOWER(TRIM(i.location)) = LOWER(TRIM(?)) AND i.created_at >= ? " +
                "ORDER BY i.issue_id DESC";

        Cursor cursor = db.rawQuery(query, new String[]{category, location, TimeUtils.daysAgo(RECURRING_WINDOW_DAYS)});
        while (cursor.moveToNext()) {
            list.add(cursorToIssue(cursor));
        }
        cursor.close();
        return list;
    }

    public ResolutionAnalytics getResolutionAnalytics() {
        ResolutionAnalytics analytics = new ResolutionAnalytics();
        SQLiteDatabase db = this.getReadableDatabase();

        int totalReachedResolved = countDistinctIssuesWithStatus(db, "new_status = 'RESOLVED'");
        int totalVerified = countDistinctIssuesWithStatus(db, "new_status IN ('VERIFIED', 'CLOSED')");
        int totalReopened = countDistinctIssuesWithStatus(db, "new_status = 'REOPENED'");

        double verificationRate = 0.0;
        double reopenRate = 0.0;
        if (totalReachedResolved > 0) {
            verificationRate = Math.min(100.0, Math.max(0.0, ((double) totalVerified / totalReachedResolved) * 100.0));
            reopenRate = Math.min(100.0, Math.max(0.0, ((double) totalReopened / totalReachedResolved) * 100.0));
        }

        double avgResolutionTimeHours = 0.0;
        String timeQuery = "SELECT AVG((julianday(h.first_resolved) - julianday(i.created_at)) * 24.0) " +
                "FROM " + TABLE_ISSUES + " i " +
                "JOIN (SELECT issue_id, MIN(timestamp) as first_resolved FROM " + TABLE_ISSUE_HISTORY + " WHERE new_status = 'RESOLVED' GROUP BY issue_id) h " +
                "ON i.issue_id = h.issue_id";

        Cursor cTime = db.rawQuery(timeQuery, null);
        if (cTime.moveToFirst() && !cTime.isNull(0)) {
            avgResolutionTimeHours = Math.max(0.0, cTime.getDouble(0));
        }
        cTime.close();

        analytics.setTotalResolved(totalReachedResolved);
        analytics.setTotalVerified(totalVerified);
        analytics.setTotalReopened(totalReopened);
        analytics.setVerificationRate(verificationRate);
        analytics.setReopenRate(reopenRate);
        analytics.setAvgResolutionTimeHours(avgResolutionTimeHours);

        return analytics;
    }

    private int countDistinctIssuesWithStatus(SQLiteDatabase db, String condition) {
        Cursor cursor = db.rawQuery("SELECT COUNT(DISTINCT issue_id) FROM " + TABLE_ISSUE_HISTORY + " WHERE " + condition, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // =====================================================================
    //  VERIFICATION LOOKUP AND NOTIFICATIONS
    // =====================================================================

    public IssueVerification getLatestVerificationForIssue(int issueId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ISSUE_VERIFICATION + " WHERE issue_id = ? ORDER BY verification_id DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(issueId)});

        IssueVerification v = null;
        if (cursor.moveToFirst()) {
            v = new IssueVerification();
            v.setVerificationId(cursor.getInt(cursor.getColumnIndexOrThrow("verification_id")));
            v.setIssueId(cursor.getInt(cursor.getColumnIndexOrThrow("issue_id")));
            v.setStudentId(cursor.getInt(cursor.getColumnIndexOrThrow("student_id")));
            v.setResolutionCycle(cursor.getInt(cursor.getColumnIndexOrThrow("resolution_cycle")));
            v.setResult(cursor.getString(cursor.getColumnIndexOrThrow("result")));
            v.setReason(cursor.getString(cursor.getColumnIndexOrThrow("reason")));
            v.setComment(cursor.getString(cursor.getColumnIndexOrThrow("comment")));
            v.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
        }
        cursor.close();
        return v;
    }

    public int getUnreadNotificationCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS + " WHERE user_id = ? AND is_read = 0",
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void markAllNotificationsRead(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, cv, "user_id = ?", new String[]{String.valueOf(userId)});
    }

    /** A user can only mark their own notifications as read. */
    public void markNotificationRead(int notificationId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, cv, "notification_id = ? AND user_id = ?",
                new String[]{String.valueOf(notificationId), String.valueOf(userId)});
    }

    public List<AppNotification> getRecentSystemActivityNotifications(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTIFICATIONS + " ORDER BY notification_id DESC LIMIT ?",
                new String[]{String.valueOf(limit)});
        return readNotifications(cursor);
    }

    public List<AppNotification> getUserNotifications(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTIFICATIONS + " WHERE user_id = ? ORDER BY notification_id DESC",
                new String[]{String.valueOf(userId)});
        return readNotifications(cursor);
    }

    private List<AppNotification> readNotifications(Cursor cursor) {
        List<AppNotification> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            AppNotification n = new AppNotification();
            n.setNotificationId(cursor.getInt(cursor.getColumnIndexOrThrow("notification_id")));
            n.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
            n.setIssueId(cursor.getInt(cursor.getColumnIndexOrThrow("issue_id")));
            n.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            n.setMessage(cursor.getString(cursor.getColumnIndexOrThrow("message")));
            n.setRead(cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1);
            n.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
            list.add(n);
        }
        cursor.close();
        return list;
    }

    // =====================================================================
    //  STATE MACHINE UPDATES
    //  Every change is one transaction that updates the issue, writes the history row and
    //  creates the notification together, so they can never disagree. Each method first checks
    //  WHO is allowed to do it and whether the status transition is valid.
    // =====================================================================

    public boolean assignIssue(int issueId, int staffId, int adminUserId, String staffName) {
        if (!isAdmin(adminUserId)) return false;

        User staff = getUserById(staffId);
        if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRole())) return false;

        Issue current = getIssueById(issueId);
        if (current == null) return false;

        String oldStatus = current.getStatus();
        String newStatus = IssueStatusManager.ASSIGNED;

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        String now = TimeUtils.now();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("assigned_staff_id", staffId);
            cv.put("status", newStatus);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                addHistory(db, issueId, oldStatus, newStatus, adminUserId, "Assigned to staff: " + staffName, now);
                addNotification(db, staffId, issueId, "New Issue Assigned",
                        "You have been assigned to issue " + IssueStatusManager.formatIssueId(issueId) + ": " + current.getTitle(), now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    /**
     * Admin only: hand an issue that is already assigned to a different staff member.
     * The status does not change, so the history gets a REASSIGNED marker row and both staff members are told.
     */
    public boolean reassignIssue(int issueId, int newStaffId, int adminUserId) {
        if (!isAdmin(adminUserId)) return false;

        User newStaff = getUserById(newStaffId);
        if (newStaff == null || !"STAFF".equalsIgnoreCase(newStaff.getRole())) return false;

        Issue current = getIssueById(issueId);
        if (current == null) return false;

        String status = current.getStatus();
        boolean canReassign = IssueStatusManager.ASSIGNED.equals(status)
                || IssueStatusManager.IN_PROGRESS.equals(status)
                || IssueStatusManager.REOPENED.equals(status);
        if (!canReassign || current.getAssignedStaffId() <= 0 || current.getAssignedStaffId() == newStaffId) {
            return false;
        }

        int oldStaffId = current.getAssignedStaffId();
        User oldStaff = getUserById(oldStaffId);
        String oldName = oldStaff != null ? oldStaff.getName() : "staff #" + oldStaffId;

        String now = TimeUtils.now();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("assigned_staff_id", newStaffId);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                addHistory(db, issueId, status, IssueStatusManager.REASSIGNED, adminUserId,
                        "Reassigned from " + oldName + " to " + newStaff.getName(), now);

                String id = IssueStatusManager.formatIssueId(issueId);
                addNotification(db, newStaffId, issueId, "New Issue Assigned",
                        "You have been assigned to issue " + id + ": " + current.getTitle(), now);
                addNotification(db, oldStaffId, issueId, "Issue Reassigned",
                        "Issue " + id + " was reassigned to " + newStaff.getName() + ".", now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    /** Admin only: change an issue's priority. It is written to the history and the student and staff are told. */
    public boolean updateIssuePriority(int issueId, String newPriority, int adminUserId) {
        if (!isAdmin(adminUserId)) return false;
        if (!IssueOptions.isValidPriority(newPriority)) return false;

        Issue current = getIssueById(issueId);
        if (current == null) return false;
        if (IssueStatusManager.CLOSED.equals(current.getStatus())) return false;
        if (newPriority.equalsIgnoreCase(current.getPriority())) return false;

        String now = TimeUtils.now();
        String note = "Priority changed from " + current.getPriority() + " to " + newPriority;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("priority", newPriority);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                addHistory(db, issueId, current.getStatus(), IssueStatusManager.PRIORITY_CHANGED, adminUserId, note, now);

                String message = "Issue " + IssueStatusManager.formatIssueId(issueId) + " (" + current.getTitle() + "): " + note + ".";
                addNotification(db, current.getUserId(), issueId, "Issue Priority Updated", message, now);
                if (current.getAssignedStaffId() > 0) {
                    addNotification(db, current.getAssignedStaffId(), issueId, "Issue Priority Updated", message, now);
                }

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public boolean updateIssueStatus(int issueId, String newStatus, int updatedByUserId, String note) {
        Issue current = getIssueById(issueId);
        if (current == null) return false;

        User updater = getUserById(updatedByUserId);
        if (updater == null) return false;

        // Staff authorization check: must be assigned to this issue
        if ("STAFF".equalsIgnoreCase(updater.getRole()) && current.getAssignedStaffId() != updatedByUserId) {
            return false;
        }

        String oldStatus = current.getStatus();

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        String now = TimeUtils.now();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                addHistory(db, issueId, oldStatus, newStatus, updatedByUserId,
                        note != null ? note : "Status updated to " + IssueStatusManager.getHumanReadableStatus(newStatus), now);

                String readable = IssueStatusManager.getHumanReadableStatus(newStatus);
                addNotification(db, current.getUserId(), issueId, "Issue Status Updated: " + readable,
                        "Your issue " + IssueStatusManager.formatIssueId(issueId) + " (" + current.getTitle() + ") is now " + readable, now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        return success;
    }

    public boolean addResolution(int issueId, String resolutionNotes, String resolutionImagePath, int staffUserId) {
        Issue current = getIssueById(issueId);
        if (current == null) return false;

        // Authorization check: Staff must be assigned to this issue
        if (current.getAssignedStaffId() != staffUserId) {
            return false;
        }

        String oldStatus = current.getStatus();
        String newStatus = IssueStatusManager.RESOLVED;

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        String now = TimeUtils.now();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;

        try {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("resolution_notes", resolutionNotes);
            // A new resolution replaces the old photo (an empty path clears it)
            cv.put("resolution_image_path", resolutionImagePath != null ? resolutionImagePath : "");
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                addHistory(db, issueId, oldStatus, newStatus, staffUserId, "Resolution submitted: " + resolutionNotes, now);
                addNotification(db, current.getUserId(), issueId, "Resolution Submitted - Action Required",
                        "Staff marked issue " + IssueStatusManager.formatIssueId(issueId) + " as resolved. Please verify if it is fixed.", now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        return success;
    }

    /** How many times this issue has been marked resolved. Each resolution can be verified or reopened once. */
    public int getCurrentResolutionCycle(int issueId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ISSUE_HISTORY + " WHERE issue_id = ? AND new_status = ?",
                new String[]{String.valueOf(issueId), IssueStatusManager.RESOLVED});
        int cycle = 1;
        if (cursor.moveToFirst()) {
            cycle = cursor.getInt(0);
        }
        cursor.close();
        return Math.max(cycle, 1);
    }

    private boolean hasVerificationForCycle(SQLiteDatabase db, int issueId, int cycle) {
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ISSUE_VERIFICATION + " WHERE issue_id = ? AND resolution_cycle = ?",
                new String[]{String.valueOf(issueId), String.valueOf(cycle)});
        boolean exists = cursor.moveToFirst() && cursor.getInt(0) > 0;
        cursor.close();
        return exists;
    }

    public boolean verifyIssue(int issueId, int studentId) {
        Issue current = getIssueById(issueId);
        if (current == null) return false;

        if (current.getUserId() != studentId) return false;

        String oldStatus = current.getStatus();
        String newStatus = IssueStatusManager.VERIFIED;

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        int currentCycle = getCurrentResolutionCycle(issueId);
        SQLiteDatabase db = this.getWritableDatabase();

        if (hasVerificationForCycle(db, issueId, currentCycle)) {
            return false;
        }

        String now = TimeUtils.now();
        db.beginTransaction();
        boolean success = false;

        try {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                ContentValues vCv = new ContentValues();
                vCv.put("issue_id", issueId);
                vCv.put("student_id", studentId);
                vCv.put("resolution_cycle", currentCycle);
                vCv.put("result", "VERIFIED");
                vCv.put("reason", "Issue confirmed fixed by student");
                vCv.put("comment", "");
                vCv.put("timestamp", now);
                db.insert(TABLE_ISSUE_VERIFICATION, null, vCv);

                addHistory(db, issueId, oldStatus, newStatus, studentId,
                        "Student verified issue as fixed (Cycle " + currentCycle + ")", now);

                String id = IssueStatusManager.formatIssueId(issueId);
                if (current.getAssignedStaffId() > 0) {
                    addNotification(db, current.getAssignedStaffId(), issueId, "Resolution Verified",
                            "Student verified fix for issue " + id, now);
                }
                notifyAdmins(db, issueId, "Resolution Verified",
                        "Student verified the fix for issue " + id + " (" + current.getTitle() + ").", now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        return success;
    }

    public boolean reopenIssue(int issueId, int studentId, String reason, String comment) {
        Issue current = getIssueById(issueId);
        if (current == null) return false;

        if (current.getUserId() != studentId) return false;

        String oldStatus = current.getStatus();
        String newStatus = IssueStatusManager.REOPENED;

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        int currentCycle = getCurrentResolutionCycle(issueId);
        SQLiteDatabase db = this.getWritableDatabase();

        if (hasVerificationForCycle(db, issueId, currentCycle)) {
            return false;
        }

        String now = TimeUtils.now();
        db.beginTransaction();
        boolean success = false;

        try {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                ContentValues vCv = new ContentValues();
                vCv.put("issue_id", issueId);
                vCv.put("student_id", studentId);
                vCv.put("resolution_cycle", currentCycle);
                vCv.put("result", "REOPENED");
                vCv.put("reason", reason);
                vCv.put("comment", comment);
                vCv.put("timestamp", now);
                db.insert(TABLE_ISSUE_VERIFICATION, null, vCv);

                addHistory(db, issueId, oldStatus, newStatus, studentId,
                        "Issue reopened by student (Cycle " + currentCycle + "). Reason: " + reason
                                + (comment != null && !comment.isEmpty() ? " - " + comment : ""), now);

                String id = IssueStatusManager.formatIssueId(issueId);
                if (current.getAssignedStaffId() > 0) {
                    addNotification(db, current.getAssignedStaffId(), issueId, "Issue Reopened",
                            "An issue you resolved (" + id + ") has been reopened by the student.", now);
                }
                notifyAdmins(db, issueId, "Issue Reopened Monitoring",
                        "Issue " + id + " has been reopened by student.", now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        return success;
    }

    public boolean closeIssueAfterFeedback(int issueId, int studentId, int rating, String comment) {
        if (rating < 1 || rating > 5) return false;

        Issue current = getIssueById(issueId);
        if (current == null) return false;

        if (current.getUserId() != studentId) return false;

        String oldStatus = current.getStatus();
        String newStatus = IssueStatusManager.CLOSED;

        if (!IssueStatusManager.isValidTransition(oldStatus, newStatus)) {
            return false;
        }

        String now = TimeUtils.now();
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;

        try {
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            cv.put("updated_at", now);

            int rows = db.update(TABLE_ISSUES, cv, "issue_id = ?", new String[]{String.valueOf(issueId)});
            if (rows > 0) {
                ContentValues fCv = new ContentValues();
                fCv.put("issue_id", issueId);
                fCv.put("user_id", studentId);
                fCv.put("rating", rating);
                fCv.put("comment", comment);
                fCv.put("created_at", now);
                db.insert(TABLE_FEEDBACK, null, fCv);

                addHistory(db, issueId, oldStatus, newStatus, studentId,
                        "Issue closed after verification and feedback (" + rating + " stars)", now);

                addNotification(db, studentId, issueId, "Issue Closed",
                        "Your issue " + IssueStatusManager.formatIssueId(issueId) + " (" + current.getTitle() + ") is now closed. Thank you for your feedback!", now);

                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        return success;
    }

    // =====================================================================
    //  HISTORY AND FEEDBACK
    // =====================================================================

    public List<IssueHistory> getIssueHistory(int issueId) {
        List<IssueHistory> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h.*, u.name as updater_name FROM " + TABLE_ISSUE_HISTORY + " h " +
                "LEFT JOIN " + TABLE_USERS + " u ON h.updated_by = u.user_id " +
                "WHERE h.issue_id = ? ORDER BY h.history_id ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(issueId)});
        while (cursor.moveToNext()) {
            IssueHistory history = new IssueHistory();
            history.setHistoryId(cursor.getInt(cursor.getColumnIndexOrThrow("history_id")));
            history.setIssueId(cursor.getInt(cursor.getColumnIndexOrThrow("issue_id")));
            history.setOldStatus(cursor.getString(cursor.getColumnIndexOrThrow("old_status")));
            history.setNewStatus(cursor.getString(cursor.getColumnIndexOrThrow("new_status")));
            history.setUpdatedBy(cursor.getInt(cursor.getColumnIndexOrThrow("updated_by")));
            history.setNote(cursor.getString(cursor.getColumnIndexOrThrow("note")));
            history.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));

            int nameIdx = cursor.getColumnIndex("updater_name");
            if (nameIdx != -1) {
                history.setUpdatedByName(cursor.getString(nameIdx));
            }
            list.add(history);
        }
        cursor.close();
        return list;
    }

    public boolean insertFeedback(Feedback feedback) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("issue_id", feedback.getIssueId());
        cv.put("user_id", feedback.getUserId());
        cv.put("rating", feedback.getRating());
        cv.put("comment", feedback.getComment());
        cv.put("created_at", TimeUtils.now());

        long res = db.insert(TABLE_FEEDBACK, null, cv);
        return res != -1;
    }

    public List<Feedback> getAllFeedback() {
        List<Feedback> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT f.*, i.title as issue_title FROM " + TABLE_FEEDBACK + " f " +
                "LEFT JOIN " + TABLE_ISSUES + " i ON f.issue_id = i.issue_id " +
                "ORDER BY f.feedback_id DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            Feedback f = new Feedback();
            f.setFeedbackId(cursor.getInt(cursor.getColumnIndexOrThrow("feedback_id")));
            f.setIssueId(cursor.getInt(cursor.getColumnIndexOrThrow("issue_id")));

            int userIdx = cursor.getColumnIndex("user_id");
            if (userIdx != -1) {
                f.setUserId(cursor.getInt(userIdx));
            }

            f.setRating(cursor.getInt(cursor.getColumnIndexOrThrow("rating")));
            f.setComment(cursor.getString(cursor.getColumnIndexOrThrow("comment")));
            f.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));

            int titleIdx = cursor.getColumnIndex("issue_title");
            if (titleIdx != -1) {
                f.setIssueTitle(cursor.getString(titleIdx));
            }
            list.add(f);
        }
        cursor.close();
        return list;
    }

    // =====================================================================
    //  DASHBOARD COUNTS
    // =====================================================================

    public int[] getStudentCounts(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = 0, pending = 0, inProgress = 0, resolved = 0;

        Cursor cursor = db.rawQuery("SELECT status, COUNT(*) as cnt FROM " + TABLE_ISSUES + " WHERE user_id = ? GROUP BY status",
                new String[]{String.valueOf(studentId)});

        while (cursor.moveToNext()) {
            String st = cursor.getString(0);
            int count = cursor.getInt(1);
            total += count;

            if (st.equals("SUBMITTED") || st.equals("ASSIGNED")) {
                pending += count;
            } else if (st.equals("IN_PROGRESS") || st.equals("REOPENED")) {
                inProgress += count;
            } else if (st.equals("RESOLVED") || st.equals("VERIFIED") || st.equals("CLOSED")) {
                resolved += count;
            }
        }
        cursor.close();
        return new int[]{total, pending, inProgress, resolved};
    }

    public int[] getStaffCounts(int staffId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int assigned = 0, inProgress = 0, resolved = 0;

        Cursor cursor = db.rawQuery("SELECT status, COUNT(*) as cnt FROM " + TABLE_ISSUES + " WHERE assigned_staff_id = ? GROUP BY status",
                new String[]{String.valueOf(staffId)});

        while (cursor.moveToNext()) {
            String st = cursor.getString(0);
            int count = cursor.getInt(1);

            if (st.equals("ASSIGNED")) {
                assigned += count;
            } else if (st.equals("IN_PROGRESS") || st.equals("REOPENED")) {
                inProgress += count;
            } else if (st.equals("RESOLVED") || st.equals("VERIFIED") || st.equals("CLOSED")) {
                resolved += count;
            }
        }
        cursor.close();
        return new int[]{assigned, inProgress, resolved};
    }

    /** Issues still waiting for an admin to assign them. */
    public int getUnassignedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ISSUES + " WHERE status = ?",
                new String[]{IssueStatusManager.SUBMITTED});
        int count = cursor.moveToFirst() ? cursor.getInt(0) : 0;
        cursor.close();
        return count;
    }

    /** New issues per week for the last `weeks` weeks, oldest first (the last number is the current week). */
    public int[] getWeeklyIssueCounts(int weeks) {
        int[] counts = new int[weeks];
        SQLiteDatabase db = this.getReadableDatabase();

        for (int i = 0; i < weeks; i++) {
            int weeksBack = weeks - i;
            String start = TimeUtils.daysAgo(weeksBack * 7);

            Cursor cursor;
            if (i == weeks - 1) {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ISSUES + " WHERE created_at >= ?",
                        new String[]{start});
            } else {
                String end = TimeUtils.daysAgo((weeksBack - 1) * 7);
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ISSUES + " WHERE created_at >= ? AND created_at < ?",
                        new String[]{start, end});
            }
            counts[i] = cursor.moveToFirst() ? cursor.getInt(0) : 0;
            cursor.close();
        }
        return counts;
    }

    /** Number of issues per category, biggest first. */
    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT category, COUNT(*) AS c FROM " + TABLE_ISSUES
                + " GROUP BY category ORDER BY c DESC, category ASC", null);
        while (cursor.moveToNext()) {
            result.put(cursor.getString(0), cursor.getInt(1));
        }
        cursor.close();
        return result;
    }

    /** Open work per staff member (assigned, in progress or reopened), keyed by the staff user id. */
    public Map<Integer, Integer> getStaffWorkloads() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT assigned_staff_id, COUNT(*) FROM " + TABLE_ISSUES
                + " WHERE assigned_staff_id IS NOT NULL AND status IN ('ASSIGNED', 'IN_PROGRESS', 'REOPENED')"
                + " GROUP BY assigned_staff_id", null);
        while (cursor.moveToNext()) {
            result.put(cursor.getInt(0), cursor.getInt(1));
        }
        cursor.close();
        return result;
    }

    public int[] getAdminCounts() {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = 0, pending = 0, inProgress = 0, resolved = 0, closed = 0;

        Cursor cursor = db.rawQuery("SELECT status, COUNT(*) as cnt FROM " + TABLE_ISSUES + " GROUP BY status", null);

        while (cursor.moveToNext()) {
            String st = cursor.getString(0);
            int count = cursor.getInt(1);
            total += count;

            if (st.equals("SUBMITTED") || st.equals("ASSIGNED")) {
                pending += count;
            } else if (st.equals("IN_PROGRESS") || st.equals("REOPENED")) {
                inProgress += count;
            } else if (st.equals("RESOLVED") || st.equals("VERIFIED")) {
                resolved += count;
            } else if (st.equals("CLOSED")) {
                closed += count;
            }
        }
        cursor.close();
        return new int[]{total, pending, inProgress, resolved, closed};
    }
}
