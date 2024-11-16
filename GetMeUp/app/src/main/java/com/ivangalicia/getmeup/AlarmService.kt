package com.ivangalicia.getmeup

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences
    private var vibratorManager: VibratorManager? = null
    private val CHANNEL_ID = "AlarmServiceChannel"
    private val NOTIFICATION_ID = 1
    private var originalVolume = 0
    private lateinit var handler: Handler
    private lateinit var vibrationRunnable: Runnable

    // Broadcast receiver to lock alarm volume
    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setAlarmVolume() // Reset the alarm volume if changed
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Initialize the media player
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(applicationContext, android.net.Uri.parse("android.resource://${packageName}/raw/alarm_sound"))
            isLooping = true
            prepare()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        handler = Handler(Looper.getMainLooper())
        vibrationRunnable = object : Runnable {
            override fun run() {
                startVibration()
                handler.postDelayed(this, 1000) // Restart vibration every second
            }
        }

        // Register receiver to listen for volume changes
        registerReceiver(volumeChangeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setAlarmVolume() // Lock alarm volume

        // Create a notification channel and start foreground service
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm Triggered!")
            .setContentText("Enter your code to deactivate.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        mediaPlayer.start() // Start alarm sound
        startVibration() // Start vibration
        handler.post(vibrationRunnable) // Schedule periodic vibration

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.release()

        stopVibration() // Stop vibration
        handler.removeCallbacks(vibrationRunnable) // Remove scheduled vibrations

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0) // Restore volume

        unregisterReceiver(volumeChangeReceiver) // Unregister receiver

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID) // Clear notification
    }

    // Method to temporarily stop vibration
    fun stopVibrationTemporarily() {
        stopVibration() // Stop any ongoing vibration
        handler.removeCallbacks(vibrationRunnable) // Cancel scheduled vibrations
    }

    // Method to resume vibration
    fun resumeVibration() {
        startVibration() // Start vibration
        handler.post(vibrationRunnable) // Schedule periodic vibration again
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Alarm Service Channel", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "This channel is used for the alarm service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setAlarmVolume() {
        val savedVolume = sharedPreferences.getFloat("alarm_volume", 100f)
        val volumeLevel = (savedVolume / 100 * audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
    }

    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrationEffect = VibrationEffect.createWaveform(
                longArrayOf(0, 500, 500), 0 // Repeat indefinitely
            )
            vibratorManager?.defaultVibrator?.vibrate(vibrationEffect)
        }
    }

    private fun stopVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.defaultVibrator?.cancel()
        }
    }

    // Binder class to expose service and MediaPlayer instance
    inner class AlarmBinder : Binder() {
        fun getMediaPlayer(): MediaPlayer = mediaPlayer
        fun getService(): AlarmService = this@AlarmService
    }

    override fun onBind(intent: Intent?): IBinder {
        return AlarmBinder() // Return the binder instance
    }
}
