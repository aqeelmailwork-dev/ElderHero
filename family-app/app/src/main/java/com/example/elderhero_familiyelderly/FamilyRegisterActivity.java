package com.example.elderhero_familiyelderly;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FamilyRegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2;

    EditText editTextEmail, editTextPassword, editTextName, editTextAge, editTextPhone;
    ImageView profileImage, togglePassword;
    TextView loginLink, passwordStrengthText;
    RadioGroup roleGroup, genderGroup;
    Button registerButton;

    Uri imageUri;
    ProgressDialog progressDialog;

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    StorageReference storageRef;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("FamilyElderly");
        storageRef = FirebaseStorage.getInstance().getReference("user_profiles");

        initUI();
        setupLiveValidation(); // Adds the text watchers

        registerButton.setOnClickListener(v -> createUser());
        loginLink.setOnClickListener(v -> finish());
    }

    private void initUI() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextName = findViewById(R.id.editTextUsername);
        editTextAge = findViewById(R.id.editTextAge);
        editTextPhone = findViewById(R.id.editTextPhone);

        passwordStrengthText = findViewById(R.id.passwordStrengthText);

        profileImage = findViewById(R.id.profileImage);
        togglePassword = findViewById(R.id.togglePassword);

        roleGroup = findViewById(R.id.roleGroup);
        genderGroup = findViewById(R.id.genderGroup);

        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        profileImage.setOnClickListener(v -> openImagePicker());

        togglePassword.setOnClickListener(v -> {
            editTextPassword.setInputType(isPasswordVisible ?
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

            togglePassword.setImageResource(isPasswordVisible ? R.drawable.ic_eye_closed : R.drawable.ic_eye_open);
            isPasswordVisible = !isPasswordVisible;
            editTextPassword.setSelection(editTextPassword.getText().length());
        });

        progressDialog = new ProgressDialog(this);
    }

    private void setupLiveValidation() {
        // Email Validation
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editTextEmail.setError("Invalid Email format");
                } else {
                    editTextEmail.setError(null);
                }
            }
        });

        // Phone Validation (Must start with 60)
        editTextPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String phone = s.toString();
                if (!phone.isEmpty() && !phone.startsWith("60")) {
                    editTextPhone.setError("Phone number must start with 60");
                } else {
                    editTextPhone.setError(null);
                }
            }
        });

        // Password Strength Validation
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                String strength = getPasswordStrength(password);

                passwordStrengthText.setVisibility(View.VISIBLE);
                passwordStrengthText.setText("Password strength: " + strength);

                switch (strength) {
                    case "Weak":
                        passwordStrengthText.setTextColor(ContextCompat.getColor(FamilyRegisterActivity.this, android.R.color.holo_red_dark));
                        break;
                    case "Medium":
                        passwordStrengthText.setTextColor(ContextCompat.getColor(FamilyRegisterActivity.this, android.R.color.holo_orange_dark));
                        break;
                    default:
                        passwordStrengthText.setTextColor(ContextCompat.getColor(FamilyRegisterActivity.this, android.R.color.holo_green_dark));
                        break;
                }

                boolean isValid = strength.equals("Strong") || strength.equals("Very Strong");
                setPasswordIcon(editTextPassword, isValid);
            }
        });
    }

    private String getPasswordStrength(String password) {
        if (password.length() < 6) return "Weak";
        else if (!password.matches(".*[A-Z].*") || !password.matches(".*[0-9].*")) return "Medium";
        else if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) return "Strong";
        else return "Very Strong";
    }

    private void setPasswordIcon(EditText editText, boolean isValid) {
        int iconId = isValid ? R.drawable.ic_check_green : R.drawable.ic_warning_red;
        try {
            Drawable icon = ContextCompat.getDrawable(this, iconId);
            if (icon != null) {
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                editText.setCompoundDrawables(null, null, icon, null);
                editText.setCompoundDrawablePadding(16);
            }
        } catch (Exception e) {
            // Failsafe if icons don't exist
        }
    }

    private void createUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String age = editTextAge.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        // Validate Empty Fields
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(age) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Phone Number starts with 60
        if (!phone.startsWith("60")) {
            editTextPhone.setError("Phone number must start with 60");
            editTextPhone.requestFocus();
            return;
        }

        // Validate Email Pattern
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid Email");
            editTextEmail.requestFocus();
            return;
        }

        // Validate Password Strength
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        // Validate Image
        if (imageUri == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Role Selection
        int selectedRoleId = roleGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a Role (Family or Elderly)", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Gender Selection
        int selectedGenderId = genderGroup.getCheckedRadioButtonId();
        if (selectedGenderId == -1) {
            Toast.makeText(this, "Please select a Gender", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Registering...");
        progressDialog.show();

        // Firebase Registration
        mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {

            String uid = mAuth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child(uid).child("profile.jpg");

            fileRef.putFile(imageUri).addOnSuccessListener(uploadTask ->
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

                        String role = ((RadioButton) findViewById(selectedRoleId)).getText().toString();
                        String gender = ((RadioButton) findViewById(selectedGenderId)).getText().toString();

                        FamilyElderly user = new FamilyElderly(uid, email, name, age, gender, phone, role, uri.toString());

                        dbRef.child(uid).setValue(user).addOnSuccessListener(done -> {

                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putBoolean("isLoggedIn", true)
                                    .putString("username", name)
                                    .putString("userImage", uri.toString())
                                    .putString("userID", uid)
                                    .putString("userType", role)
                                    .apply();

                            progressDialog.dismiss();
                            Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(this, FamilyLoginActivity.class));
                            finish();
                        });
                    })
            ).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Image Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }
}