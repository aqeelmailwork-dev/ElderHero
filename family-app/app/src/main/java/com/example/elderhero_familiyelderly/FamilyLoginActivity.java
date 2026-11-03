package com.example.elderhero_familiyelderly;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FamilyLoginActivity extends AppCompatActivity {

    EditText emailField, passwordField;
    ImageView togglePassword;
    FirebaseAuth mAuth;
    DatabaseReference familyRef;
    DatabaseReference managementRef;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_login);

        emailField = findViewById(R.id.editTextEmail);
        passwordField = findViewById(R.id.editTextPassword);
        togglePassword = findViewById(R.id.togglePassword);

        mAuth = FirebaseAuth.getInstance();
        familyRef = FirebaseDatabase.getInstance().getReference("FamilyElderly");
        managementRef = FirebaseDatabase.getInstance().getReference("FamilyElderlyManagment");

        togglePasswordLogic();

        TextView registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, FamilyRegisterActivity.class))
        );
        TextView forgotPassLink = findViewById(R.id.forgotPasswordLink);
        forgotPassLink.setOnClickListener(v -> {
            startActivity(new Intent(FamilyLoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void togglePasswordLogic() {
        togglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordField.setSelection(passwordField.getText().length());
        });
    }

    public void loginUser(android.view.View v) {

        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Enter valid email");
            return;
        }

        if (password.isEmpty()) {
            passwordField.setError("Enter password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            String uid = user.getUid();

            familyRef.child(uid).get().addOnCompleteListener(snapshotTask -> {

                if (!snapshotTask.isSuccessful() || !snapshotTask.getResult().exists()) {
                    mAuth.signOut();
                    Toast.makeText(this, "User not registered", Toast.LENGTH_SHORT).show();
                    return;
                }

                DataSnapshot snapshot = snapshotTask.getResult();
                String type = snapshot.child("FamilyElderlyType").getValue(String.class);
                String name = snapshot.child("FamilyElderlyName").getValue(String.class);
                String imageUrl = snapshot.child("FamilyElderlyURL").getValue(String.class);
                String userID = snapshot.child("FamilyElderlyID").getValue(String.class);

                proceedLogin(type, name, imageUrl, userID);
            });
        });
    }

    private void proceedLogin(String type, String name, String imageUrl, String userID) {

        if (!"Family".equals(type) && !"Elderly".equals(type)) {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("username", name);
        editor.putString("userImage", imageUrl);
        editor.putString("userID", userID);
        editor.putString("userType", type);

        // 🔥 Important: mark as logged in
        editor.putBoolean("isLoggedIn", true);

        editor.apply();

        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(this, BrowseServicesActivity.class));
        finish();
    }
}
