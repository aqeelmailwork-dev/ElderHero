package com.example.elderhero_familiyelderly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button  familySignupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        familySignupBtn = findViewById(R.id.familySignupBtn);

        // Family login
        familySignupBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FamilyHomePreviewActivity.class));
        });

    }
}
