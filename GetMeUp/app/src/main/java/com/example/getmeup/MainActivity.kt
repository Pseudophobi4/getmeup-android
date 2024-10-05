package com.example.getmeup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Disable all buttons when the alarm is triggered
            btnSetAlarm.isEnabled = false
            btnCancelAlarm.isEnabled = false
            btnGenerateCode.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSetAlarm = findViewById(R.id.btn_set_alarm)
        btnCancelAlarm = findViewById(R.id.btn_cancel_alarm)
        btnGenerateCode = findViewById(R.id.btn_generate_code)
        tvAlarmTime = findViewById(R.id.tv_alarm_time)

        // Set default alarm time display to XX:XX
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedTime = sharedPreferences.getString("alarm_time", "XX:XX")
        tvAlarmTime.text = savedTime

        // Check if the alarm is active on app launch
        if (sharedPreferences.getBoolean("is_alarm_active", false)) {
            disableAllButtons()
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

        // Set an OnClickListener on the Button to launch ActivitySetAlarm
        btnSetAlarm.setOnClickListener {
            val intent = Intent(this, ActivitySetAlarm::class.java)
            setAlarmLauncher.launch(intent)
        }

        // Set an OnClickListener on the Cancel Button to cancel the alarm
        btnCancelAlarm.setOnClickListener {
            cancelAlarm()
            tvAlarmTime.text = "XX:XX"

            // Enable the buttons when the alarm is canceled
            enableAllButtons()

            // Update SharedPreferences to indicate alarm is inactive
            with(sharedPreferences.edit()) {
                putBoolean("is_alarm_active", false)
                remove("alarm_time")
                apply()
            }
        }

        // Set OnClickListener for Generate Code button
        btnGenerateCode.setOnClickListener {
            val intent = Intent(this, ActivityGenerateCode::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the local broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
            alarmReceiver, IntentFilter("ALARM_TRIGGERED")
        )

        // Check if the alarm is active on resume
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getBoolean("is_alarm_active", false)) {
            disableAllButtons()
        } else {
            enableAllButtons()
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the local broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmReceiver)
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

    private fun disableAllButtons() {
        btnSetAlarm.isEnabled = false
        btnCancelAlarm.isEnabled = false
        btnGenerateCode.isEnabled = false
    }

    private fun enableAllButtons() {
        btnSetAlarm.isEnabled = true
        btnCancelAlarm.isEnabled = true
        btnGenerateCode.isEnabled = true
    }
}
