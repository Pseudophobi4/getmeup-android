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
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val CHANNEL_ID = "AlarmServiceChannel"
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences

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
        startForeground(1, notification)

        // Start the media player
        mediaPlayer.start()

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
}
