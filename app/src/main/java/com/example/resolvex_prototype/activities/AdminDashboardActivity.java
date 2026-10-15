package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.RecurringIssueAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.AppNotification;
import com.example.resolvex_prototype.models.Feedback;
import com.example.resolvex_prototype.models.RecurringIssueGroup;
import com.example.resolvex_prototype.models.ResolutionAnalytics;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.NotificationHelper;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.TimeUtils;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Admin overview: key numbers, a weekly chart, category shares, feedback, recurring issues and recent activity. */
public class AdminDashboardActivity extends AppCompatActivity {

    private static final int WEEKS = 8;
    private static final int[] CATEGORY_COLORS = {
            R.color.rx_accent, R.color.rx_dot_purple, R.color.rx_dot_amber, R.color.rx_dot_green, R.color.rx_dot_pink};

    private View statOpen, statUnassigned, statAvgRes, statReopenRate;
    private LinearLayout llWeekChart, llCategories, llRecurringSection, llActivity;
    private RecyclerView rvRecurringIssues;
    private TextView tvFeedbackSummary;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            startActivity(new Intent(AdminDashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);

        statOpen = findViewById(R.id.statOpen);
        statUnassigned = findViewById(R.id.statUnassigned);
        statAvgRes = findViewById(R.id.statAvgRes);
        statReopenRate = findViewById(R.id.statReopenRate);
        llWeekChart = findViewById(R.id.llWeekChart);
        llCategories = findViewById(R.id.llCategories);
        llRecurringSection = findViewById(R.id.llRecurringSection);
        llActivity = findViewById(R.id.llActivity);
        rvRecurringIssues = findViewById(R.id.rvRecurringIssues);
        tvFeedbackSummary = findViewById(R.id.tvFeedbackSummary);

        rvRecurringIssues.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.cardFeedback).setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, FeedbackListActivity.class)));

        BottomBar.attach(this, "ADMIN");
        UiUtils.applySystemBarSpace(this);
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            loadAdminData();
        }
    }

    private void loadAdminData() {
        int userId = sessionManager.getUserId();
        int unread = dbHelper.getUnreadNotificationCount(userId);
        UiUtils.bindHeader(this, "Campus administration", "Overview", unread);
        NotificationHelper.showUnreadSummaryIfNew(this, userId, unread);

        loadStats();
        loadWeeklyChart();
        loadCategories();
        loadFeedbackSummary();
        loadRecurring();
        loadActivity();
    }

    private void loadStats() {
        int[] counts = dbHelper.getAdminCounts(); // total, pending, in progress, resolved, closed
        ResolutionAnalytics analytics = dbHelper.getResolutionAnalytics();

        UiUtils.bindStat(statOpen, R.color.rx_dot_purple, String.valueOf(counts[0] - counts[4]), "Open issues");
        UiUtils.bindStat(statUnassigned, R.color.rx_accent, String.valueOf(dbHelper.getUnassignedCount()), "Unassigned");

        double hours = analytics.getAvgResolutionTimeHours();
        String avg;
        if (analytics.getTotalResolved() == 0) {
            avg = "-";
        } else if (hours >= 24) {
            avg = String.format(Locale.US, "%.1fd", hours / 24.0);
        } else {
            avg = String.format(Locale.US, "%.0fh", hours);
        }
        UiUtils.bindStat(statAvgRes, R.color.rx_dot_green, avg, "Avg. resolution");
        UiUtils.bindStat(statReopenRate, R.color.rx_dot_pink, String.format(Locale.US, "%.0f%%", analytics.getReopenRate()), "Reopen rate");
    }

    /** One bar per week, the current week in orange. Heights are scaled to the busiest week. */
    private void loadWeeklyChart() {
        int[] weeks = dbHelper.getWeeklyIssueCounts(WEEKS);
        int max = 1;
        for (int w : weeks) max = Math.max(max, w);

        llWeekChart.removeAllViews();
        for (int i = 0; i < weeks.length; i++) {
            View bar = new View(this);
            int height = UiUtils.dp(this, weeks[i] == 0 ? 4 : 10 + 86f * weeks[i] / max);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
            lp.setMargins(UiUtils.dp(this, 3), 0, UiUtils.dp(this, 3), 0);
            bar.setLayoutParams(lp);
            UiUtils.rounded(bar, i == weeks.length - 1 ? R.color.rx_accent : R.color.rx_segment_off, 5);
            bar.setContentDescription(weeks[i] + " new issues");
            llWeekChart.addView(bar);
        }
    }

    private void loadCategories() {
        Map<String, Integer> counts = dbHelper.getCategoryCounts();
        int total = 0;
        for (int c : counts.values()) total += c;

        llCategories.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (index >= CATEGORY_COLORS.length) break; // top 5 only
            int percent = total == 0 ? 0 : Math.round(entry.getValue() * 100f / total);

            View row = inflater.inflate(R.layout.item_category_bar, llCategories, false);
            ((TextView) row.findViewById(R.id.tvCatName)).setText(entry.getKey());
            ((TextView) row.findViewById(R.id.tvCatPercent)).setText(percent + "%");

            View fill = row.findViewById(R.id.viewCatFill);
            View rest = row.findViewById(R.id.viewCatRest);
            ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = Math.max(percent, 1);
            ((LinearLayout.LayoutParams) rest.getLayoutParams()).weight = Math.max(100 - percent, 0);
            UiUtils.rounded(fill, CATEGORY_COLORS[index], 3);

            llCategories.addView(row);
            index++;
        }
    }

    private void loadFeedbackSummary() {
        List<Feedback> feedback = dbHelper.getAllFeedback();
        if (feedback.isEmpty()) {
            tvFeedbackSummary.setText("No feedback yet");
            return;
        }
        double sum = 0;
        for (Feedback f : feedback) sum += f.getRating();
        tvFeedbackSummary.setText(String.format(Locale.US, "Average %.1f of 5 from %d review%s",
                sum / feedback.size(), feedback.size(), feedback.size() == 1 ? "" : "s"));
    }

    private void loadRecurring() {
        List<RecurringIssueGroup> recurring = dbHelper.getRecurringIssues();
        if (recurring.isEmpty()) {
            llRecurringSection.setVisibility(View.GONE);
            return;
        }
        rvRecurringIssues.setAdapter(new RecurringIssueAdapter(this, recurring, group -> {
            Intent intent = new Intent(AdminDashboardActivity.this, RecurringIssueDetailsActivity.class);
            intent.putExtra("category", group.getCategory());
            intent.putExtra("location", group.getLocation());
            startActivity(intent);
        }));
        llRecurringSection.setVisibility(View.VISIBLE);
    }

    private void loadActivity() {
        llActivity.removeAllViews();
        List<AppNotification> recent = dbHelper.getRecentSystemActivityNotifications(5);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (AppNotification n : recent) {
            View row = inflater.inflate(R.layout.item_notification, llActivity, false);
            ((TextView) row.findViewById(R.id.tvNotifTitle)).setText(n.getTitle());
            ((TextView) row.findViewById(R.id.tvNotifMessage)).setText(n.getMessage());
            ((TextView) row.findViewById(R.id.tvNotifDate)).setText(TimeUtils.ago(n.getCreatedAt()));
            row.findViewById(R.id.viewUnreadDot).setVisibility(View.GONE);

            row.setOnClickListener(v -> {
                if (n.getIssueId() > 0) {
                    Intent intent = new Intent(AdminDashboardActivity.this, IssueDetailsActivity.class);
                    intent.putExtra("issue_id", n.getIssueId());
                    startActivity(intent);
                }
            });
            llActivity.addView(row);
        }
    }
}
