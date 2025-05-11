package com.animetone.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private final List<NotificationItem> notificationList;

    public NotificationAdapter(List<NotificationItem> list) {
        this.notificationList = new ArrayList<>(list);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTimestamp;

        public ViewHolder(View view) {
            super(view);
            tvSender = view.findViewById(R.id.tvSender);
            tvMessage = view.findViewById(R.id.tvMessage);
            tvTimestamp = view.findViewById(R.id.tvTimestamp);
        }
    }

    @NonNull
    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);
        holder.tvSender.setText("+" + item.getSender());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTimestamp.setText(formatTime(item.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private String formatTime(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            java.util.Date date = input.parse(timestamp);

            SimpleDateFormat output = new SimpleDateFormat("d MMM yyyy, h:mm a");
            output.setTimeZone(TimeZone.getDefault());
            return output.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    // ✅ This is the key method to update your list efficiently
    public void updateData(List<NotificationItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return notificationList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // Use unique field like timestamp to identify items
                return notificationList.get(oldItemPosition).getTimestamp()
                        .equals(newList.get(newItemPosition).getTimestamp());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return notificationList.get(oldItemPosition).equals(newList.get(newItemPosition));
            }
        });

        notificationList.clear();
        notificationList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }
}

