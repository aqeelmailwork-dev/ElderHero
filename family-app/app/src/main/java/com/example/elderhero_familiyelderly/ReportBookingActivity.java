package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ReportBookingActivity extends AppCompatActivity {

    private TextView tvReportTitle, tvBookingInfo;
    private Spinner spinnerCategory;
    private EditText etDetails;
    private Button btnSubmit;

    private String bookingID;
    private String caretakerID;
    private String familyID;

    private DatabaseReference reportsRef;
    private DatabaseReference caretakerRef;
    private DatabaseReference familyElderlyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_booking);

        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvBookingInfo = findViewById(R.id.tvReportBookingInfo);
        spinnerCategory = findViewById(R.id.spinnerReportCategory);
        etDetails = findViewById(R.id.etReportDetails);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        bookingID = getIntent().getStringExtra("bookingId");
        caretakerID = getIntent().getStringExtra("caretakerId");
        familyID = getIntent().getStringExtra("familyId");

        if (bookingID == null || caretakerID == null || familyID == null) {
            Toast.makeText(this, "Missing booking data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvBookingInfo.setText("Booking ID: " + bookingID);

        String[] categories = {
                "Select category",
                "No show",
                "Late arrival",
                "Rude behaviour",
                "Payment issue",
                "Safety concern",
                "Other"
        };

        spinnerCategory.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
        ));

        reportsRef = FirebaseDatabase.getInstance().getReference("Reports");
        caretakerRef = FirebaseDatabase.getInstance().getReference("Caretakers");
        familyElderlyRef = FirebaseDatabase.getInstance().getReference("FamilyElderly");

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {

        String category = spinnerCategory.getSelectedItem().toString();
        String details = etDetails.getText().toString().trim();

        if ("Select category".equals(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(details)) {
            Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        caretakerRef.child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot caretakerSnap) {

                        if (caretakerSnap.exists()) {
                            saveReport(currentUserId, "Caretaker", category, details);
                        } else {
                            familyElderlyRef.child(currentUserId)
                                    .child("FamilyElderlyType")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot typeSnap) {

                                            String type = typeSnap.getValue(String.class);

                                            if (type == null) {
                                                saveReport(currentUserId, "Unknown", category, details);
                                            } else {
                                                saveReport(currentUserId, type, category, details);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(ReportBookingActivity.this,
                                                    "Role detection failed: " + error.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ReportBookingActivity.this,
                                "Caretaker check failed: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveReport(String reporterId, String reporterRole,
                            String category, String details) {

        long now = System.currentTimeMillis();
        String reportID = reportsRef.child(bookingID).push().getKey();

        Report report = new Report(
                reportID,
                bookingID,
                category,
                details,
                "Pending",
                now,
                caretakerID,
                familyID,
                null,
                reporterId,
                reporterRole
        );

        reportsRef.child(bookingID).child(reportID)
                .setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Report submitted as " + reporterRole,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to submit: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
