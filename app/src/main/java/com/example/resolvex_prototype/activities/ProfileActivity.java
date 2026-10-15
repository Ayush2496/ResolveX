package com.example.resolvex_prototype.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.database.DatabaseHelper;
import com.example.resolvex_prototype.models.User;
import com.example.resolvex_prototype.utils.BottomBar;
import com.example.resolvex_prototype.utils.SessionManager;
import com.example.resolvex_prototype.utils.UiUtils;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileInitials, tvProfileEmail, tvProfileDept, tvProfileYear, tvProfilePhone, chipRole;
    private View llProfileYear;
    private MaterialButton btnLogout;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean logoutArmed = false;
    private final Runnable resetLogoutButton = () -> {
        logoutArmed = false;
        if (btnLogout != null) {
            btnLogout.setText("Log out");
        }
    };

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileInitials = findViewById(R.id.tvProfileInitials);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileDept = findViewById(R.id.tvProfileDept);
        tvProfileYear = findViewById(R.id.tvProfileYear);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        llProfileYear = findViewById(R.id.llProfileYear);
        chipRole = findViewById(R.id.chipRole);

        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> onLogoutTapped());

        BottomBar.attach(this, sessionManager.getRole());
        UiUtils.applySystemBarSpace(this);
        loadProfileData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            UiUtils.bindHeader(this, "", "Profile", dbHelper.getUnreadNotificationCount(sessionManager.getUserId()));
        }
    }

    private void loadProfileData() {
        User user = dbHelper.getUserById(sessionManager.getUserId());

        if (user != null) {
            tvProfileName.setText(user.getName());
            tvProfileInitials.setText(UiUtils.initials(user.getName()));
            UiUtils.bindPill(chipRole, user.getRole().substring(0, 1) + user.getRole().substring(1).toLowerCase(),
                    R.color.rx_purple_bg, R.color.rx_purple_fg);
            tvProfileEmail.setText(user.getEmail());
            tvProfileDept.setText(user.getDepartment() != null ? user.getDepartment() : "N/A");
            tvProfilePhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "N/A");

            if ("STUDENT".equalsIgnoreCase(user.getRole()) && user.getYear() > 0) {
                tvProfileYear.setText("Year " + user.getYear());
                llProfileYear.setVisibility(View.VISIBLE);
            } else {
                llProfileYear.setVisibility(View.GONE);
            }
        } else {
            tvProfileName.setText(sessionManager.getName());
            tvProfileInitials.setText(UiUtils.initials(sessionManager.getName()));
            tvProfileEmail.setText(sessionManager.getEmail());
            llProfileYear.setVisibility(View.GONE);
        }
    }

    /**
     * Logging out asks for a second tap instead of opening a dialog: the first tap changes the
     * button to "Tap again to log out" for a few seconds; a second tap logs out.
     */
    private void onLogoutTapped() {
        if (!logoutArmed) {
            logoutArmed = true;
            btnLogout.setText("Tap again to log out");
            handler.postDelayed(resetLogoutButton, 4000);
            return;
        }

        handler.removeCallbacks(resetLogoutButton);
        sessionManager.logoutUser();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(resetLogoutButton);
        resetLogoutButton.run();
    }
}
