package com.example.resolvex_prototype.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.activities.IssueDetailsActivity;
import com.example.resolvex_prototype.activities.NotificationsActivity;
import com.example.resolvex_prototype.models.Issue;

import java.util.Locale;

/**
 * Small helpers shared by every redesigned screen, so each screen does not repeat the styling code:
 * status/priority pills, the 6-step progress bar, the header with the bell, and system-bar spacing.
 */
public class UiUtils {

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------ pills

    /** Colours (resource ids) for one pill: background, text and the small dot. */
    private static int[] statusColors(String status) {
        if (status == null) status = "";
        switch (status.toUpperCase(Locale.ROOT)) {
            case IssueStatusManager.ASSIGNED:
                return new int[]{R.color.rx_purple_bg, R.color.rx_purple_fg};
            case IssueStatusManager.IN_PROGRESS:
                return new int[]{R.color.rx_amber_bg, R.color.rx_amber_fg};
            case IssueStatusManager.RESOLVED:
            case IssueStatusManager.VERIFIED:
                return new int[]{R.color.rx_green_bg, R.color.rx_green_fg};
            case IssueStatusManager.REOPENED:
                return new int[]{R.color.rx_pink_bg, R.color.rx_pink_fg};
            default: // SUBMITTED, CLOSED
                return new int[]{R.color.rx_grey_bg, R.color.rx_grey_fg};
        }
    }

    private static void paintPill(TextView pill, String text, int bgRes, int fgRes) {
        Context c = pill.getContext();
        int fg = c.getColor(fgRes);

        GradientDrawable background = new GradientDrawable();
        background.setColor(c.getColor(bgRes));
        background.setCornerRadius(dp(c, 99));
        pill.setBackground(background);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(fg);
        dot.setSize(dp(c, 5), dp(c, 5));

        pill.setText(text);
        pill.setTextColor(fg);
        pill.setCompoundDrawablePadding(dp(c, 5));
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        pill.setVisibility(View.VISIBLE);
    }

    /** Shows the issue status as a tinted pill, e.g. "In Progress" in amber. */
    public static void bindStatus(TextView pill, String status) {
        bindStatus(pill, status, IssueStatusManager.getHumanReadableStatus(status));
    }

    /** Same, with your own wording, e.g. "Verify resolution" for the student's action list. */
    public static void bindStatus(TextView pill, String status, String text) {
        int[] colors = statusColors(status);
        paintPill(pill, text, colors[0], colors[1]);
    }

    public static void bindPriority(TextView pill, String priority) {
        int[] colors;
        if ("High".equalsIgnoreCase(priority)) {
            colors = new int[]{R.color.rx_pink_bg, R.color.rx_pink_fg};
        } else if ("Medium".equalsIgnoreCase(priority)) {
            colors = new int[]{R.color.rx_amber_bg, R.color.rx_amber_fg};
        } else {
            colors = new int[]{R.color.rx_grey_bg, R.color.rx_grey_fg};
        }
        paintPill(pill, priority == null ? "" : priority, colors[0], colors[1]);
    }

    /** A pill with colours chosen by the caller, e.g. "Trend". */
    public static void bindPill(TextView pill, String text, int bgRes, int fgRes) {
        paintPill(pill, text, bgRes, fgRes);
    }

    // ------------------------------------------------------------------ progress bar

    private static final String[] STEPS = {
            IssueStatusManager.SUBMITTED, IssueStatusManager.ASSIGNED, IssueStatusManager.IN_PROGRESS,
            IssueStatusManager.RESOLVED, IssueStatusManager.VERIFIED, IssueStatusManager.CLOSED
    };

