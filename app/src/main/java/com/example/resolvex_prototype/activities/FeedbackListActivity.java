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
import com.example.resolvex_prototype.adapters.FeedbackAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.Feedback;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.List;

public class FeedbackListActivity extends AppCompatActivity {

    private RecyclerView rvFeedback;
    private TextView tvEmptyFeedback;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(FeedbackListActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_feedback_list);

        rvFeedback = findViewById(R.id.rvFeedback);
        tvEmptyFeedback = findViewById(R.id.tvEmptyFeedback);

        rvFeedback.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText("Student feedback");
        findViewById(R.id.tvTopSub).setVisibility(View.GONE);
        UiUtils.applySystemBarSpace(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            loadFeedback();
        }
    }

    private void loadFeedback() {
        List<Feedback> feedbackList = dbHelper.getAllFeedback();
        if (feedbackList != null && !feedbackList.isEmpty()) {
            FeedbackAdapter adapter = new FeedbackAdapter(this, feedbackList);
            rvFeedback.setAdapter(adapter);
            rvFeedback.setVisibility(View.VISIBLE);
            tvEmptyFeedback.setVisibility(View.GONE);
        } else {
            rvFeedback.setVisibility(View.GONE);
            tvEmptyFeedback.setVisibility(View.VISIBLE);
        }
    }
}