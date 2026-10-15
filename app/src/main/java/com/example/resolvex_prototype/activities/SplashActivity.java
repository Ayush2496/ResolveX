package com.example.resolvex_prototype.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.utils.NotificationHelper;
import com.example.resolvex_prototype.utils.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ensure notification channel exists
        NotificationHelper.createNotificationChannel(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkNavigation, 1500);
    }

    private void checkNavigation() {
        SessionManager session = new SessionManager(this);

        if (session.isLoggedIn()) {
            String role = session.getRole();
            if ("STUDENT".equalsIgnoreCase(role)) {
                startActivity(new Intent(SplashActivity.this, StudentDashboardActivity.class));
            } else if ("STAFF".equalsIgnoreCase(role)) {
                startActivity(new Intent(SplashActivity.this, StaffDashboardActivity.class));
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }
}