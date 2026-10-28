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
import com.example.resolvex_prototype.adapters.NotificationAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.AppNotification;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0, FILTER_ACTION = 1, FILTER_UPDATES = 2;

    private RecyclerView rvNotifications;
    private TextView tvEmptyNotifications, chipAll, chipAction, chipUpdates;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private int filter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(NotificationsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_notifications);

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications);
        chipAll = findViewById(R.id.chipAll);
        chipAction = findViewById(R.id.chipAction);
        chipUpdates = findViewById(R.id.chipUpdates);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        chipAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        chipAction.setOnClickListener(v -> setFilter(FILTER_ACTION));
        chipUpdates.setOnClickListener(v -> setFilter(FILTER_UPDATES));

        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> {
            dbHelper.markAllNotificationsRead(sessionManager.getUserId());
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
            loadNotifications();
        });

        BottomBar.attach(this, sessionManager.getRole());
        UiUtils.applySystemBarSpace(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadNotifications();
        }
    }

    private void setFilter(int newFilter) {
        filter = newFilter;
        loadNotifications();
    }

    /** An unread notification that asks the user to do something (verify, start work, assign...). */
    private boolean isActionNeeded(AppNotification n) {
        if (n.isRead()) return false;
        String title = n.getTitle() == null ? "" : n.getTitle();
        return title.contains("Action Required") || title.contains("Assigned")
                || title.contains("Reopened") || title.contains("New Issue Submitted")
                || title.contains("Recurring");
    }

    private void loadNotifications() {
        int userId = sessionManager.getUserId();
        int unreadCount = dbHelper.getUnreadNotificationCount(userId);

        UiUtils.bindHeader(this, unreadCount > 0 ? unreadCount + " unread" : "", "Alerts", unreadCount);

        chipAll.setSelected(filter == FILTER_ALL);
        chipAction.setSelected(filter == FILTER_ACTION);
        chipUpdates.setSelected(filter == FILTER_UPDATES);

        List<AppNotification> shown = new ArrayList<>();
        for (AppNotification n : dbHelper.getUserNotifications(userId)) {
            boolean action = isActionNeeded(n);
            if (filter == FILTER_ALL || (filter == FILTER_ACTION && action) || (filter == FILTER_UPDATES && !action)) {
                shown.add(n);
            }
        }

        if (shown.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            tvEmptyNotifications.setVisibility(View.VISIBLE);
            return;
        }

        NotificationAdapter adapter = new NotificationAdapter(this, shown, notif -> {
            dbHelper.markNotificationRead(notif.getNotificationId(), userId);
            if (notif.getIssueId() > 0) {
                Intent intent = new Intent(NotificationsActivity.this, IssueDetailsActivity.class);
                intent.putExtra("issue_id", notif.getIssueId());
                startActivity(intent);
            } else {
                loadNotifications();
            }
        });
        rvNotifications.setAdapter(adapter);
        rvNotifications.setVisibility(View.VISIBLE);
        tvEmptyNotifications.setVisibility(View.GONE);
    }
}
