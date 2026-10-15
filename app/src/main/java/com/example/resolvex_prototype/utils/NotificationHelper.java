package com.example.resolvex_prototype.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.resolvex_prototype.R;
import com.example.resolvex_prototype.activities.SplashActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID = "resolvex_status_channel";
    public static final String CHANNEL_NAME = "ResolveX Issue Updates";
    public static final String CHANNEL_DESC = "Notifications for campus issue status updates";

    private static final String PREFS = "ResolveXNotificationState";
    private static final String KEY_PERMISSION_ASKED = "permission_asked";
    private static final int PERMISSION_REQUEST_CODE = 2001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * From Android 13 an app must ask the user before it may show notifications.
     * We ask once, the first time a dashboard opens.
     */
    public static void requestPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_PERMISSION_ASKED, false)) return;

        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply();
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
    }

    /**
     * The in-app notification list is stored in the database. This shows a phone notification
     * when the user has more unread updates than the last time we told them, so opening the
     * dashboard does not repeat the same alert.
     */
    public static void showUnreadSummaryIfNew(Context context, int userId, int unreadCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "last_unread_" + userId;
        int lastTold = prefs.getInt(key, 0);

        if (unreadCount > lastTold) {
            String message = unreadCount == 1
                    ? "You have 1 unread update."
                    : "You have " + unreadCount + " unread updates.";
            showNotification(context, "ResolveX", message, 1000 + userId);
        }
        prefs.edit().putInt(key, unreadCount).apply();
    }

    @SuppressLint("MissingPermission")
    public static void showNotification(Context context, String title, String message, int notificationId) {
        createNotificationChannel(context);

        // Tapping the notification opens the app (Splash sends the user to the right dashboard)
        PendingIntent openApp = PendingIntent.getActivity(
                context, 0, new Intent(context, SplashActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openApp)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    manager.notify(notificationId, builder.build());
                }
            } else {
                manager.notify(notificationId, builder.build());
            }
        } catch (SecurityException ignored) {
        }
    }
}