    /** Index (0-5) of the current step. A reopened issue is back at "In progress". */
    public static int stepIndex(String status) {
        if (IssueStatusManager.REOPENED.equals(status)) return 2;
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i].equals(status)) return i;
        }
        return 0;
    }

    /** Fills a horizontal LinearLayout with 6 segments: finished = dark, current = coloured, to do = grey. */
    public static void buildProgressBar(LinearLayout bar, String status) {
        Context c = bar.getContext();
        bar.removeAllViews();
        int current = stepIndex(status);

        int currentColor = c.getColor(R.color.rx_accent);
        if (IssueStatusManager.REOPENED.equals(status)) {
            currentColor = c.getColor(R.color.rx_dot_pink);
        } else if (current >= 3) {
            currentColor = c.getColor(R.color.rx_dot_green);
        }

        for (int i = 0; i < STEPS.length; i++) {
            View segment = new View(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 5), 1f);
            lp.setMargins(dp(c, 2), 0, dp(c, 2), 0);
            segment.setLayoutParams(lp);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(c, 3));
            if (i < current) {
                shape.setColor(c.getColor(R.color.rx_ink));
            } else if (i == current) {
                shape.setColor(currentColor);
            } else {
                shape.setColor(c.getColor(R.color.rx_segment_off));
            }
            segment.setBackground(shape);
            bar.addView(segment);
        }
    }

    /** Gives a view a plain rounded background in one of the design colours (used for chart bars). */
    public static void rounded(View view, int colorRes, float radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(view.getContext().getColor(colorRes));
        shape.setCornerRadius(dp(view.getContext(), radiusDp));
        view.setBackground(shape);
    }

    // ------------------------------------------------------------------ cards

    /** Fills one "stat" card (see view_stat_card): coloured dot, big number, label. */
    public static void bindStat(View card, int dotColorRes, String value, String label) {
        View dot = card.findViewById(R.id.viewStatDot);
        dot.setBackgroundTintList(ColorStateList.valueOf(card.getContext().getColor(dotColorRes)));
        ((TextView) card.findViewById(R.id.tvStatValue)).setText(value);
        ((TextView) card.findViewById(R.id.tvStatLabel)).setText(label);
    }

    /**
     * Adds one issue card to a LinearLayout (used on the dashboards, where a RecyclerView would be
     * overkill for 2-5 cards). Tapping it opens the issue. pillText replaces the status wording when given.
     */
    public static void addIssueCard(ViewGroup container, Issue issue, String pillText, boolean showPriority) {
        Context c = container.getContext();
        View card = LayoutInflater.from(c).inflate(R.layout.item_issue, container, false);

        ((TextView) card.findViewById(R.id.tvIssueId)).setText(IssueStatusManager.formatIssueId(issue.getIssueId()));
        ((TextView) card.findViewById(R.id.tvIssueTitle)).setText(issue.getTitle());

        TextView pill = card.findViewById(R.id.chipStatus);
        if (pillText != null) {
            bindStatus(pill, issue.getStatus(), pillText);
        } else {
            bindStatus(pill, issue.getStatus());
        }

        String meta = issue.getCategory() + (showPriority ? " \u00B7 " + issue.getPriority() : "")
                + " \u00B7 " + TimeUtils.ago(issue.getUpdatedAt()) + " ago";
        ((TextView) card.findViewById(R.id.tvIssueMeta)).setText(meta);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(c, IssueDetailsActivity.class);
            intent.putExtra("issue_id", issue.getIssueId());
            c.startActivity(intent);
        });
        container.addView(card);
    }

    // ------------------------------------------------------------------ small things

    /** "Aarav Sharma" becomes "AS". */
    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    /** Fills the header used on the main tabs: small line, big title, bell with a red dot when there are unread alerts. */
    public static void bindHeader(Activity activity, String small, String title, int unreadCount) {
        TextView tvSmall = activity.findViewById(R.id.tvHeaderSub);
        TextView tvTitle = activity.findViewById(R.id.tvHeaderTitle);
        tvSmall.setText(small);
        tvSmall.setVisibility(small == null || small.isEmpty() ? View.GONE : View.VISIBLE);
        tvTitle.setText(title);
        bindBell(activity, R.id.btnBell, R.id.viewBellDot, unreadCount);
    }

    public static void bindBell(Activity activity, int bellId, int dotId, int unreadCount) {
        View bell = activity.findViewById(bellId);
        View dot = activity.findViewById(dotId);
        if (dot != null) {
            dot.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
        }
        if (bell != null) {
            bell.setOnClickListener(v -> {
                if (!(activity instanceof NotificationsActivity)) {
                    activity.startActivity(new Intent(activity, NotificationsActivity.class));
                }
            });
        }
    }

    /**
     * Recent Android versions draw apps behind the status and navigation bars. This adds the
     * matching space so content is not hidden: at the top for the screen, at the bottom for the
     * bottom bar (or for the screen when there is no bottom bar).
     */
    public static void applySystemBarSpace(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        View bottomBar = activity.findViewById(R.id.bottomBar);
        final int barBottom = bottomBar != null ? bottomBar.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bottomBar != null ? 0 : bars.bottom);
            if (bottomBar != null) {
                bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                        bottomBar.getPaddingRight(), barBottom + bars.bottom);
            }
            return insets;
        });
    }
}
