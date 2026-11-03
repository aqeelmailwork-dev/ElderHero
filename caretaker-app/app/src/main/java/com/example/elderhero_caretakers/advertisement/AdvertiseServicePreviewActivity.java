package com.example.elderhero_caretakers.advertisement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.elderhero_caretakers.authenticate.LoginActivity;
import com.example.elderhero_caretakers.authenticate.RegisterActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvertiseServicePreviewActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private CaretakerDrawer caretakerDrawer;
    private ImageButton btnMenu;

    private Spinner spinnerCategory, spinnerAvailability;
    private LinearLayout layoutSubServices;
    private Button btnLogin, btnSignUp;
    private EditText etPriceMin, etPriceMax, etLocation;

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
        setContentView(R.layout.activity_advertise_service_preview);

        drawerLayout = findViewById(R.id.drawerLayout);
        View drawerView = findViewById(R.id.drawerView);
        btnMenu = findViewById(R.id.btnMenu);

        caretakerDrawer = new CaretakerDrawer(this, drawerView, true);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerAvailability = findViewById(R.id.spinnerAvailability);
        layoutSubServices = findViewById(R.id.layoutSubServices);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        etPriceMin = findViewById(R.id.etPriceMin);
        etPriceMax = findViewById(R.id.etPriceMax);
        etLocation = findViewById(R.id.etLocation);

        btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(AdvertiseServicePreviewActivity.this, LoginActivity.class);
            startActivity(i);
        });

        btnSignUp.setOnClickListener(v -> {
            Intent i = new Intent(AdvertiseServicePreviewActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        setupAvailabilitySpinner();

        // Initialize Category Adapter
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNamesList);
        spinnerCategory.setAdapter(categoryAdapter);

        // Spinner Listener
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!categoryNamesList.isEmpty() && pos < categoryNamesList.size()) {
                    populateSubServices(categoryNamesList.get(pos));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupRealTimeCategories();
    }

    private void setupAvailabilitySpinner() {
        ArrayAdapter<String> availAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Full-time", "Part-time", "Night shifts"));
        spinnerAvailability.setAdapter(availAdapter);
    }

    private void setupRealTimeCategories() {
        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("Categories");

        // Changed to addValueEventListener for Real-Time updates
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
                    DataSnapshot subCategoriesSnapshot = s.child("SubCategories");
                    for (DataSnapshot sub : subCategoriesSnapshot.getChildren()) {
                        if (!sub.hasChild("SubCategoryName")) continue;

                        String subId = sub.getKey();
                        String subName = sub.child("SubCategoryName").getValue(String.class);
                        if (subId != null && subName != null) {
                            subs.add(new SubItem(subId, subName));
                        }
                    }
                    categorySubs.put(catName, subs);
                }

                categoryAdapter.notifyDataSetChanged();

                // Refresh the subservices view based on current selection
                if (!categoryNamesList.isEmpty()) {
                    int currentPos = spinnerCategory.getSelectedItemPosition();
                    if (currentPos == AdapterView.INVALID_POSITION || currentPos >= categoryNamesList.size()) {
                        currentPos = 0;
                    }
                    spinnerCategory.setSelection(currentPos);
                    populateSubServices(categoryNamesList.get(currentPos));
                } else {
                    layoutSubServices.removeAllViews();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdvertiseServicePreviewActivity.this, "Failed to load categories: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateSubServices(String catName) {
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
            // In preview mode, allow checking or disable depending on UX requirements
            // Here we allow checking for demonstration
            cb.setEnabled(true);
            layoutSubServices.addView(cb);
        }
    }
}