package com.example.glumedic

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var buttonGlucose: Button
    private lateinit var buttonPress: Button
    private lateinit var buttonNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        buttonGlucose = findViewById<Button>(R.id.btn_glu)
        buttonPress = findViewById<Button>(R.id.btn_press)
        buttonPress.isEnabled=false
        buttonNext = findViewById<Button>(R.id.btn_next)
        buttonNext.isEnabled=false
    }

    private fun setupClickListeners (){
        buttonGlucose.setOnClickListener{
            val intent = Intent(this, Glucose::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}