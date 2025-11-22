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
        
        // Настройка RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = GalleryAdapter(emptyList()) { imageFile ->
            openImageDetail(imageFile)
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

    override fun onResume() {
        super.onResume()
        loadImages() // Обновляем список при возвращении на экран
    }
}

// Адаптер для галереи
class GalleryAdapter(
    private var imageFiles: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val fileName: TextView = itemView.findViewById(R.id.tvFileName)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageFile = imageFiles[position]
        
        // Загрузка изображения
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        holder.imageView.setImageBitmap(bitmap)
        
        // Отображение имени файла
        holder.fileName.text = "Документ ${position + 1}"
        
        holder.itemView.setOnClickListener {
            onItemClick(imageFile)
        }
    }

    override fun getItemCount(): Int = imageFiles.size

    fun updateImages(newImages: List<File>) {
        imageFiles = newImages
        notifyDataSetChanged()
    }
}