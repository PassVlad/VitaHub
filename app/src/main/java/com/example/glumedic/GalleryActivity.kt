package com.example.glumedic

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog

class GalleryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        initViews()
        setupToolbar()
        loadImages()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = GalleryAdapter(emptyList()) { imageFile, action ->
            when (action) {
                GalleryAdapter.ACTION_VIEW -> openImageDetail(imageFile)
                GalleryAdapter.ACTION_RENAME -> renameImage(imageFile)
            }
        }
        recyclerView.adapter = adapter
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Мои документы"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadImages() {
        val documentsDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "glucose_docs")
        val imageFiles = if (documentsDir.exists()) {
            documentsDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".jpeg"))
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }

        if (imageFiles.isNotEmpty()) {
            adapter.updateImages(imageFiles.sortedByDescending { it.lastModified() })
            recyclerView.visibility = RecyclerView.VISIBLE
            tvEmpty.visibility = TextView.GONE
        } else {
            recyclerView.visibility = RecyclerView.GONE
            tvEmpty.visibility = TextView.VISIBLE
        }
    }

    private fun openImageDetail(imageFile: File) {
        val intent = android.content.Intent(this, ImageDetailActivity::class.java)
        intent.putExtra("image_path", imageFile.absolutePath)
        startActivity(intent)
    }

    private fun renameImage(imageFile: File) {
        val input = EditText(this)
        val currentName = getDisplayName(imageFile.name)
        input.setText(currentName)
        input.hint = "Введите название документа"

        AlertDialog.Builder(this)
            .setTitle("Переименовать документ")
            .setMessage("Можно использовать любые языки и символы (кроме / \\ : * ? \" < > |)")
            .setView(input)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    performRename(imageFile, newName)
                } else {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performRename(oldFile: File, newName: String) {
        try {
            // Очищаем имя файла от недопустимых символов
            val cleanName = cleanFileName(newName)
            if (cleanName.isEmpty()) {
                Toast.makeText(this, "Название содержит недопустимые символы", Toast.LENGTH_SHORT).show()
                return
            }

            val extension = oldFile.extension
            val newFileName = if (cleanName.contains(".")) cleanName else "$cleanName.$extension"
            val newFile = File(oldFile.parent, newFileName)

            if (newFile.exists()) {
                Toast.makeText(this, "Файл с таким именем уже существует", Toast.LENGTH_SHORT).show()
                return
            }

            if (oldFile.renameTo(newFile)) {
                Toast.makeText(this, "✓ Документ переименован", Toast.LENGTH_SHORT).show()
                loadImages()
            } else {
                Toast.makeText(this, "Ошибка переименования", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanFileName(fileName: String): String {
        // Удаляем недопустимые для файловой системы символы
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        var cleaned = fileName

        // Заменяем недопустимые символы на подчеркивания
        invalidChars.forEach { char ->
            cleaned = cleaned.replace(char, '_')
        }

        // Удаляем начальные и конечные пробелы, точки
        cleaned = cleaned.trim().trimEnd('.')

        // Проверяем, что имя не пустое
        return if (cleaned.isNotEmpty()) cleaned else ""
    }

    private fun getDisplayName(fileName: String): String {
        return fileName.substringBeforeLast(".").removePrefix("document_")
    }

    override fun onResume() {
        super.onResume()
        loadImages()
    }
}