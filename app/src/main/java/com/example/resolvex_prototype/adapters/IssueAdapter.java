package com.example.resolvex_prototype.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.activities.UserDetailActivity;
import com.example.resolvex_prototype.models.Issue;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.TimeUtils;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/** The list of issue cards. Every role uses it; the admin turns on the assignee row with setAdminActions(). */
public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

    public interface OnIssueClickListener {
        void onIssueClick(Issue issue);
    }

    public interface OnAssignClickListener {
        void onAssignClick(Issue issue);
    }

    private final Context context;
    private final List<Issue> issueList;
    private final OnIssueClickListener listener;

    private boolean showPriority = false;
    private OnAssignClickListener assignListener;

    public IssueAdapter(Context context, List<Issue> issueList, OnIssueClickListener listener) {
        this.context = context;
        this.issueList = issueList;
        this.listener = listener;
    }

    /** Staff and admin lists also show the priority in the small grey line. */
    public void setShowPriority(boolean showPriority) {
        this.showPriority = showPriority;
    }

    /** Admin only: shows who the issue is assigned to and an Assign / Reassign button. */
    public void setAdminActions(OnAssignClickListener assignListener) {
        this.assignListener = assignListener;
        this.showPriority = true;
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_issue, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        Issue issue = issueList.get(position);

        holder.tvId.setText(IssueStatusManager.formatIssueId(issue.getIssueId()));
        holder.tvTitle.setText(issue.getTitle());
        UiUtils.bindStatus(holder.chipStatus, issue.getStatus());

        // "Water / Plumbing · High · 21 Sep · 2d ago"
        StringBuilder meta = new StringBuilder(issue.getCategory());
        if (showPriority) {
            meta.append(" \u00B7 ").append(issue.getPriority());
        }
        meta.append(" \u00B7 ").append(TimeUtils.shortDate(issue.getCreatedAt()));
        meta.append(" \u00B7 updated ").append(TimeUtils.ago(issue.getUpdatedAt())).append(" ago");
        holder.tvMeta.setText(meta.toString());

        bindAdminFooter(holder, issue);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIssueClick(issue);
            }
        });
    }

    private void bindAdminFooter(IssueViewHolder holder, Issue issue) {
        if (assignListener == null) {
            holder.footer.setVisibility(View.GONE);
            return;
        }

        holder.footer.setVisibility(View.VISIBLE);
        boolean assigned = issue.getAssignedStaffId() > 0 && issue.getAssignedStaffName() != null;

        if (assigned) {
            holder.tvInitials.setVisibility(View.VISIBLE);
            holder.tvInitials.setText(UiUtils.initials(issue.getAssignedStaffName()));
            holder.tvAssignee.setText(issue.getAssignedStaffName());

            // tap the person to see their details
            View.OnClickListener openPerson = v -> {
                Intent intent = new Intent(context, UserDetailActivity.class);
                intent.putExtra(UserDetailActivity.EXTRA_USER_ID, issue.getAssignedStaffId());
                context.startActivity(intent);
            };
            holder.tvInitials.setOnClickListener(openPerson);
            holder.tvAssignee.setOnClickListener(openPerson);
        } else {
            holder.tvInitials.setVisibility(View.GONE);
            holder.tvAssignee.setText("Unassigned");
            holder.tvAssignee.setOnClickListener(null);
            holder.tvAssignee.setClickable(false);
        }

        String status = issue.getStatus();
        boolean canAssign = IssueStatusManager.SUBMITTED.equals(status);
        boolean canReassign = assigned && (IssueStatusManager.ASSIGNED.equals(status)
                || IssueStatusManager.IN_PROGRESS.equals(status)
                || IssueStatusManager.REOPENED.equals(status));

        if (canAssign || canReassign) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText(canAssign ? "Assign" : "Reassign");
            holder.btnAction.setOnClickListener(v -> assignListener.onAssignClick(issue));
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return issueList != null ? issueList.size() : 0;
    }

    static class IssueViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvTitle, tvMeta, chipStatus, tvInitials, tvAssignee;
        View footer;
        MaterialButton btnAction;

        public IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvIssueId);
            tvTitle = itemView.findViewById(R.id.tvIssueTitle);
            tvMeta = itemView.findViewById(R.id.tvIssueMeta);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            footer = itemView.findViewById(R.id.llIssueFooter);
            tvInitials = itemView.findViewById(R.id.tvAssigneeInitials);
            tvAssignee = itemView.findViewById(R.id.tvAssignee);
            btnAction = itemView.findViewById(R.id.btnIssueAction);
        }
    }
}
