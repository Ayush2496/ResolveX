package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.StaffAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.List;

/**
 * Admin screen: choose the staff member for an issue. A new issue is assigned; an issue that
 * already has an owner is reassigned. Used from the issue queue and from the issue screen.
 */
public class AssignStaffActivity extends AppCompatActivity {

    public static final String EXTRA_ISSUE_ID = "issue_id";

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Issue issue;
    private boolean reassign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AssignStaffActivity.this, LoginActivity.class));
            finish();
            return;
        }

        issue = dbHelper.getIssueById(getIntent().getIntExtra(EXTRA_ISSUE_ID, 0));
        if (issue == null) {
            Toast.makeText(this, "Issue not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        reassign = issue.getAssignedStaffId() > 0 && !IssueStatusManager.SUBMITTED.equals(issue.getStatus());

        setContentView(R.layout.activity_assign_staff);
        UiUtils.applySystemBarSpace(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText(reassign ? "Reassign issue" : "Assign issue");
        ((TextView) findViewById(R.id.tvTopSub)).setText(IssueStatusManager.formatIssueId(issue.getIssueId()));

        ((TextView) findViewById(R.id.tvAssignIssue)).setText(issue.getTitle());
        ((TextView) findViewById(R.id.tvAssignCurrent)).setText(
                reassign && issue.getAssignedStaffName() != null
                        ? "Currently with " + issue.getAssignedStaffName()
                        : "Not assigned yet");

        List<User> staff = dbHelper.getAllStaff();
        RecyclerView rv = findViewById(R.id.rvAssignStaff);
        rv.setLayoutManager(new LinearLayoutManager(this));

        StaffAdapter adapter = new StaffAdapter(this, staff);
        adapter.setWorkloads(dbHelper.getStaffWorkloads());
        adapter.setOnUserClickListener(this::choose);
        rv.setAdapter(adapter);

        if (staff.isEmpty()) {
            Toast.makeText(this, "No staff members registered yet", Toast.LENGTH_LONG).show();
        }
    }

    private void choose(User chosen) {
        int adminId = sessionManager.getUserId();
        boolean done = reassign
                ? dbHelper.reassignIssue(issue.getIssueId(), chosen.getUserId(), adminId)
                : dbHelper.assignIssue(issue.getIssueId(), chosen.getUserId(), adminId, chosen.getName());

        if (done) {
            Toast.makeText(this, (reassign ? "Reassigned to " : "Assigned to ") + chosen.getName(), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, reassign ? "That person already has this issue" : "Could not assign this issue",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
