package com.example.elderhero_caretakers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.VH> {

    private final List<Submission> list;
    private final Context ctx;
    private final DatabaseReference db = FirebaseDatabase.getInstance().getReference();

    public SubmissionAdapter(List<Submission> list, Context ctx) {
        this.list = list;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_submission, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Submission s = list.get(position);

        holder.tvId.setText(safe(s.SubmissionID));
        holder.tvDate.setText(safe(s.SubmissionDate));
        holder.tvTime.setText(safe(s.SubmissionTime));
        holder.tvRemark.setText(isEmpty(s.SubmissionRemark) ? "No remarks." : s.SubmissionRemark);

        //  Status Styling
        String status = safe(s.SubmissionStatus);
        holder.tvStatus.setText(status);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(50f);

        if (status.equalsIgnoreCase("Approved")) {
            shape.setColor(Color.parseColor("#43A047")); // Green
        } else if (status.equalsIgnoreCase("Rejected")) {
            shape.setColor(Color.parseColor("#E53935")); // Red
        } else {
            shape.setColor(Color.parseColor("#FFB300")); // Amber/Yellow
        }
        holder.tvStatus.setBackground(shape);

        //  Admin Name Fetching
        if (isEmpty(s.AdminID)) {
            holder.tvAdmin.setText("Reviewed by: Pending");
        } else {
            db.child("Admins").child(s.AdminID).child("AdminName")
                    .get().addOnSuccessListener(nameSnap -> {
                        String name = nameSnap.exists() ? nameSnap.getValue(String.class) : "Admin";
                        holder.tvAdmin.setText("Reviewed by: " + name);
                    }).addOnFailureListener(e -> holder.tvAdmin.setText("Reviewed by: Admin"));
        }

        //  Button Logic
        holder.btnOpen.setOnClickListener(v -> {
            if (!isEmpty(s.SubmissionURL)) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(s.SubmissionURL));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    ctx.startActivity(i);
                } catch (Exception e) {
                    // Handle case where no PDF viewer is installed
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        // Updated to remove tvDateTime and add tvDate, tvTime
        TextView tvId, tvStatus, tvDate, tvTime, tvRemark, tvAdmin;
        Button btnOpen;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvSubmissionId);
            tvStatus = itemView.findViewById(R.id.tvSubmissionStatus);
            tvDate = itemView.findViewById(R.id.tvSubmissionDate);
            tvTime = itemView.findViewById(R.id.tvSubmissionTime);
            tvRemark = itemView.findViewById(R.id.tvSubmissionRemark);
            tvAdmin = itemView.findViewById(R.id.tvSubmissionAdmin);
            btnOpen = itemView.findViewById(R.id.btnOpenSubmission);
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }
}