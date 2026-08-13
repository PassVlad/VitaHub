@file:Suppress("DEPRECATION")

package com.example.glumedic

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnTakePhoto: Button
    private lateinit var btnChooseFromGallery: Button
    private lateinit var imageView: ImageView
    private lateinit var tvNoImage: TextView
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private var currentPhotoPath: String? = null
    private var currentBitmap: Bitmap? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 1
        private const val GALLERY_REQUEST_CODE = 2
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        initViews()
        setupToolbar()
        setupClickListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnChooseFromGallery = findViewById(R.id.btnChooseFromGallery)
        imageView = findViewById(R.id.imageView)
        tvNoImage = findViewById(R.id.tvNoImage)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Фото документов"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        btnTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        btnChooseFromGallery.setOnClickListener {
            openGallery()
        }

        btnSave.setOnClickListener {
            saveImage()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Разрешения необходимы для работы камеры", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        
        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "DOCUMENT_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    // Фото сделано камерой
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    displayImage(bitmap)
                }
                GALLERY_REQUEST_CODE -> {
                    // Фото выбрано из галереи
                    val selectedImage = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
                    displayImage(bitmap)
                }
            }
        }
    }

    private fun displayImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        imageView.setImageBitmap(bitmap)
        imageView.visibility = ImageView.VISIBLE
        tvNoImage.visibility = TextView.GONE
        btnSave.isEnabled = true
    }

    private fun saveImage() {
        if (currentBitmap != null) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "document_$timeStamp.jpg"

                val file = File(cacheDir, fileName)
                val outputStream = FileOutputStream(file)
                currentBitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.flush()
                outputStream.close()

                Toast.makeText(this, "📤 Загружаю на сервер...", Toast.LENGTH_SHORT).show()
                uploadToServer(file, fileName)

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadToServer(file: File, fileName: String) {
        lifecycleScope.launch {
            try {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestFile)
                val resp = ApiClient.apiService.uploadDocumentFile(part)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(this@CameraActivity, "📸 Документ загружен на сервер!", Toast.LENGTH_SHORT).show()
                        resetCamera()
                    } else {
                        Toast.makeText(this@CameraActivity, "Ошибка сервера: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun resetCamera() {
        imageView.visibility = ImageView.GONE
        tvNoImage.visibility = TextView.VISIBLE
        btnSave.isEnabled = false
        currentBitmap = null
        currentPhotoPath = null
    }

    override fun onBackPressed() {
        if (currentBitmap != null) {
            AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("У вас есть несохраненное фото. Вы уверены, что хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}