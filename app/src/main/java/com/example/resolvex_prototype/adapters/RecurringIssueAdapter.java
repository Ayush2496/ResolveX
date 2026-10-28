package com.example.resolvex_prototype.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.models.RecurringIssueGroup;

import java.util.List;

public class RecurringIssueAdapter extends RecyclerView.Adapter<RecurringIssueAdapter.RecurringViewHolder> {

    public interface OnRecurringClickListener {
        void onRecurringClick(RecurringIssueGroup group);
    }

    private final Context context;
    private final List<RecurringIssueGroup> recurringList;
    private final OnRecurringClickListener listener;

    public RecurringIssueAdapter(Context context, List<RecurringIssueGroup> recurringList, OnRecurringClickListener listener) {
        this.context = context;
        this.recurringList = recurringList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecurringViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recurring_issue, parent, false);
        return new RecurringViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecurringViewHolder holder, int position) {
        RecurringIssueGroup group = recurringList.get(position);

        holder.tvCategory.setText(group.getCategory());
        holder.tvLocation.setText(group.getLocation());
        holder.tvCount.setText(group.getReportCount() + " reports");
        UiUtils.bindPill(holder.chipTrend, "Trend", R.color.rx_pink_bg, R.color.rx_pink_fg);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecurringClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recurringList != null ? recurringList.size() : 0;
    }

    static class RecurringViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvLocation, tvCount, chipTrend;

        public RecurringViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvRecurringCategory);
            tvLocation = itemView.findViewById(R.id.tvRecurringLocation);
            tvCount = itemView.findViewById(R.id.tvRecurringCount);
            chipTrend = itemView.findViewById(R.id.chipTrend);
        }
    }
}