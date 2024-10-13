package com.example.getmeup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.slider.Slider
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var btnSetAlarm: Button
    private lateinit var btnCancelAlarm: Button
    private lateinit var btnGenerateCode: Button
    private lateinit var tvAlarmTime: TextView
    private lateinit var setAlarmLauncher: ActivityResultLauncher<Intent>
    private lateinit var btnDeactivateAlarm: Button
    private lateinit var volumeSlider: Slider
    private lateinit var audioManager: AudioManager
    private lateinit var btnExit: Button

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Disable buttons when the alarm is triggered
            disableAllButtons()
        }
    }

    private val enableButtonsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Enable buttons when the code is successfully submitted
            enableAllButtons()
            // Check alarm state after enabling buttons
            val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            updateButtonState(sharedPreferences)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        btnSetAlarm = findViewById(R.id.btn_set_alarm)
        btnCancelAlarm = findViewById(R.id.btn_cancel_alarm)
        btnGenerateCode = findViewById(R.id.btn_generate_code)
        btnDeactivateAlarm = findViewById(R.id.btn_deactivate_alarm)
        tvAlarmTime = findViewById(R.id.tv_alarm_time)
        volumeSlider = findViewById(R.id.volume_slider)
        btnExit = findViewById(R.id.btn_exit)

        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedTime = sharedPreferences.getString("alarm_time", "OFF")
        val militaryTime = savedTime ?: "OFF"
        tvAlarmTime.text = convertMilitaryTimeTo12HourFormat(militaryTime)

        // Set the slider value from SharedPreferences
        val savedVolume = sharedPreferences.getFloat("alarm_volume", 100f) // Default to 100 if not set
        volumeSlider.value = savedVolume

        // Check if alarm_code is present
        val alarmCode = sharedPreferences.getString("alarm_code", null)
        if (alarmCode.isNullOrEmpty()) {
            // Disable all buttons except Generate Code
            disableAllButtonsExceptGenerateCode()
        } else {
            // Enable/Disable buttons based on alarm state
            updateButtonState(sharedPreferences)
        }

        // Register the activity result launcher for setting alarm
        setAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val alarmTime = result.data?.getStringExtra("alarm_time")
                tvAlarmTime.text = alarmTime ?: "OFF"

                with(sharedPreferences.edit()) {
                    putString("alarm_time", alarmTime)
                    apply()
                }
            }
        }

        // Set OnClickListener for the Set Alarm button
        btnSetAlarm.setOnClickListener {
            val intent = Intent(this, SetAlarmActivity::class.java)
            setAlarmLauncher.launch(intent)
        }

        // Set OnClickListener for Cancel Alarm button
        btnCancelAlarm.setOnClickListener {
            cancelAlarm()
            tvAlarmTime.text = "OFF" // Update the TextView to show "OFF"

            // Update the alarm state and time in SharedPreferences
            with(sharedPreferences.edit()) {
                putString("alarm_time", "OFF") // Set alarm_time to "OFF"
                apply()
            }
        }

        // Set OnClickListener for Generate Code button
        btnGenerateCode.setOnClickListener {
            val intent = Intent(this, GenerateCodeActivity::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Deactivate Alarm button
        btnDeactivateAlarm.setOnClickListener {
            val intent = Intent(this, DeactivateAlarmActivity::class.java)
            startActivity(intent)
        }

        // Set an OnChangeListener to update SharedPreferences when the slider value changes
        volumeSlider.addOnChangeListener { slider, value, fromUser ->
            val volumeLevel = (value / 100 * audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)

            // Save the slider value in SharedPreferences
            with(sharedPreferences.edit()) {
                putFloat("alarm_volume", value)
                apply()
            }
        }

        // Set OnClickListener for the Exit button
        btnExit.setOnClickListener {
            // Exit the app
            finishAffinity() // Closes all activities and exits the app
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the local broadcast receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmReceiver, IntentFilter("ALARM_TRIGGERED")
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            enableButtonsReceiver, IntentFilter("ENABLE_BUTTONS")
        )

        // Check again if alarm_code is present when resuming
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmCode = sharedPreferences.getString("alarm_code", null)
        if (alarmCode.isNullOrEmpty()) {
            disableAllButtonsExceptGenerateCode()
        } else {
            updateButtonState(sharedPreferences)
        }

        // Update the displayed alarm time in 12-hour format
        val savedTime = sharedPreferences.getString("alarm_time", "OFF") ?: "OFF"
        tvAlarmTime.text = convertMilitaryTimeTo12HourFormat(savedTime)

        // Restore the slider value from SharedPreferences
        val savedVolume = sharedPreferences.getFloat("alarm_volume", 100f) // Default to 100 if not set
        volumeSlider.value = savedVolume

        // Check if the alarm is NOT active before setting the alarm volume
        val isAlarmActive = sharedPreferences.getBoolean("is_alarm_active", false)
        if (!isAlarmActive) {
            // Set the alarm volume to match the slider value
            val volumeLevel = (savedVolume / 100 * audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeLevel, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the local broadcast receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(enableButtonsReceiver)
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel the alarm
        alarmManager.cancel(pendingIntent)
    }

    private fun updateButtonState(sharedPreferences: SharedPreferences) {
        if (sharedPreferences.getBoolean("is_alarm_active", false)) {
            disableAllButtons()
        } else {
            enableAllButtons()
        }
    }

    private fun disableAllButtons() {
        btnSetAlarm.isEnabled = false
        btnCancelAlarm.isEnabled = false
        btnGenerateCode.isEnabled = false
        btnDeactivateAlarm.isEnabled = true // Enable Deactivate Alarm when alarm is active
        volumeSlider.isEnabled = false
        btnExit.isEnabled = true
    }

    private fun enableAllButtons() {
        btnSetAlarm.isEnabled = true
        btnCancelAlarm.isEnabled = true
        btnGenerateCode.isEnabled = true
        btnDeactivateAlarm.isEnabled = false // Disable Deactivate Alarm when alarm is inactive
        volumeSlider.isEnabled = true
        btnExit.isEnabled = true
    }

    private fun disableAllButtonsExceptGenerateCode() {
        btnSetAlarm.isEnabled = false
        btnCancelAlarm.isEnabled = false
        btnGenerateCode.isEnabled = true // Only Generate Code button enabled
        btnDeactivateAlarm.isEnabled = false
        volumeSlider.isEnabled = false
        btnExit.isEnabled = true
    }

    private fun convertMilitaryTimeTo12HourFormat(militaryTime: String): String {
        // Check if the time is "OFF" and return it unchanged
        if (militaryTime == "OFF") {
            return militaryTime
        }

        val parts = militaryTime.split(":")
        if (parts.size != 2) {
            return militaryTime // Return the original time if the format is incorrect
        }

        val hour = parts[0].toInt()
        val minute = parts[1]
        val amPm = if (hour >= 12) "PM" else "AM"
        val formattedHour = if (hour % 12 == 0) 12 else hour % 12

        // Specify Locale for formatting
        return String.format(Locale.getDefault(), "%02d:%s %s", formattedHour, minute, amPm)
    }
}
