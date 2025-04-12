package com.example.mycameraapp

import com.example.mycameraapp.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private var btnStartCamera: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartCamera: Button = findViewById<Button>(R.id.btnStartCamera)
        btnStartCamera.setOnClickListener(View.OnClickListener {
            val intent = Intent(
                this@MainActivity,
                CameraActivity::class.java
            )
            startActivity(intent)
        })

        val btnAnalyze = findViewById<Button>(R.id.btnAnalyze)
        btnAnalyze.setOnClickListener {
            val intent = Intent(this, AnalyzeSetupActivity::class.java)
            startActivity(intent)
        }
    }
}
