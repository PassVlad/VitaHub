package com.example.glumedic

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryAdapter(
    private var imageFiles: List<File>,
    private val onItemClick: (File, String) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    companion object {
        const val ACTION_VIEW = "view"
        const val ACTION_RENAME = "rename"
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val fileName: TextView = itemView.findViewById(R.id.tvFileName)
        val btnOptions: ImageButton = itemView.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageFile = imageFiles[position]

        // Загрузка изображения
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        holder.imageView.setImageBitmap(bitmap)

        // Отображение имени файла (без расширения и префикса)
        val displayName = imageFile.name.substringBeforeLast(".").removePrefix("document_")
        holder.fileName.text = displayName

        // Клик по изображению - просмотр
        holder.imageView.setOnClickListener {
            onItemClick(imageFile, ACTION_VIEW)
        }

        // Клик по названию - переименование
        holder.fileName.setOnClickListener {
            onItemClick(imageFile, ACTION_RENAME)
        }

        // Кнопка меню с дополнительными опциями
        holder.btnOptions.setOnClickListener {
            showOptionsMenu(holder.btnOptions, imageFile)
        }

        // Долгое нажатие на карточку - тоже меню опций
        holder.itemView.setOnLongClickListener {
            showOptionsMenu(holder.btnOptions, imageFile)
            true
        }
    }

    private fun showOptionsMenu(anchor: View, imageFile: File) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menuInflater.inflate(R.menu.gallery_item_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_rename -> {
                    onItemClick(imageFile, ACTION_RENAME)
                    true
                }
                R.id.menu_view -> {
                    onItemClick(imageFile, ACTION_VIEW)
                    true
                }
                R.id.menu_delete -> {
                    showDeleteConfirmation(anchor.context, imageFile)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(context: android.content.Context, imageFile: File) {
        android.app.AlertDialog.Builder(context)
            .setTitle("Удаление")
            .setMessage("Вы уверены, что хотите удалить этот документ?")
            .setPositiveButton("Удалить") { dialog, _ ->
                if (imageFile.delete()) {
                    Toast.makeText(context, "Документ удален", Toast.LENGTH_SHORT).show()
                    // Обновляем адаптер
                    val newList = imageFiles.toMutableList().apply { remove(imageFile) }
                    updateImages(newList)
                } else {
                    Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun getItemCount(): Int = imageFiles.size

    fun updateImages(newImages: List<File>) {
        imageFiles = newImages
        notifyDataSetChanged()
    }
}