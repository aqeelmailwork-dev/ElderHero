package com.example.elderhero_caretakers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SubmissionActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 100;
    Uri pdfUri;
    TextView fileNameText;
    Button pickFileBtn, uploadBtn, btnViewHistory;
    ProgressDialog progressDialog;
    StorageReference storageRef;
    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    LinearLayout notificationBox;
    TextView notificationText;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        new CaretakerDrawer(this, findViewById(R.id.leftDrawer), false);

        fileNameText = findViewById(R.id.fileNameText);
        pickFileBtn = findViewById(R.id.pickFileBtn);
        uploadBtn = findViewById(R.id.uploadBtn);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        notificationBox = findViewById(R.id.notificationBox);
        notificationText = findViewById(R.id.notificationText);

        progressDialog = new ProgressDialog(this);
        storageRef = FirebaseStorage.getInstance().getReference("submissions");
        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        pickFileBtn.setOnClickListener(v -> openFilePicker());
        uploadBtn.setOnClickListener(v -> uploadPdf());

        btnViewHistory.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(SubmissionActivity.this, SubmissionHistoryActivity.class));
        });

        findViewById(R.id.closeNotificationBtn).setOnClickListener(v ->
                notificationBox.setVisibility(View.GONE)
        );

        checkSubmissionStatus();
    }

    private void openFilePicker() {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfUri = data.getData();
            String name = pdfUri.getLastPathSegment();
            if (name == null) name = "selected_file.pdf";
            fileNameText.setText(name);
        }
    }

    private void uploadPdf() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pdfUri == null) {
            Toast.makeText(this, "Please select a PDF file", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Uploading...");
        progressDialog.show();

        String uid = mAuth.getCurrentUser().getUid();
        String fileName = uid + "_" + System.currentTimeMillis() + ".pdf";

        StorageReference fileRef = storageRef.child(uid).child(fileName);
        fileRef.putFile(pdfUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    saveSubmissionToDatabase(uid, downloadUri.toString());
                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SubmissionActivity.this, "Failed to retrieve download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
        ).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(SubmissionActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveSubmissionToDatabase(String uid, String fileUrl) {
        // Create a new submission record under Submissions
        DatabaseReference submissionsRef = dbRef.child("Submissions");
        DatabaseReference newSubRef = submissionsRef.push(); // generates SubmissionID
        String submissionId = newSubRef.getKey();
        if (submissionId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to create submission ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> submission = new HashMap<>();
        submission.put("SubmissionID", submissionId);
        submission.put("CaretakerID", uid);
        submission.put("SubmissionURL", fileUrl);
        submission.put("SubmissionStatus", "Pending");
        submission.put("SubmissionDate", date);
        submission.put("SubmissionTime", time);
        submission.put("SubmissionRemark", ""); // empty until admin reviews
        submission.put("AdminID", ""); // empty until admin sets it

        newSubRef.setValue(submission)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(SubmissionActivity.this, "Submission uploaded successfully!", Toast.LENGTH_SHORT).show();
                    // refresh status display
                    checkSubmissionStatus();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SubmissionActivity.this, "Failed to save submission: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkSubmissionStatus() {
        if (mAuth.getCurrentUser() == null) {
            notificationBox.setVisibility(View.GONE);
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();
        Query q = dbRef.child("Submissions").orderByChild("CaretakerID").equalTo(uid);

        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    showNotification("No document uploaded yet.", "#FFEB3B");
                    return;
                }

                // find most recent submission by SubmissionDate+SubmissionTime (or push order)
                DataSnapshot newest = null;
                long newestTimestamp = Long.MIN_VALUE;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String date = child.child("SubmissionDate").getValue(String.class);
                    String time = child.child("SubmissionTime").getValue(String.class);
                    // fallback to children key push priority if date/time absent
                    long candidate = child.getKey() != null ? child.getKey().hashCode() : 0L;

                    try {
                        if (date != null && time != null) {
                            String combined = date + " " + time; // "yyyy-MM-dd HH:mm"
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            Date d = sdf.parse(combined);
                            if (d != null) candidate = d.getTime();
                        }
                    } catch (Exception ignored) {}

                    if (candidate > newestTimestamp) {
                        newestTimestamp = candidate;
                        newest = child;
                    }
                }

                if (newest == null) {
                    showNotification("Submission pending review.", "#FFEB3B");
                    return;
                }

                String status = newest.child("SubmissionStatus").getValue(String.class);
                String remark = newest.child("SubmissionRemark").getValue(String.class);

                if (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("Pending")) {
                    showNotification("Submission pending review.", "#FFEB3B");
                } else if (status.equalsIgnoreCase("Approved")) {
                    showNotification("Document approved by admin.", "#8BC34A");
                } else if (status.equalsIgnoreCase("Rejected")) {
                    if (remark != null && !remark.trim().isEmpty()) {
                        showNotification("Rejected: " + remark, "#F44336");
                    } else {
                        showNotification("Document rejected by admin.", "#F44336");
                    }
                } else {
                    showNotification("Submission status: " + status, "#FFEB3B");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showNotification("Error fetching status: " + error.getMessage(), "#F44336");
            }
        });
    }

    private void showNotification(String message, String colorHex) {
        notificationBox.setVisibility(View.VISIBLE);
        try {
            notificationBox.setBackgroundColor(android.graphics.Color.parseColor(colorHex));
        } catch (Exception e) {
            notificationBox.setBackgroundColor(android.graphics.Color.LTGRAY);
        }

        if (colorHex.equalsIgnoreCase("#F44336") || colorHex.equalsIgnoreCase("#F8D7DA")) {
            notificationText.setTextColor(android.graphics.Color.WHITE);
        } else {
            notificationText.setTextColor(android.graphics.Color.BLACK);
        }

        notificationText.setText(message);
    }
}
