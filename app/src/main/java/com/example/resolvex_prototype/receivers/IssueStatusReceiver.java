package com.example.resolvex_prototype.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.resolvex_prototype.utils.NotificationHelper;

public class IssueStatusReceiver extends BroadcastReceiver {

    public static final String ACTION_STATUS_CHANGED = "com.example.resolvex_prototype.ACTION_ISSUE_STATUS_CHANGED";
    public static final String EXTRA_ISSUE_ID = "extra_issue_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_STATUS_CHANGED.equals(intent.getAction())) {
            int issueId = intent.getIntExtra(EXTRA_ISSUE_ID, 0);
            String title = intent.getStringExtra(EXTRA_TITLE);
            String message = intent.getStringExtra(EXTRA_MESSAGE);

            if (title == null) title = "ResolveX Issue Update";
            if (message == null) message = "Status changed for issue #" + issueId;

            NotificationHelper.showNotification(context, title, message, issueId > 0 ? issueId : (int) System.currentTimeMillis());
        }
    }
}