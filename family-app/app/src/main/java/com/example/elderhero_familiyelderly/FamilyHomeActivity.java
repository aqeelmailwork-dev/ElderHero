package com.example.elderhero_familiyelderly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FamilyHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    private TextView welcomeText, tvPendingCount, tvUpcomingDate, tvUpcomingTime,
            tvUnreviewedCount, tvCompletedVisitsCount;

    FirebaseAuth mAuth;
    DatabaseReference bookingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);

        welcomeText = findViewById(R.id.welcomeText);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvUpcomingDate = findViewById(R.id.tvUpcomingDate);
        tvUpcomingTime = findViewById(R.id.tvUpcomingTime);
        tvUnreviewedCount = findViewById(R.id.tvUnreviewedCount);
        tvCompletedVisitsCount = findViewById(R.id.tvCompletedVisitsCount);

        mAuth = FirebaseAuth.getInstance();

        loadNameForWelcome();
        DrawerHelper.setupDrawer(this, drawerLayout, false);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        setupDashboardListeners();
    }

    private void loadNameForWelcome() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String name = prefs.getString("username", "User");
        welcomeText.setText("Welcome, " + name);
    }

    private void setupDashboardListeners() {

        String uid = mAuth.getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                long pending = 0;
                long completed = 0;
                long unreviewed = 0;

                long now = System.currentTimeMillis();
                long nearest = Long.MAX_VALUE;
                String nDate = null, nTime = null;

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (DataSnapshot s : snapshot.getChildren()) {

                    String familyID = s.child("FamiliyElderlyID").getValue(String.class);
                    // Ensure we only count for the current logged-in family user
                    if (familyID == null || !familyID.equals(uid)) continue;

                    String status = s.child("BookingStatus").getValue(String.class);
                    String date = s.child("BookingDate").getValue(String.class);
                    String time = s.child("BookingTime").getValue(String.class);

                    // FIX: Changed key to "Reviewed" (Capital R) and added null safety
                    Boolean reviewedObj = s.child("Reviewed").getValue(Boolean.class);
                    boolean isReviewed = (reviewedObj != null && reviewedObj);

                    // Logic for Pending
                    if ("Pending".equalsIgnoreCase(status)) {
                        pending++;
                    }

                    // Logic for Accepted (Find Nearest)
                    if ("Accepted".equalsIgnoreCase(status)) {
                        try {
                            if (date != null && time != null) {
                                Date parsed = df.parse(date + " " + time);

                                if (parsed != null &&
                                        parsed.getTime() >= now &&
                                        parsed.getTime() < nearest) {

                                    nearest = parsed.getTime();
                                    nDate = date;
                                    nTime = time;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // FIX: Logic for Completed & Unreviewed
                    if ("Completed".equalsIgnoreCase(status)) {
                        completed++; // Increment Total Completed Count

                        if (!isReviewed) {
                            unreviewed++; // Increment ONLY if not reviewed yet
                        }
                    }
                }

                // Update UI
                tvPendingCount.setText(String.valueOf(pending));
                tvCompletedVisitsCount.setText(String.valueOf(completed));
                tvUnreviewedCount.setText(String.valueOf(unreviewed));

                if (nDate != null) {
                    tvUpcomingDate.setText(nDate);
                    tvUpcomingTime.setText(nTime);
                } else {
                    tvUpcomingDate.setText("No visit scheduled");
                    tvUpcomingTime.setText("");
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            DrawerHelper.closeDrawer(drawerLayout);
        } else {
            super.onBackPressed();
        }
    }
}