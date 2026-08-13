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
    private lateinit var buttonChatbot: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        
        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        buttonChatbot=findViewById<Button>(R.id.btn_chat_menu)
        buttonGlucose = findViewById<Button>(R.id.btn_glu)
        buttonPress = findViewById<Button>(R.id.btn_press)
        buttonNext = findViewById<Button>(R.id.btn_next)
        buttonChatbot.isEnabled = false
    }

    private fun setupClickListeners (){
        buttonGlucose.setOnClickListener{
            val intent = Intent(this, Glucose::class.java)
            startActivity(intent)
        }
        buttonPress.setOnClickListener{
            val intent = Intent(this, PressureActivity::class.java)
            startActivity(intent)
        }
        buttonNext.setOnClickListener{
            val intent = Intent(this, DocumentsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}