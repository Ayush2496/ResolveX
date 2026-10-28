package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.IssueAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.CsvExporter;
import com.example.resolvex_prototype.utils.IssueFilterBar;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/** Admin issue queue: search, quick filters with counts, the filter panel, inline Assign/Reassign and CSV export. */
public class AllIssuesActivity extends AppCompatActivity {

    private static final int Q_UNASSIGNED = 0, Q_OPEN = 1, Q_HIGH = 2, Q_REOPENED = 3, Q_ALL = 4;

    private RecyclerView rvAllIssues;
    private TextView tvEmptyAll;
    private TextView[] chips;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private String currentSearch = "";
    private IssueFilterBar filterBar;
    private int quickFilter = Q_UNASSIGNED;
    private boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AllIssuesActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_all_issues);

        rvAllIssues = findViewById(R.id.rvAllIssues);
        tvEmptyAll = findViewById(R.id.tvEmptyAll);
        rvAllIssues.setLayoutManager(new LinearLayoutManager(this));

        chips = new TextView[]{
                findViewById(R.id.chipUnassigned), findViewById(R.id.chipOpen), findViewById(R.id.chipHigh),
                findViewById(R.id.chipReopenedAdmin), findViewById(R.id.chipEveryIssue)};
        for (int i = 0; i < chips.length; i++) {
            final int index = i;
            chips[i].setOnClickListener(v -> {
                quickFilter = index;
                loadAllIssues();
            });
        }

        // The CSV contains exactly what the list currently shows
        findViewById(R.id.btnExportCsv).setOnClickListener(v ->
                CsvExporter.exportAndShareIssues(AllIssuesActivity.this, queryIssues()));

        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s != null ? s.toString().trim() : "";
                loadAllIssues();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterBar = new IssueFilterBar(this, this::loadAllIssues);

        BottomBar.attach(this, "ADMIN");
        UiUtils.applySystemBarSpace(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            UiUtils.bindHeader(this, "", "Issue queue", dbHelper.getUnreadNotificationCount(sessionManager.getUserId()));
            loadAllIssues();
        }
    }

    private boolean matchesQuickFilter(Issue issue) {
        String status = issue.getStatus();
        switch (quickFilter) {
            case Q_UNASSIGNED:
                return IssueStatusManager.SUBMITTED.equals(status);
            case Q_OPEN:
                return !IssueStatusManager.CLOSED.equals(status);
            case Q_HIGH:
                return "High".equalsIgnoreCase(issue.getPriority()) && !IssueStatusManager.CLOSED.equals(status);
            case Q_REOPENED:
                return IssueStatusManager.REOPENED.equals(status);
            default:
                return true;
        }
    }

    /** Database filters (search + filter panel) first, then the quick chip. */
    private List<Issue> queryIssues() {
        List<Issue> filtered = new ArrayList<>();
        for (Issue issue : dbHelper.getAllIssues(currentSearch, filterBar.getCategory(), filterBar.getStatus(),
                filterBar.getPriority(), filterBar.getSince())) {
            if (matchesQuickFilter(issue)) {
                filtered.add(issue);
            }
        }
        return filtered;
    }

    private void updateChipCounts() {
        int unassigned = 0, open = 0, high = 0, reopened = 0;
        List<Issue> every = dbHelper.getAllIssues(null, null, null, null);
        for (Issue issue : every) {
            String status = issue.getStatus();
            boolean isOpen = !IssueStatusManager.CLOSED.equals(status);
            if (IssueStatusManager.SUBMITTED.equals(status)) unassigned++;
            if (isOpen) open++;
            if (isOpen && "High".equalsIgnoreCase(issue.getPriority())) high++;
            if (IssueStatusManager.REOPENED.equals(status)) reopened++;
        }

        // On the first visit show the work that needs the admin: unassigned issues, if there are any
        if (firstLoad) {
            quickFilter = unassigned > 0 ? Q_UNASSIGNED : Q_OPEN;
            firstLoad = false;
        }

        chips[Q_UNASSIGNED].setText("Unassigned \u00B7 " + unassigned);
        chips[Q_OPEN].setText("All open \u00B7 " + open);
        chips[Q_HIGH].setText("High priority \u00B7 " + high);
        chips[Q_REOPENED].setText("Reopened \u00B7 " + reopened);
        chips[Q_ALL].setText("All \u00B7 " + every.size());

        for (int i = 0; i < chips.length; i++) {
            chips[i].setSelected(i == quickFilter);
        }
    }

    private void loadAllIssues() {
        updateChipCounts();
        List<Issue> issues = queryIssues();

        if (issues.isEmpty()) {
            rvAllIssues.setVisibility(View.GONE);
            tvEmptyAll.setVisibility(View.VISIBLE);
            return;
        }

        IssueAdapter adapter = new IssueAdapter(this, issues, issue -> {
            Intent intent = new Intent(AllIssuesActivity.this, IssueDetailsActivity.class);
            intent.putExtra("issue_id", issue.getIssueId());
            startActivity(intent);
        });
        adapter.setAdminActions(issue -> {
            Intent intent = new Intent(AllIssuesActivity.this, AssignStaffActivity.class);
            intent.putExtra(AssignStaffActivity.EXTRA_ISSUE_ID, issue.getIssueId());
            startActivity(intent);
        });
        rvAllIssues.setAdapter(adapter);
        rvAllIssues.setVisibility(View.VISIBLE);
        tvEmptyAll.setVisibility(View.GONE);
    }
}
