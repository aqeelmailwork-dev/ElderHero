package com.example.elderhero_familiyelderly.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderhero_familiyelderly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    TextView tvCaretakerName, tvRateRange;
    EditText etNotes;

    // Duration Inputs
    EditText etDurationQty;
    Spinner spDurationUnit;

    TextView tvDate, tvMeetingTime;
    Button btnPickDate, btnPickMeetingTime, btnSubmit;

    String caretakerID;
    String advertismentID;

    DatabaseReference rootRef;
    FirebaseAuth mAuth;

    String familyElderlyID;
    String caretakerName;
    String rateRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        tvCaretakerName = findViewById(R.id.tvCaretakerName);
        tvRateRange = findViewById(R.id.tvRateRange);
        tvDate = findViewById(R.id.tvDate);
        tvMeetingTime = findViewById(R.id.tvMeetingTime);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickMeetingTime = findViewById(R.id.btnPickMeetingTime);
        etNotes = findViewById(R.id.etNotes);
        btnSubmit = findViewById(R.id.btnSubmitBooking);

        // Initialize Duration Views
        etDurationQty = findViewById(R.id.etDurationQty);
        spDurationUnit = findViewById(R.id.spDurationUnit);

        setupDurationSpinner();

        mAuth = FirebaseAuth.getInstance();
        familyElderlyID = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        rootRef = FirebaseDatabase.getInstance().getReference();

        caretakerID = getIntent().getStringExtra("caretakerID");
        advertismentID = getIntent().getStringExtra("advertismentID");
        caretakerName = getIntent().getStringExtra("caretakerName");
        rateRange = getIntent().getStringExtra("rateRange");

        tvCaretakerName.setText(caretakerName != null ? caretakerName : "Caretaker");
        tvRateRange.setText("Estimated Rate: " + (rateRange != null ? rateRange : "N/A"));

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickMeetingTime.setOnClickListener(v -> showTimePicker());

        btnSubmit.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Booking")
                    .setMessage("Once confirmed, date/time cannot be changed. Proceed?")
                    .setPositiveButton("Yes", (dialog, which) -> submitBooking())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void setupDurationSpinner() {
        // Create an ArrayAdapter using string array
        String[] units = new String[]{"Hours", "Days"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDurationUnit.setAdapter(adapter);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            month += 1;
            String dateStr = String.format("%04d-%02d-%02d", year, month, dayOfMonth);
            tvDate.setText(dateStr);
        }, y, m, d);

        dp.getDatePicker().setMinDate(System.currentTimeMillis());
        dp.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY), minute = c.get(Calendar.MINUTE);

        TimePickerDialog tp = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            String t = String.format("%02d:%02d", hourOfDay, minute1);
            tvMeetingTime.setText(t);
        }, hour, minute, true);

        tp.show();
    }

    private void submitBooking() {

        String bookingDate = tvDate.getText().toString().trim();
        String bookingTime = tvMeetingTime.getText().toString().trim();
        String bookingNote = etNotes.getText().toString().trim();

        // Handle Duration Logic
        String qty = etDurationQty.getText().toString().trim();
        String unit = spDurationUnit.getSelectedItem().toString();

        if (TextUtils.isEmpty(qty)) {
            Toast.makeText(this, "Please enter duration quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        String bookingDuration = qty + " " + unit; // e.g., "2 Hours" or "3 Days"

        if (TextUtils.isEmpty(caretakerID)) {
            Toast.makeText(this, "No caretaker selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(bookingDate) || TextUtils.isEmpty(bookingTime)) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        String bookingID = rootRef.child("Bookings").push().getKey();
        if (bookingID == null) {
            Toast.makeText(this, "Error generating ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long ts = System.currentTimeMillis();

        Booking booking = new Booking(
                bookingID,
                caretakerID,
                familyElderlyID,
                advertismentID,
                bookingDate,
                bookingTime,
                bookingDuration,
                bookingNote,
                "Pending",
                ts
        );

        Map<String, Object> data = new HashMap<>();
        data.put("/Bookings/" + bookingID, booking);
        data.put("/CaretakerBookings/" + caretakerID + "/" + bookingID, true);
        data.put("/FamilyElderlyBookings/" + familyElderlyID + "/" + bookingID, true);

        rootRef.updateChildren(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking confirmed", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(BookingActivity.this, FamilyBookingListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}