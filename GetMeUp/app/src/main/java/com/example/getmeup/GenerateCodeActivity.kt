package com.example.getmeup

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class GenerateCodeActivity : AppCompatActivity() {

    private lateinit var tvCodeValue: TextView
    private lateinit var btnGenerateCode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_code)

        tvCodeValue = findViewById(R.id.tv_code_value)
        btnGenerateCode = findViewById(R.id.btn_generate_code)

        // Retrieve the stored code from SharedPreferences, if available
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("alarm_code", "")
        tvCodeValue.text = savedCode ?: "No code generated"

        btnGenerateCode.setOnClickListener {
            val generatedCode = generateRandomCode(4)
            tvCodeValue.text = generatedCode

            // Store the generated code in SharedPreferences
            with(sharedPreferences.edit()) {
                putString("alarm_code", generatedCode)
                apply()
            }
        }
    }

    // Function to generate a random 12-character string with numbers and uppercase letters
    private fun generateRandomCode(length: Int): String {
        val charPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = Random()
        val code = StringBuilder()

        for (i in 0 until length) {
            val index = random.nextInt(charPool.length)
            code.append(charPool[index])
        }

        return code.toString()
    }
}
