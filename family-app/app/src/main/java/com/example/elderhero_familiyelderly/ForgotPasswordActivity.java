package com.example.elderhero_familiyelderly;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.elderhero_familiyelderly.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailField;
    private Button resetBtn;
    private TextView backLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI
        emailField = findViewById(R.id.editTextResetEmail);
        resetBtn = findViewById(R.id.resetButton);
        backLogin = findViewById(R.id.backToLoginLink);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        resetBtn.setOnClickListener(v -> sendResetEmail());

        backLogin.setOnClickListener(v -> finish()); // Closes this activity and goes back to Login
    }

    private void sendResetEmail() {
        String email = emailField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            emailField.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Enter a valid email");
            emailField.requestFocus();
            return;
        }

        // Disable button to prevent multiple clicks
        resetBtn.setEnabled(false);
        resetBtn.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent to your email.", Toast.LENGTH_LONG).show();
                        finish(); // Go back to login screen
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error occurred";
                        Toast.makeText(ForgotPasswordActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();

                        // Re-enable button on failure
                        resetBtn.setEnabled(true);
                        resetBtn.setText("Send Reset Link");
                    }
                });
    }
}