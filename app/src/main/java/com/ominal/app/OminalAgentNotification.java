package com.ominal.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ominal.R;

/** User-visible results for agent work that completes while GIR is backgrounded. */
final class OminalAgentNotification {
    private static final String CHANNEL_ID = "gir_agent_results";
    private static final String CHANNEL_NAME = "Agent results";

    private OminalAgentNotification() {
    }

    static void post(@NonNull Context context, @NonNull String sessionId,
                     boolean attention, @NonNull String detail) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Completion and attention updates for background work");
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, OringutanActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
            sessionId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = attention ? "GIR needs your attention" : "Task complete";
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setColor(0xFF22D3EE)
            .setContentTitle(title)
            .setContentText(detail)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build();
        manager.notify(0x47000000 | (sessionId.hashCode() & 0x00ffffff), notification);
    }
}
