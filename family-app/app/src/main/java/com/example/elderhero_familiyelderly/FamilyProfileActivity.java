package com.example.elderhero_familiyelderly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import android.widget.TextView;

public class FamilyProfileActivity extends AppCompatActivity {

    private static final int REQ_EDIT_PROFILE = 1010;

    private TextView familyName, familyEmail, familyPhone, familyRole, familyAge, familyGender;
    private CircleImageView profileImageView;
    private Button editProfileButton;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_profile);

        initViews();
        DrawerHelper.setupDrawer(this, drawerLayout, false);

        loadProfileData();

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditFamilyProfileActivity.class);
            startActivityForResult(intent, REQ_EDIT_PROFILE);
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnDrawer);

        familyName = findViewById(R.id.familyName);
        familyEmail = findViewById(R.id.familyEmail);
        familyPhone = findViewById(R.id.familyPhone);
        familyRole = findViewById(R.id.familyRole);
        familyAge = findViewById(R.id.familyAge);
        familyGender = findViewById(R.id.familyGender);

        profileImageView = findViewById(R.id.profileImageView);
        editProfileButton = findViewById(R.id.editProfileButton);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            dbRef = FirebaseDatabase.getInstance()
                    .getReference("FamilyElderly")
                    .child(mAuth.getCurrentUser().getUid());
        }
    }

    private void loadProfileData() {
        if (dbRef == null) return;

        dbRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "No profile data found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String username = nonNull(snapshot.child("FamilyElderlyName").getValue(String.class));
            String email = nonNull(snapshot.child("FamilyElderlyEmail").getValue(String.class));
            String phone = nonNull(snapshot.child("FamilyElderlyPhone").getValue(String.class));
            String role = nonNull(snapshot.child("FamilyElderlyType").getValue(String.class));
            String age = nonNull(snapshot.child("FamilyElderlyAge").getValue(String.class));
            String gender = nonNull(snapshot.child("FamilyElderlyGender").getValue(String.class));
            String imageUrl = snapshot.child("FamilyElderlyURL").getValue(String.class);

            familyName.setText(username);
            familyEmail.setText(email);
            familyPhone.setText(phone);
            familyRole.setText(role);
            familyAge.setText(age);
            familyGender.setText(gender);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.ic_person_placeholder);
            }

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private String nonNull(String v) {
        return (v != null && !v.trim().isEmpty()) ? v : "-";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_EDIT_PROFILE && resultCode == RESULT_OK) {
            loadProfileData();
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
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
}
