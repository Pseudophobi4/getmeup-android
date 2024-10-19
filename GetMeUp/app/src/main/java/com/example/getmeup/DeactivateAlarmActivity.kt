package com.example.getmeup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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

import android.view.animation.LinearInterpolator
import com.google.android.material.progressindicator.CircularProgressIndicator

class DeactivateAlarmActivity : AppCompatActivity() {

    private lateinit var etCodeInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button
    private lateinit var btnPause: Button // New pause button
    private lateinit var mediaPlayer: MediaPlayer // MediaPlayer reference

    private lateinit var progressBar: CircularProgressIndicator
    private var valueAnimator: ValueAnimator? = null

    private var alarmService: AlarmService? = null // AlarmService reference
    private val handler = Handler(Looper.getMainLooper()) // Handler for delayed tasks
    private var isPaused = false // Track if the pause was activated

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

        progressBar = findViewById(R.id.progressBar)

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
                isPaused = false
                deactivateAlarm()
            } else {
                Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle the back button click
        btnBack.setOnClickListener { finish() }

        // Handle the pause button click
        btnPause.setOnClickListener {
            if (isPaused) {
                resetProgressBarSmoothly(progressBar.progress)

                // Add a delay before calling pauseMediaPlayer and startCountdown
                Handler(Looper.getMainLooper()).postDelayed({
                    pauseMediaPlayer()
                    startCountdown()
                }, 500) // 500 ms delay
            } else {
                pauseMediaPlayer()
                startCountdown()
            }
        }
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

    private fun rescheduleAlarm(
        alarmTime: String,
        alarmManager: AlarmManager,
        pendingIntent: PendingIntent
    ) {
        val alarmCalendar = Calendar.getInstance()
        val (hour, minute) = alarmTime.split(":").map { it.toInt() }

        alarmCalendar.set(Calendar.HOUR_OF_DAY, hour)
        alarmCalendar.set(Calendar.MINUTE, minute)
        alarmCalendar.set(Calendar.SECOND, 0)

        if (alarmCalendar.timeInMillis <= System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmCalendar.timeInMillis,
            pendingIntent
        )
    }

    private fun pauseMediaPlayer() {
        isPaused = true // Set the flag to true


        mediaPlayer.stop() // Stop MediaPlayer

        try {
            mediaPlayer.prepare() // Prepare it for later use
        } catch (e: IOException) {
            e.printStackTrace()
        }

        alarmService?.stopVibrationTemporarily() // Stop vibration via AlarmService
        handler.removeCallbacksAndMessages(null) // Clear any scheduled tasks

        // Restart after 30 seconds unless onDestroy is called first
        handler.postDelayed({
            restartAlarm()
        }, 30000)
    }

    private fun startCountdown() {
        // Cancel any existing animator
        valueAnimator?.cancel()

        val totalTime = 30000L
        progressBar.max = 999999999

        // Create a ValueAnimator that counts down from 999999999 to 0
        valueAnimator = ValueAnimator.ofInt(999999999, 0).apply {
            duration = totalTime
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                progressBar.progress = progress
            }

            // Call this when the animation finishes
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Pass the current progress to resetProgressBarSmoothly
                    resetProgressBarSmoothly(progressBar.progress)
                }
            })
        }

        // Start the animation
        valueAnimator?.start()
    }

    private fun resetProgressBarSmoothly(currentProgress: Int) {
        // Create a ValueAnimator that starts from the current progress to max (999999999)
        ValueAnimator.ofInt(currentProgress, 999999999).apply {
            duration = 500
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
        }.start()
    }

    private fun restartAlarm() {
        if (isPaused) {
            mediaPlayer.start()
            alarmService?.resumeVibration()
            isPaused = false // Reset the flag after restarting
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPaused) {
            // Cancel the value animator
            valueAnimator?.cancel()
            valueAnimator = null // Reset the reference to avoid memory leaks
            // Pass the current progress to resetProgressBarSmoothly
            resetProgressBarSmoothly(progressBar.progress)
            // Immediately restart media and vibration if paused
            restartAlarm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection) // Unbind the AlarmService
    }
}
