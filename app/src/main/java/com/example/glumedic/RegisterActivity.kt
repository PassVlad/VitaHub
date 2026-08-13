package com.example.glumedic

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.glumedic.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val fullName = binding.etFullName.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    full_name = fullName.ifEmpty { null }
                )
                viewModel.register(request)
            } else {
                Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }

        observeRegisterResult()
    }

    private fun observeRegisterResult() {
        viewModel.registerResult.observe(this) { result ->
            result.onSuccess { authResponse ->
                saveTokens(authResponse.access_token, "")
                Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.onFailure {
                Toast.makeText(this, "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTokens(access: String, refresh: String) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putString("access_token", access).putString("refresh_token", refresh).apply()
    }
}
