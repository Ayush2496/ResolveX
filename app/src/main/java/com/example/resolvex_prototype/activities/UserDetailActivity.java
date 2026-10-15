package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.TimeUtils;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/** Admin screen: one student or staff member, their issues, and a password reset. */
public class UserDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";
    private static final int MAX_ISSUES = 10;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private User user;

    private TextView tvIssuesTitle, tvIssuesEmpty;
    private LinearLayout llUserIssues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(UserDetailActivity.this, LoginActivity.class));
            finish();
            return;
        }

        user = dbHelper.getUserById(getIntent().getIntExtra(EXTRA_USER_ID, 0));
        if (user == null) {
            Toast.makeText(this, "Person not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_user_detail);
        UiUtils.applySystemBarSpace(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText(user.getName());
        ((TextView) findViewById(R.id.tvTopSub)).setText(
                "STUDENT".equalsIgnoreCase(user.getRole()) ? "Student" : user.getRole().substring(0, 1) + user.getRole().substring(1).toLowerCase());

        tvIssuesTitle = findViewById(R.id.tvUserIssuesTitle);
        tvIssuesEmpty = findViewById(R.id.tvUserIssuesEmpty);
        llUserIssues = findViewById(R.id.llUserIssues);

        bindProfile();

        TextInputEditText etNewPassword = findViewById(R.id.etNewPassword);
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
            if (!ValidationUtils.isValidPassword(newPassword)) {
                etNewPassword.setError("Password must be at least 6 characters");
                etNewPassword.requestFocus();
                return;
            }

            boolean done = dbHelper.resetPassword(user.getUserId(), newPassword, sessionManager.getUserId());
            Toast.makeText(this, done ? "Password updated for " + user.getName() : "Could not update the password",
                    Toast.LENGTH_SHORT).show();
            if (done) {
                etNewPassword.setText("");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) {
            loadIssues();
        }
    }

    private void bindProfile() {
        ((TextView) findViewById(R.id.tvDetailInitials)).setText(UiUtils.initials(user.getName()));
        ((TextView) findViewById(R.id.tvUserName)).setText(user.getName());
        UiUtils.bindPill((TextView) findViewById(R.id.chipUserRole),
                user.getRole().substring(0, 1) + user.getRole().substring(1).toLowerCase(),
                R.color.rx_purple_bg, R.color.rx_purple_fg);

        ((TextView) findViewById(R.id.tvUserEmail)).setText(user.getEmail());
        ((TextView) findViewById(R.id.tvUserPhone)).setText(
                user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "-");
        ((TextView) findViewById(R.id.tvUserDept)).setText(
                user.getDepartment() != null && !user.getDepartment().isEmpty() ? user.getDepartment() : "-");
        ((TextView) findViewById(R.id.tvUserJoined)).setText(TimeUtils.shortDate(user.getCreatedAt()));

        boolean student = "STUDENT".equalsIgnoreCase(user.getRole());
        findViewById(R.id.llUserYear).setVisibility(student ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tvUserYear)).setText("Year " + user.getYear());
    }

    /** Staff: their open work. Students: the issues they reported. Admins have neither. */
    private void loadIssues() {
        llUserIssues.removeAllViews();
        String role = user.getRole();

        List<Issue> issues;
        if ("STAFF".equalsIgnoreCase(role)) {
            tvIssuesTitle.setText("Open work");
            tvIssuesEmpty.setText("No open work right now.");
            issues = new java.util.ArrayList<>();
            for (Issue issue : dbHelper.getAssignedIssues(user.getUserId(), null, null)) {
                String status = issue.getStatus();
                if (IssueStatusManager.ASSIGNED.equals(status) || IssueStatusManager.IN_PROGRESS.equals(status)
                        || IssueStatusManager.REOPENED.equals(status)) {
                    issues.add(issue);
                }
            }
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            tvIssuesTitle.setText("Reported issues");
            tvIssuesEmpty.setText("No issues reported yet.");
            issues = dbHelper.getStudentIssues(user.getUserId(), null, null, null);
        } else {
            tvIssuesTitle.setVisibility(View.GONE);
            tvIssuesEmpty.setVisibility(View.GONE);
            return;
        }

        for (int i = 0; i < Math.min(MAX_ISSUES, issues.size()); i++) {
            UiUtils.addIssueCard(llUserIssues, issues.get(i), null, true);
        }
        tvIssuesEmpty.setVisibility(issues.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
