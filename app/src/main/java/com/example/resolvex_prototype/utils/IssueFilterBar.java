package com.example.resolvex_prototype.utils;

import android.app.Activity;
import android.view.View;

import com.example.resolvex_prototype.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

/**
 * The filter panel (status, category, priority, date) shared by My Issues, All Issues and the staff queue.
 * It needs the layout "view_filter_bar" to be included in the screen.
 * The screen only has to ask for getStatus(), getCategory(), getPriority() and getSince().
 */
public class IssueFilterBar {

    public interface OnFilterChangedListener {
        void onFilterChanged();
    }

    private static final String[] STATUS_KEYS = {
            null,
            IssueStatusManager.SUBMITTED, IssueStatusManager.ASSIGNED, IssueStatusManager.IN_PROGRESS,
            IssueStatusManager.RESOLVED, IssueStatusManager.VERIFIED, IssueStatusManager.REOPENED,
            IssueStatusManager.CLOSED
    };

    private static final String[] PRIORITY_LABELS = {"All priorities", "High", "Medium", "Low"};
    private static final String[] DATE_LABELS = {"Any time", "Today", "Last 7 days", "Last 30 days"};

    private int statusIndex = 0;
    private int categoryIndex = 0;
    private int priorityIndex = 0;
    private int dateIndex = 0;

    private final MaterialButton btnToggle;
    private final View body;
    private final OnFilterChangedListener listener;

    public IssueFilterBar(Activity activity, OnFilterChangedListener listener) {
        this.listener = listener;

        btnToggle = activity.findViewById(R.id.btnToggleFilters);
        body = activity.findViewById(R.id.llFilterBody);

        btnToggle.setOnClickListener(v ->
                body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        // Labels are built from the same helper the rest of the app uses for status names
        String[] statusLabels = new String[STATUS_KEYS.length];
        statusLabels[0] = "All statuses";
        for (int i = 1; i < STATUS_KEYS.length; i++) {
            statusLabels[i] = IssueStatusManager.getHumanReadableStatus(STATUS_KEYS[i]);
        }

        String[] categoryLabels = new String[IssueOptions.CATEGORIES.length + 1];
        categoryLabels[0] = "All categories";
        System.arraycopy(IssueOptions.CATEGORIES, 0, categoryLabels, 1, IssueOptions.CATEGORIES.length);

        MaterialAutoCompleteTextView actStatus = activity.findViewById(R.id.actFilterStatus);
        MaterialAutoCompleteTextView actCategory = activity.findViewById(R.id.actFilterCategory);
        MaterialAutoCompleteTextView actPriority = activity.findViewById(R.id.actFilterPriority);
        MaterialAutoCompleteTextView actDate = activity.findViewById(R.id.actFilterDate);

        DropdownHelper.setup(activity, actStatus, statusLabels, 0, index -> { statusIndex = index; changed(); });
        DropdownHelper.setup(activity, actCategory, categoryLabels, 0, index -> { categoryIndex = index; changed(); });
        DropdownHelper.setup(activity, actPriority, PRIORITY_LABELS, 0, index -> { priorityIndex = index; changed(); });
        DropdownHelper.setup(activity, actDate, DATE_LABELS, 0, index -> { dateIndex = index; changed(); });
    }

    private void changed() {
        int active = 0;
        if (statusIndex != 0) active++;
        if (categoryIndex != 0) active++;
        if (priorityIndex != 0) active++;
        if (dateIndex != 0) active++;

        btnToggle.setText(active == 0 ? "Filters" : "Filters (" + active + ")");

        if (listener != null) {
            listener.onFilterChanged();
        }
    }

    /** Returns a status key such as "IN_PROGRESS", or null for "all". */
    public String getStatus() {
        return STATUS_KEYS[statusIndex];
    }

    public String getCategory() {
        return categoryIndex == 0 ? null : IssueOptions.CATEGORIES[categoryIndex - 1];
    }

    public String getPriority() {
        return priorityIndex == 0 ? null : PRIORITY_LABELS[priorityIndex];
    }

    /** Returns the oldest creation time to include, or null for "any time". */
    public String getSince() {
        switch (dateIndex) {
            case 1:
                return TimeUtils.startOfToday();
            case 2:
                return TimeUtils.daysAgo(7);
            case 3:
                return TimeUtils.daysAgo(30);
            default:
                return null;
        }
    }
}
