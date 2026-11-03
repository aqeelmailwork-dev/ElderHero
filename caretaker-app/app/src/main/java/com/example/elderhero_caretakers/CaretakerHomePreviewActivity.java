package com.example.elderhero_caretakers;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class CaretakerHomePreviewActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private TextView welcomeText, tvPendingCount, tvAverageRating, tvUpcomingDate, tvUpcomingTime, tvAdCount;
    private ImageView profileImage;

    // Runnable to handle the repeated arrow pulse animation
    private final Runnable arrowPulseRunnable = new Runnable() {
        private int pulseCount = 0;
        private static final int MAX_PULSES = 3; // Number of times the arrow moves

        @Override
        public void run() {
            if (pulseCount >= MAX_PULSES) {
                // Animation finished, stop the handler loop
                btnMenu.removeCallbacks(this);
                return;
            }

            // Animate arrow out (to the right)
            btnMenu.animate()
                    .translationX(20f) // Increased movement distance
                    .setDuration(150) // Faster movement
                    .withEndAction(() -> {
                        // Animate arrow back (to the left)
                        btnMenu.animate()
                                .translationX(0f)
                                .setDuration(150)
                                .withEndAction(() -> {
                                    // Schedule the next pulse after a small delay
                                    pulseCount++;
                                    if (pulseCount < MAX_PULSES) {
                                        btnMenu.postDelayed(this, 300); // 300ms pause between nudges
                                    }
                                })
                                .start();
                    })
                    .start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using the original, full dashboard XML as requested
        setContentView(R.layout.activity_caretaker_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        welcomeText = findViewById(R.id.welcomeText);
        profileImage = findViewById(R.id.profileImage);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvUpcomingDate = findViewById(R.id.tvUpcomingDate);
        tvUpcomingTime = findViewById(R.id.tvUpcomingTime);
        tvAdCount = findViewById(R.id.tvAdCount);

        profileImage.setImageResource(R.drawable.elderhero);

        // Menu button listener: opens the navigation drawer
        btnMenu.setOnClickListener(v -> {
            // Stop any ongoing pulse animation
            btnMenu.removeCallbacks(arrowPulseRunnable);
            // Revert icon back to normal menu icon when clicked
            btnMenu.setImageResource(R.drawable.ic_menu);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        View drawerView = findViewById(R.id.leftDrawer);
        // Initialize the drawer functionality (limited to Home/Post Ad in preview mode)
        new CaretakerDrawer(this, drawerView, true);

        welcomeText.setText("Welcome, Guest Caretaker");

        // Set dummy/zero data for all dashboard stats
        tvPendingCount.setText("0");
        tvAverageRating.setText("0.0");
        tvUpcomingDate.setText("No visit scheduled");
        tvUpcomingTime.setText("");
        tvAdCount.setText("0");

        // GUIDANCE STEP: Implement repeated arrow pulse and instruction
        showNavigationGuidance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Crucial: Clean up the Runnable when the activity is destroyed
        if (btnMenu != null) {
            btnMenu.removeCallbacks(arrowPulseRunnable);
        }
    }

    private void showNavigationGuidance() {
        // Change the menu icon to an arrow (R.drawable.ic_arrow_back must exist)
        btnMenu.setImageResource(R.drawable.ic_arrow_back);

        // Start the repeated pulsing animation
        btnMenu.post(arrowPulseRunnable);

        // Show instruction via Toast
        Toast.makeText(this, "Click the arrow (top left) to navigate the preview.", Toast.LENGTH_LONG).show();
    }
}