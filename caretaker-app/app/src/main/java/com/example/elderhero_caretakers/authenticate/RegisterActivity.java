package com.example.elderhero_caretakers.authenticate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.elderhero_caretakers.Caretaker;
import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    EditText emailField, passwordField, usernameField, phoneField, professionField, experienceField, ageRangeField, otherRaceField, otherLanguageField;
    TextView passwordStrengthText;
    ImageView profileImageView, togglePassword; // Added togglePassword here
    Uri imageUri;

    RadioGroup radioGroupRace, radioGroupGender;
    CheckBox checkMalay, checkMandarin, checkTamil, checkEnglish, checkOtherLanguage;

    FirebaseAuth mAuth;
    DatabaseReference dbRef;
    StorageReference storageRef;
    ProgressDialog progressDialog;

    private static final int PICK_IMAGE_REQUEST = 1;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.editTextEmail);
        passwordField = findViewById(R.id.editTextPassword);
        togglePassword = findViewById(R.id.togglePassword);
        usernameField = findViewById(R.id.editTextUsername);
        phoneField = findViewById(R.id.editTextPhone);
        professionField = findViewById(R.id.editTextProfession);
        experienceField = findViewById(R.id.editTextExperience);
        ageRangeField = findViewById(R.id.editTextAgeRange);
        passwordStrengthText = findViewById(R.id.passwordStrengthText);
        profileImageView = findViewById(R.id.profileImage);
        otherRaceField = findViewById(R.id.editTextOtherRace);
        otherLanguageField = findViewById(R.id.editTextOtherLanguage);

        radioGroupRace = findViewById(R.id.radioGroupRace);
        radioGroupGender = findViewById(R.id.radioGroupGender);

        checkMalay = findViewById(R.id.checkMalay);
        checkMandarin = findViewById(R.id.checkMandarin);
        checkTamil = findViewById(R.id.checkTamil);
        checkEnglish = findViewById(R.id.checkEnglish);
        checkOtherLanguage = findViewById(R.id.checkOtherLanguage);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Caretakers");
        storageRef = FirebaseStorage.getInstance().getReference("caretaker_profiles");

        progressDialog = new ProgressDialog(this);

        setupLiveValidation();
        setupRaceSelection();
        setupLanguageSelection();
        setupPasswordToggle();

        profileImageView.setOnClickListener(v -> openImagePicker());

        TextView loginLink = findViewById(R.id.loginLink);
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupPasswordToggle() {
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
            // Move cursor to end
            passwordField.setSelection(passwordField.getText().length());
        });
    }

    private void setupRaceSelection() {
        radioGroupRace.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selected = findViewById(checkedId);
            if (selected != null && selected.getId() == R.id.radioOther) {
                otherRaceField.setVisibility(View.VISIBLE);
            } else {
                otherRaceField.setVisibility(View.GONE);
                otherRaceField.setText("");
            }
        });
    }

    private void setupLanguageSelection() {
        checkOtherLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                otherLanguageField.setVisibility(View.VISIBLE);
            } else {
                otherLanguageField.setVisibility(View.GONE);
                otherLanguageField.setText("");
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void setupLiveValidation() {
        // Phone Validation (Live Feedback)
        phoneField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String phone = s.toString();
                if (!phone.isEmpty() && !phone.startsWith("60")) {
                    phoneField.setError("Phone number must start with 60");
                } else {
                    phoneField.setError(null);
                }
            }
        });

        emailField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString();
                if (!email.contains("@") || !email.endsWith(".com")) {
                    emailField.setError("Email must contain '@' and end with '.com'");
                } else {
                    emailField.setError(null);
                }
            }
        });

        passwordField.addTextChangedListener(new TextWatcher() {
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
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_red_dark));
                        break;
                    case "Medium":
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_orange_dark));
                        break;
                    default:
                        passwordStrengthText.setTextColor(ContextCompat.getColor(RegisterActivity.this, android.R.color.holo_green_dark));
                        break;
                }

                boolean isValid = strength.equals("Strong") || strength.equals("Very Strong");
                setPasswordIcon(passwordField, isValid);
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
        Drawable icon = ContextCompat.getDrawable(this, iconId);
        if (icon != null) {
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            // Set to right side of text
            editText.setCompoundDrawables(null, null, icon, null);
            editText.setCompoundDrawablePadding(16);
        }
    }

    public void createUser(View v) {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String username = usernameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String profession = professionField.getText().toString().trim();
        String experience = experienceField.getText().toString().trim();
        String ageRange = ageRangeField.getText().toString().trim();


        if (!phone.startsWith("60")) {
            phoneField.setError("Phone number must start with 60");
            phoneField.requestFocus();
            return;
        }

        RadioButton selectedRace = findViewById(radioGroupRace.getCheckedRadioButtonId());
        RadioButton selectedGender = findViewById(radioGroupGender.getCheckedRadioButtonId());

        if (selectedRace == null || selectedGender == null) {
            Toast.makeText(this, "Please select race and gender", Toast.LENGTH_SHORT).show();
            return;
        }

        String race;
        if (selectedRace.getId() == R.id.radioOther) {
            race = otherRaceField.getText().toString().trim();
            if (TextUtils.isEmpty(race)) {
                Toast.makeText(this, "Please specify your race", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            race = selectedRace.getText().toString();
        }

        String gender = selectedGender.getText().toString();

        List<String> languages = new ArrayList<>();
        if (checkMalay.isChecked()) languages.add("Malay");
        if (checkMandarin.isChecked()) languages.add("Mandarin");
        if (checkTamil.isChecked()) languages.add("Tamil");
        if (checkEnglish.isChecked()) languages.add("English");
        if (checkOtherLanguage.isChecked()) {
            String customLang = otherLanguageField.getText().toString().trim();
            if (!TextUtils.isEmpty(customLang)) languages.add(customLang);
            else {
                Toast.makeText(this, "Please specify your other language", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)
                || TextUtils.isEmpty(phone) || TextUtils.isEmpty(profession) || TextUtils.isEmpty(experience)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (languages.isEmpty()) {
            Toast.makeText(this, "Please select at least one language", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Registering caretaker...");
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        String uid = mAuth.getCurrentUser().getUid();

                        StorageReference fileRef = storageRef.child(uid).child("profile.jpg");

                        fileRef.putFile(imageUri)
                                .addOnCompleteListener(uploadTask -> {
                                    if (uploadTask.isSuccessful()) {
                                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                            String imageUrl = uri.toString();

                                            Caretaker caretaker = new Caretaker(
                                                    uid,
                                                    username,
                                                    email,
                                                    phone,
                                                    profession,
                                                    experience,
                                                    imageUrl,
                                                    race,
                                                    languages,
                                                    gender,
                                                    ageRange
                                            );

                                            dbRef.child(uid).setValue(caretaker)
                                                    .addOnCompleteListener(saveTask -> {
                                                        progressDialog.dismiss();
                                                        if (saveTask.isSuccessful()) {
                                                            Toast.makeText(this, "Caretaker registered successfully. Please log in.", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(this, LoginActivity.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(this, "Saving profile failed: " +
                                                                    (saveTask.getException() != null ? saveTask.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }).addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(this, "Could not get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}