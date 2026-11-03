package com.example.elderhero_caretakers;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// --- CRITICAL IMPORT: Ensure this matches your package structure ---
import com.example.elderhero_caretakers.booking.Booking;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CaretakerHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private ImageView profileImage;
    private TextView welcomeText, tvPendingCount, tvAverageRating, tvUpcomingDate, tvUpcomingTime, tvAdCount;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    // Using Queries to filter data specifically for this Caretaker
    private Query bookingsQuery;
    private Query reviewsQuery;
    private ValueEventListener bookingsListener, adsListener, reviewsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_home);

        mAuth = FirebaseAuth.getInstance();
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        profileImage = findViewById(R.id.profileImage);
        welcomeText = findViewById(R.id.welcomeText);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvUpcomingDate = findViewById(R.id.tvUpcomingDate);
        tvUpcomingTime = findViewById(R.id.tvUpcomingTime);
        tvAdCount = findViewById(R.id.tvAdCount);

        profileImage.setImageResource(R.drawable.elderhero);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        View drawerView = findViewById(R.id.leftDrawer);
        new CaretakerDrawer(this, drawerView, false);

        loadCaretakerName();
        setupRealTimeDashboard();
    }

    private void loadCaretakerName() {
        if (mAuth.getCurrentUser() == null) {
            welcomeText.setText("Welcome, Guest");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Caretakers").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("CaretakerName").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            welcomeText.setText("Welcome, " + name);
                        } else {
                            welcomeText.setText("Welcome, Caretaker");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupRealTimeDashboard() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Bookings where CaretakerID matches current User
        bookingsQuery = rootRef.child("Bookings").orderByChild("CaretakerID").equalTo(uid);

        bookingsListener = bookingsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long pendingCount = 0;
                long now = System.currentTimeMillis();
                long nearestTime = Long.MAX_VALUE;
                String nearestDateStr = null;
                String nearestTimeStr = null;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (DataSnapshot data : snapshot.getChildren()) {
                    // Map data to Booking class
                    Booking booking = data.getValue(Booking.class);

                    if (booking != null) {
                        // Debugging: Check what the app is actually seeing
                        Log.d("Dashboard", "Booking Found: " + booking.BookingDate + " Status: " + booking.BookingStatus);

                        // --- PENDING COUNT ---
                        if ("Pending".equalsIgnoreCase(booking.BookingStatus)) {
                            pendingCount++;
                        }

                        // --- UPCOMING VISIT ---
                        // Only count "Accepted" visits as upcoming
                        if ("Accepted".equalsIgnoreCase(booking.BookingStatus)) {
                            String d = booking.BookingDate;
                            String t = booking.BookingTime;

                            if (t == null || t.isEmpty()) {
                                t = "00:00";
                            }

                            if (d != null) {
                                try {
                                    Date visitDate = sdf.parse(d + " " + t);
                                    if (visitDate != null) {
                                        long visitMillis = visitDate.getTime();
                                        // Logic: Must be in future (>= now) AND closer than the current 'nearest'
                                        if (visitMillis >= now && visitMillis < nearestTime) {
                                            nearestTime = visitMillis;
                                            nearestDateStr = d;
                                            nearestTimeStr = booking.BookingTime; // Display original time string
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("Dashboard", "Date Parse Error: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                tvPendingCount.setText(String.valueOf(pendingCount));

                if (nearestDateStr != null) {
                    tvUpcomingDate.setText(nearestDateStr);
                    tvUpcomingTime.setText(nearestTimeStr != null ? nearestTimeStr : "");
                } else {
                    tvUpcomingDate.setText("No visit scheduled");
                    tvUpcomingTime.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Dashboard", "Database Error: " + error.getMessage());
            }
        });


        DatabaseReference adsRef = rootRef.child("Advertisements").child(uid);
        adsListener = adsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvAdCount.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        reviewsQuery = rootRef.child("Reviews").orderByChild("CaretakerID").equalTo(uid);
        reviewsListener = reviewsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float total = 0;
                int count = 0;

                for (DataSnapshot s : snapshot.getChildren()) {
                    Object ratingObj = s.child("ReviewRating").getValue();
                    if (ratingObj != null) {
                        try {
                            float r = Float.parseFloat(ratingObj.toString());
                            total += r;
                            count++;
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                if (count == 0) {
                    tvAverageRating.setText("0.0");
                } else {
                    tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", total / count));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up all listeners
        if (bookingsQuery != null && bookingsListener != null) {
            bookingsQuery.removeEventListener(bookingsListener);
        }
        if (rootRef != null && mAuth.getCurrentUser() != null && adsListener != null) {
            rootRef.child("Advertisements").child(mAuth.getCurrentUser().getUid()).removeEventListener(adsListener);
        }
        if (reviewsQuery != null && reviewsListener != null) {
            reviewsQuery.removeEventListener(reviewsListener);
        }
    }
}