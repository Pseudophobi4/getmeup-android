package com.example.getmeup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var btnSetAlarm: Button
    private lateinit var btnCancelAlarm: Button
    private lateinit var btnGenerateCode: Button
    private lateinit var tvAlarmTime: TextView
    private lateinit var setAlarmLauncher: ActivityResultLauncher<Intent>
    private lateinit var btnDeactivateAlarm: Button

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

        btnSetAlarm = findViewById(R.id.btn_set_alarm)
        btnCancelAlarm = findViewById(R.id.btn_cancel_alarm)
        btnGenerateCode = findViewById(R.id.btn_generate_code)
        btnDeactivateAlarm = findViewById(R.id.btn_deactivate_alarm)
        tvAlarmTime = findViewById(R.id.tv_alarm_time)

        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedTime = sharedPreferences.getString("alarm_time", "XX:XX")
        tvAlarmTime.text = savedTime

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
                tvAlarmTime.text = alarmTime ?: "XX:XX"

                with(sharedPreferences.edit()) {
                    putString("alarm_time", alarmTime)
                    apply()
                }
            }
        }

        // Set OnClickListener for the Set Alarm button
        btnSetAlarm.setOnClickListener {
            val intent = Intent(this, ActivitySetAlarm::class.java)
            setAlarmLauncher.launch(intent)
        }

        // Set OnClickListener for Cancel Alarm button
        btnCancelAlarm.setOnClickListener {
            cancelAlarm()
            tvAlarmTime.text = "XX:XX"
            enableAllButtons() // Enable buttons when the alarm is canceled
            updateAlarmStateInPreferences(sharedPreferences, false) // Update state to inactive
        }

        // Set OnClickListener for Generate Code button
        btnGenerateCode.setOnClickListener {
            val intent = Intent(this, ActivityGenerateCode::class.java)
            startActivity(intent)
        }

        // Set OnClickListener for Deactivate Alarm button
        btnDeactivateAlarm.setOnClickListener {
            val intent = Intent(this, ActivityDeactivateAlarm::class.java)
            startActivity(intent)
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
    }

    private fun enableAllButtons() {
        btnSetAlarm.isEnabled = true
        btnCancelAlarm.isEnabled = true
        btnGenerateCode.isEnabled = true
        btnDeactivateAlarm.isEnabled = false // Disable Deactivate Alarm when alarm is inactive
    }

    private fun disableAllButtonsExceptGenerateCode() {
        btnSetAlarm.isEnabled = false
        btnCancelAlarm.isEnabled = false
        btnGenerateCode.isEnabled = true // Only Generate Code button enabled
        btnDeactivateAlarm.isEnabled = false
    }

    private fun updateAlarmStateInPreferences(sharedPreferences: SharedPreferences, isActive: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("is_alarm_active", isActive)
            apply()
        }
    }
}
