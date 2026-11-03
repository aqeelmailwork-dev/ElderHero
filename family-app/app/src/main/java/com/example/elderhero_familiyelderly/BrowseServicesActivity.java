package com.example.elderhero_familiyelderly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.*;

public class BrowseServicesActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ImageButton btnDrawer;

    // Search Component
    SearchView searchView;

    RecyclerView recyclerView;
    ProgressBar progressBar;
    ServiceAdapter adapter;

    List<Advertisement> adsList = new ArrayList<>();
    List<Advertisement> filteredList = new ArrayList<>();

    // Database References
    DatabaseReference adsRef, caretakersRef, categoriesRef, managementRef;

    // Listeners (Stored to remove them later)
    ValueEventListener adsListener, caretakersListener, categoriesListener, managementListener;

    // Filter Components
    Spinner spinnerCategory, spinnerSubService, spinnerAvailability,
            spinnerGender, spinnerRace, spinnerAge, spinnerLanguage;
    TextView labelSubService;
    Button btnFilter;

    ImageButton btnOpenFilter;
    LinearLayout filterCardContainer;
    CardView filterCard;
    View filterIncludeRoot;

    EditText etMinRate, etMaxRate;
    Spinner spinnerLocation;
    Spinner spinnerProfession;
    Spinner spinnerExperience;

    List<String> categories = new ArrayList<>();
    Map<String, List<String>> categoryToSubservices = new HashMap<>();

    Map<String, String> categoryIdToName = new HashMap<>();
    Map<String, Map<String, String>> categoryIdToSubNames = new HashMap<>();

    // Map to store caretaker status (Active/Suspended)
    Map<String, String> caretakerStatusMap = new HashMap<>();

    private final List<String> availabilities = Arrays.asList("All", "Full-time", "Part-time", "Night shifts");
    private final List<String> genders = Arrays.asList("All", "Male", "Female");
    private final List<String> races = Arrays.asList("All", "Malay", "Chinese", "Indian", "Others");
    private final List<String> ages = Arrays.asList("All", "20-29", "30-39", "40-49", "50+");
    private final List<String> languages = Arrays.asList("All", "English", "Malay", "Mandarin", "Tamil");
    private final List<String> experiences = Arrays.asList("All", "0-1 years", "1-3 years", "3-5 years", "5+ years");

    Map<String, Caretaker> caretakerMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_services);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnDrawer = findViewById(R.id.btnDrawer);

        // Setup Search View
        searchView = findViewById(R.id.searchView);
        setupSearchListener();

        // Detect if user is logged in
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        // Setup drawer based on login state
        DrawerHelper.setupDrawer(this, drawerLayout, !isLoggedIn);
        btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        recyclerView = findViewById(R.id.recyclerAds);
        progressBar = findViewById(R.id.progressLoading);

        btnOpenFilter = findViewById(R.id.btnOpenFilter);
        filterCardContainer = findViewById(R.id.filterCardContainer);
        filterIncludeRoot = findViewById(R.id.includeFilterCard);

        if (filterIncludeRoot != null) {
            filterCard = filterIncludeRoot.findViewById(R.id.filterCard);
        } else {
            filterCard = findViewById(R.id.filterCard);
        }

        // Initialize Filter UI
        spinnerCategory = filterIncludeRoot.findViewById(R.id.spinnerFilterCategory);
        spinnerSubService = filterIncludeRoot.findViewById(R.id.spinnerFilterSubService);
        spinnerAvailability = filterIncludeRoot.findViewById(R.id.spinnerFilterAvailability);
        spinnerGender = filterIncludeRoot.findViewById(R.id.spinnerFilterGender);
        spinnerRace = filterIncludeRoot.findViewById(R.id.spinnerFilterRace);
        spinnerAge = filterIncludeRoot.findViewById(R.id.spinnerFilterAge);
        spinnerLanguage = filterIncludeRoot.findViewById(R.id.spinnerFilterLanguage);
        btnFilter = filterIncludeRoot.findViewById(R.id.btnApplyFilter);
        labelSubService = filterIncludeRoot.findViewById(R.id.labelSubService);

        etMinRate = filterIncludeRoot.findViewById(R.id.etMinRate);
        etMaxRate = filterIncludeRoot.findViewById(R.id.etMaxRate);
        spinnerLocation = filterIncludeRoot.findViewById(R.id.spinnerFilterLocation);
        spinnerProfession = filterIncludeRoot.findViewById(R.id.spinnerFilterProfession);
        spinnerExperience = filterIncludeRoot.findViewById(R.id.spinnerFilterExperience);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServiceAdapter(filteredList, this);
        recyclerView.setAdapter(adapter);

        // Firebase references
        adsRef = FirebaseDatabase.getInstance().getReference("Advertisements");
        caretakersRef = FirebaseDatabase.getInstance().getReference("Caretakers");
        categoriesRef = FirebaseDatabase.getInstance().getReference("Categories");
        managementRef = FirebaseDatabase.getInstance().getReference("CaretakerManagement");

        setUpStaticSpinners();

        // Start Real-time listeners
        attachRealtimeListeners();

        // Filter Button Actions
        btnFilter.setOnClickListener(v -> {
            applyFilter();
            fadeOutView(filterCard);
            filterCardContainer.setVisibility(View.GONE);
        });

        btnOpenFilter.setOnClickListener(v -> {
            filterCardContainer.setVisibility(View.VISIBLE);
            fadeInView(filterCard);
        });

        filterCardContainer.setOnClickListener(v -> {
            fadeOutView(filterCard);
            filterCardContainer.setVisibility(View.GONE);
        });

        if (filterCard != null) filterCard.setOnClickListener(v -> {});

        // Initial visibility
        labelSubService.setVisibility(View.GONE);
        spinnerSubService.setVisibility(View.GONE);
        spinnerLocation.setVisibility(View.GONE);
        spinnerProfession.setVisibility(View.GONE);
        spinnerExperience.setVisibility(View.GONE);
    }

    private void attachRealtimeListeners() {
        progressBar.setVisibility(View.VISIBLE);

        // 1. Categories Listener
        categoriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                categories.add("All");
                categoryToSubservices.clear();
                categoryIdToName.clear();
                categoryIdToSubNames.clear();

                for (DataSnapshot catSnap : snapshot.getChildren()) {
                    String categoryID = catSnap.getKey();
                    String categoryName = catSnap.child("CategoryName").getValue(String.class);
                    if (categoryID == null || categoryName == null) continue;
                    categories.add(categoryName);
                    categoryIdToName.put(categoryID, categoryName);
                    List<String> subList = new ArrayList<>();
                    subList.add("All");
                    Map<String, String> subIdToName = new HashMap<>();
                    for (DataSnapshot subSnap : catSnap.child("SubCategories").getChildren()) {
                        String subID = subSnap.getKey();
                        String subName = subSnap.child("SubCategoryName").getValue(String.class);
                        if (subID == null || subName == null) continue;
                        subList.add(subName);
                        subIdToName.put(subID, subName);
                    }
                    categoryToSubservices.put(categoryName, subList);
                    categoryIdToSubNames.put(categoryID, subIdToName);
                }
                // Refresh spinner
                spinnerCategory.setAdapter(new ArrayAdapter<>(BrowseServicesActivity.this, android.R.layout.simple_spinner_dropdown_item, categories));

                // Refresh UI in case category names changed
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        categoriesRef.addValueEventListener(categoriesListener);

        // 2. Caretakers Listener
        caretakersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                caretakerMap.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Caretaker c = snap.getValue(Caretaker.class);
                    if (c != null) caretakerMap.put(snap.getKey(), c);
                }
                populateDynamicFilterOptions(); // Update profession filter
                applyFilter(); // Refresh list immediately
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        caretakersRef.addValueEventListener(caretakersListener);

        // 3. Management Listener (Suspension Status)
        managementListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                caretakerStatusMap.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String caretakerId = snap.child("CaretakerID").getValue(String.class); // Case sensitive fix kept
                    String status = snap.child("CaretakerManagementStatus").getValue(String.class);

                    if (caretakerId != null && status != null) {
                        caretakerStatusMap.put(caretakerId, status);
                    }
                }
                applyFilter(); // Immediate ban/unban reflection
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        managementRef.addValueEventListener(managementListener);

        // 4. Advertisements Listener
        adsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adsList.clear();
                for (DataSnapshot caretakerNode : snapshot.getChildren()) {
                    for (DataSnapshot adSnap : caretakerNode.getChildren()) {
                        Advertisement ad = adSnap.getValue(Advertisement.class);
                        if (ad != null) {
                            if (ad.getCaretakerID() == null || ad.getCaretakerID().isEmpty()) {
                                String parentKey = caretakerNode.getKey();
                                if (parentKey != null) ad.setCaretakerID(parentKey);
                            }

                            // Sanitize duplicates
                            if (ad.getSubCategoriesID() != null) {
                                Set<String> uniqueIds = new LinkedHashSet<>(ad.getSubCategoriesID());
                                ad.setSubCategoriesID(new ArrayList<>(uniqueIds));
                            }
                            adsList.add(ad);
                        }
                    }
                }
                populateDynamicFilterOptions();
                applyFilter();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BrowseServicesActivity.this, "Failed to sync ads.", Toast.LENGTH_SHORT).show();
            }
        };
        adsRef.addValueEventListener(adsListener);
    }

    /**
     * Stop listening when activity is destroyed to prevent crashes/memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adsRef != null && adsListener != null) adsRef.removeEventListener(adsListener);
        if (caretakersRef != null && caretakersListener != null) caretakersRef.removeEventListener(caretakersListener);
        if (categoriesRef != null && categoriesListener != null) categoriesRef.removeEventListener(categoriesListener);
        if (managementRef != null && managementListener != null) managementRef.removeEventListener(managementListener);
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilter();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilter();
                return true;
            }
        });
    }

    private void setUpStaticSpinners() {
        spinnerAvailability.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, availabilities));
        spinnerGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders));
        spinnerRace.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, races));
        spinnerAge.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ages));
        spinnerLanguage.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages));
        spinnerExperience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, experiences));

        spinnerSubService.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Arrays.asList("All")));

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = getSpinnerValue(spinnerCategory);
                if ("All".equals(selected)) {
                    spinnerSubService.setAdapter(new ArrayAdapter<>(BrowseServicesActivity.this, android.R.layout.simple_spinner_dropdown_item, Arrays.asList("All")));
                    spinnerSubService.setSelection(0);
                    fadeOutView(labelSubService);
                    fadeOutView(spinnerSubService);
                } else {
                    List<String> subs = categoryToSubservices.getOrDefault(selected, Collections.singletonList("All"));
                    spinnerSubService.setAdapter(new ArrayAdapter<>(BrowseServicesActivity.this, android.R.layout.simple_spinner_dropdown_item, subs));
                    spinnerSubService.setSelection(0);
                    fadeInView(labelSubService);
                    fadeInView(spinnerSubService);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void populateDynamicFilterOptions() {
        // Only run if we have data to avoid wiping selections prematurely
        if (adsList.isEmpty() && caretakerMap.isEmpty()) return;

        Set<String> locSet = new LinkedHashSet<>();
        locSet.add("All");
        for (Advertisement ad : adsList) {
            if (ad.getAdvertisementLocation() != null && !ad.getAdvertisementLocation().trim().isEmpty()) {
                locSet.add(ad.getAdvertisementLocation().trim());
            }
        }
        List<String> locList = new ArrayList<>(locSet);

        // Save current selection before resetting adapter
        String currentLoc = getSpinnerValue(spinnerLocation);
        spinnerLocation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locList));
        if (locList.contains(currentLoc)) spinnerLocation.setSelection(locList.indexOf(currentLoc));

        fadeInView(spinnerLocation);

        Set<String> profSet = new LinkedHashSet<>();
        profSet.add("All");
        for (Caretaker c : caretakerMap.values()) {
            if (c != null && c.CaretakerProfession != null && !c.CaretakerProfession.trim().isEmpty()) {
                profSet.add(c.CaretakerProfession.trim());
            }
        }
        List<String> profList = new ArrayList<>(profSet);

        String currentProf = getSpinnerValue(spinnerProfession);
        spinnerProfession.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profList));
        if (profList.contains(currentProf)) spinnerProfession.setSelection(profList.indexOf(currentProf));

        fadeInView(spinnerProfession);
        fadeInView(spinnerExperience);
    }

    private void applyFilter() {
        // 1. Get Search Query
        String searchQuery = "";
        if (searchView.getQuery() != null) {
            searchQuery = searchView.getQuery().toString().toLowerCase().trim();
        }

        // 2. Get Filter Values
        String selectedCategory = getSpinnerValue(spinnerCategory);
        String selectedSub = getSpinnerValue(spinnerSubService);
        String selectedAvailability = getSpinnerValue(spinnerAvailability);
        String selectedGender = getSpinnerValue(spinnerGender);
        String selectedRace = getSpinnerValue(spinnerRace);
        String selectedAge = getSpinnerValue(spinnerAge);
        String selectedLang = getSpinnerValue(spinnerLanguage);
        String selectedLocation = getSpinnerValue(spinnerLocation);
        String selectedProfession = getSpinnerValue(spinnerProfession);
        String selectedExperience = getSpinnerValue(spinnerExperience);

        Double minRate = parseDouble(etMinRate.getText().toString());
        Double maxRate = parseDouble(etMaxRate.getText().toString());

        filteredList.clear();

        for (Advertisement ad : adsList) {

            // --- CHECK SUSPENSION ---
            String status = caretakerStatusMap.get(ad.getCaretakerID());
            if (status != null && !status.equalsIgnoreCase("Active")) {
                continue; // Hide suspended users in real-time
            }
            // -----------------------

            Caretaker c = caretakerMap.get(ad.getCaretakerID());
            if (c == null) continue; // Wait for caretaker data to load

            String adCategoryName = categoryIdToName.getOrDefault(ad.getCategoriesID(), "");

            // --- SEARCH FILTER LOGIC ---
            if (!searchQuery.isEmpty()) {
                boolean matchName = (c.CaretakerName != null && c.CaretakerName.toLowerCase().contains(searchQuery));
                boolean matchCat = (adCategoryName != null && adCategoryName.toLowerCase().contains(searchQuery));
                boolean matchLoc = (ad.getAdvertisementLocation() != null && ad.getAdvertisementLocation().toLowerCase().contains(searchQuery));
                boolean matchProf = (c.CaretakerProfession != null && c.CaretakerProfession.toLowerCase().contains(searchQuery));

                boolean matchSubName = false;
                Map<String, String> subMap = categoryIdToSubNames.get(ad.getCategoriesID());
                if(subMap != null && ad.getSubCategoriesID() != null) {
                    for(String subId : ad.getSubCategoriesID()) {
                        String sName = subMap.get(subId);
                        if(sName != null && sName.toLowerCase().contains(searchQuery)) {
                            matchSubName = true;
                            break;
                        }
                    }
                }

                if (!matchName && !matchCat && !matchLoc && !matchProf && !matchSubName) {
                    continue;
                }
            }

            // Category (exact)
            boolean matchCategory = "All".equals(selectedCategory) || (adCategoryName != null && adCategoryName.equals(selectedCategory));
            if (!matchCategory) continue;

            // Subcategory (exact)
            boolean matchSub = true;
            if (!"All".equals(selectedSub)) {
                matchSub = false;
                Map<String, String> subMap = categoryIdToSubNames.get(ad.getCategoriesID());
                if (subMap != null && ad.getSubCategoriesID() != null) {
                    for (String subID : ad.getSubCategoriesID()) {
                        String subName = subMap.get(subID);
                        if (subName != null && subName.equals(selectedSub)) {
                            matchSub = true;
                            break;
                        }
                    }
                }
            }
            if (!matchSub) continue;

            // Availability
            boolean matchAvailability = "All".equals(selectedAvailability) ||
                    (ad.getAdvertisementAvailability() != null && ad.getAdvertisementAvailability().equals(selectedAvailability));
            if (!matchAvailability) continue;

            // Gender
            boolean matchGender = "All".equals(selectedGender) ||
                    (c.CaretakerGender != null && c.CaretakerGender.equals(selectedGender));
            if (!matchGender) continue;

            // Race
            boolean matchRace = "All".equals(selectedRace) ||
                    (c.CaretakerRace != null && c.CaretakerRace.equals(selectedRace));
            if (!matchRace) continue;

            // Age
            String caretakerAgeRange = convertAgeToRange(c.CaretakerAge);
            boolean matchAge = "All".equals(selectedAge) || caretakerAgeRange.equals(selectedAge);
            if (!matchAge) continue;

            // Language
            boolean matchLang = true;
            if (!"All".equals(selectedLang)) {
                matchLang = false;
                if (c.CaretakerLanguage != null) {
                    for (String lang : c.CaretakerLanguage) {
                        if (lang != null && lang.equals(selectedLang)) {
                            matchLang = true; break;
                        }
                    }
                }
            }
            if (!matchLang) continue;

            // Location
            boolean matchLocation = "All".equals(selectedLocation) ||
                    (ad.getAdvertisementLocation() != null && ad.getAdvertisementLocation().equals(selectedLocation));
            if (!matchLocation) continue;

            // Profession
            boolean matchProfession = "All".equals(selectedProfession) ||
                    (c.CaretakerProfession != null && c.CaretakerProfession.equals(selectedProfession));
            if (!matchProfession) continue;

            // Experience
            String caretakerExpRange = convertExperienceToRange(c.CaretakerExperience);
            boolean matchExperience = "All".equals(selectedExperience) || caretakerExpRange.equals(selectedExperience);
            if (!matchExperience) continue;

            // Rate
            boolean matchRate = matchRateRange(ad, minRate, maxRate);
            if (!matchRate) continue;

            filteredList.add(ad);
        }

        adapter.notifyDataSetChanged();
    }

    // helpers
    private String getSpinnerValue(Spinner s) {
        if (s == null || s.getSelectedItem() == null) return "All";
        return s.getSelectedItem().toString().trim();
    }

    private Double parseDouble(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchRateRange(Advertisement ad, Double minRate, Double maxRate) {
        Double adMin = parseDouble(ad.getAdvertisementMinRate());
        Double adMax = parseDouble(ad.getAdvertisementMaxRate());

        if (adMin == null && adMax == null) return true;

        if (minRate != null) {
            if (adMax != null && adMax < minRate) return false;
            if (adMin != null && adMin < minRate) return false;
        }

        if (maxRate != null) {
            if (adMin != null && adMin > maxRate) return false;
            if (adMax != null && adMax > maxRate) return false;
        }

        return true;
    }

    private String convertAgeToRange(String ageStr) {
        if (ageStr == null || ageStr.isEmpty()) return "";
        try {
            String digits = ageStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return "";
            int age = Integer.parseInt(digits);
            if (age >= 20 && age <= 29) return "20-29";
            if (age >= 30 && age <= 39) return "30-39";
            if (age >= 40 && age <= 49) return "40-49";
            if (age >= 50) return "50+";
        } catch (Exception ignored) {}
        return "";
    }

    private String convertExperienceToRange(String expStr) {
        if (expStr == null || expStr.isEmpty()) return "";
        try {
            String digits = expStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return "";
            int exp = Integer.parseInt(digits);
            if (exp <= 1) return "0-1 years";
            if (exp <= 3) return "1-3 years";
            if (exp <= 5) return "3-5 years";
            return "5+ years";
        } catch (Exception ignored) {}
        return "";
    }

    private void fadeInView(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).setDuration(200).start();
    }

    private void fadeOutView(View view) {
        if (view == null) return;
        view.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> view.setVisibility(View.GONE)).start();
    }
}