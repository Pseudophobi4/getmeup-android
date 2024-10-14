package com.example.getmeup

import android.app.*
import android.content.*
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
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

    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Reset the volume back to alarm level if the user changes it
            setAlarmVolume()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        // Save the current system alarm volume to restore later
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Initialize media player
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            setDataSource(applicationContext, android.net.Uri.parse("android.resource://${packageName}/raw/alarm_sound"))
            isLooping = true
            prepare()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        // Register volume change receiver
        registerReceiver(volumeChangeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

        // Initialize Handler and Runnable for periodic vibration
        handler = Handler(Looper.getMainLooper())
        vibrationRunnable = object : Runnable {
            override fun run() {
                startVibration() // Restart vibration periodically
                handler.postDelayed(this, 1000) // Repeat every second (adjust as needed)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Lock the alarm volume when alarm starts
        setAlarmVolume()

        // Create and display notification
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm is ringing")
            .setContentText("It's time to get up!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start media player and vibration
        mediaPlayer.start()
        startVibration()
        handler.post(vibrationRunnable) // Start the periodic vibration

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the media player and release resources
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        mediaPlayer.release()

        // Stop vibration and remove the periodic vibration callback
        stopVibration()
        handler.removeCallbacks(vibrationRunnable)

        // Restore the original system alarm volume
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)

        // Unregister the volume change receiver
        unregisterReceiver(volumeChangeReceiver)

        // Clear notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Alarm Service Channel", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "This channel is used for the alarm service"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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
}
