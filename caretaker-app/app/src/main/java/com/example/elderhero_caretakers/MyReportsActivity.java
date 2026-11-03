package com.example.elderhero_caretakers;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

    private RecyclerView recyclerView;
    private CaretakerReportAdapter adapter;
    private List<Report> reportList;

    private DatabaseReference reportsRef;
    private String caretakerId;

    // Drawer Variables
    private DrawerLayout drawerLayout;
    private CaretakerDrawer caretakerDrawer;
    private ImageView btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        // 1. Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        recyclerView = findViewById(R.id.recyclerReports);

        // 2. Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportList = new ArrayList<>();
        adapter = new CaretakerReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        // 3. Setup Navigation Drawer
        View drawerView = findViewById(R.id.navigation_view);
        // Pass 'false' for preview mode since this is a real logged-in activity
        caretakerDrawer = new CaretakerDrawer(this, drawerView, false);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 4. Firebase Setup
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            caretakerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            reportsRef = FirebaseDatabase.getInstance().getReference("Reports");
            loadMyReports();
        } else {
            Toast.makeText(this, "Please log in to view reports.", Toast.LENGTH_SHORT).show();
        }
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

                        // Created by this caretaker AND role is Caretaker
                        if (caretakerId.equals(reporterId) &&
                                "Caretaker".equalsIgnoreCase(reporterRole)) {
                            reportList.add(report);
                        }
                    }
                }
                adapter.notifyDataSetChanged();

                if (reportList.isEmpty()) {
                    Toast.makeText(MyReportsActivity.this, "No reports found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyReportsActivity.this,
                        "Failed: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}