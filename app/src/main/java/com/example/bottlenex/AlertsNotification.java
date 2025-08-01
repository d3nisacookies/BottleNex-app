package com.example.bottlenex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlertsNotification {
    private static final String CHANNEL_ID_SPEED_LIMIT = "alerts_channel_speed_limit";
    private static final String CHANNEL_ID_ROAD_INCIDENT = "alerts_channel_road_incident";
    private static final String CHANNEL_ID_SPEED_CAMERA = "alerts_channel_speed_camera";
    private static final int NOTIFICATION_ID_SPEED_LIMIT = 1001;
    private static final int NOTIFICATION_ID_ROAD_INCIDENT = 1002;
    private static final int NOTIFICATION_ID_SPEED_CAMERA = 1003;
    
    // Add unique request codes for PendingIntents to prevent conflicts
    private static final int REQUEST_CODE_SPEED_LIMIT = 1001;
    private static final int REQUEST_CODE_ROAD_INCIDENT = 1002;
    private static final int REQUEST_CODE_SPEED_CAMERA = 1003;

    public static void sendSpeedLimitAlert(Context context, String title, String message) {
        createNotificationChannel(context, CHANNEL_ID_SPEED_LIMIT, "Speed Limit Alerts", "Notifications for speed limit alerts");
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_SPEED_LIMIT, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SPEED_LIMIT)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setGroup("speed_limit_alerts") // Prevent grouping with other alert types
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibrate pattern
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Default notification sound
                .setLights(0xFF0000FF, 3000, 3000) // Blue light, 3 seconds on/off
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Use all default settings

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Clear any existing speed limit notifications before showing new one
        notificationManager.cancel(NOTIFICATION_ID_SPEED_LIMIT);
        // Use timestamp to make notification ID unique
        int uniqueId = NOTIFICATION_ID_SPEED_LIMIT + (int)(System.currentTimeMillis() % 1000);
        notificationManager.notify(uniqueId, builder.build());
    }

    public static void sendRoadIncidentAlert(Context context, String title, String message) {
        createNotificationChannel(context, CHANNEL_ID_ROAD_INCIDENT, "Road Incident Alerts", "Notifications for road incident alerts");
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_ROAD_INCIDENT, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ROAD_INCIDENT)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setGroup("road_incident_alerts") // Prevent grouping with other alert types
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibrate pattern
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Default notification sound
                .setLights(0xFFFF8800, 3000, 3000) // Orange light, 3 seconds on/off
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Use all default settings

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Clear any existing road incident notifications before showing new one
        notificationManager.cancel(NOTIFICATION_ID_ROAD_INCIDENT);
        // Use timestamp to make notification ID unique
        int uniqueId = NOTIFICATION_ID_ROAD_INCIDENT + (int)(System.currentTimeMillis() % 1000);
        notificationManager.notify(uniqueId, builder.build());
    }

    public static void sendSpeedCameraAlert(Context context, String title, String message) {
        createNotificationChannel(context, CHANNEL_ID_SPEED_CAMERA, "Speed Camera Alerts", "Notifications for speed camera alerts");
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_SPEED_CAMERA, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SPEED_CAMERA)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setGroup("speed_camera_alerts") // Prevent grouping with other alert types
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibrate pattern
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Default notification sound
                .setLights(0xFFFF0000, 3000, 3000) // Red light, 3 seconds on/off
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Use all default settings

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Clear any existing speed camera notifications before showing new one
        notificationManager.cancel(NOTIFICATION_ID_SPEED_CAMERA);
        // Use timestamp to make notification ID unique
        int uniqueId = NOTIFICATION_ID_SPEED_CAMERA + (int)(System.currentTimeMillis() % 1000);
        notificationManager.notify(uniqueId, builder.build());
    }

    private static void createNotificationChannel(Context context, String channelId, String name, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            
            // Enable sound, vibration, and lights
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);
            
            // Set different light colors for each channel
            if (channelId.equals(CHANNEL_ID_SPEED_LIMIT)) {
                channel.setLightColor(0xFF0000FF); // Blue light
            } else if (channelId.equals(CHANNEL_ID_ROAD_INCIDENT)) {
                channel.setLightColor(0xFFFF8800); // Orange light
            } else if (channelId.equals(CHANNEL_ID_SPEED_CAMERA)) {
                channel.setLightColor(0xFFFF0000); // Red light
            }
            
            channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null);
            
            // Set as alarm category for heads-up display
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            // Disable notification grouping to ensure each alert shows separately
            channel.setAllowBubbles(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 