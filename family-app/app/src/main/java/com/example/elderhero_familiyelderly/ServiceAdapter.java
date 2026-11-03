package com.example.elderhero_familiyelderly;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_familiyelderly.booking.BookingActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.VH> {

    private final List<Advertisement> list;
    private final Context ctx;

    public ServiceAdapter(List<Advertisement> list, Context ctx) {
        this.list = list;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.activity_service_adapter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Advertisement ad = list.get(position);

        holder.rate.setText("Rate: " + (ad.getDisplayRate() != null ? ad.getDisplayRate() : "Not specified"));
        holder.location.setText("Location: " + (ad.getAdvertisementLocation() != null ? ad.getAdvertisementLocation() : "-"));

        // Load Category
        if (ad.getCategoriesID() != null) {
            FirebaseDatabase.getInstance().getReference("Categories")
                    .child(ad.getCategoriesID())
                    .child("CategoryName")
                    .get()
                    .addOnSuccessListener(snap -> holder.title.setText("Category: " + snap.getValue(String.class)));
        }

        // Load Caretaker Name
        if (ad.getCaretakerID() != null) {
            FirebaseDatabase.getInstance().getReference("Caretakers")
                    .child(ad.getCaretakerID())
                    .child("CaretakerName")
                    .get()
                    .addOnSuccessListener(snap -> holder.caretakerName.setText("Caretaker: " + snap.getValue(String.class)));
        }

        // Load Subcategories to preview card
        loadPreviewSubcategoryTags(holder.chipGroup, ad);

        holder.card.setOnClickListener(v -> showBottomSheet(ad));
    }

    /** ========================= PREVIEW SUBCATEGORY TAGS ========================= **/
    private void loadPreviewSubcategoryTags(ChipGroup chipGroup, Advertisement ad) {
        chipGroup.removeAllViews();

        if (ad.getCategoriesID() == null || ad.getSubCategoriesID() == null) return;

        FirebaseDatabase.getInstance().getReference("Categories")
                .child(ad.getCategoriesID())
                .child("SubCategories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    chipGroup.removeAllViews();
                    for (String subId : ad.getSubCategoriesID()) {
                        if (snapshot.hasChild(subId)) {
                            String name = snapshot.child(subId).child("SubCategoryName").getValue(String.class);
                            if (name != null) {
                                Chip chip = new Chip(ctx);
                                chip.setText(name);
                                chip.setTextSize(12);
                                chip.setChipBackgroundColorResource(R.color.subcategory_chip_bg);
                                chip.setTextColor(ctx.getColor(R.color.subcategory_chip_text));
                                chip.setClickable(false);
                                chip.setCheckable(false);
                                chipGroup.addView(chip);
                            }
                        }
                    }
                });
    }

    /** ============================ BOTTOM SHEET ============================ **/
    private void showBottomSheet(Advertisement ad) {
        BottomSheetDialog sheet = new BottomSheetDialog(ctx);
        View view = LayoutInflater.from(ctx).inflate(R.layout.bottomsheet_service_details, null);
        sheet.setContentView(view);

        SharedPreferences prefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        View authButtons = view.findViewById(R.id.authButtons);
        View actionButtons = view.findViewById(R.id.actionButtons);
        LinearLayout ratingSection = view.findViewById(R.id.layoutRatingSection);

        if (!isLoggedIn) {
            authButtons.setVisibility(View.VISIBLE);
            actionButtons.setVisibility(View.GONE);

            view.findViewById(R.id.btnLogin).setOnClickListener(v ->
                    ctx.startActivity(new Intent(ctx, FamilyLoginActivity.class)));

            view.findViewById(R.id.btnSignup).setOnClickListener(v ->
                    ctx.startActivity(new Intent(ctx, FamilyRegisterActivity.class)));

        } else {
            authButtons.setVisibility(View.GONE);
            actionButtons.setVisibility(View.VISIBLE);
        }

        // IMAGE + DETAILS
        ImageView caretakerImg = view.findViewById(R.id.caretakerImg);
        ImageView serviceImage = view.findViewById(R.id.serviceImage);

        if (ad.getAdvertisementURL() != null && !ad.getAdvertisementURL().isEmpty())
            Picasso.get().load(ad.getAdvertisementURL()).into(serviceImage);

        TextView serviceRate = view.findViewById(R.id.serviceRate);
        TextView serviceLocation = view.findViewById(R.id.serviceLocation);
        TextView serviceAvailability = view.findViewById(R.id.serviceAvailability);
        TextView serviceCategory = view.findViewById(R.id.serviceCategory);

        serviceRate.setText("Rate: " + ad.getDisplayRate());
        serviceLocation.setText("Location: " + ad.getAdvertisementLocation());
        serviceAvailability.setText("Availability: " + ad.getAdvertisementAvailability());

        FirebaseDatabase.getInstance().getReference("Categories")
                .child(ad.getCategoriesID())
                .child("CategoryName")
                .get()
                .addOnSuccessListener(s -> serviceCategory.setText("Category: " + s.getValue(String.class)));

        // Load Full Caretaker Data
        FirebaseDatabase.getInstance().getReference("Caretakers")
                .child(ad.getCaretakerID())
                .get()
                .addOnSuccessListener(snapshot -> {
                    Caretaker c = snapshot.getValue(Caretaker.class);
                    if (c == null) return;

                    ((TextView) view.findViewById(R.id.caretakerName)).setText("Name: " + c.CaretakerName);
                    ((TextView) view.findViewById(R.id.caretakerGender)).setText("Gender: " + c.CaretakerGender);
                    ((TextView) view.findViewById(R.id.caretakerRace)).setText("Race: " + c.CaretakerRace);
                    ((TextView) view.findViewById(R.id.caretakerAge)).setText("Age: " + c.CaretakerAge);
                    ((TextView) view.findViewById(R.id.caretakerProfession)).setText("Profession: " + c.CaretakerProfession);
                    ((TextView) view.findViewById(R.id.caretakerExperience)).setText("Experience: " + c.CaretakerExperience + " years");
                    ((TextView) view.findViewById(R.id.caretakerLang)).setText("Languages: " + (c.CaretakerLanguage != null ? String.join(", ", c.CaretakerLanguage) : "-"));

                    if (c.CaretakerProfileUrl != null && !c.CaretakerProfileUrl.isEmpty())
                        Picasso.get().load(c.CaretakerProfileUrl).into(caretakerImg);

                    RatingBar ratingBar = view.findViewById(R.id.caretakerRatingBar);
                    TextView tvRatingSummary = view.findViewById(R.id.tvRatingSummary);
                    loadRatingData(ad.getCaretakerID(), ratingBar, tvRatingSummary);

                    // --- WHATSAPP LOGIC ---
                    view.findViewById(R.id.btnWhatsApp).setOnClickListener(v -> {
                        if (c.CaretakerPhoneNo != null && !c.CaretakerPhoneNo.isEmpty()) {
                            openWhatsApp(c.CaretakerPhoneNo);
                        } else {
                            Toast.makeText(ctx, "Phone number not available", Toast.LENGTH_SHORT).show();
                        }
                    });

                    ratingSection.setOnClickListener(v -> {
                        Intent intent = new Intent(ctx, FamilyViewAllReviewsActivity.class);
                        // FIXED: Changed "caretakerID" to "caretakerUid" to match Activity
                        intent.putExtra("caretakerUid", ad.getCaretakerID());
                        // FIXED: Added Name extra
                        intent.putExtra("caretakerName", c.CaretakerName);
                        ctx.startActivity(intent);
                    });
                });

        // Load Subcategories into bottom sheet
        ChipGroup fullSubs = view.findViewById(R.id.chipGroupFullSubs);
        loadFullSubcategoryTags(fullSubs, ad);


        if (isLoggedIn) {
            view.findViewById(R.id.btnChat).setOnClickListener(v -> {
                Intent i = new Intent(ctx, ChatActivity.class);
                i.putExtra("receiverUid", ad.getCaretakerID());
                ctx.startActivity(i);
            });

            view.findViewById(R.id.btnBook).setOnClickListener(v -> {
                Intent i = new Intent(ctx, BookingActivity.class);
                i.putExtra("caretakerID", ad.getCaretakerID());
                i.putExtra("advertismentID", ad.getAdvertisementID());
                i.putExtra("rateRange", ad.getDisplayRate());
                ctx.startActivity(i);
                sheet.dismiss();
            });
        }

        sheet.show();
    }

    /** ========= HELPER: OPEN WHATSAPP ========= **/
    private void openWhatsApp(String phone) {
        try {
            // Clean the number format. Assume Malaysia (+60).
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (cleanPhone.startsWith("01")) {
                cleanPhone = "6" + cleanPhone; // Convert 012... to 6012...
            }

            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            ctx.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(ctx, "WhatsApp not installed or error opening.", Toast.LENGTH_SHORT).show();
        }
    }

    /** ========= LOAD SUBCATEGORIES IN BOTTOM SHEET ========= **/
    private void loadFullSubcategoryTags(ChipGroup chipGroup, Advertisement ad) {
        chipGroup.removeAllViews();
        if (ad.getCategoriesID() == null || ad.getSubCategoriesID() == null) return;

        FirebaseDatabase.getInstance().getReference("Categories")
                .child(ad.getCategoriesID())
                .child("SubCategories")
                .get()
                .addOnSuccessListener(snapshot -> {
                    chipGroup.removeAllViews();
                    for (String subId : ad.getSubCategoriesID()) {
                        if (snapshot.hasChild(subId)) {
                            String name = snapshot.child(subId).child("SubCategoryName").getValue(String.class);
                            if (name != null) {
                                Chip chip = new Chip(ctx);
                                chip.setText(name);
                                chip.setTextSize(14);
                                chip.setChipBackgroundColorResource(R.color.subcategory_chip_bg);
                                chip.setTextColor(ctx.getColor(R.color.subcategory_chip_text));
                                chip.setClickable(false);
                                chip.setCheckable(false);
                                chipGroup.addView(chip);
                            }
                        }
                    }
                });
    }

    /** ========= LOAD AVERAGE RATING ========= **/
    private void loadRatingData(String caretakerID, RatingBar ratingBar, TextView summary) {
        FirebaseDatabase.getInstance().getReference("Reviews").get().addOnSuccessListener(snapshot -> {
            float total = 0;
            int count = 0;

            for (DataSnapshot child : snapshot.getChildren()) {
                String id = child.child("CaretakerID").getValue(String.class);
                if (id != null && id.equals(caretakerID)) {
                    Float rating = child.child("ReviewRating").getValue(Float.class);
                    if (rating != null) {
                        total += rating;
                        count++;
                    }
                }
            }

            if (count == 0) {
                summary.setText("(No reviews)");
                ratingBar.setRating(0);
                return;
            }

            float avg = total / count;
            ratingBar.setRating(avg);
            summary.setText(String.format("%.1f (%d reviews) >", avg, count));
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        TextView title, caretakerName, rate, location;
        ChipGroup chipGroup;

        VH(View v) {
            super(v);
            card = v.findViewById(R.id.cardRoot);
            title = v.findViewById(R.id.adTitle);
            caretakerName = v.findViewById(R.id.adCaretakerName);
            rate = v.findViewById(R.id.adRate);
            location = v.findViewById(R.id.adLocation);
            chipGroup = v.findViewById(R.id.chipGroupPreview);
        }
    }
}