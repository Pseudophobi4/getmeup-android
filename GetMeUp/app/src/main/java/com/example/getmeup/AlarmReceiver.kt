package com.example.getmeup

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service to play sound
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)

        // Show the notification when the alarm is triggered
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Create the notification
        val notification = NotificationCompat.Builder(context, "YOUR_CHANNEL_ID") // Replace with your actual channel ID
            .setSmallIcon(R.mipmap.ic_launcher) // Set your notification icon
            .setContentTitle("Alarm Triggered")
            .setContentText("Tap to open the app")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Show the notification
                with(NotificationManagerCompat.from(context)) {
                    notify(1001, notification)
                }
            } else {
                // Handle the case where permission is not granted
                // You may want to log this or handle it in a user-friendly way
                // For now, just logging a message
                println("Notification permission not granted.")
            }
        } else {
            // For older versions, send the notification directly
            with(NotificationManagerCompat.from(context)) {
                notify(1001, notification)
            }
        }
    }
}


