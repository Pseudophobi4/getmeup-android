package com.example.getmeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service to play sound
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)

        // Launch the app using the launch intent
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Start the activity to bring it to the foreground
        try {
            context.startActivity(launchIntent)
            Log.d("AlarmReceiver", "Successfully launched the app.")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to launch the app: ${e.message}")
        }
    }
}
