package com.example.taxiapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final List<Comment> commentsList;

    public CommentsAdapter(List<Comment> commentsList) {
        this.commentsList = commentsList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentsList.get(position);
        holder.passengerNameTextView.setText(comment.getPassengerName());
        holder.commentTextView.setText(comment.getText());
        holder.timestampTextView.setText(comment.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView passengerNameTextView;
        TextView commentTextView;
        TextView timestampTextView;

        public CommentViewHolder(View itemView) {
            super(itemView);
            passengerNameTextView = itemView.findViewById(R.id.passengerNameTextView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}
