package com.example.glumedic

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

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