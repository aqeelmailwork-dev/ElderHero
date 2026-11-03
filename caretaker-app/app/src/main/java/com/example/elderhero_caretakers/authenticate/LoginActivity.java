package com.example.elderhero_caretakers.authenticate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.elderhero_caretakers.CaretakerHomeActivity;
import com.example.elderhero_caretakers.ForgotPasswordActivity;
import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoginActivity extends AppCompatActivity {

    EditText emailField, passwordField;
    ImageView togglePassword;
    FirebaseAuth mAuth;
    DatabaseReference caretakerRef;
    DatabaseReference managementRef;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.editTextEmail);
        passwordField = findViewById(R.id.editTextPassword);
        togglePassword = findViewById(R.id.togglePassword);

        mAuth = FirebaseAuth.getInstance();
        caretakerRef = FirebaseDatabase.getInstance().getReference("Caretakers");
        managementRef = FirebaseDatabase.getInstance().getReference("CaretakerManagement");

        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide Password
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                // Show Password
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            // Move cursor to the end
            passwordField.setSelection(passwordField.getText().length());
        });

        TextView registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        TextView forgotPassLink = findViewById(R.id.forgotPasswordLink);
        forgotPassLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    public void loginUser(android.view.View v) {

        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

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

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            passwordField.requestFocus();
            return;
        }

        v.setEnabled(false);
        Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        v.setEnabled(true);
                        Toast.makeText(getApplicationContext(),
                                "Login failed: " +
                                        (task.getException() != null ?
                                                task.getException().getMessage() : ""),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = mAuth.getCurrentUser().getUid();

                    AtomicBoolean finished = new AtomicBoolean(false);
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable timeoutRunnable = () -> {
                        if (finished.compareAndSet(false, true)) {
                            mAuth.signOut();
                            Toast.makeText(this,
                                    "Cannot reach the database server. Check your network and try again.",
                                    Toast.LENGTH_SHORT).show();
                            v.setEnabled(true);
                        }
                    };
                    handler.postDelayed(timeoutRunnable, 15000);

                    caretakerRef.child(uid).get().addOnCompleteListener(checkTask -> {
                        if (!finished.compareAndSet(false, true)) {
                            return;
                        }
                        handler.removeCallbacks(timeoutRunnable);

                        if (!checkTask.isSuccessful()) {
                            v.setEnabled(true);
                            Toast.makeText(this,
                                    "Error checking caretaker profile",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot caretakerSnap = checkTask.getResult();

                        if (!caretakerSnap.exists()) {
                            mAuth.signOut();
                            v.setEnabled(true);
                            Toast.makeText(this,
                                    "This account is not registered as a caretaker",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        managementRef.child(uid).get().addOnCompleteListener(managementTask -> {

                            if (!managementTask.isSuccessful()) {
                                proceedLogin(uid);
                                return;
                            }

                            DataSnapshot manageSnap = managementTask.getResult();

                            if (!manageSnap.exists()) {
                                proceedLogin(uid);
                                return;
                            }

                            String status = manageSnap.child("CaretakerManagementStatus")
                                    .getValue(String.class);

                            String reason = manageSnap.child("CaretakerManagementReason")
                                    .getValue(String.class);

                            if (status == null) {
                                proceedLogin(uid);
                                return;
                            }

                            switch (status) {

                                case "Suspended":
                                    mAuth.signOut();
                                    v.setEnabled(true);
                                    showSuspendedDialog(reason);
                                    break;

                                case "Warning":
                                    showWarningDialog(reason, uid);
                                    break;

                                case "Active":
                                default:
                                    proceedLogin(uid);
                                    break;
                            }

                        });
                    });
                });
    }

    private void proceedLogin(String uid) {

        Toast.makeText(getApplicationContext(),
                "Caretaker logged in successfully",
                Toast.LENGTH_SHORT).show();

        getSharedPreferences("ElderHeroPrefs", MODE_PRIVATE)
                .edit()
                .putString("CaretakerID", uid)
                .apply();

        Intent toHome = new Intent(getApplicationContext(),
                CaretakerHomeActivity.class);

        finish();
        startActivity(toHome);
    }

    private void showSuspendedDialog(String reason) {

        new AlertDialog.Builder(this)
                .setTitle("Account Suspended")
                .setMessage(
                        "Your account has been suspended by the admin.\n\n" +
                                "Reason:\n" + reason
                )
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showWarningDialog(String reason, String uid) {

        new AlertDialog.Builder(this)
                .setTitle("Warning from Admin")
                .setMessage(
                        "You have received a warning from admin.\n\n" +
                                "Reason:\n" + reason
                )
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    dialog.dismiss();
                    proceedLogin(uid);
                })
                .show();
    }
}