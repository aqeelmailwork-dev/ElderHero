package com.example.elderhero_caretakers.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_caretakers.CaretakerDrawer;
import com.example.elderhero_caretakers.R;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class CaretakerBookingListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    BookingAdapter adapter;

    // Maintain two lists: one for all data, one for the visible filtered data
    List<Booking> allBookings = new ArrayList<>();
    List<Booking> displayedBookings = new ArrayList<>();

    DrawerLayout drawerLayout;
    ImageButton btnMenu;
    ChipGroup chipGroupFilter;

    // Default filter
    String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_booking_list);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        recyclerView = findViewById(R.id.recyclerBookings);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        // UI Setup
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        View drawerView = findViewById(R.id.leftDrawer);
        if(drawerView != null) {
            new CaretakerDrawer(this, drawerView, false);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with the displayed list
        adapter = new BookingAdapter(displayedBookings);
        adapter.setCaretakerMode(true);
        recyclerView.setAdapter(adapter);

        // Setup Filter Listeners
        setupFilters();

        // Load Data
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUid != null) {
            loadBookings(currentUid);
        }
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentFilter = "All";
            else if (checkedId == R.id.chipPending) currentFilter = "Pending";
            else if (checkedId == R.id.chipAccepted) currentFilter = "Accepted";
            else if (checkedId == R.id.chipRejected) currentFilter = "Rejected";
            else if (checkedId == R.id.chipCompleted) currentFilter = "Completed";
            else if (checkedId == R.id.chipUnreviewed) currentFilter = "Unreviewed";

            filterList();
        });
    }

    private void loadBookings(String caretakerID) {
        DatabaseReference caretIndexRef = FirebaseDatabase.getInstance()
                .getReference("CaretakerBookings").child(caretakerID);
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        caretIndexRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBookings.clear();
                if (!snapshot.exists()) {
                    filterList(); // Update empty state
                    return;
                }

                List<String> ids = new ArrayList<>();
                for (DataSnapshot s : snapshot.getChildren()) ids.add(s.getKey());

                if (ids.isEmpty()) {
                    filterList();
                    return;
                }

                final int[] loadedCount = {0};
                int total = ids.size();

                for (String bid : ids) {
                    bookingsRef.child(bid).get().addOnSuccessListener(s -> {
                        Booking b = s.getValue(Booking.class);
                        if (b != null) allBookings.add(b);

                        loadedCount[0]++;
                        if (loadedCount[0] == total) {
                            // Data loading complete, apply sort and filter
                            allBookings.sort((a, b2) -> Long.compare(b2.BookingTimeStamp, a.BookingTimeStamp));
                            filterList();
                        }
                    }).addOnFailureListener(e -> {
                        loadedCount[0]++;
                        if (loadedCount[0] == total) filterList();
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterList() {
        displayedBookings.clear();

        if (currentFilter.equals("All")) {
            displayedBookings.addAll(allBookings);
            adapter.notifyDataSetChanged();
            return;
        }

        // Special handling for "Unreviewed"
        if (currentFilter.equals("Unreviewed")) {
            filterUnreviewed();
            return;
        }

        // Standard string filtering
        for (Booking b : allBookings) {
            if (b.BookingStatus != null && b.BookingStatus.equalsIgnoreCase(currentFilter)) {
                displayedBookings.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterUnreviewed() {
        // First find all Completed bookings
        List<Booking> completedBookings = new ArrayList<>();
        for (Booking b : allBookings) {
            if ("Completed".equalsIgnoreCase(b.BookingStatus)) {
                completedBookings.add(b);
            }
        }

        if (completedBookings.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        // Check Reviews node for each
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews");
        final int[] checkedCount = {0};
        int totalToCheck = completedBookings.size();

        for (Booking b : completedBookings) {
            reviewsRef.child(b.BookingID).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    // No review exists -> Add to list
                    displayedBookings.add(b);
                }
                checkedCount[0]++;
                if (checkedCount[0] == totalToCheck) {
                    // Maintain sort order
                    displayedBookings.sort((a, b2) -> Long.compare(b2.BookingTimeStamp, a.BookingTimeStamp));
                    adapter.notifyDataSetChanged();
                }
            }).addOnFailureListener(e -> {
                checkedCount[0]++;
                if (checkedCount[0] == totalToCheck) adapter.notifyDataSetChanged();
            });
        }
    }
}