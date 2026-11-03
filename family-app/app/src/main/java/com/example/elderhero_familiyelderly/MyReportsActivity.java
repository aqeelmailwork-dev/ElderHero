package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MyReportsActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageButton btnDrawer;

    RecyclerView recyclerView;
    ReportAdapter adapter;
    List<Report> reportList;

    DatabaseReference reportsRef;
    String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnDrawer = findViewById(R.id.btnDrawer);

        DrawerHelper.setupDrawer(this, drawerLayout, false);

        btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        recyclerView = findViewById(R.id.recyclerReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reportsRef = FirebaseDatabase.getInstance().getReference("Reports");

        loadMyReports();
    }

    private void loadMyReports() {

        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                reportList.clear();

                for (DataSnapshot bookingSnap : snapshot.getChildren()) {

                    for (DataSnapshot reportSnap : bookingSnap.getChildren()) {

                        Report report = reportSnap.getValue(Report.class);
                        if (report == null) continue;

                        String reporterId = report.ReporterID;
                        String reporterRole = report.ReporterRole;

                        if (currentUserId.equals(reporterId) &&
                                ("Family".equalsIgnoreCase(reporterRole) ||
                                        "Elderly".equalsIgnoreCase(reporterRole))) {

                            reportList.add(report);
                        }
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyReportsActivity.this,
                        "Failed to load reports: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
