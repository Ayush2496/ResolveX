package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Spinner spinnerDepartment, spinnerYear;
    private MaterialButton btnRegisterSubmit;
    private TextView tvLoginLink;

    private DatabaseHelper dbHelper;

    private static final String[] DEPARTMENTS = {"Select Department", "STME", "SOC", "SOL"};
    private static final String[] YEARS = {"Select Year", "Year 1", "Year 2", "Year 3", "Year 4", "Year 5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        UiUtils.applySystemBarSpace(this);
        ((TextView) findViewById(R.id.tvTopTitle)).setText("Sign up");
        findViewById(R.id.tvTopSub).setVisibility(View.GONE);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dbHelper = new DatabaseHelper(this);

        etName = findViewById(R.id.etRegName);
        etEmail = findViewById(R.id.etRegEmail);
        etPhone = findViewById(R.id.etRegPhone);
        etPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);

        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerYear = findViewById(R.id.spinnerYear);

        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, DEPARTMENTS);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(deptAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, YEARS);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        btnRegisterSubmit.setOnClickListener(v -> handleRegistration());

        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        int deptPos = spinnerDepartment.getSelectedItemPosition();
        int yearPos = spinnerYear.getSelectedItemPosition();

        if (!ValidationUtils.isNotEmpty(name)) {
            etName.setError("Full name is required");
            etName.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Valid @nmims.in email address is required");
            etEmail.requestFocus();
            return;
        }

        if (deptPos <= 0) {
            Toast.makeText(this, "Please select your department (STME, SOC, or SOL)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (yearPos <= 0) {
            Toast.makeText(this, "Please select your academic year (1-5)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            etPhone.setError("Enter a 10-digit phone number");
            etPhone.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        String selectedDept = DEPARTMENTS[deptPos];
        int selectedYear = yearPos; // 1..5

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPassword(password);
        newUser.setRole("STUDENT");
        newUser.setDepartment(selectedDept);
        newUser.setYear(selectedYear);

        boolean success = dbHelper.insertUser(newUser);
        if (success) {
            Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Email may already be registered.", Toast.LENGTH_LONG).show();
        }
    }
}