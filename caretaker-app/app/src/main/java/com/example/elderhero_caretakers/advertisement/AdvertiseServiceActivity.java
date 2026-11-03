package com.example.elderhero_caretakers.advertisement;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.elderhero_caretakers.CaretakerDrawer;
import com.example.elderhero_caretakers.R;
// Make sure to import your EditAdvertismentActivity if it's in a different package
// import com.example.elderhero_caretakers.EditAdvertismentActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdvertiseServiceActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 10;

    private Spinner spinnerCategory, spinnerAvailability;
    private LinearLayout layoutSubServices;
    private EditText rateMinField, rateMaxField, locationField;
    private ImageView pickImageView;
    private Button submitBtn;
    private Uri imageUri;

    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private DatabaseReference adsRef;
    private StorageReference storageRef;
    private DatabaseReference catRef;

    // Maps to hold ID and data
    private final Map<String, String> categoryIdMap = new HashMap<>();
    private final Map<String, List<SubItem>> categorySubs = new HashMap<>();
    private final List<String> categoryNamesList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    private static class SubItem {
        String id;
        String name;
        SubItem(String id, String name) { this.id = id; this.name = name; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertise_service);

        // Drawer Setup
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        new CaretakerDrawer(this, findViewById(R.id.leftDrawer), false);

        // UI Initialization
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerAvailability = findViewById(R.id.spinnerAvailability);
        layoutSubServices = findViewById(R.id.layoutSubServices);
        rateMinField = findViewById(R.id.editRateMin);
        rateMaxField = findViewById(R.id.editRateMax);
        locationField = findViewById(R.id.editLocation);
        pickImageView = findViewById(R.id.imagePreview);
        submitBtn = findViewById(R.id.btnSubmitAd);

        // Firebase Setup
        progressDialog = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        adsRef = FirebaseDatabase.getInstance().getReference("Advertisements");
        storageRef = FirebaseStorage.getInstance().getReference("advertisements");
        catRef = FirebaseDatabase.getInstance().getReference("Categories");

        // Availability Spinner Setup
        ArrayAdapter<String> availAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Full-time", "Part-time", "Night shifts"));
        spinnerAvailability.setAdapter(availAdapter);

        // Category Spinner Adapter
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNamesList);
        spinnerCategory.setAdapter(categoryAdapter);

        // Listeners
        pickImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        submitBtn.setOnClickListener(v -> performAdSubmission());

        // Category Selection Listener
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!categoryNamesList.isEmpty() && pos < categoryNamesList.size()) {
                    populateSub(categoryNamesList.get(pos));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load Categories with Real-time Updates
        setupRealTimeCategories();
    }

    private void setupRealTimeCategories() {
        catRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryIdMap.clear();
                categorySubs.clear();
                categoryNamesList.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    String catId = s.getKey();
                    String catName = s.child("CategoryName").getValue(String.class);
                    if (catId == null || catName == null) continue;

                    categoryIdMap.put(catName, catId);
                    categoryNamesList.add(catName);

                    List<SubItem> subs = new ArrayList<>();
                    for (DataSnapshot sub : s.child("SubCategories").getChildren()) {
                        String subId = sub.getKey();
                        String subName = sub.child("SubCategoryName").getValue(String.class);
                        if (subId != null && subName != null) {
                            subs.add(new SubItem(subId, subName));
                        }
                    }
                    categorySubs.put(catName, subs);
                }

                categoryAdapter.notifyDataSetChanged();

                if (!categoryNamesList.isEmpty()) {
                    int currentPos = spinnerCategory.getSelectedItemPosition();
                    if (currentPos == AdapterView.INVALID_POSITION || currentPos >= categoryNamesList.size()) {
                        currentPos = 0;
                    }
                    spinnerCategory.setSelection(currentPos);
                    populateSub(categoryNamesList.get(currentPos));
                } else {
                    layoutSubServices.removeAllViews();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdvertiseServiceActivity.this, "Failed to load categories: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateSub(String catName) {
        layoutSubServices.removeAllViews();
        List<SubItem> subs = categorySubs.get(catName);

        if (subs == null || subs.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("No subservices available");
            layoutSubServices.addView(t);
            return;
        }

        for (SubItem s : subs) {
            CheckBox cb = new CheckBox(this);
            cb.setText(s.name);
            cb.setTag(s.id);
            cb.setTextColor(getResources().getColor(android.R.color.black));
            layoutSubServices.addView(cb);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent d) {
        super.onActivityResult(requestCode, resultCode, d);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && d != null) {
            imageUri = d.getData();
            pickImageView.setImageURI(imageUri);
        }
    }

    private void performAdSubmission() {
        if (mAuth.getCurrentUser() == null) return;

        if (spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        String categoryName = spinnerCategory.getSelectedItem().toString();
        String catId = categoryIdMap.get(categoryName);

        String availability = spinnerAvailability.getSelectedItem().toString();
        String min = rateMinField.getText().toString().trim();
        String max = rateMaxField.getText().toString().trim();
        String loc = locationField.getText().toString().trim();

        if (TextUtils.isEmpty(min) || TextUtils.isEmpty(loc)) {
            Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> subs = new ArrayList<>();
        for (int i = 0; i < layoutSubServices.getChildCount(); i++) {
            View v = layoutSubServices.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox)v).isChecked()) {
                subs.add(v.getTag().toString());
            }
        }

        progressDialog.setMessage("Posting...");
        progressDialog.show();

        String uid = mAuth.getUid();
        DatabaseReference newAd = adsRef.child(uid).push();
        String adId = newAd.getKey();

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        if (imageUri != null) {
            StorageReference imgRef = storageRef.child(uid).child(adId + ".jpg");
            imgRef.putFile(imageUri).addOnSuccessListener(t ->
                    imgRef.getDownloadUrl().addOnSuccessListener(uri ->
                            saveAd(newAd, adId, uid, catId, subs, availability, min, max, loc, uri.toString(), date, time)
                    )
            ).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
            });
        } else {
            saveAd(newAd, adId, uid, catId, subs, availability, min, max, loc, "", date, time);
        }
    }

    private void saveAd(DatabaseReference ref, String adId, String uid, String catId,
                        List<String> subs, String availability, String min, String max,
                        String loc, String url, String date, String time) {

        Map<String,Object> map = new HashMap<>();
        map.put("AdvertisementID", adId);
        map.put("CaretakerID", uid);
        map.put("CategoriesID", catId);
        map.put("SubCategoriesID", subs);
        map.put("AdvertisementAvailability", availability);
        map.put("AdvertisementMinRate", min);
        map.put("AdvertisementMaxRate", max);
        map.put("AdvertisementLocation", loc);
        map.put("AdvertisementURL", url);
        map.put("AdvertisementDate", date);
        map.put("AdvertisementTime", time);

        ref.setValue(map).addOnSuccessListener(v -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Posted Successfully!", Toast.LENGTH_SHORT).show();

            // --- REDIRECT TO EDIT ADVERTISEMENT ACTIVITY ---
            Intent intent = new Intent(AdvertiseServiceActivity.this, MyAdvertisementsActivity.class);
            startActivity(intent);
            finish(); // Closes the current AdvertiseServiceActivity

        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}