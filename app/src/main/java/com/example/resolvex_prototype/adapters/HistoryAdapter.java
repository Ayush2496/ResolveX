package com.example.resolvex_prototype.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.models.IssueHistory;
import com.example.resolvex_prototype.utils.IssueStatusManager;
import com.example.resolvex_prototype.utils.TimeUtils;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<IssueHistory> historyList;

    public HistoryAdapter(Context context, List<IssueHistory> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        IssueHistory history = historyList.get(position);

        holder.tvStatus.setText(IssueStatusManager.getHumanReadableStatus(history.getNewStatus()));
        holder.tvNote.setText(history.getNote());

        // "Amit Patel \u00B7 30h ago"
        String who = history.getUpdatedByName() != null ? history.getUpdatedByName() : "";
        String when = TimeUtils.ago(history.getTimestamp()) + " ago";
        holder.tvUpdater.setText(who.isEmpty() ? when : who + " \u00B7 " + when);

        // The list is newest first: the latest entry gets the orange dot, the oldest has no line below it
        holder.dot.setBackgroundResource(position == 0 ? R.drawable.bg_rx_dot_accent : R.drawable.bg_rx_dot);
        holder.line.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvNote, tvUpdater;
        View dot, line;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
            dot = itemView.findViewById(R.id.ivHistoryDot);
            line = itemView.findViewById(R.id.viewHistoryLine);
            tvNote = itemView.findViewById(R.id.tvHistoryNote);
            tvUpdater = itemView.findViewById(R.id.tvHistoryUpdater);
        }
    }
}