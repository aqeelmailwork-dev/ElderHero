package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class FamilyHomePreviewActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    // Runnable to handle the repeated arrow pulse animation
    private final Runnable arrowPulseRunnable = new Runnable() {
        private int pulseCount = 0;
        private static final int MAX_PULSES = 3; // Number of times the arrow moves

        @Override
        public void run() {
            if (pulseCount >= MAX_PULSES) {
                // Animation finished, stop the handler loop
                if (btnMenu != null) {
                    btnMenu.removeCallbacks(this);
                }
                return;
            }

            // Animate arrow out (to the right)
            if (btnMenu != null) {
                btnMenu.animate()
                        .translationX(20f) // Movement distance
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        TextView welcomeText = findViewById(R.id.welcomeText);

        welcomeText.setText("Welcome Family (Preview Mode)");

        // Menu button listener: stops animation, resets icon, and opens drawer
        btnMenu.setOnClickListener(v -> {
            // Stop any ongoing pulse animation
            btnMenu.removeCallbacks(arrowPulseRunnable);
            // Revert icon back to normal menu icon when clicked
            btnMenu.setImageResource(R.drawable.ic_menu);
            // Reset translation just in case it was caught mid-animation
            btnMenu.setTranslationX(0f);

            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Preview mode enabled
        DrawerHelper.setupDrawer(this, drawerLayout, true);

        // Implement repeated arrow pulse and instruction
        showNavigationGuidance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the Runnable when the activity is destroyed
        if (btnMenu != null) {
            btnMenu.removeCallbacks(arrowPulseRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            DrawerHelper.closeDrawer(drawerLayout);
        } else {
            super.onBackPressed();
        }
    }

    private void showNavigationGuidance() {
        // Change the menu icon to an arrow (Ensure R.drawable.ic_arrow_back exists)
        btnMenu.setImageResource(R.drawable.ic_arrow_back);

        // Start the repeated pulsing animation
        btnMenu.post(arrowPulseRunnable);

        // Show instruction via Toast
        Toast.makeText(this, "Click the arrow (top left) to navigate the preview.", Toast.LENGTH_LONG).show();
    }
}