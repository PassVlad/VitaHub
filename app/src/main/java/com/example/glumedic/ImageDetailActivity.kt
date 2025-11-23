package com.example.glumedic

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import androidx.appcompat.app.AlertDialog

class ImageDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imageView: ImageView
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button

    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        imagePath = intent.getStringExtra("image_path") ?: ""

        initViews()
        setupToolbar()
        setupClickListeners()
        loadImage()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        imageView = findViewById(R.id.imageView)
        btnDelete = findViewById(R.id.btnDelete)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Просмотр документа"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        btnDelete.setOnClickListener {
            deleteImage()
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Добавляем кнопку переименования
        val btnRename: Button = findViewById(R.id.btnRename)
        btnRename.setOnClickListener {
            renameCurrentImage()
        }
    }

    // Добавьте метод для переименования
    private fun renameCurrentImage() {
        val input = EditText(this)
        val currentFile = File(imagePath)
        val currentName = currentFile.name.substringBeforeLast(".").removePrefix("document_")
        input.setText(currentName)
        input.hint = "Введите название документа"

        AlertDialog.Builder(this)
            .setTitle("Переименовать документ")
            .setMessage("Можно использовать любые языки и символы (кроме / \\ : * ? \" < > |)")
            .setView(input)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    performRename(currentFile, newName)
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
                imagePath = newFile.absolutePath
                finish()
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

    private fun loadImage() {
        if (imagePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteImage() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Вы уверены, что хотите удалить этот документ?")
            .setPositiveButton("Удалить") { _, _ ->
                val file = java.io.File(imagePath)
                if (file.exists() && file.delete()) {
                    Toast.makeText(this, "Документ удален", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}