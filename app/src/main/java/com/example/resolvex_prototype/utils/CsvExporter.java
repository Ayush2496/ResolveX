package com.example.resolvex_prototype.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.resolvex_prototype.models.Issue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class CsvExporter {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void exportAndShareIssues(Context context, List<Issue> issues) {
        if (issues == null || issues.isEmpty()) {
            Toast.makeText(context, "No issue records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        // A byte-order mark at the start tells Excel the file is UTF-8
        sb.append('\uFEFF');
        sb.append("Issue ID,Title,Description,Category,Priority,Location,Status,Reporter,Assigned Staff,Resolution Notes,Created At,Updated At\n");

        for (Issue issue : issues) {
            sb.append(escapeCsv(IssueStatusManager.formatIssueId(issue.getIssueId()))).append(",");
            sb.append(escapeCsv(issue.getTitle())).append(",");
            sb.append(escapeCsv(issue.getDescription())).append(",");
            sb.append(escapeCsv(issue.getCategory())).append(",");
            sb.append(escapeCsv(issue.getPriority())).append(",");
            sb.append(escapeCsv(issue.getLocation())).append(",");
            sb.append(escapeCsv(IssueStatusManager.getHumanReadableStatus(issue.getStatus()))).append(",");
            sb.append(escapeCsv(issue.getStudentName() != null ? issue.getStudentName() : "Student #" + issue.getUserId())).append(",");
            sb.append(escapeCsv(issue.getAssignedStaffName() != null ? issue.getAssignedStaffName() : (issue.getAssignedStaffId() > 0 ? "Staff #" + issue.getAssignedStaffId() : "Unassigned"))).append(",");
            sb.append(escapeCsv(issue.getResolutionNotes())).append(",");
            sb.append(escapeCsv(issue.getCreatedAt())).append(",");
            sb.append(escapeCsv(issue.getUpdatedAt())).append("\n");
        }

        try {
            File exportDir = new File(context.getCacheDir(), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File csvFile = new File(exportDir, "ResolveX_Issues_Export.csv");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8")) {
                writer.write(sb.toString());
                writer.flush();
            }

            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "ResolveX Campus Issues Export");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Export Issues CSV"));

        } catch (IOException e) {
            Toast.makeText(context, "Failed to generate CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Wraps a value in quotes for CSV. Text typed by students ends up in this file, and a cell that
     * starts with = + - or @ is treated by Excel as a formula. A leading apostrophe makes it plain text.
     */
    private static String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\"", "\"\"");
        if (!clean.isEmpty()) {
            char first = clean.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                clean = "'" + clean;
            }
        }
        return "\"" + clean + "\"";
    }
}
