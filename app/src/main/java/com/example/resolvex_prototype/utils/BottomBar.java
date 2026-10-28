package com.example.resolvex_prototype.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.activities.AdminDashboardActivity;
import com.example.resolvex_prototype.activities.AllIssuesActivity;
import com.example.resolvex_prototype.activities.CreateIssueActivity;
import com.example.resolvex_prototype.activities.MyIssuesActivity;
import com.example.resolvex_prototype.activities.NotificationsActivity;
import com.example.resolvex_prototype.activities.ProfileActivity;
import com.example.resolvex_prototype.activities.StaffDashboardActivity;
import com.example.resolvex_prototype.activities.StudentDashboardActivity;
import com.example.resolvex_prototype.activities.UserManagementActivity;

/**
 * The bottom tab bar. Every main screen contains the layout "view_bottom_bar" and calls
 * BottomBar.attach(...). The tabs depend on the role. Students also get a raised "+" button.
 *
 * Navigation rule (keeps the back button sensible): the first tab is the "home" screen.
 * Going to home clears everything above it; going to any other tab opens it and closes the
 * previous tab, so there is never a long pile of screens behind you.
 */
public class BottomBar {

    private static class Tab {
        final int icon;
        final String label;
        final Class<?> target;

        Tab(int icon, String label, Class<?> target) {
            this.icon = icon;
            this.label = label;
            this.target = target;
        }
    }

    public static void attach(Activity activity, String role) {
        LinearLayout bar = activity.findViewById(R.id.bottomBar);
        bar.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);

        Tab[] tabs;
        boolean raisedAdd = false;
        if ("ADMIN".equalsIgnoreCase(role)) {
            tabs = new Tab[]{
                    new Tab(R.drawable.ic_overview, "Overview", AdminDashboardActivity.class),
                    new Tab(R.drawable.ic_inbox, "Issues", AllIssuesActivity.class),
                    new Tab(R.drawable.ic_nav_users, "Users", UserManagementActivity.class),
                    new Tab(R.drawable.ic_nav_alerts, "Alerts", NotificationsActivity.class),
                    new Tab(R.drawable.ic_nav_person, "Profile", ProfileActivity.class)};
        } else if ("STAFF".equalsIgnoreCase(role)) {
            tabs = new Tab[]{
                    new Tab(R.drawable.ic_inbox, "Queue", StaffDashboardActivity.class),
                    new Tab(R.drawable.ic_nav_alerts, "Alerts", NotificationsActivity.class),
                    new Tab(R.drawable.ic_nav_person, "Profile", ProfileActivity.class)};
        } else {
            raisedAdd = true;
            tabs = new Tab[]{
                    new Tab(R.drawable.ic_nav_home, "Home", StudentDashboardActivity.class),
                    new Tab(R.drawable.ic_inbox, "Issues", MyIssuesActivity.class),
                    new Tab(R.drawable.ic_nav_alerts, "Alerts", NotificationsActivity.class),
                    new Tab(R.drawable.ic_nav_person, "Profile", ProfileActivity.class)};
        }

        final Class<?> home = tabs[0].target;
        for (int i = 0; i < tabs.length; i++) {
            // the raised + sits in the middle of the student bar
            if (raisedAdd && i == 2) {
                View fab = inflater.inflate(R.layout.item_bottom_fab, bar, false);
                fab.setOnClickListener(v -> activity.startActivity(new Intent(activity, CreateIssueActivity.class)));
                bar.addView(fab);
            }

            final Tab tab = tabs[i];
            View item = inflater.inflate(R.layout.item_bottom_tab, bar, false);
            ImageView icon = item.findViewById(R.id.ivTabIcon);
            TextView label = item.findViewById(R.id.tvTabLabel);
            icon.setImageResource(tab.icon);
            label.setText(tab.label);

            int color = activity.getColor(activity.getClass() == tab.target ? R.color.rx_accent : R.color.rx_muted);
            icon.setColorFilter(color);
            label.setTextColor(color);

            item.setOnClickListener(v -> open(activity, tab.target, home));
            bar.addView(item);
        }
    }

    private static void open(Activity from, Class<?> target, Class<?> home) {
        if (from.getClass() == target) return;

        Intent intent = new Intent(from, target);
        if (target == home) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            from.startActivity(intent);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            from.startActivity(intent);
            if (from.getClass() != home) {
                from.finish();
            }
        }
    }
}
