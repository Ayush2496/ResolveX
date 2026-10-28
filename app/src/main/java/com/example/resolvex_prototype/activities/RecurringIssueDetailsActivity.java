package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.List;

public class RecurringIssueDetailsActivity extends AppCompatActivity {

    private TextView tvSummaryCategory, tvSummaryLocation, tvSummaryCount;
    private RecyclerView rvRecurringReports;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private String category = "";
    private String location = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RecurringIssueDetailsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_recurring_issue_details);

        category = getIntent().getStringExtra("category");
        location = getIntent().getStringExtra("location");

        tvSummaryCategory = findViewById(R.id.tvSummaryCategory);
        tvSummaryLocation = findViewById(R.id.tvSummaryLocation);
        tvSummaryCount = findViewById(R.id.tvSummaryCount);
        rvRecurringReports = findViewById(R.id.rvRecurringReports);

        rvRecurringReports.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText("Recurring issue");
        findViewById(R.id.tvTopSub).setVisibility(View.GONE);
        UiUtils.applySystemBarSpace(this);

        loadRecurringDetails();
    }

    private void loadRecurringDetails() {
        if (category == null || location == null) {
            Toast.makeText(this, "Invalid recurring issue selection", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSummaryCategory.setText(category);
        tvSummaryLocation.setText(location);

        List<Issue> reports = dbHelper.getRecurringIssueReports(category, location);

        // Revalidate threshold: must have at least 3 reports in the last 30 days
        if (reports == null || reports.size() < 3) {
            Toast.makeText(this, "This issue group no longer meets the recurring threshold (minimum 3 reports in 30 days).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvSummaryCount.setText(reports.size() + " reports in the last 30 days");

        IssueAdapter adapter = new IssueAdapter(this, reports, issue -> {
            Intent intent = new Intent(RecurringIssueDetailsActivity.this, IssueDetailsActivity.class);
            intent.putExtra("issue_id", issue.getIssueId());
            startActivity(intent);
        });
        adapter.setShowPriority(true);
        rvRecurringReports.setAdapter(adapter);
        rvRecurringReports.setVisibility(View.VISIBLE);
    }
}