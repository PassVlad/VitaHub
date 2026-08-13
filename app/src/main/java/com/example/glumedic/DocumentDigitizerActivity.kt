package com.example.glumedic

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentDigitizerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imageView: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var btnDigitize: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvResult: TextView
    private lateinit var resultCard: MaterialCardView

    private var selectedImageUri: Uri? = null
    private var recognizedText: String = ""

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_digitizer)

        initViews()
        setupToolbar()
        setupClickListeners()

        // Показать информацию о сервере для отладки
//        Toast.makeText(this, "Сервер: ${ApiClient.getBaseUrl()}", Toast.LENGTH_LONG).show()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        imageView = findViewById(R.id.imageView)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnDigitize = findViewById(R.id.btnDigitize)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        tvResult = findViewById(R.id.tvResult)
        resultCard = findViewById(R.id.resultCard)

        // Начальное состояние
        resultCard.visibility = android.view.View.GONE
        progressBar.visibility = android.view.View.GONE
        btnDigitize.isEnabled = false
        btnSave.isEnabled = false
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Оцифровка документов"

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnDigitize.setOnClickListener {
            selectedImageUri?.let { uri ->
                sendImageToServer(uri)
            } ?: run {
                Toast.makeText(this, "Сначала выберите изображение", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            saveRecognizedText()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            imageView.setImageURI(selectedImageUri)
            imageView.visibility = android.view.View.VISIBLE
            btnDigitize.isEnabled = true
            resultCard.visibility = android.view.View.GONE
            recognizedText = ""
        }
    }

    private fun sendImageToServer(imageUri: Uri) {
        try {
            progressBar.visibility = android.view.View.VISIBLE
            btnDigitize.isEnabled = false
            btnSelectImage.isEnabled = false
            resultCard.visibility = android.view.View.GONE

            val optimizedFile = optimizeImageForOcr(imageUri)
            if (optimizedFile == null) {
                Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show()
                resetButtons()
                return
            }

            println("Отправка изображения: ${optimizedFile.length()} байт")

            val requestFile = RequestBody.create(
                "image/*".toMediaTypeOrNull(),
                optimizedFile
            )
            val imagePart = MultipartBody.Part.createFormData(
                "file",
                optimizedFile.name,
                requestFile
            )

            val call = ApiClient.ocrApi.recognizeImage(imagePart)
            call.enqueue(object : Callback<OcrResponse> {
                override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                    progressBar.visibility = android.view.View.GONE
                    resetButtons()

                    if (response.isSuccessful && response.body() != null) {
                        val result = response.body()!!

                        if (result.error != null) {
                            showError("Ошибка сервера: ${result.error}")
                        } else {
                            recognizedText = result.text ?: "Текст не распознан"
                            tvResult.text = recognizedText
                            resultCard.visibility = android.view.View.VISIBLE
                            btnSave.isEnabled = true

                            Toast.makeText(
                                this@DocumentDigitizerActivity,
                                "Текст успешно распознан!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        showError("Ошибка сервера: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                    progressBar.visibility = android.view.View.GONE
                    resetButtons()

                    val errorMessage = when {
                        t.message?.contains("Failed to connect") == true ->
                            "Не удалось подключиться к серверу. Проверьте:\n" +
                                    "1. Сервер запущен\n" +
                                    "2. Телефон и ПК в одной сети\n" +
                                    "3. IP адрес правильный: ${ApiClient.getBaseUrl()}"
                        t.message?.contains("timeout") == true ->
                            "Таймаут соединения. Сервер не отвечает"
                        else -> "Ошибка сети: ${t.message}"
                    }

                    showError(errorMessage)
                    t.printStackTrace()
                }
            })

        } catch (e: Exception) {
            progressBar.visibility = android.view.View.GONE
            resetButtons()
            showError("Ошибка: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun optimizeImageForOcr(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val maxDimension = 1200
            val (width, height) = if (originalBitmap.width > originalBitmap.height) {
                if (originalBitmap.width > maxDimension) {
                    val ratio = maxDimension.toFloat() / originalBitmap.width
                    Pair(maxDimension, (originalBitmap.height * ratio).toInt())
                } else {
                    Pair(originalBitmap.width, originalBitmap.height)
                }
            } else {
                if (originalBitmap.height > maxDimension) {
                    val ratio = maxDimension.toFloat() / originalBitmap.height
                    Pair((originalBitmap.width * ratio).toInt(), maxDimension)
                } else {
                    Pair(originalBitmap.width, originalBitmap.height)
                }
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)


            val file = File.createTempFile(
                "ocr_${System.currentTimeMillis()}",
                ".jpg",
                cacheDir
            )

            val outputStream = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            originalBitmap.recycle()
            scaledBitmap.recycle()

            file

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resetButtons() {
        btnDigitize.isEnabled = selectedImageUri != null
        btnSelectImage.isEnabled = true
    }

    private fun showError(message: String) {
        tvResult.text = message
        resultCard.visibility = android.view.View.VISIBLE
        btnSave.isEnabled = false
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun saveRecognizedText() {
        if (recognizedText.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Сохранить результат")
                .setMessage("Распознанный текст можно сохранить в документы или скопировать в буфер обмена")
                .setPositiveButton("💾 В документы") { _, _ ->
                    uploadTextToDocuments()
                }
                .setNeutralButton("Копировать") { _, _ ->
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("OCR Result", recognizedText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Текст скопирован", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun uploadTextToDocuments() {
        val name = "OCR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.uploadDocument(
                    UploadDocumentRequest(filename = name, text = recognizedText)
                )
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        Toast.makeText(this@DocumentDigitizerActivity, "Документ сохранён на сервер", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@DocumentDigitizerActivity, "Ошибка сервера: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DocumentDigitizerActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (recognizedText.isNotEmpty() && resultCard.visibility == android.view.View.VISIBLE) {
            AlertDialog.Builder(this)
                .setTitle("Выход")
                .setMessage("Результат распознавания будет потерян. Продолжить?")
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