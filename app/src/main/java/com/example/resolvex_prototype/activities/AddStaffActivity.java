package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.DropdownHelper;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.example.resolvex_prototype.utils.ValidationUtils;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

/** Admin screen: create a staff account. (Students register themselves.) */
public class AddStaffActivity extends AppCompatActivity {

    private static final String[] STAFF_DEPTS = {
            "IT Support", "Electrical Maintenance", "General Maintenance", "Plumbing", "Housekeeping", "Administration"
    };

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AddStaffActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_add_staff);
        UiUtils.applySystemBarSpace(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvTopTitle)).setText("Add staff member");
        findViewById(R.id.tvTopSub).setVisibility(View.GONE);

        TextInputEditText etName = findViewById(R.id.etStaffName);
        TextInputEditText etEmail = findViewById(R.id.etStaffEmail);
        TextInputEditText etPhone = findViewById(R.id.etStaffPhone);
        TextInputEditText etPassword = findViewById(R.id.etStaffPassword);
        MaterialAutoCompleteTextView actDept = findViewById(R.id.actStaffDepartment);

        DropdownHelper.setup(this, actDept, STAFF_DEPTS, -1, null);

        findViewById(R.id.btnAddStaffSubmit).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String dept = actDept.getText() != null ? actDept.getText().toString().trim() : "";

            if (!ValidationUtils.isNotEmpty(name)) {
                etName.setError("Name is required");
                etName.requestFocus();
                return;
            }
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.setError("Enter a valid @nmims.in email address");
                etEmail.requestFocus();
                return;
            }
            if (!phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
                etPhone.setError("Phone number must be 10 digits");
                etPhone.requestFocus();
                return;
            }
            if (!ValidationUtils.isValidPassword(password)) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            User newStaff = new User();
            newStaff.setName(name);
            newStaff.setEmail(email);
            newStaff.setPhone(phone);
            newStaff.setPassword(password);
            newStaff.setRole("STAFF");
            newStaff.setDepartment(dept.isEmpty() ? "General Support" : dept);

            if (dbHelper.insertUser(newStaff)) {
                Toast.makeText(this, "Staff account created", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                etEmail.setError("An account with this email already exists");
                etEmail.requestFocus();
            }
        });
    }
}
