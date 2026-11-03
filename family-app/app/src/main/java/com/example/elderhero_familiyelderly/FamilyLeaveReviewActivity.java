package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class FamilyLeaveReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etReview;
    private Button btnSubmit;

    private String caretakerUid, bookingId;
    private String familyUid;

    private DatabaseReference reviewRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_leave_review);

        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        btnSubmit = findViewById(R.id.btnSubmitReview);

        caretakerUid = getIntent().getStringExtra("caretakerUid");
        bookingId = getIntent().getStringExtra("bookingId");

        familyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ✅ New structure: direct under "Reviews"
        reviewRef = FirebaseDatabase.getInstance()
                .getReference("Reviews")
                .child(bookingId);

        loadExistingReview();

        btnSubmit.setOnClickListener(v -> submitOrUpdateReview());
    }

    /** ✅ Load existing review if already submitted */
    private void loadExistingReview() {
        reviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    Float rating = snapshot.child("ReviewRating").getValue(Float.class);
                    String reviewText = snapshot.child("ReviewDetails").getValue(String.class);

                    if (rating != null) ratingBar.setRating(rating);
                    if (reviewText != null) etReview.setText(reviewText);

                    btnSubmit.setText("Update Review");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FamilyLeaveReviewActivity.this,
                        "Failed to load review: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** ✅ Submit or Update Review */
    private void submitOrUpdateReview() {
        float rating = ratingBar.getRating();
        String review = etReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String reviewId = bookingId;  // ✅ One review per booking

        Map<String, Object> data = new HashMap<>();
        data.put("ReviewID", reviewId);
        data.put("ReviewRating", rating);
        data.put("ReviewDetails", review);
        data.put("ReviewTime", System.currentTimeMillis());
        data.put("FamilyElderlyID", familyUid);
        data.put("CaretakerID", caretakerUid);

        reviewRef.setValue(data)
                .addOnSuccessListener(aVoid -> {

                    // ✅ Mark booking as reviewed
                    DatabaseReference root = FirebaseDatabase.getInstance().getReference();

                    root.child("Bookings")
                            .child(bookingId)
                            .child("Reviewed")
                            .setValue(true);

                    root.child("FamilyElderlyBookings")
                            .child(familyUid)
                            .child(bookingId)
                            .child("Reviewed")
                            .setValue(true);

                    root.child("CaretakerBookings")
                            .child(caretakerUid)
                            .child(bookingId)
                            .child("Reviewed")
                            .setValue(true);

                    Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
