package com.example.elderhero_caretakers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CaretakerProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    private CircleImageView profileImageView;
    private Button editProfileButton;
    private TextView caretakerName, caretakerEmail, caretakerPhone, caretakerAge,
            caretakerGender, caretakerRace, caretakerLanguages, caretakerProfession, caretakerExperience;

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private ValueEventListener profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_profile);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        profileImageView = findViewById(R.id.profileImageView);
        editProfileButton = findViewById(R.id.editProfileButton);

        caretakerName = findViewById(R.id.caretakerName);
        caretakerEmail = findViewById(R.id.caretakerEmail);
        caretakerPhone = findViewById(R.id.caretakerPhone);
        caretakerAge = findViewById(R.id.caretakerAge);
        caretakerGender = findViewById(R.id.caretakerGender);
        caretakerRace = findViewById(R.id.caretakerRace);
        caretakerLanguages = findViewById(R.id.caretakerLanguages);
        caretakerProfession = findViewById(R.id.caretakerProfession);
        caretakerExperience = findViewById(R.id.caretakerExperience);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        new CaretakerDrawer(this, findViewById(R.id.leftDrawer), false);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance()
                .getReference("Caretakers")
                .child(mAuth.getCurrentUser().getUid());

        loadProfileData();

        editProfileButton.setOnClickListener(v -> {
            Intent i = new Intent(this, EditCaretakerProfileActivity.class);
            startActivity(i);
        });
    }

    private void loadProfileData() {
        profileListener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(CaretakerProfileActivity.this, "No profile data found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = snapshot.child("CaretakerName").getValue(String.class);
                String email = snapshot.child("CaretakerEmail").getValue(String.class);
                String phone = snapshot.child("CaretakerPhoneNo").getValue(String.class);
                String ageRange = snapshot.child("CaretakerAge").getValue(String.class);
                String gender = snapshot.child("CaretakerGender").getValue(String.class);
                String race = snapshot.child("CaretakerRace").getValue(String.class);
                String profession = snapshot.child("CaretakerProfession").getValue(String.class);
                String experience = snapshot.child("CaretakerExperience").getValue(String.class);
                String imageUrl = snapshot.child("CaretakerProfileUrl").getValue(String.class);

                // Handle language array
                List<String> langs = new ArrayList<>();
                DataSnapshot langSnap = snapshot.child("CaretakerLanguage");
                if (langSnap.exists()) {
                    for (DataSnapshot l : langSnap.getChildren()) {
                        String lang = l.getValue(String.class);
                        if (lang != null) langs.add(lang);
                    }
                }
                String languagesStr = langs.isEmpty() ? "-" : String.join(", ", langs);

                caretakerName.setText(getSafeText(name));
                caretakerEmail.setText(getSafeText(email));
                caretakerPhone.setText(getSafeText(phone));
                caretakerAge.setText(getSafeText(ageRange));
                caretakerGender.setText(getSafeText(gender));
                caretakerRace.setText(getSafeText(race));
                caretakerLanguages.setText(languagesStr);
                caretakerProfession.setText(getSafeText(profession));
                caretakerExperience.setText(getSafeText(experience));

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get().load(imageUrl)
                            .placeholder(R.drawable.ic_person_placeholder)
                            .error(R.drawable.ic_person_placeholder)
                            .into(profileImageView);
                } else {
                    profileImageView.setImageResource(R.drawable.ic_person_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CaretakerProfileActivity.this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSafeText(String value) {
        if (value == null || value.trim().isEmpty()) return "-";
        return value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && profileListener != null) {
            dbRef.removeEventListener(profileListener);
        }
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
