package com.example.elderhero_familiyelderly;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.elderhero_familiyelderly.booking.FamilyBookingListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

public class DrawerHelper {

    public static void setupDrawer(Activity activity, DrawerLayout drawerLayout, boolean isPreviewMode) {

        TextView drawerName = drawerLayout.findViewById(R.id.drawerUserName);
        ImageView drawerImage = drawerLayout.findViewById(R.id.drawerUserImage);
        TextView logoutLabel = drawerLayout.findViewById(R.id.menuText);

        // Menu items
        View menuHome = drawerLayout.findViewById(R.id.menuHome);
        View menuProfile = drawerLayout.findViewById(R.id.menuProfile);
        View menuBooking = drawerLayout.findViewById(R.id.menuBooking);
        View menuJobs = drawerLayout.findViewById(R.id.menuJobs);
        View menuMessages = drawerLayout.findViewById(R.id.menuMessages);
        View menuReports = drawerLayout.findViewById(R.id.menuReports);

        if (isPreviewMode) {

            if (drawerName != null) drawerName.setText("Guest");
            if (drawerImage != null) drawerImage.setImageResource(R.drawable.ic_profile);
            if (logoutLabel != null) logoutLabel.setText("Login");

            // Only show: Home & Browse Caretakers
            menuHome.setVisibility(View.VISIBLE);
            menuJobs.setVisibility(View.VISIBLE);

            // Hide restricted menus
            menuProfile.setVisibility(View.GONE);
            menuBooking.setVisibility(View.GONE);
            menuMessages.setVisibility(View.GONE);
            menuReports.setVisibility(View.GONE);

            // Preview mode navigation
            setupPreviewNavigation(activity, drawerLayout);
            return;
        }

        // -------- Normal logged-in mode --------
        SharedPreferences prefs = activity.getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        String userImage = prefs.getString("userImage", null);

        if (drawerName != null) drawerName.setText(username);

        if (userImage != null && !userImage.isEmpty()) {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_profile).into(drawerImage);
        }

        setupRealNavigation(activity, drawerLayout);
    }

    private static void setupPreviewNavigation(Activity activity, DrawerLayout drawerLayout) {

        // Home → Navigate to Preview Home (Prevent loop if already there)
        drawerLayout.findViewById(R.id.menuHome).setOnClickListener(v -> {
            if (!(activity instanceof FamilyHomePreviewActivity)) {
                activity.startActivity(new Intent(activity, FamilyHomePreviewActivity.class));
            }
            closeDrawer(drawerLayout);
        });

        // Browse Caretaker → Browsing allowed in preview
        drawerLayout.findViewById(R.id.menuJobs).setOnClickListener(v -> {
            if (!(activity instanceof BrowseServicesActivity)) {
                activity.startActivity(new Intent(activity, BrowseServicesActivity.class));
            }
            closeDrawer(drawerLayout);
        });

        // Logout button becomes → Login
        drawerLayout.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            Intent loginIntent = new Intent(activity, FamilyLoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(loginIntent);
            activity.finish();
        });
    }

    private static void setupRealNavigation(Activity activity, DrawerLayout drawerLayout) {

        drawerLayout.findViewById(R.id.menuHome).setOnClickListener(v -> {
            if (!(activity instanceof FamilyHomeActivity)) {
                activity.startActivity(new Intent(activity, FamilyHomeActivity.class));
            }
            closeDrawer(drawerLayout);
        });

        drawerLayout.findViewById(R.id.menuProfile).setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, FamilyProfileActivity.class));
            closeDrawer(drawerLayout);
        });

        drawerLayout.findViewById(R.id.menuBooking).setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, FamilyBookingListActivity.class));
            closeDrawer(drawerLayout);
        });

        drawerLayout.findViewById(R.id.menuJobs).setOnClickListener(v -> {
            if (!(activity instanceof BrowseServicesActivity)) {
                activity.startActivity(new Intent(activity, BrowseServicesActivity.class));
            }
            closeDrawer(drawerLayout);
        });

        drawerLayout.findViewById(R.id.menuMessages).setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, FamilyMessagesActivity.class));
            closeDrawer(drawerLayout);
        });

        drawerLayout.findViewById(R.id.menuReports).setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, MyReportsActivity.class));
            closeDrawer(drawerLayout);
        });

        // -------- LOGOUT FIX (Full Logout) --------
        drawerLayout.findViewById(R.id.menuLogout).setOnClickListener(v -> {

            // Clear saved user session
            SharedPreferences prefs = activity.getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // Firebase logout (safe)
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}

            // Redirect to Login Screen & clear activity stack
            Intent intent = new Intent(activity, FamilyLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            activity.startActivity(intent);
            activity.finish();
        });
    }

    public static void closeDrawer(DrawerLayout drawerLayout) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}