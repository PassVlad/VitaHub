package com.example.glumedic

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnAddDocument: MaterialButton
    private lateinit var rvDocuments: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DocumentAdapter

    private var documents: List<DocumentInfo> = emptyList()

    private val token: String?
        get() = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("access_token", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents)

        toolbar = findViewById(R.id.toolbar)
        btnAddDocument = findViewById(R.id.btnAddDocument)
        rvDocuments = findViewById(R.id.rvDocuments)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupToolbar()
        setupList()
        setupButtons()

        loadDocuments()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupList() {
        adapter = DocumentAdapter(emptyList()) { doc ->
            confirmDelete(doc)
        }
        rvDocuments.layoutManager = LinearLayoutManager(this)
        rvDocuments.adapter = adapter
    }

    private fun setupButtons() {
        btnAddDocument.setOnClickListener { showAddDocumentDialog() }
    }

    private fun loadDocuments() {
        val currentToken = token
        if (currentToken.isNullOrEmpty()) {
            showToast("Требуется авторизация")
            return
        }
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.listDocuments()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        documents = resp.body() ?: emptyList()
                        adapter.updateDocuments(documents)
                        updateEmptyState()
                    } else {
                        showToast("Ошибка загрузки: ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка сети: ${e.message}")
                }
            }
        }
    }

    private fun showAddDocumentDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 8.dp, 24.dp, 0.dp)
        }

        val etName = EditText(this).apply {
            hint = "Название документа (например: Выписка_01)"
        }
        val etText = EditText(this).apply {
            hint = "Текст документа (можно из OCR)"
            setLines(6)
            gravity = android.view.Gravity.TOP
        }
        container.addView(etName, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        container.addView(etText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8.dp })

        AlertDialog.Builder(this)
            .setTitle("Добавить документ")
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val text = etText.text.toString().trim()
                if (name.isEmpty() || text.isEmpty()) {
                    showToast("Заполните название и текст")
                } else {
                    uploadDocument(name, text)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun uploadDocument(name: String, text: String) {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.uploadDocument(
                    UploadDocumentRequest(filename = name, text = text)
                )
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        showToast("Документ сохранён")
                        loadDocuments()
                    } else {
                        showToast("Ошибка сервера: ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка сети: ${e.message}")
                }
            }
        }
    }

    private fun confirmDelete(doc: DocumentInfo) {
        AlertDialog.Builder(this)
            .setTitle("Удалить документ?")
            .setMessage(doc.filename)
            .setPositiveButton("Удалить") { _, _ ->
                deleteDocument(doc)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteDocument(doc: DocumentInfo) {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.deleteDocument(doc.id)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        showToast("Документ удалён")
                        loadDocuments()
                    } else {
                        showToast("Ошибка сервера: ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка сети: ${e.message}")
                }
            }
        }
    }

    private fun updateEmptyState() {
        val isEmpty = documents.isEmpty()
        rvDocuments.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
