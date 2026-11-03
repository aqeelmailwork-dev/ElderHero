package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FamilyViewAllReviewsActivity extends AppCompatActivity {

    private LinearLayout reviewsContainer;
    private TextView tvCaretakerName, tvAvgRating, tvReviewCount;
    private RatingBar overallRatingBar;

    private String caretakerUid, caretakerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_view_all_reviews);

        tvCaretakerName = findViewById(R.id.tvCaretakerName);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        overallRatingBar = findViewById(R.id.overallRatingBar);
        reviewsContainer = findViewById(R.id.reviewsContainer);

        caretakerUid = getIntent().getStringExtra("caretakerUid");
        caretakerName = getIntent().getStringExtra("caretakerName");

        tvCaretakerName.setText("Reviews for " + caretakerName);

        loadReviews();
    }

    private void loadReviews() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Reviews");

        ref.orderByChild("CaretakerID").equalTo(caretakerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        reviewsContainer.removeAllViews();
                        float totalRating = 0;
                        int count = 0;

                        for (DataSnapshot reviewSnap : snapshot.getChildren()) {

                            String familyId = reviewSnap.child("FamilyElderlyID").getValue(String.class);
                            String reviewText = reviewSnap.child("ReviewDetails").getValue(String.class);
                            Float ratingValue = reviewSnap.child("ReviewRating").getValue(Float.class);
                            Long timeStamp = reviewSnap.child("ReviewTime").getValue(Long.class);

                            if (ratingValue == null) continue;

                            totalRating += ratingValue;
                            count++;

                            View reviewView = getLayoutInflater()
                                    .inflate(R.layout.item_review, null);

                            TextView tvFamilyName = reviewView.findViewById(R.id.tvFamilyName);
                            TextView tvReviewText = reviewView.findViewById(R.id.tvReviewText);
                            RatingBar ratingBar = reviewView.findViewById(R.id.reviewRatingBar);
                            TextView tvDate = reviewView.findViewById(R.id.tvReviewDate);

                            ratingBar.setRating(ratingValue);

                            tvReviewText.setText(
                                    reviewText != null && !reviewText.isEmpty()
                                            ? reviewText
                                            : "(No written review)"
                            );

                            // 🔹 Get family name from FamilyElderly table
                            if (familyId != null) {
                                FirebaseDatabase.getInstance()
                                        .getReference("FamilyElderly")
                                        .child(familyId)
                                        .child("FamilyElderlyName")
                                        .get()
                                        .addOnSuccessListener(nameSnap -> {
                                            String name = nameSnap.getValue(String.class);
                                            tvFamilyName.setText(
                                                    name != null ? name : "Anonymous"
                                            );
                                        });
                            } else {
                                tvFamilyName.setText("Anonymous");
                            }

                            // Date formatting
                            if (timeStamp != null) {
                                String date = new SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault()
                                ).format(new Date(timeStamp));

                                tvDate.setText(date);
                            }

                            reviewsContainer.addView(reviewView);
                        }

                        if (count > 0) {
                            float avg = totalRating / count;
                            overallRatingBar.setRating(avg);
                            tvAvgRating.setText(
                                    String.format(Locale.getDefault(),
                                            "Average Rating: %.1f ★", avg)
                            );
                            tvReviewCount.setText("(" + count + " reviews)");
                        } else {
                            tvAvgRating.setText("No reviews yet");
                            tvReviewCount.setText("(0 reviews)");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvAvgRating.setText("Failed to load reviews");
                    }
                });
    }
}
