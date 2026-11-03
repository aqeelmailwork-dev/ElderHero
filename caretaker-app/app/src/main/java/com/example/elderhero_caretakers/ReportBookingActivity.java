package com.example.elderhero_caretakers;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ReportBookingActivity extends AppCompatActivity {

    private TextView tvBookingInfo;
    private Spinner spinnerCategory;
    private EditText etDetails;
    private Button btnSubmit;

    private String bookingID;
    private String familyID;
    private String caretakerID;

    private DatabaseReference reportsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_booking);

        tvBookingInfo = findViewById(R.id.tvReportBookingInfo);
        spinnerCategory = findViewById(R.id.spinnerReportCategory);
        etDetails = findViewById(R.id.etReportDetails);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        bookingID = getIntent().getStringExtra("bookingId");
        familyID = getIntent().getStringExtra("familyId");
        caretakerID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (bookingID == null || familyID == null) {
            Toast.makeText(this, "Missing booking information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvBookingInfo.setText("Booking ID: " + bookingID);

        String[] categories = new String[]{
                "Select category",
                "Client not available",
                "Disrespectful behavior",
                "Wrong address",
                "Safety concern",
                "Payment issue",
                "Other"
        };

        spinnerCategory.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                )
        );

        reportsRef = FirebaseDatabase.getInstance().getReference("Reports");

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {

        String category = spinnerCategory.getSelectedItem().toString();
        String details = etDetails.getText().toString().trim();

        if ("Select category".equals(category)) {
            Toast.makeText(this, "Choose a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(details)) {
            Toast.makeText(this, "Describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }

        long time = System.currentTimeMillis();

        String reportID = reportsRef.child(bookingID).push().getKey();

        Report report = new Report(
                reportID,
                bookingID,
                category,
                details,
                "Pending",
                time,
                caretakerID,
                familyID,
                null,
                caretakerID,
                "Caretaker"
        );

        reportsRef.child(bookingID).child(reportID)
                .setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report submitted.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
