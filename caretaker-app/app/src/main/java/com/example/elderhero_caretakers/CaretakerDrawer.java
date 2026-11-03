package com.example.elderhero_caretakers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.example.elderhero_caretakers.advertisement.AdvertiseServiceActivity;
import com.example.elderhero_caretakers.advertisement.AdvertiseServicePreviewActivity;
import com.example.elderhero_caretakers.advertisement.MyAdvertisementsActivity;
import com.example.elderhero_caretakers.authenticate.LoginActivity;
import com.example.elderhero_caretakers.booking.CaretakerBookingListActivity;
import com.example.elderhero_caretakers.chats.CaretakerMessagesActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CaretakerDrawer {

    private final Activity activity;
    private final View drawerView;
    private final FirebaseAuth mAuth;
    private final boolean isPreview;

    public CaretakerDrawer(Activity activity, View drawerView, boolean isPreview) {
        this.activity = activity;
        this.drawerView = drawerView;
        this.mAuth = FirebaseAuth.getInstance();
        this.isPreview = isPreview;
        setupDrawer();
    }

    private void setupDrawer() {
        LinearLayout menuHome = drawerView.findViewById(R.id.menuHome);
        LinearLayout menuProfile = drawerView.findViewById(R.id.menuProfile);
        LinearLayout menuSubmit = drawerView.findViewById(R.id.menuSubmit);
        LinearLayout menuAdvertisement = drawerView.findViewById(R.id.menuAdvertisement);
        LinearLayout subMenuAdvertisement = drawerView.findViewById(R.id.subMenuAdvertisement);
        TextView subMenuAdvertiseService = drawerView.findViewById(R.id.subMenuAdvertiseService);
        TextView subMenuViewAds = drawerView.findViewById(R.id.subMenuViewAds);
        LinearLayout menuBookings = drawerView.findViewById(R.id.menuBookings);
        LinearLayout menuMessages = drawerView.findViewById(R.id.menuMessages);
        LinearLayout menuMyReports = drawerView.findViewById(R.id.menuMyReports);
        LinearLayout menuLogout = drawerView.findViewById(R.id.menuLogout);
        ImageView profileImage = drawerView.findViewById(R.id.drawerProfileImage);
        TextView profileName = drawerView.findViewById(R.id.drawerProfileName);
        TextView menuLogoutText = drawerView.findViewById(R.id.menuLogoutText);

        // ------------------ PREVIEW MODE --------------------
        if (isPreview) {
            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            profileName.setText("Guest (Preview)");
            menuLogoutText.setText("Log In");

            menuProfile.setVisibility(View.GONE);
            menuSubmit.setVisibility(View.GONE);
            menuBookings.setVisibility(View.GONE);
            menuMessages.setVisibility(View.GONE);
            menuMyReports.setVisibility(View.GONE);
            subMenuAdvertisement.setVisibility(View.GONE);

            menuHome.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, CaretakerHomePreviewActivity.class)));

            menuAdvertisement.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, AdvertiseServicePreviewActivity.class)));

            menuLogout.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, LoginActivity.class)));

            return;
        }

        // ------------------ NORMAL LOGGED-IN MODE --------------------
        if (mAuth.getCurrentUser() != null) {
            loadCaretakerInfo(profileImage, profileName);
            menuLogoutText.setText("Log Out");

            // 🔥 FULL SECURE LOGOUT
            menuLogout.setOnClickListener(v -> {

                // 1️⃣ Clear saved user session
                SharedPreferences prefs = activity.getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);
                prefs.edit().clear().apply();

                // 2️⃣ Firebase logout
                try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}

                // 3️⃣ Redirect and clear history
                Intent intent = new Intent(activity, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);

                activity.startActivity(intent);
                activity.finish();
            });

        } else {
            // Should not happen but added as safety fallback
            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            profileName.setText("Guest");
            menuLogoutText.setText("Log In");

            menuLogout.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, LoginActivity.class)));
        }

        // ----------- Navigation & Permission-Controlled Menus --------------
        menuAdvertisement.setOnClickListener(v -> {
            checkApprovalStatus(isApproved -> {
                if (isApproved) {
                    TransitionManager.beginDelayedTransition((LinearLayout) drawerView, new AutoTransition());
                    subMenuAdvertisement.setVisibility(
                            subMenuAdvertisement.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
                    );
                } else {
                    showRestrictionDialog();
                }
            });
        });

        menuHome.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, CaretakerHomeActivity.class)));

        subMenuAdvertiseService.setOnClickListener(v ->
                checkApprovalStatus(isApproved -> {
                    if (isApproved)
                        activity.startActivity(new Intent(activity, AdvertiseServiceActivity.class));
                    else showRestrictionDialog();
                }));

        subMenuViewAds.setOnClickListener(v ->
                checkApprovalStatus(isApproved -> {
                    if (isApproved)
                        activity.startActivity(new Intent(activity, MyAdvertisementsActivity.class));
                    else showRestrictionDialog();
                }));

        menuProfile.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, CaretakerProfileActivity.class)));

        menuSubmit.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, SubmissionActivity.class)));

        menuBookings.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, CaretakerBookingListActivity.class)));

        menuMessages.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, CaretakerMessagesActivity.class)));

        menuMyReports.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, MyReportsActivity.class)));
    }


    private void loadCaretakerInfo(ImageView profileImage, TextView profileName) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        DatabaseReference caretakerRef = FirebaseDatabase.getInstance()
                .getReference("Caretakers").child(uid);

        caretakerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("CaretakerName").getValue(String.class);
                String imageUrl = snapshot.child("CaretakerProfileUrl").getValue(String.class);

                profileName.setText(name != null ? name : "Caretaker");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(activity).load(imageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ======================== APPROVAL CHECK =========================
    private interface ApprovalCallback { void onResult(boolean isApproved); }

    private void checkApprovalStatus(ApprovalCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onResult(false);
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference subRef = FirebaseDatabase.getInstance().getReference("Submissions");

        subRef.orderByChild("CaretakerID").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            callback.onResult(false);
                            return;
                        }

                        String latestStatus = "Pending";
                        long newestTimestamp = Long.MIN_VALUE;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String date = child.child("SubmissionDate").getValue(String.class);
                            String time = child.child("SubmissionTime").getValue(String.class);
                            String status = child.child("SubmissionStatus").getValue(String.class);

                            long timestamp = 0;
                            try {
                                if (date != null && time != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    Date d = sdf.parse(date + " " + time);
                                    timestamp = d != null ? d.getTime() : 0;
                                }
                            } catch (Exception ignored) {}

                            if (timestamp > newestTimestamp) {
                                newestTimestamp = timestamp;
                                latestStatus = status;
                            }
                        }

                        callback.onResult("Approved".equalsIgnoreCase(latestStatus));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(activity,"Error checking approval",Toast.LENGTH_SHORT).show();
                        callback.onResult(false);
                    }
                });
    }

    // ======================== RESTRICTION DIALOG =========================
    private void showRestrictionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Access Restricted")
                .setMessage("You cannot post advertisements until admin approves your submission.")
                .setCancelable(false)
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Upload Now", (dialog, which) -> {
                    activity.startActivity(new Intent(activity, SubmissionActivity.class));
                    dialog.dismiss();
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#5A3D2B"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.DKGRAY);
    }
}
