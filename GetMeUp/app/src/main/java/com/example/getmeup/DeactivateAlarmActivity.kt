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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.util.Calendar

import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.google.android.material.progressindicator.CircularProgressIndicator

class DeactivateAlarmActivity : AppCompatActivity() {

    private lateinit var etCodeInput: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: Button
    private lateinit var btnPause: Button // New pause button
    private lateinit var mediaPlayer: MediaPlayer // MediaPlayer reference
    private lateinit var tvErrorMessage: TextView
    private lateinit var tvCodeLabel: TextView

    private lateinit var progressBar: CircularProgressIndicator
    private var valueAnimator: ValueAnimator? = null

    private var alarmService: AlarmService? = null // AlarmService reference
    private val handler = Handler(Looper.getMainLooper()) // Handler for delayed tasks
    private var isPaused = false // Track if the pause was activated

    private var isBackDisabled = false // Track if back is disabled
    private var activityPaused = false // Track if the app is paused

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

        etCodeInput = findViewById(R.id.et_code_input)
        btnSubmit = findViewById(R.id.buttonSubmit)
        btnBack = findViewById(R.id.btn_back)
        btnPause = findViewById(R.id.buttonPause) // Initialize pause button
        tvCodeLabel = findViewById(R.id.tv_code_label)

        progressBar = findViewById(R.id.progressBar)
        tvErrorMessage = findViewById(R.id.tv_error_message)

        // Initially hide the error message
        tvErrorMessage.text = ""

        etCodeInput.requestFocus() // Set focus to the code input field

        // Bind to the AlarmService to control MediaPlayer and vibration
        val serviceIntent = Intent(this, AlarmService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Handle the submit button click
        btnSubmit.setOnClickListener {
            val inputCode = etCodeInput.text.toString()
            val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val correctCode = sharedPreferences.getString("alarm_code", "")

            Log.d(
                "DeactivateAlarmActivity",
                "Input Code: $inputCode, Correct Code: $correctCode"
            )

            if (inputCode == correctCode) {
                isPaused = false
                activityPaused = true

                // Stop sound and vibration immediately
                stopAlarmSoundAndVibration()

                // Proceed with alarm deactivation
                deactivateAlarm()

                // Show the deactivation message
                showDeactivationMessage()
            } else {
                tvErrorMessage.text = "Incorrect code. Please try again."
            }
        }

        // Handle the back button click
        btnBack.setOnClickListener {
            if (!isBackDisabled)
                finish()
        }

        // Handle the pause button click
        btnPause.setOnClickListener {
            if (isPaused) {
                resetProgressBarSmoothly(progressBar.progress)

                // Add a delay before calling pauseMediaPlayer and startCountdown
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!activityPaused) { // Check if the activity is not paused
                        pauseMediaPlayer()
                        startCountdown()
                    }
                }, 500) // 500 ms delay
            } else {
                pauseMediaPlayer()
                startCountdown()
            }
        }

        // Add a TextWatcher to clear the error message when the input changes
        etCodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvErrorMessage.text = ""
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Register the OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isBackDisabled) {
                    isEnabled = false // Disable this callback
                    // Call finish() or any other back action you want
                    finish() // Example: finish the activity
                }
                // If back is disabled, do nothing (back press is ignored)
            }
        })
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
    }

    private fun showDeactivationMessage() {

        // Get reference to the pre-defined TextView
        val tvDeactivationMessage = findViewById<TextView>(R.id.tv_deactivation_message)

        // List of views to fade out
        val viewsToHide = listOf(etCodeInput, btnSubmit, btnPause, progressBar, tvErrorMessage, tvCodeLabel)

        // Fade out all other views
        viewsToHide.forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(500) // Animation duration in ms
                .withEndAction { view.visibility = android.view.View.GONE } // Set GONE after fading out
                .start()
        }

        // Delay the fade-in of the deactivation message
        Handler(Looper.getMainLooper()).postDelayed({
            tvDeactivationMessage.visibility = android.view.View.VISIBLE // Make visible before animating
            tvDeactivationMessage.animate()
                .alpha(1f) // Animate to full opacity
                .setDuration(500) // Animation duration in ms
                .start()
        }, 500) // 500ms delay before showing the message
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

        // alarmService?.stopVibrationTemporarily() // Stop vibration via AlarmService
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
            // alarmService?.resumeVibration()
            isPaused = false // Reset the flag after restarting
        }
    }

    private fun stopAlarmSoundAndVibration() {
        // Stop MediaPlayer if playing
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop() // Stop playback
            try {
                mediaPlayer.prepare() // Prepare for potential reuse
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Stop the vibration via AlarmService
        alarmService?.stopVibrationTemporarily()

        // Clear any scheduled callbacks
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        activityPaused = false
    }

    override fun onPause() {
        super.onPause()
        activityPaused = true
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
