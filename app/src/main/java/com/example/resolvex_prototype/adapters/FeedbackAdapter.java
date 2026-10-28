package com.example.resolvex_prototype.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.models.Feedback;

import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {

    private final Context context;
    private final List<Feedback> feedbackList;

    public FeedbackAdapter(Context context, List<Feedback> feedbackList) {
        this.context = context;
        this.feedbackList = feedbackList;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        Feedback f = feedbackList.get(position);

        holder.tvTitle.setText(f.getIssueTitle() != null ? f.getIssueTitle() : "Issue #" + f.getIssueId());
        holder.tvRating.setText("★ " + f.getRating() + "/5");
        holder.tvComment.setText(f.getComment() != null && !f.getComment().isEmpty() ? f.getComment() : "(No written comment)");
        holder.tvDate.setText(f.getCreatedAt());
    }

    @Override
    public int getItemCount() {
        return feedbackList != null ? feedbackList.size() : 0;
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvRating, tvComment, tvDate;

        public FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFeedbackTitle);
            tvRating = itemView.findViewById(R.id.tvFeedbackRating);
            tvComment = itemView.findViewById(R.id.tvFeedbackComment);
            tvDate = itemView.findViewById(R.id.tvFeedbackDate);
        }
    }
}