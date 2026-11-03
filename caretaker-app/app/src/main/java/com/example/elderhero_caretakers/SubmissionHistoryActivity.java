package com.example.elderhero_caretakers;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SubmissionHistoryActivity extends AppCompatActivity {

    private ImageButton btnMenu;
    private RecyclerView recyclerView;
    private SubmissionAdapter adapter;
    private final List<Submission> submissions = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private ValueEventListener submissionsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_history);

        btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> onBackPressed());
        }

        recyclerView = findViewById(R.id.submissionRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubmissionAdapter(submissions, this);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to see your submissions.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("Submissions");
        loadSubmissions();
    }

    private void loadSubmissions() {
        String uid = mAuth.getCurrentUser().getUid();

        // Query by CaretakerID == uid
        Query q = dbRef.orderByChild("CaretakerID").equalTo(uid);

        submissionsListener = q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                submissions.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Submission s = child.getValue(Submission.class);
                    if (s != null) {
                        // ensure SubmissionID is set if missing
                        if (s.SubmissionID == null || s.SubmissionID.isEmpty()) {
                            s.SubmissionID = child.getKey();
                        }
                        submissions.add(s);
                    }
                }

                // sort newest first using date+time if available
                Collections.sort(submissions, new Comparator<Submission>() {
                    @Override
                    public int compare(Submission a, Submission b) {
                        String aKey = (a.SubmissionDate == null ? "" : a.SubmissionDate) + " " + (a.SubmissionTime == null ? "" : a.SubmissionTime);
                        String bKey = (b.SubmissionDate == null ? "" : b.SubmissionDate) + " " + (b.SubmissionTime == null ? "" : b.SubmissionTime);
                        return bKey.compareTo(aKey);
                    }
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SubmissionHistoryActivity.this, "Failed to load submissions: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbRef != null && submissionsListener != null) {
            dbRef.removeEventListener(submissionsListener);
        }
    }
}
