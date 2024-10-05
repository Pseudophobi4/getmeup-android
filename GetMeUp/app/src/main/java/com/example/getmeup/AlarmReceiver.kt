package com.example.getmeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service to play sound
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)

        // Log that the alarm has been triggered
        println("Alarm has been triggered. Starting AlarmService...")

        // Update SharedPreferences to indicate alarm is active
        val sharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_alarm_active", true)
            apply()
        }

        // Send a local broadcast to notify MainActivity about the alarm
        val alarmIntent = Intent("ALARM_TRIGGERED")
        LocalBroadcastManager.getInstance(context).sendBroadcast(alarmIntent)
    }
}
