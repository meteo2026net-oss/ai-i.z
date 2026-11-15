package com.example.data.repository

import com.example.domain.model.StudioProjectSpec
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
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

    private data class PreparedEntry(val path: String, val bytes: ByteArray)

    fun writeZip(project: StudioProjectSpec, output: OutputStream) {
        require(project.files.isNotEmpty()) { "لا توجد ملفات لتصديرها." }
        require(project.files.size <= MAX_FILES) { "عدد ملفات المشروع يتجاوز حد التصدير." }

        // Validate the complete project before touching the caller's output stream.
        // Invalid late entries must not leave a partially written archive behind.
        var totalBytes = 0
        val paths = mutableSetOf<String>()
        val preparedEntries = project.files.map { file ->
            val path = file.path
            validateArchivePath(path)
            val normalizedPath = Normalizer.normalize(path, Normalizer.Form.NFC)
            val pathKey = normalizedPath.lowercase(Locale.ROOT)
            require(paths.add(pathKey)) { "مسار مكرر داخل المشروع: $path" }

            val bytes = file.content.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size <= MAX_FILE_BYTES) { "الملف $path أكبر من حد التصدير." }
            totalBytes += bytes.size
            require(totalBytes <= MAX_TOTAL_BYTES) { "حجم المشروع يتجاوز حد التصدير." }
            PreparedEntry(path, bytes)
        }

        ZipOutputStream(output).use { zip ->
            preparedEntries.forEach { prepared ->
                val entry = ZipEntry(prepared.path).apply { time = 0L }
                zip.putNextEntry(entry)
                zip.write(prepared.bytes)
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
        val segments = path.split('/')
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "تم رفض مسار يتجاوز مجلد المشروع: $path"
        }
        val invalidPortableCharacters = "<>:" + 34.toChar() + "|?*"
        require(segments.none { segment ->
            segment.any { it.code < 32 || it in invalidPortableCharacters } ||
                segment.endsWith(' ') || segment.endsWith('.')
        }) {
            "يحتوي المسار على اسم غير متوافق مع أنظمة الملفات الشائعة: $path"
        }
        val reservedWindowsNames = setOf("CON", "PRN", "AUX", "NUL") +
            (1..9).flatMap { listOf("COM$it", "LPT$it") }
        require(segments.none { segment ->
            segment.substringBefore('.').uppercase(Locale.ROOT) in reservedWindowsNames
        }) {
            "يحتوي المسار على اسم جهاز محجوز في Windows: $path"
        }
        require(segments.none { it.equals(".git", ignoreCase = true) }) {
            "لا يمكن تصدير محتويات مجلد Git الداخلي."
        }
        val normalizedSegments = segments.map { it.lowercase(Locale.ROOT) }
        val allowedEnvFiles = setOf(".env.example", ".env.template", ".env.sample")
        require(normalizedSegments.none { segment ->
            (segment.startsWith(".env") && segment !in allowedEnvFiles) ||
                segment.endsWith(".pem") || segment.endsWith(".key") ||
                segment.endsWith(".p12") || segment.endsWith(".pfx") ||
                segment.endsWith(".jks") || segment.endsWith(".keystore") ||
                segment in setOf(
                    "id_rsa", "id_ed25519", "credentials", "credentials.json",
                    "secrets.json", "secrets.yaml", "secrets.yml",
                    ".npmrc", ".pypirc", ".netrc"
                )
        }) {
            "تم رفض تصدير ملف قد يحتوي على أسرار أو مفاتيح خاصة."
        }
    }
}
