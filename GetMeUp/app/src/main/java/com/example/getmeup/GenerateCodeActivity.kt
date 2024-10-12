package com.example.getmeup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class GenerateCodeActivity : AppCompatActivity() {

    private lateinit var tvCodeValue: TextView
    private lateinit var btnGenerateCode: Button
    private lateinit var btnToggleVisibility: Button
    private var isCodeVisible = false // Track visibility state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_code)

        tvCodeValue = findViewById(R.id.tv_code_value)
        btnGenerateCode = findViewById(R.id.btn_generate_code)
        btnToggleVisibility = findViewById(R.id.btn_toggle_visibility) // Button to toggle visibility

        // Retrieve the stored code from SharedPreferences
        val sharedPreferences = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("alarm_code", "")

        // Check if the savedCode is empty or null, and if so, generate a new one
        if (savedCode.isNullOrEmpty()) {
            val generatedCode = generateRandomCode(4)
            tvCodeValue.text = generatedCode

            // Show the new code
            tvCodeValue.transformationMethod = null
            btnToggleVisibility.text = "Hide"
            isCodeVisible = true

            // Store the generated code in SharedPreferences
            with(sharedPreferences.edit()) {
                putString("alarm_code", generatedCode)
                apply()
            }
        } else {
            // Use the saved code
            tvCodeValue.text = savedCode
        }

        // Initially hide the code if one was loaded from SharedPreferences
        if (!isCodeVisible) {
            tvCodeValue.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }

        btnGenerateCode.setOnClickListener {
            showMaterialConfirmationDialog(sharedPreferences) // Show Material Design dialog before generating a new code
        }

        btnToggleVisibility.setOnClickListener {
            toggleCodeVisibility()
        }
    }

    // Function to generate a random 4-character string with numbers and uppercase letters
    private fun generateRandomCode(length: Int): String {
        val charPool = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val random = Random()
        val code = StringBuilder()

        for (i in 0 until length) {
            val index = random.nextInt(charPool.length)
            code.append(charPool[index])
        }

        return code.toString()
    }

    // Function to show a Material Design confirmation dialog before generating a new code
    private fun showMaterialConfirmationDialog(sharedPreferences: SharedPreferences) {
        MaterialAlertDialogBuilder(this)
            .setMessage("Are you sure you want to generate a new code? This will overwrite the existing one.")
            .setPositiveButton("Yes") { dialog, _ ->
                val generatedCode = generateRandomCode(8)
                tvCodeValue.text = generatedCode

                // Show the new code
                tvCodeValue.transformationMethod = null
                btnToggleVisibility.text = "Hide"
                isCodeVisible = true

                // Store the generated code in SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("alarm_code", generatedCode)
                    apply()
                }
                dialog.dismiss() // Dismiss the dialog after generating the code
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Cancel and close the dialog if the user presses "Cancel"
            }
            .show()
    }

    // Toggle the visibility of the code in the TextView
    private fun toggleCodeVisibility() {
        if (isCodeVisible) {
            // Hide the code
            tvCodeValue.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            btnToggleVisibility.text = "Show"
        } else {
            // Show the code
            tvCodeValue.transformationMethod = null
            btnToggleVisibility.text = "Hide"
        }
        isCodeVisible = !isCodeVisible
    }
}
