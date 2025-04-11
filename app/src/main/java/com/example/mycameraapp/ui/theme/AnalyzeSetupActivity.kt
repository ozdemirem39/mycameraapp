package com.example.mycameraapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AnalyzeSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze_setup)

        val spinnerChannel = findViewById<Spinner>(R.id.spinnerChannel)
        val editM = findViewById<EditText>(R.id.editM)
        val editN = findViewById<EditText>(R.id.editN)
        val btnNext = findViewById<Button>(R.id.btnStartAnalyze)

        val channels = arrayOf("R", "G", "B", "H", "S", "V")
        spinnerChannel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, channels)

        btnNext.setOnClickListener {
            val selectedChannel = spinnerChannel.selectedItem.toString()
            val mValue = editM.text.toString().toFloatOrNull()
            val nValue = editN.text.toString().toFloatOrNull()

            if (mValue == null || nValue == null) {
                Toast.makeText(this, "Lütfen geçerli m ve n değerleri girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AnalyzeCameraActivity::class.java).apply {
                putExtra("channel", selectedChannel)
                putExtra("m", mValue)
                putExtra("n", nValue)
            }
            startActivity(intent)
        }
    }
}
