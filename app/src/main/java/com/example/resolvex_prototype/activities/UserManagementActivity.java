package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.adapters.StaffAdapter;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Admin "Manage" screen: the list of resolvers and students.
 * Tap a person to open their details (and reset their password); "Add staff" opens the add-staff screen.
 */
public class UserManagementActivity extends AppCompatActivity {

    private MaterialButton btnAddStaff;
    private TextView chipTabStaff, chipTabStudents, tvUserCount;
    private RecyclerView rvStaff;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private boolean showingStudents = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            Toast.makeText(this, "Unauthorized access. Admin privileges required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(UserManagementActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_user_management);

        btnAddStaff = findViewById(R.id.btnAddStaff);
        chipTabStaff = findViewById(R.id.chipTabStaff);
        chipTabStudents = findViewById(R.id.chipTabStudents);
        tvUserCount = findViewById(R.id.tvUserCount);
        rvStaff = findViewById(R.id.rvStaff);

        rvStaff.setLayoutManager(new LinearLayoutManager(this));

        btnAddStaff.setOnClickListener(v -> startActivity(new Intent(this, AddStaffActivity.class)));

        chipTabStaff.setOnClickListener(v -> {
            showingStudents = false;
            loadUserList();
        });
        chipTabStudents.setOnClickListener(v -> {
            showingStudents = true;
            loadUserList();
        });

        BottomBar.attach(this, "ADMIN");
        UiUtils.applySystemBarSpace(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn() && "ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
            UiUtils.bindHeader(this, "Admin", "Manage", dbHelper.getUnreadNotificationCount(sessionManager.getUserId()));
            loadUserList();
        }
    }

    private void loadUserList() {
        // Staff accounts are created by the admin; students register themselves, so there is no "add" for them
        btnAddStaff.setVisibility(showingStudents ? View.GONE : View.VISIBLE);
        chipTabStaff.setSelected(!showingStudents);
        chipTabStudents.setSelected(showingStudents);

        List<User> users = dbHelper.getUsersByRole(showingStudents ? "STUDENT" : "STAFF");
        tvUserCount.setText(users.size() + (showingStudents ? " registered students" : " resolvers"));

        StaffAdapter adapter = new StaffAdapter(this, users);
        adapter.setWorkloads(dbHelper.getStaffWorkloads());
        adapter.setOnUserClickListener(user -> {
            Intent intent = new Intent(UserManagementActivity.this, UserDetailActivity.class);
            intent.putExtra(UserDetailActivity.EXTRA_USER_ID, user.getUserId());
            startActivity(intent);
        });
        rvStaff.setAdapter(adapter);
    }
}
