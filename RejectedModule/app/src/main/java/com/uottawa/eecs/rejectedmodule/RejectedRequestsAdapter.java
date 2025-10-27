package com.uottawa.eecs.rejectedmodule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RejectedRequestsAdapter extends RecyclerView.Adapter<RejectedRequestsAdapter.ViewHolder> {

    private List<RejectedRequest> mRequestList;
    private OnApproveListener mListener;

    public RejectedRequestsAdapter(List<RejectedRequest> requestList, OnApproveListener listener) {
        mRequestList = requestList;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rejected, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RejectedRequest request = mRequestList.get(position);
        holder.tvUsername.setText(request.getUsername());
        holder.tvRole.setText("Role: " + request.getRole());
        holder.tvReason.setText("Reject Reason: " + request.getRejectReason());

        holder.btnApprove.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onApproveClick(request.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRequestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSelector, tvUsername, tvRole, tvReason;
        Button btnApprove;

        ViewHolder(View itemView) {
            super(itemView);
            tvSelector = itemView.findViewById(R.id.tv_selector);
            tvUsername = itemView.findViewById(R.id.tv_item_fullname);
            tvRole = itemView.findViewById(R.id.tv_item_role);
            tvReason = itemView.findViewById(R.id.tv_item_reject_reason);
            btnApprove = itemView.findViewById(R.id.btn_item_approve);
        }
    }

    public interface OnApproveListener {
        void onApproveClick(int requestId);
    }
}
