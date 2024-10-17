package com.example.getmeup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.util.Calendar

class DeactivateAlarmActivity : AppCompatActivity() {

    private lateinit var etCodeInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button
    private lateinit var btnPause: Button // New pause button
    private lateinit var mediaPlayer: MediaPlayer // MediaPlayer reference

    private var alarmService: AlarmService? = null // AlarmService reference
    private val handler = Handler(Looper.getMainLooper()) // Handler for delayed tasks

    // Service connection to get AlarmService instance
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AlarmService.AlarmBinder
            alarmService = binder.getService() // Get the AlarmService instance
            mediaPlayer = binder.getMediaPlayer() // Get the MediaPlayer instance
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            alarmService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deactivate_alarm)

        etCodeInput = findViewById(R.id.editTextCode)
        btnSubmit = findViewById(R.id.buttonSubmit)
        btnBack = findViewById(R.id.btn_back)
        btnPause = findViewById(R.id.buttonPause) // Initialize pause button

        etCodeInput.requestFocus() // Set focus to the code input field

        // Bind to the AlarmService to control MediaPlayer and vibration
        val serviceIntent = Intent(this, AlarmService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Handle the submit button click
        btnSubmit.setOnClickListener {
            val inputCode = etCodeInput.text.toString()
            val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val correctCode = sharedPreferences.getString("alarm_code", "")

            Log.d("DeactivateAlarmActivity", "Input Code: $inputCode, Correct Code: $correctCode")

            if (inputCode == correctCode) {
                deactivateAlarm()
            } else {
                Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle the back button click
        btnBack.setOnClickListener { finish() }

        // Handle the pause button click
        btnPause.setOnClickListener { pauseMediaPlayer() }
    }

    private fun deactivateAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the active alarm
        alarmManager.cancel(pendingIntent)

        // Stop the AlarmService
        stopService(Intent(this, AlarmService::class.java))

        // Reschedule the alarm for the next day (if needed)
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmTime = sharedPreferences.getString("alarm_time", null)
        alarmTime?.let { rescheduleAlarm(it, alarmManager, pendingIntent) }

        // Update alarm state in SharedPreferences
        sharedPreferences.edit().putBoolean("is_alarm_active", false).apply()

        // Broadcast to enable buttons in MainActivity
        val enableButtonsIntent = Intent("ENABLE_BUTTONS")
        LocalBroadcastManager.getInstance(this).sendBroadcast(enableButtonsIntent)

        // Finish the activity and return to MainActivity
        finish()
    }

    private fun rescheduleAlarm(alarmTime: String, alarmManager: AlarmManager, pendingIntent: PendingIntent) {
        val alarmCalendar = Calendar.getInstance()
        val (hour, minute) = alarmTime.split(":").map { it.toInt() }

        alarmCalendar.set(Calendar.HOUR_OF_DAY, hour)
        alarmCalendar.set(Calendar.MINUTE, minute)
        alarmCalendar.set(Calendar.SECOND, 0)

        if (alarmCalendar.timeInMillis <= System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.timeInMillis, pendingIntent)
    }

    private fun pauseMediaPlayer() {
        mediaPlayer.stop() // Stop MediaPlayer

        try {
            mediaPlayer.prepare() // Prepare it for later use
        } catch (e: IOException) {
            e.printStackTrace()
        }

        alarmService?.stopVibrationTemporarily() // Stop vibration via AlarmService

        handler.removeCallbacksAndMessages(null) // Clear any scheduled tasks

        // Restart MediaPlayer and vibration after 30 seconds
        handler.postDelayed({
            mediaPlayer.start() // Restart MediaPlayer
            alarmService?.resumeVibration() // Resume vibration
        }, 30000) // 30 seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection) // Unbind the AlarmService
    }
}
