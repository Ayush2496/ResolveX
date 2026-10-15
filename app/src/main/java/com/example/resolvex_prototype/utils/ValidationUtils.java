package com.example.resolvex_prototype.utils;

import android.text.TextUtils;
import android.util.Patterns;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email)
                && Patterns.EMAIL_ADDRESS.matcher(email).matches()
                && email.trim().toLowerCase().endsWith("@nmims.in");
    }

    public static boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /** A phone number is exactly 10 digits (spaces are ignored). */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.replace(" ", "").matches("\\d{10}");
    }
}
