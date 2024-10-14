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
import android.os.Looper
import android.os.IBinder
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
    private lateinit var btnPause: Button // New button for pausing
    private lateinit var mediaPlayer: MediaPlayer // Declare MediaPlayer variable
    private val handler = Handler(Looper.getMainLooper()) // Updated Handler instantiation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deactivate_alarm)

        etCodeInput = findViewById(R.id.editTextCode)
        btnSubmit = findViewById(R.id.buttonSubmit)
        btnBack = findViewById(R.id.btn_back)
        btnPause = findViewById(R.id.buttonPause) // Initialize new button

        // Focus editText
        etCodeInput.requestFocus()

        // Get reference to the MediaPlayer from AlarmService
        val serviceIntent = Intent(this, AlarmService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Set an OnClickListener for the submit button
        btnSubmit.setOnClickListener {
            val inputCode = etCodeInput.text.toString()
            val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val correctCode = sharedPreferences.getString("alarm_code", "")

            // Log codes for debugging
            Log.d("DeactivateAlarmActivity", "Input Code: $inputCode, Correct Code: $correctCode")

            // Check if the input code matches the stored code
            if (inputCode == correctCode) {
                // Cancel the alarm
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.cancel(pendingIntent)

                // Stop the AlarmService
                val serviceIntent = Intent(this, AlarmService::class.java)
                stopService(serviceIntent)

                // Get the alarm time from SharedPreferences
                val alarmTime = sharedPreferences.getString("alarm_time", null)
                if (alarmTime != null) {
                    // Schedule the alarm again for the next day
                    val alarmCalendar = Calendar.getInstance()
                    val alarmParts = alarmTime.split(":")
                    val hour = alarmParts[0].toInt()
                    val minute = alarmParts[1].toInt()

                    // Set the calendar time to tomorrow's time
                    alarmCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    alarmCalendar.set(Calendar.MINUTE, minute)
                    alarmCalendar.set(Calendar.SECOND, 0)

                    // If the alarm time is in the past, add a day
                    if (alarmCalendar.timeInMillis <= System.currentTimeMillis()) {
                        alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    // Set the alarm again
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCalendar.timeInMillis, pendingIntent)
                }

                // Update alarm state in preferences
                with(sharedPreferences.edit()) {
                    putBoolean("is_alarm_active", false) // Update preference to indicate alarm is inactive
                    apply()
                }

                // Send broadcast to enable buttons in MainActivity
                val enableButtonsIntent = Intent("ENABLE_BUTTONS")
                LocalBroadcastManager.getInstance(this).sendBroadcast(enableButtonsIntent)

                // Return to MainActivity
                finish()
            } else {
                // Show error message
                Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            // Finish the current activity and go back to the previous one
            finish()
        }

        // Set an OnClickListener for the pause button
        btnPause.setOnClickListener {
            pauseMediaPlayer()
        }
    }

    private fun pauseMediaPlayer() {
        mediaPlayer.let {
            // Stop the MediaPlayer
            it.stop()

            // Prepare the MediaPlayer again for playback
            try {
                it.prepare()
            } catch (e: IOException) {
                e.printStackTrace() // Handle any errors while preparing
            }

            // Remove any previously scheduled restart
            handler.removeCallbacksAndMessages(null)

            // Use Handler to restart after 15 seconds
            handler.postDelayed({
                it.start() // Restart the MediaPlayer
            }, 15000) // 15000 milliseconds = 15 seconds
        }
    }

    // Service connection to access MediaPlayer from AlarmService
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AlarmService.AlarmBinder
            mediaPlayer = binder.getMediaPlayer() // Get the MediaPlayer instance
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Handle service disconnection if necessary
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection) // Unbind the service
    }
}
