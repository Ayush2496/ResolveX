package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.IssueAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.IssueFilterBar;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class MyIssuesActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private RecyclerView rvIssues;
    private LinearLayout llEmptyState;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private String currentSearch = "";
    private IssueFilterBar filterBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"STUDENT".equalsIgnoreCase(sessionManager.getRole())) {
            startActivity(new Intent(MyIssuesActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_my_issues);

        etSearch = findViewById(R.id.etSearch);
        rvIssues = findViewById(R.id.rvIssues);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvIssues.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnEmptyReport).setOnClickListener(v ->
                startActivity(new Intent(MyIssuesActivity.this, CreateIssueActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s != null ? s.toString().trim() : "";
                loadIssues();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        filterBar = new IssueFilterBar(this, this::loadIssues);

        BottomBar.attach(this, "STUDENT");
        UiUtils.applySystemBarSpace(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "STUDENT".equalsIgnoreCase(sessionManager.getRole())) {
            UiUtils.bindHeader(this, "", "My issues", dbHelper.getUnreadNotificationCount(sessionManager.getUserId()));
            loadIssues();
        }
    }

    private void loadIssues() {
        int userId = sessionManager.getUserId();
        List<Issue> issues = dbHelper.getStudentIssues(userId, currentSearch, filterBar.getCategory(), filterBar.getStatus(),
                filterBar.getPriority(), filterBar.getSince());

        if (issues != null && !issues.isEmpty()) {
            IssueAdapter adapter = new IssueAdapter(this, issues, issue -> {
                Intent intent = new Intent(MyIssuesActivity.this, IssueDetailsActivity.class);
                intent.putExtra("issue_id", issue.getIssueId());
                startActivity(intent);
            });
            rvIssues.setAdapter(adapter);
            rvIssues.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        } else {
            rvIssues.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        }
    }
}
