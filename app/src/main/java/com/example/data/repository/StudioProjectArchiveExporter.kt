package com.example.data.repository

import com.example.domain.model.StudioProjectSpec
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a portable ZIP archive from the current in-memory project.
 * File names are revalidated because editor state is not trusted.
 */
object StudioProjectArchiveExporter {
    private const val MAX_FILES = 24
    private const val MAX_FILE_BYTES = 256_000
    private const val MAX_TOTAL_BYTES = 2_000_000

    fun writeZip(project: StudioProjectSpec, output: OutputStream) {
        require(project.files.isNotEmpty()) { "لا توجد ملفات لتصديرها." }
        require(project.files.size <= MAX_FILES) { "عدد ملفات المشروع يتجاوز حد التصدير." }

        var totalBytes = 0
        val paths = mutableSetOf<String>()
        ZipOutputStream(output).use { zip ->
            project.files.forEach { file ->
                val path = file.path
                validateArchivePath(path)
                require(paths.add(path.lowercase())) { "مسار مكرر داخل المشروع: $path" }

                val bytes = file.content.toByteArray(StandardCharsets.UTF_8)
                require(bytes.size <= MAX_FILE_BYTES) { "الملف $path أكبر من حد التصدير." }
                totalBytes += bytes.size
                require(totalBytes <= MAX_TOTAL_BYTES) { "حجم المشروع يتجاوز حد التصدير." }

                val entry = ZipEntry(path).apply { time = 0L }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
            zip.finish()
        }
    }

    private fun validateArchivePath(path: String) {
        require(path.isNotBlank() && path.length <= 180) { "مسار ملف غير صالح." }
        require(!path.startsWith('/') && !path.contains('\\') && !path.contains(':')) {
            "تم رفض مسار مطلق أو غير آمن: $path"
        }
        require(path.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "تم رفض مسار يتجاوز مجلد المشروع: $path"
        }
        require(path.split('/').none { it.equals(".git", ignoreCase = true) }) {
            "لا يمكن تصدير محتويات مجلد Git الداخلي."
        }
    }
}
