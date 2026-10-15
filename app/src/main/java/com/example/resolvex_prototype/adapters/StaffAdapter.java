package com.example.resolvex_prototype.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.UiUtils;

import java.util.List;
import java.util.Map;

/**
 * The people list of the admin "Manage" screen. Staff show how much open work they have
 * (orange when it is a lot), students show their academic year.
 */
public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    /** From this many open issues a staff member's count is shown in orange. */
    private static final int HIGH_WORKLOAD = 8;

    private final Context context;
    private final List<User> users;
    private OnUserClickListener clickListener;
    private Map<Integer, Integer> workloads;

    public StaffAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    public void setOnUserClickListener(OnUserClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /** Open issues per staff user id (see DatabaseHelper.getStaffWorkloads). */
    public void setWorkloads(Map<Integer, Integer> workloads) {
        this.workloads = workloads;
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        User user = users.get(position);

        holder.tvName.setText(user.getName());
        holder.tvInitials.setText(UiUtils.initials(user.getName()));
        String dept = user.getDepartment() != null ? user.getDepartment() : "";

        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            holder.tvDepartment.setText(dept);
            holder.tvBadge.setText("Year " + user.getYear());
            holder.tvBadge.setTextColor(context.getColor(R.color.rx_muted));
        } else {
            holder.tvDepartment.setText(dept.isEmpty() ? "General Staff" : dept);
            Integer open = workloads != null ? workloads.get(user.getUserId()) : null;
            int count = open == null ? 0 : open;
            holder.tvBadge.setText(count + " active");
            holder.tvBadge.setTextColor(context.getColor(count >= HIGH_WORKLOAD ? R.color.rx_accent : R.color.rx_muted));
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvDepartment, tvBadge;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvUserInitials);
            tvName = itemView.findViewById(R.id.tvStaffName);
            tvDepartment = itemView.findViewById(R.id.tvStaffDepartment);
            tvBadge = itemView.findViewById(R.id.tvUserBadge);
        }
    }
}
