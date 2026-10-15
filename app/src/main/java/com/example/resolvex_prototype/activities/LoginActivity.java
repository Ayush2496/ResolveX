package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbStaff, rbAdmin;
    private MaterialButton btnLogin;
    private TextView tvRegister;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        UiUtils.applySystemBarSpace(this);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rgRole = findViewById(R.id.rgRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbStaff = findViewById(R.id.rbStaff);
        rbAdmin = findViewById(R.id.rbAdmin);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void handleLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        String selectedRole = "STUDENT";
        int checkedId = rgRole.getCheckedRadioButtonId();
        if (checkedId == R.id.rbStaff) {
            selectedRole = "STAFF";
        } else if (checkedId == R.id.rbAdmin) {
            selectedRole = "ADMIN";
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Please enter a valid @nmims.in email address");
            etEmail.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        User user = dbHelper.checkLogin(email, password, selectedRole);
        if (user != null) {
            sessionManager.createLoginSession(user);
            Toast.makeText(this, "Welcome back, " + user.getName() + "!", Toast.LENGTH_SHORT).show();

            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
            } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                startActivity(new Intent(this, StaffDashboardActivity.class));
            } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            }
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials or incorrect role selected", Toast.LENGTH_LONG).show();
        }
    }
}