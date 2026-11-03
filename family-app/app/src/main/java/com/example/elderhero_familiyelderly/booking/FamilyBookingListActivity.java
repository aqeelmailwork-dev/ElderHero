package com.example.elderhero_familiyelderly.booking;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_familiyelderly.DrawerHelper;
import com.example.elderhero_familiyelderly.R;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FamilyBookingListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BookingAdapter adapter;

    // allBookings holds the complete data from Firebase
    private List<Booking> allBookings = new ArrayList<>();
    // displayBookings holds only the data allowed by the current filter
    private List<Booking> displayBookings = new ArrayList<>();

    // Map to store 'Reviewed' status externally (BookingID -> Boolean)
    private Map<String, Boolean> reviewStatusMap = new HashMap<>();

    private DatabaseReference familyIndexRef;
    private DatabaseReference bookingsRef;
    private FirebaseAuth mAuth;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private ChipGroup chipGroupFilter;

    // Default filter
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_booking_list);

        // Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnDrawer);
        recyclerView = findViewById(R.id.recyclerBookingsFamily);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        // Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(displayBookings); // Pass the display list
        adapter.setCaretakerMode(false);
        recyclerView.setAdapter(adapter);

        // Setup Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String familyUID = mAuth.getCurrentUser().getUid();
        familyIndexRef = FirebaseDatabase.getInstance().getReference("FamilyElderlyBookings").child(familyUID);
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        // Setup Drawer
        DrawerHelper.setupDrawer(this, drawerLayout, false);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Setup Filter Listener
        setupFilters();

        // Load Data
        loadBookingsByIndex();
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentFilter = "All";
            } else if (checkedId == R.id.chipPending) {
                currentFilter = "Pending";
            } else if (checkedId == R.id.chipAccepted) {
                currentFilter = "Accepted";
            } else if (checkedId == R.id.chipRejected) {
                currentFilter = "Rejected";
            } else if (checkedId == R.id.chipCompleted) {
                currentFilter = "Completed";
            } else if (checkedId == R.id.chipUnreviewed) {
                currentFilter = "Unreviewed";
            } else {
                currentFilter = "All";
            }
            applyFilter();
        });
    }

    private void applyFilter() {
        displayBookings.clear();

        if (currentFilter.equals("All")) {
            displayBookings.addAll(allBookings);
        }
        else if (currentFilter.equals("Unreviewed")) {
            // Logic: Show items that are Completed AND NOT Reviewed
            for (Booking b : allBookings) {
                boolean isReviewed = reviewStatusMap.containsKey(b.BookingID) && Boolean.TRUE.equals(reviewStatusMap.get(b.BookingID));

                if ("Completed".equalsIgnoreCase(b.BookingStatus) && !isReviewed) {
                    displayBookings.add(b);
                }
            }
        }
        else if (currentFilter.equals("Completed")) {
            // Logic: Show ALL Completed items (Reviewed or not)
            for (Booking b : allBookings) {
                if ("Completed".equalsIgnoreCase(b.BookingStatus)) {
                    displayBookings.add(b);
                }
            }
        }
        else {
            // Strict string match for Pending, Accepted, Rejected
            for (Booking b : allBookings) {
                if (b.BookingStatus != null && b.BookingStatus.equalsIgnoreCase(currentFilter)) {
                    displayBookings.add(b);
                }
            }
        }

        // IMPORTANT: Pass the map to the adapter so it knows which button text to show
        adapter.setReviewStatusMap(reviewStatusMap);
        adapter.notifyDataSetChanged();
    }

    private void loadBookingsByIndex() {
        familyIndexRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    allBookings.clear();
                    reviewStatusMap.clear();
                    applyFilter();
                    return;
                }

                List<String> ids = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    ids.add(s.getKey());
                }

                if (ids.isEmpty()) {
                    allBookings.clear();
                    reviewStatusMap.clear();
                    applyFilter();
                    return;
                }

                // Temporary list to hold fetches before syncing
                fetchBookingDetails(ids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FamilyBookingListActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBookingDetails(List<String> ids) {
        // Clear main list to rebuild based on current index
        allBookings.clear();
        reviewStatusMap.clear();

        for (String bookingId : ids) {
            bookingsRef.child(bookingId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot bookSnap) {
                    if (bookSnap.exists()) {
                        Booking b = bookSnap.getValue(Booking.class);

                        // Manually fetch "Reviewed" since it's not in the Booking class
                        boolean isReviewed = false;
                        if (bookSnap.hasChild("Reviewed")) {
                            Boolean val = bookSnap.child("Reviewed").getValue(Boolean.class);
                            isReviewed = (val != null && val);
                        }

                        if (b != null) {
                            updateMasterList(b, isReviewed);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void updateMasterList(Booking b, boolean isReviewed) {
        // 1. Update the Map
        reviewStatusMap.put(b.BookingID, isReviewed);

        // 2. Update the List
        boolean updated = false;
        for (int i = 0; i < allBookings.size(); i++) {
            if (allBookings.get(i).BookingID.equals(b.BookingID)) {
                allBookings.set(i, b);
                updated = true;
                break;
            }
        }

        if (!updated) {
            allBookings.add(b);
        }

        // Sort by timestamp descending (newest first)
        Collections.sort(allBookings, (o1, o2) -> Long.compare(o2.BookingTimeStamp, o1.BookingTimeStamp));

        // Re-apply filter to update UI
        applyFilter();
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