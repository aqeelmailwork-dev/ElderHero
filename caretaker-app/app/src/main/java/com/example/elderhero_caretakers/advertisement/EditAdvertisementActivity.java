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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.*;

public class EditAdvertisementActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 22;

    private Spinner spinnerCategory, spinnerAvailability;
    private LinearLayout layoutSubServices;
    private EditText rateMinField, rateMaxField, locationField;
    private ImageView imagePreview;
    private Button btnSaveChanges;

    private Uri newImageUri;
    private String adId, caretakerUid;
    private DatabaseReference adRef;
    private StorageReference storageRef;

    private ProgressDialog progressDialog;

    private final Map<String, String> categoryIdMap = new HashMap<>();
    private final Map<String, List<SubItem>> categorySubs = new HashMap<>();

    private Advertisement currentAd;

    private static class SubItem {
        String id, name;
        SubItem(String id, String name) { this.id = id; this.name = name; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_advertisement);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerAvailability = findViewById(R.id.spinnerAvailability);
        layoutSubServices = findViewById(R.id.layoutSubServices);
        rateMinField = findViewById(R.id.editRateMin);
        rateMaxField = findViewById(R.id.editRateMax);
        locationField = findViewById(R.id.editLocation);
        imagePreview = findViewById(R.id.imagePreview);
        btnSaveChanges = findViewById(R.id.btnSaveAd);

        caretakerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adId = getIntent().getStringExtra("adId");

        adRef = FirebaseDatabase.getInstance()
                .getReference("Advertisements")
                .child(caretakerUid)
                .child(adId);

        storageRef = FirebaseStorage.getInstance().getReference("advertisements");

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        setupAvailabilitySpinner();
        loadCategoriesThenAd();

        imagePreview.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, PICK_IMAGE);
        });

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void setupAvailabilitySpinner() {
        spinnerAvailability.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Full-time", "Part-time", "Night shifts")
        ));
    }

    private void loadCategoriesThenAd() {
        progressDialog.setMessage("Loading categories...");
        progressDialog.show();

        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("Categories");
        catRef.get().addOnSuccessListener(snapshot -> {
            List<String> names = new ArrayList<>();

            for (DataSnapshot cat : snapshot.getChildren()) {
                String catId = cat.getKey();
                String catName = cat.child("CategoryName").getValue(String.class);

                if (catId == null || catName == null) continue;

                names.add(catName);
                categoryIdMap.put(catName, catId);

                List<SubItem> subs = new ArrayList<>();
                for (DataSnapshot s : cat.child("SubCategories").getChildren()) {
                    String sId = s.getKey();
                    String sName = s.child("SubCategoryName").getValue(String.class);
                    if (sId != null && sName != null) subs.add(new SubItem(sId, sName));
                }
                categorySubs.put(catName, subs);
            }

            spinnerCategory.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
            ));

            spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int i, long l) {
                    populateSub(names.get(i));
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });

            loadAdDetails();

        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateSub(String catName) {
        layoutSubServices.removeAllViews();
        List<SubItem> list = categorySubs.get(catName);

        if (list == null || list.isEmpty()) {
            TextView t = new TextView(this);
            t.setText("No subservices available.");
            layoutSubServices.addView(t);
            return;
        }

        for (SubItem s : list) {
            CheckBox cb = new CheckBox(this);
            cb.setText(s.name);
            cb.setTag(s.id);
            layoutSubServices.addView(cb);

            if (currentAd != null &&
                    currentAd.getSubCategoriesID() != null &&
                    currentAd.getSubCategoriesID().contains(s.id)) {
                cb.setChecked(true);
            }
        }
    }

    private void loadAdDetails() {
        progressDialog.setMessage("Loading advertisement...");
        adRef.get().addOnSuccessListener(snapshot -> {

            currentAd = snapshot.getValue(Advertisement.class);

            if (currentAd == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to load details.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            rateMinField.setText(currentAd.getAdvertisementMinRate());
            rateMaxField.setText(currentAd.getAdvertisementMaxRate());
            locationField.setText(currentAd.getAdvertisementLocation());

            if (!TextUtils.isEmpty(currentAd.getAdvertisementURL())) {
                Picasso.get().load(currentAd.getAdvertisementURL())
                        .placeholder(R.drawable.ic_person_placeholder)
                        .into(imagePreview);
            }

            ArrayAdapter<String> availAdapter = (ArrayAdapter<String>) spinnerAvailability.getAdapter();
            spinnerAvailability.setSelection(availAdapter.getPosition(currentAd.getAdvertisementAvailability()));

            String catId = currentAd.getCategoriesID();
            if (catId != null) {
                String catName = null;
                for (String key : categoryIdMap.keySet()) {
                    if (categoryIdMap.get(key).equals(catId)) {
                        catName = key;
                        break;
                    }
                }
                if (catName != null) {
                    ArrayAdapter<String> catAdapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
                    spinnerCategory.setSelection(catAdapter.getPosition(catName));
                }
            }

            progressDialog.dismiss();

        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Error loading ad", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent d) {
        super.onActivityResult(requestCode, resultCode, d);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && d != null) {
            newImageUri = d.getData();
            imagePreview.setImageURI(newImageUri);
        }
    }

    private void saveChanges() {

        String catName = spinnerCategory.getSelectedItem().toString();
        String catId = categoryIdMap.get(catName);

        String avail = spinnerAvailability.getSelectedItem().toString();
        String min = rateMinField.getText().toString().trim();
        String max = rateMaxField.getText().toString().trim();
        String loc = locationField.getText().toString().trim();

        if (TextUtils.isEmpty(min) || TextUtils.isEmpty(loc)) {
            Toast.makeText(this, "Required fields missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> subs = new ArrayList<>();
        for (int i = 0; i < layoutSubServices.getChildCount(); i++) {
            View v = layoutSubServices.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox)v).isChecked()) {
                subs.add(v.getTag().toString());
            }
        }

        progressDialog.setMessage("Saving...");
        progressDialog.show();

        if (newImageUri != null) {
            StorageReference imgRef = storageRef.child(caretakerUid).child(adId + ".jpg");
            imgRef.putFile(newImageUri).addOnSuccessListener(t ->
                    imgRef.getDownloadUrl().addOnSuccessListener(uri ->
                            updateAd(catId, subs, avail, min, max, loc, uri.toString())
                    )
            );
        } else {
            updateAd(catId, subs, avail, min, max, loc, currentAd.getAdvertisementURL());
        }
    }

    private void updateAd(String catId,
                          List<String> subs,
                          String avail,
                          String min,
                          String max,
                          String loc,
                          String url) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Map<String,Object> map = new HashMap<>();
        map.put("CategoriesID", catId);
        map.put("SubCategoriesID", subs);
        map.put("AdvertisementAvailability", avail);
        map.put("AdvertisementMinRate", min);
        map.put("AdvertisementMaxRate", max);
        map.put("AdvertisementLocation", loc);
        map.put("AdvertisementURL", url);
        map.put("AdvertisementDate", date);
        map.put("AdvertisementTime", time);

        adRef.updateChildren(map).addOnSuccessListener(v -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
