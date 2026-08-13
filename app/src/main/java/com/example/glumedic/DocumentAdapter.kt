package com.example.glumedic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocumentAdapter(
    private var documents: List<DocumentInfo>,
    private val onDelete: (DocumentInfo) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileMeta: TextView = itemView.findViewById(R.id.tvFileMeta)
        val tvPreview: TextView = itemView.findViewById(R.id.tvPreview)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val doc = documents[position]
        holder.tvFileName.text = doc.filename

        val parts = mutableListOf<String>()
        doc.file_size?.let { parts.add(formatSize(it)) }
        doc.chunks_count?.takeIf { it > 0 }?.let { parts.add("${it} фрагментов") }
        doc.uploaded_at?.let { parts.add(formatDate(it)) }
        holder.tvFileMeta.text = parts.joinToString(" · ")

        holder.tvPreview.text = doc.preview?.takeIf { it.isNotBlank() } ?: "Без распознанного текста"
        holder.tvPreview.visibility = if (doc.preview.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.btnDelete.setOnClickListener {
            onDelete(doc)
        }
    }

    override fun getItemCount(): Int = documents.size

    fun updateDocuments(newDocuments: List<DocumentInfo>) {
        documents = newDocuments
        notifyDataSetChanged()
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f МБ", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.1f КБ", bytes / 1024f)
            else -> "$bytes Б"
        }
    }

    private fun formatDate(raw: String): String {
        return try {
            val source = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()
            )
            val target = java.text.SimpleDateFormat(
                "dd.MM.yyyy HH:mm", java.util.Locale.getDefault()
            )
            source.parse(raw)?.let { target.format(it) } ?: raw
        } catch (e: Exception) {
            raw
        }
    }
}
