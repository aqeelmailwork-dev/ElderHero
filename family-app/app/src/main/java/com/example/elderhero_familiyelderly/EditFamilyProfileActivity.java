package com.example.elderhero_familiyelderly;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditFamilyProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 3001;

    private EditText familyName, familyPhone, familyRole, familyAge, familyGender;
    private CircleImageView profileImage;
    private Button btnSave, btnCancel;

    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private Uri selectedImageUri = null;
    private String oldImageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_family_profile);

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
        familyName = findViewById(R.id.familyName);
        familyPhone = findViewById(R.id.familyPhone);
        familyRole = findViewById(R.id.familyRole);
        familyAge = findViewById(R.id.familyAge);
        familyGender = findViewById(R.id.familyGender);

        profileImage = findViewById(R.id.profileImageView);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("FamilyElderly")
                .child(mAuth.getCurrentUser().getUid());

        storageRef = FirebaseStorage.getInstance().getReference("user_profiles");
    }

    private void loadProfileData() {
        dbRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            familyName.setText(snapshot.child("FamilyElderlyName").getValue(String.class));
            familyPhone.setText(snapshot.child("FamilyElderlyPhone").getValue(String.class));
            familyRole.setText(snapshot.child("FamilyElderlyType").getValue(String.class));
            familyAge.setText(snapshot.child("FamilyElderlyAge").getValue(String.class));
            familyGender.setText(snapshot.child("FamilyElderlyGender").getValue(String.class));

            oldImageUrl = snapshot.child("FamilyElderlyURL").getValue(String.class);

            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                Picasso.get()
                        .load(oldImageUrl)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_person_placeholder);
            }
        });
    }

    private void saveChanges() {
        String name = familyName.getText().toString().trim();
        String phone = familyPhone.getText().toString().trim();
        String role = familyRole.getText().toString().trim();
        String age = familyAge.getText().toString().trim();
        String gender = familyGender.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Name and phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("FamilyElderlyName", name);
        updates.put("FamilyElderlyPhone", phone);
        updates.put("FamilyElderlyType", role);
        updates.put("FamilyElderlyAge", age);
        updates.put("FamilyElderlyGender", gender);

        if (selectedImageUri != null) {
            deleteOldAndUpload(updates);
        } else {
            updateDatabase(updates);
        }
    }

    private void deleteOldAndUpload(Map<String, Object> updates) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl)
                    .delete()
                    .addOnSuccessListener(a -> uploadImage(updates))
                    .addOnFailureListener(a -> uploadImage(updates));
        } else uploadImage(updates);
    }

    private void uploadImage(Map<String, Object> updates) {
        String uid = mAuth.getCurrentUser().getUid();
        StorageReference imgRef = storageRef.child(uid + "/profile.jpg");

        imgRef.putFile(selectedImageUri)
                .addOnSuccessListener(task ->
                        imgRef.getDownloadUrl().addOnSuccessListener(url -> {
                            updates.put("FamilyElderlyURL", url.toString());
                            updateDatabase(updates);
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateDatabase(Map<String, Object> updates) {
        dbRef.updateChildren(updates)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImage.setImageURI(selectedImageUri);
        }
    }
}
