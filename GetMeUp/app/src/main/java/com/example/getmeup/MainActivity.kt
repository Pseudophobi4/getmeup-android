package com.example.getmeup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnSetAlarm: Button
    private lateinit var tvAlarmTime: TextView
    private lateinit var setAlarmLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSetAlarm = findViewById(R.id.btn_set_alarm)
        tvAlarmTime = findViewById(R.id.tv_alarm_time)

        // Set the alarm time if previously set
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedTime = sharedPreferences.getString("alarm_time", "No alarm set")
        tvAlarmTime.text = savedTime

        // Register the activity result launcher
        setAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Get the alarm time from the intent
                val alarmTime = result.data?.getStringExtra("alarm_time")
                // Update tvAlarmTime with the new alarm time
                tvAlarmTime.text = alarmTime ?: "No alarm set"

                // Save the new alarm time in SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("alarm_time", alarmTime)
                    apply()
                }
            }
        }

        // Set an OnClickListener on the Button
        btnSetAlarm.setOnClickListener {
            // Start ActivitySetAlarm for a result
            val intent = Intent(this, ActivitySetAlarm::class.java)
            setAlarmLauncher.launch(intent)
        }
    }
}
