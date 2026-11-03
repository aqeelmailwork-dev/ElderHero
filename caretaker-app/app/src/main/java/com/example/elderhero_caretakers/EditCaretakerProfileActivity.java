package com.example.elderhero_caretakers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import com.squareup.picasso.Picasso;

import java.util.*;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditCaretakerProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 2001;

    private EditText caretakerName, caretakerPhone, caretakerExperience, caretakerRace,
            caretakerGender, caretakerLanguages, caretakerProfession, caretakerAge;
    private CircleImageView profileImage;
    private Button btnSave, btnCancel;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private Uri selectedImageUri = null;
    private String oldImageUrl = null; // Keep track of previous image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_caretaker_profile);

        initViews();
        loadProfileData();

        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> finish());

        profileImage.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK);
            pick.setType("image/*");
            startActivityForResult(pick, PICK_IMAGE);
        });
    }

    private void initViews() {
        caretakerName = findViewById(R.id.caretakerName);
        caretakerPhone = findViewById(R.id.caretakerPhone);
        caretakerExperience = findViewById(R.id.caretakerExperience);
        caretakerRace = findViewById(R.id.caretakerRace);
        caretakerGender = findViewById(R.id.caretakerGender);
        caretakerLanguages = findViewById(R.id.caretakerLanguages);
        caretakerProfession = findViewById(R.id.caretakerProfession);
        caretakerAge = findViewById(R.id.caretakerAge);
        profileImage = findViewById(R.id.profileImageView);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance()
                .getReference("Caretakers")
                .child(mAuth.getCurrentUser().getUid());
        storageRef = FirebaseStorage.getInstance().getReference("caretaker_profiles");
    }

    private void loadProfileData() {
        dbRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(this, "No profile data found.", Toast.LENGTH_SHORT).show();
                return;
            }

            caretakerName.setText(nonNull(snapshot.child("CaretakerName").getValue(String.class)));
            caretakerPhone.setText(nonNull(snapshot.child("CaretakerPhoneNo").getValue(String.class)));
            caretakerExperience.setText(nonNull(snapshot.child("CaretakerExperience").getValue(String.class)));
            caretakerRace.setText(nonNull(snapshot.child("CaretakerRace").getValue(String.class)));
            caretakerGender.setText(nonNull(snapshot.child("CaretakerGender").getValue(String.class)));
            caretakerProfession.setText(nonNull(snapshot.child("CaretakerProfession").getValue(String.class)));
            caretakerAge.setText(nonNull(snapshot.child("CaretakerAge").getValue(String.class)));

            // Load languages (CaretakerLanguage -> List)
            List<String> langList = new ArrayList<>();
            DataSnapshot langSnapshot = snapshot.child("CaretakerLanguage");
            if (langSnapshot.exists()) {
                for (DataSnapshot l : langSnapshot.getChildren()) {
                    String lang = l.getValue(String.class);
                    if (lang != null) langList.add(lang);
                }
            }
            String languages = langList.isEmpty() ? "" : String.join(", ", langList);
            caretakerLanguages.setText(languages);

            // Save current image URL for deletion later
            oldImageUrl = snapshot.child("CaretakerProfileUrl").getValue(String.class);
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                Picasso.get()
                        .load(oldImageUrl)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_person_placeholder);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void saveChanges() {
        String name = caretakerName.getText().toString().trim();
        String phone = caretakerPhone.getText().toString().trim();
        String experience = caretakerExperience.getText().toString().trim();
        String race = caretakerRace.getText().toString().trim();
        String gender = caretakerGender.getText().toString().trim();
        String languagesRaw = caretakerLanguages.getText().toString().trim();
        String profession = caretakerProfession.getText().toString().trim();
        String age = caretakerAge.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Name and phone are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("CaretakerName", name);
        updates.put("CaretakerPhoneNo", phone);
        updates.put("CaretakerExperience", experience);
        updates.put("CaretakerRace", race);
        updates.put("CaretakerGender", gender);
        updates.put("CaretakerProfession", profession);
        updates.put("CaretakerAge", age);

        // Convert languages to List<String>
        List<String> langs = new ArrayList<>();
        if (!languagesRaw.isEmpty()) {
            String[] parts = languagesRaw.split(",");
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) langs.add(t);
            }
        }
        updates.put("CaretakerLanguage", langs);

        if (selectedImageUri != null) {
            deleteOldAndUpload(updates); // delete previous image if present then upload new
        } else {
            updateDatabase(updates);
        }
    }

    /** Delete previous image if exists, then upload new one */
    private void deleteOldAndUpload(Map<String, Object> updates) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl)
                        .delete()
                        .addOnSuccessListener(aVoid -> uploadImageAndSave(updates))
                        .addOnFailureListener(e -> {
                            // proceed with upload even if deletion fails
                            uploadImageAndSave(updates);
                        });
            } catch (Exception ex) {
                // invalid URL or other issue — proceed to upload
                uploadImageAndSave(updates);
            }
        } else {
            uploadImageAndSave(updates);
        }
    }

    /** Upload image to caretaker_profiles/<uid>/profile.jpg */
    private void uploadImageAndSave(Map<String, Object> updates) {
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference ref = storageRef.child(uid + "/profile.jpg");

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(url -> {
                    updates.put("CaretakerProfileUrl", url.toString());
                    updateDatabase(updates);
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Update Realtime Database fields */
    private void updateDatabase(Map<String, Object> updates) {
        dbRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Handle selected image from gallery */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }

    private String nonNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : "";
    }
}
