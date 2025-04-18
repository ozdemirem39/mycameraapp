package com.example.mycameraapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AnalyzeSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyze_setup)

        val editFormula = findViewById<EditText>(R.id.editFormula)
        val editM = findViewById<EditText>(R.id.editM)
        val editN = findViewById<EditText>(R.id.editN)
        val btnNext = findViewById<Button>(R.id.btnStartAnalyze)

        btnNext.setOnClickListener {
            val formula = editFormula.text.toString().trim()
            val mValue = editM.text.toString().toFloatOrNull()
            val nValue = editN.text.toString().toFloatOrNull()

            if (formula.isEmpty()) {
                Toast.makeText(this, "Lütfen geçerli bir formül girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mValue == null || nValue == null) {
                Toast.makeText(this, "Lütfen geçerli m ve n değerleri girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AnalyzeCameraActivity::class.java).apply {
                putExtra("formula", formula)
                putExtra("m", mValue)
                putExtra("n", nValue)
            }
            startActivity(intent)
        }
    }
}