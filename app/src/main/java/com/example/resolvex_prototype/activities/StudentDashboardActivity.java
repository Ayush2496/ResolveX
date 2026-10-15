package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.NotificationHelper;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.Calendar;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    private LinearLayout llActionList, llRecent;
    private TextView tvEmptyAction, tvEmptyRecent, tvViewAll;
    private View statActive, statAwaiting, statClosed, statTotal;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"STUDENT".equalsIgnoreCase(sessionManager.getRole())) {
            startActivity(new Intent(StudentDashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_student_dashboard);

        llActionList = findViewById(R.id.llActionList);
        llRecent = findViewById(R.id.llRecent);
        tvEmptyAction = findViewById(R.id.tvEmptyAction);
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent);
        tvViewAll = findViewById(R.id.tvViewAll);
        statActive = findViewById(R.id.statActive);
        statAwaiting = findViewById(R.id.statAwaiting);
        statClosed = findViewById(R.id.statClosed);
        statTotal = findViewById(R.id.statTotal);

        findViewById(R.id.btnReportIssue).setOnClickListener(v ->
                startActivity(new Intent(StudentDashboardActivity.this, CreateIssueActivity.class)));
        tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(StudentDashboardActivity.this, MyIssuesActivity.class)));

        BottomBar.attach(this, "STUDENT");
        UiUtils.applySystemBarSpace(this);
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "STUDENT".equalsIgnoreCase(sessionManager.getRole())) {
            loadData();
        }
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning,";
        if (hour < 17) return "Good afternoon,";
        return "Good evening,";
    }

    private void loadData() {
        int userId = sessionManager.getUserId();
        int unread = dbHelper.getUnreadNotificationCount(userId);
        String name = sessionManager.getName();

        UiUtils.bindHeader(this, getGreeting(), name == null || name.isEmpty() ? "Student" : name, unread);
        NotificationHelper.showUnreadSummaryIfNew(this, userId, unread);

        List<Issue> issues = dbHelper.getStudentIssues(userId, null, null, null);

        int active = 0, awaiting = 0, closed = 0;
        llActionList.removeAllViews();
        llRecent.removeAllViews();

        for (Issue issue : issues) {
            String status = issue.getStatus();
            if (IssueStatusManager.CLOSED.equals(status)) {
                closed++;
            } else if (IssueStatusManager.RESOLVED.equals(status) || IssueStatusManager.VERIFIED.equals(status)) {
                awaiting++;
                // resolved by staff: verify or reopen; verified but not closed: feedback is missing
                String text = IssueStatusManager.RESOLVED.equals(status) ? "Verify resolution" : "Give feedback";
                UiUtils.addIssueCard(llActionList, issue, text, false);
            } else {
                active++;
            }
        }

        UiUtils.bindStat(statActive, R.color.rx_dot_amber, String.valueOf(active), "Active");
        UiUtils.bindStat(statAwaiting, R.color.rx_accent, String.valueOf(awaiting), "Awaiting you");
        UiUtils.bindStat(statClosed, R.color.rx_dot_green, String.valueOf(closed), "Closed");
        UiUtils.bindStat(statTotal, R.color.rx_dot_grey, String.valueOf(issues.size()), "Total reported");

        tvEmptyAction.setVisibility(awaiting == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < Math.min(3, issues.size()); i++) {
            UiUtils.addIssueCard(llRecent, issues.get(i), null, false);
        }
        tvEmptyRecent.setVisibility(issues.isEmpty() ? View.VISIBLE : View.GONE);
        tvViewAll.setVisibility(issues.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
