package com.example.getmeup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "AlarmServiceChannel"
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences
    private var vibratorManager: VibratorManager? = null // VibratorManager for vibration control
    private val NOTIFICATION_ID = 1 // Notification ID for cancellation

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Set up AudioAttributes for alarm sounds
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        // Initialize the MediaPlayer with the sound and set looping
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setDataSource(applicationContext, android.net.Uri.parse("android.resource://${packageName}/raw/alarm_sound")) // Replace with your sound resource
            isLooping = true // Set to loop the sound
            prepare() // Prepare the media player for playback
        }

        // Initialize the VibratorManager (for Android 12 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Set the alarm volume when the alarm starts ringing
        setAlarmVolume()

        // Create the notification and run the service in the foreground
        createNotificationChannel()  // For Android O and higher
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm is ringing")
            .setContentText("It's time to get up!")
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's icon
            .setContentIntent(pendingIntent)
            .build()

        // Start the service in the foreground
        startForeground(NOTIFICATION_ID, notification)

        // Start the media player
        mediaPlayer.start()

        // Start the vibration if supported
        startVibration()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()

        // Stop the vibration
        stopVibration()

        // Clear the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID) // Cancel the notification with the specific ID
    }

    // Create notification channel for Android O and above
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "This channel is used for the alarm service"
            }

            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setAlarmVolume() {
        // Retrieve the saved volume from SharedPreferences
        val savedVolume = sharedPreferences.getFloat("alarm_volume", 100f) // Default to 100 if not set
        val volumeLevel = (savedVolume / 100 * audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
    }

    // Start vibration using VibratorManager with a built-in pattern
    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use a predefined vibration effect for a simple repeating pattern
            val vibrationEffect = VibrationEffect.createWaveform(
                longArrayOf(0, 500, 500), // Pattern: wait 0ms, vibrate 500ms, pause 500ms
                0 // Repeat indefinitely
            )
            vibratorManager?.defaultVibrator?.vibrate(vibrationEffect) // Use defaultVibrator
        }
    }

    // Stop vibration
    private fun stopVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.defaultVibrator?.cancel() // Use defaultVibrator to stop vibration
        }
    }
}
