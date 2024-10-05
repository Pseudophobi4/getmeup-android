package com.example.getmeup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service to play sound
        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)

        // You can optionally log that the alarm has been triggered
        println("Alarm has been triggered. Starting AlarmService...")
    }
}
