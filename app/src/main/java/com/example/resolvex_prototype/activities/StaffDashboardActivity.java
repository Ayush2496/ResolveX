package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.IssueFilterBar;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.NotificationHelper;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/** Staff home: the numbers, the issues that need attention first, and the queue of assigned work. */
public class StaffDashboardActivity extends AppCompatActivity {

    private static final int QUICK_NEEDS_ACTION = 0, QUICK_IN_PROGRESS = 1, QUICK_REOPENED = 2, QUICK_COMPLETED = 3, QUICK_ALL = 4;

    private LinearLayout llAttention, llQueue;
    private TextView tvEmptyAttention, tvEmptyStaff;
    private TextView[] chips;
    private View statAssigned, statAwaitingMe, statAwaitingStudent, statReopened;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private IssueFilterBar filterBar;
    private String currentSearch = "";
    private int quickFilter = QUICK_NEEDS_ACTION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"STAFF".equalsIgnoreCase(sessionManager.getRole())) {
            startActivity(new Intent(StaffDashboardActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_staff_dashboard);

        llAttention = findViewById(R.id.llAttention);
        llQueue = findViewById(R.id.llQueue);
        tvEmptyAttention = findViewById(R.id.tvEmptyAttention);
        tvEmptyStaff = findViewById(R.id.tvEmptyStaff);
        statAssigned = findViewById(R.id.statAssigned);
        statAwaitingMe = findViewById(R.id.statAwaitingMe);
        statAwaitingStudent = findViewById(R.id.statAwaitingStudent);
        statReopened = findViewById(R.id.statReopened);

        // quick filters: the index in this array is the QUICK_* constant
        chips = new TextView[]{
                findViewById(R.id.chipNeedsAction), findViewById(R.id.chipInProgress), findViewById(R.id.chipReopened),
                findViewById(R.id.chipResolved), findViewById(R.id.chipAllWork)};
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> {
                quickFilter = index;
                loadStaffData();
            });
        }

        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s != null ? s.toString().trim() : "";
                loadStaffData();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterBar = new IssueFilterBar(this, this::loadStaffData);

        BottomBar.attach(this, "STAFF");
        UiUtils.applySystemBarSpace(this);
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "STAFF".equalsIgnoreCase(sessionManager.getRole())) {
            loadStaffData();
        }
    }

    private boolean matchesQuickFilter(String status) {
        switch (quickFilter) {
            case QUICK_NEEDS_ACTION:
                return IssueStatusManager.ASSIGNED.equals(status) || IssueStatusManager.REOPENED.equals(status);
            case QUICK_IN_PROGRESS:
                return IssueStatusManager.IN_PROGRESS.equals(status);
            case QUICK_REOPENED:
                return IssueStatusManager.REOPENED.equals(status);
            case QUICK_COMPLETED:
                return IssueStatusManager.RESOLVED.equals(status) || IssueStatusManager.VERIFIED.equals(status)
                        || IssueStatusManager.CLOSED.equals(status);
            default:
                return true;
        }
    }

    private void loadStaffData() {
        int staffId = sessionManager.getUserId();
        int unread = dbHelper.getUnreadNotificationCount(staffId);

        String dept = sessionManager.getDepartment();
        UiUtils.bindHeader(this, dept == null || dept.isEmpty() ? "Staff" : dept, sessionManager.getName(), unread);
        NotificationHelper.showUnreadSummaryIfNew(this, staffId, unread);

        // --- the four numbers (always from all of this staff member's issues, ignoring filters)
        List<Issue> all = dbHelper.getAssignedIssues(staffId, null, null);
        int active = 0, awaitingMe = 0, awaitingStudent = 0, reopened = 0;
        llAttention.removeAllViews();
        int attentionShown = 0;

        // reopened issues first, then high-priority work that has not been finished
        for (Issue issue : all) {
            String status = issue.getStatus();
            if (IssueStatusManager.ASSIGNED.equals(status) || IssueStatusManager.IN_PROGRESS.equals(status)
                    || IssueStatusManager.REOPENED.equals(status)) {
                active++;
            }
            if (IssueStatusManager.ASSIGNED.equals(status) || IssueStatusManager.REOPENED.equals(status)) {
                awaitingMe++;
            }
            if (IssueStatusManager.RESOLVED.equals(status) || IssueStatusManager.VERIFIED.equals(status)) {
                awaitingStudent++;
            }
            if (IssueStatusManager.REOPENED.equals(status)) {
                reopened++;
            }
        }
        for (Issue issue : all) { // the list is already ordered with reopened work first
            boolean isReopened = IssueStatusManager.REOPENED.equals(issue.getStatus());
            boolean isUrgent = "High".equalsIgnoreCase(issue.getPriority())
                    && (IssueStatusManager.ASSIGNED.equals(issue.getStatus()) || IssueStatusManager.IN_PROGRESS.equals(issue.getStatus()));
            if ((isReopened || isUrgent) && attentionShown < 2) {
                UiUtils.addIssueCard(llAttention, issue, null, true);
                attentionShown++;
            }
        }
        tvEmptyAttention.setVisibility(attentionShown == 0 ? View.VISIBLE : View.GONE);

        UiUtils.bindStat(statAssigned, R.color.rx_dot_purple, String.valueOf(active), "Assigned active");
        UiUtils.bindStat(statAwaitingMe, R.color.rx_accent, String.valueOf(awaitingMe), "Awaiting my action");
        UiUtils.bindStat(statAwaitingStudent, R.color.rx_dot_amber, String.valueOf(awaitingStudent), "Awaiting student");
        UiUtils.bindStat(statReopened, R.color.rx_dot_pink, String.valueOf(reopened), "Reopened");

        // --- the queue: search + filter bar (database) and the quick chips (here)
        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(i == quickFilter);
        }

        llQueue.removeAllViews();
        int shown = 0;
        List<Issue> queue = dbHelper.getAssignedIssues(staffId, currentSearch, filterBar.getStatus(),
                filterBar.getCategory(), filterBar.getPriority(), filterBar.getSince());
        for (Issue issue : queue) {
            if (matchesQuickFilter(issue.getStatus())) {
                UiUtils.addIssueCard(llQueue, issue, null, true);
                shown++;
            }
        }
        tvEmptyStaff.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }
}
