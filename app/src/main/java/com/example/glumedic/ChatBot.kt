package com.example.glumedic

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatBot : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnClearHistory: Button
    private lateinit var swInternet: Switch
    private lateinit var adapter: ChatAdapter

    private val messages = mutableListOf<ChatMessageResponse>()
    private var sending = false

    private val token: String?
        get() = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("access_token", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        swInternet = findViewById(R.id.swInternet)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Медицинский ассистент"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            val question = etMessage.text.toString().trim()
            if (question.isNotEmpty() && !sending) {
                hideKeyboard()
                sendMessage(question)
                etMessage.text.clear()
            } else if (question.isEmpty()) {
                Toast.makeText(this, "Введите вопрос", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearHistory.setOnClickListener {
            messages.clear()
            adapter.updateMessages(messages)
            Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage(question: String) {
        val currentToken = token
        if (currentToken.isNullOrEmpty()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            return
        }

        sending = true
        btnSend.isEnabled = false

        val tempMessage = ChatMessageResponse(
            id = -System.currentTimeMillis().toInt(),
            question = question,
            answer = "🤔 Думаю...",
            created_at = ""
        )
        messages.add(tempMessage)
        adapter.updateMessages(messages)
        rvMessages.scrollToPosition(messages.size - 1)

        val useInternet = swInternet.isChecked

        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.ask(AskRequest(query = question, use_internet = useInternet))
                withContext(Dispatchers.Main) {
                    messages.removeAt(messages.size - 1)
                    if (resp.isSuccessful && resp.body() != null) {
                        val body = resp.body()!!
                        messages.add(
                            ChatMessageResponse(
                                id = body.chat_id,
                                question = question,
                                answer = "${sourceBadge(body.source)}${body.answer}",
                                created_at = body.created_at ?: ""
                            )
                        )
                    } else {
                        messages.add(
                            ChatMessageResponse(
                                id = -System.currentTimeMillis().toInt(),
                                question = question,
                                answer = "Ошибка: ${resp.code()}",
                                created_at = ""
                            )
                        )
                        Toast.makeText(this@ChatBot, "Ошибка сервера: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                    adapter.updateMessages(messages)
                    rvMessages.scrollToPosition(messages.size - 1)
                    sending = false
                    btnSend.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages.removeAt(messages.size - 1)
                    messages.add(
                        ChatMessageResponse(
                            id = -System.currentTimeMillis().toInt(),
                            question = question,
                            answer = "Ошибка: ${e.message}",
                            created_at = ""
                        )
                    )
                    adapter.updateMessages(messages)
                    rvMessages.scrollToPosition(messages.size - 1)
                    sending = false
                    btnSend.isEnabled = true
                    Toast.makeText(this@ChatBot, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sourceBadge(source: String): String {
        return when (source) {
            "documents" -> "📚 Ответ по вашим документам:\n\n"
            "internet" -> "🌐 Ответ из интернета:\n\n"
            else -> ""
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etMessage.windowToken, 0)
        etMessage.clearFocus()
    }
}
