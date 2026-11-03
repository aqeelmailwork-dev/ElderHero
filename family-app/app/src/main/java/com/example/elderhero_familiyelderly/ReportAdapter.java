package com.example.elderhero_familiyelderly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private final List<Report> reports;

    public ReportAdapter(List<Report> reports) {
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Report r = reports.get(position);

        holder.tvBooking.setText("Booking ID: " + r.BookingID);
        holder.tvCategory.setText("Category: " + r.ReportCategory);
        holder.tvDetails.setText(r.ReportDetails);
        holder.tvStatus.setText("Status: " + r.ReportStatus);
        holder.tvReporter.setText("Reported by: " + r.ReporterRole);

        String time = new SimpleDateFormat("dd MMM yyyy, hh:mm a",
                Locale.getDefault()).format(new Date(r.ReportTime));

        holder.tvTime.setText("Reported on: " + time);

        if ("Resolved".equalsIgnoreCase(r.ReportStatus)) {
            holder.tvStatus.setTextColor(holder.tvStatus.getContext()
                    .getColor(android.R.color.holo_green_dark));
        } else if ("InReview".equalsIgnoreCase(r.ReportStatus)) {
            holder.tvStatus.setTextColor(holder.tvStatus.getContext()
                    .getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvStatus.setTextColor(holder.tvStatus.getContext()
                    .getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvBooking, tvCategory, tvStatus, tvDetails, tvTime, tvReporter;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBooking = itemView.findViewById(R.id.tvReportBooking);
            tvCategory = itemView.findViewById(R.id.tvReportCategory);
            tvStatus = itemView.findViewById(R.id.tvReportStatus);
            tvDetails = itemView.findViewById(R.id.tvReportDetails);
            tvTime = itemView.findViewById(R.id.tvReportTime);
            tvReporter = itemView.findViewById(R.id.tvReportReporter);
        }
    }
}
