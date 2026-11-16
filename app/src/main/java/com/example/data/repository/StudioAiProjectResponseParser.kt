package com.example.data.repository

import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioFileItem
import com.squareup.moshi.Moshi
import java.util.Locale

/**
 * Validates untrusted AI output before it becomes a project workspace.
 * This parser never writes files to disk or executes generated commands.
 */
object StudioAiProjectResponseParser {
    private const val MAX_FILES = 24
    private const val MAX_RESPONSE_BYTES = 260_000
    private const val MAX_FILE_BYTES = 48_000
    private const val MAX_TOTAL_BYTES = 220_000
    private const val MAX_PATH_LENGTH = 180

    private val mapAdapter = Moshi.Builder().build().adapter(Map::class.java)

    fun parseFiles(
        response: String,
        defaultLanguage: ProgrammingLanguage
    ): List<StudioFileItem> {
        require(response.toByteArray(Charsets.UTF_8).size <= MAX_RESPONSE_BYTES) {
            "استجابة النموذج أكبر من الحد المسموح."
        }
        val json = extractJsonObject(response)
        val root = mapAdapter.fromJson(json) as? Map<*, *>
            ?: throw IllegalArgumentException("استجابة النموذج ليست كائن JSON صالحاً.")

        val rawFiles = root["files"] as? List<*>
            ?: throw IllegalArgumentException("استجابة النموذج لا تحتوي على قائمة files.")
        require(rawFiles.isNotEmpty()) { "لم يُنشئ النموذج أي ملفات للمشروع." }
        require(rawFiles.size <= MAX_FILES) { "تجاوز عدد الملفات الحد المسموح ($MAX_FILES)." }

        var totalBytes = 0
        val seenPaths = mutableSetOf<String>()
        val files = rawFiles.mapIndexed { index, raw ->
            val item = raw as? Map<*, *>
                ?: throw IllegalArgumentException("تعريف الملف رقم ${index + 1} غير صالح.")
            val path = (item["path"] as? String)?.trim()
                ?: throw IllegalArgumentException("الملف رقم ${index + 1} لا يحتوي على path.")
            validatePath(path)
            val normalizedPath = path.lowercase(Locale.ROOT)
            require(seenPaths.add(normalizedPath)) { "يوجد مسار ملف مكرر: $path" }

            val source = item["content"] as? String
                ?: throw IllegalArgumentException("الملف $path لا يحتوي على محتوى نصي.")
            require(source.isNotBlank()) { "الملف $path فارغ." }
            val size = source.toByteArray(Charsets.UTF_8).size
            require(size <= MAX_FILE_BYTES) { "الملف $path أكبر من الحد المسموح." }
            totalBytes += size
            require(totalBytes <= MAX_TOTAL_BYTES) { "الحجم الإجمالي للملفات تجاوز الحد المسموح." }

            val languageName = item["language"] as? String
            val language = languageName
                ?.let { runCatching { ProgrammingLanguage.valueOf(it.trim().uppercase(Locale.ROOT)) }.getOrNull() }
                ?: defaultLanguage
            val purpose = (item["purpose"] as? String)?.trim().orEmpty()
                .ifBlank { "ملف مولّد للمشروع" }
                .take(240)

            StudioFileItem(
                path = path,
                language = language,
                content = source,
                purpose = purpose,
                isEntrypoint = item["isEntrypoint"] as? Boolean ?: false
            )
        }

        require(files.count { it.isEntrypoint } == 1) {
            "يجب أن تحدد الاستجابة ملف تشغيل رئيسياً واحداً فقط."
        }
        return files
    }

    private fun extractJsonObject(response: String): String {
        val trimmed = response.trim()
        val withoutFence = trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = withoutFence.indexOf('{')
        val end = withoutFence.lastIndexOf('}')
        require(start >= 0 && end > start) { "لم يعِد النموذج كائن JSON." }
        return withoutFence.substring(start, end + 1)
    }

    private fun validatePath(path: String) {
        require(path.isNotBlank() && path.length <= MAX_PATH_LENGTH) { "مسار الملف فارغ أو طويل جداً." }
        require(!path.startsWith('/') && !path.contains('\\\\') && !path.contains(':')) {
            "مسار غير نسبي أو يحتوي على فاصل غير مسموح: $path"
        }
        val segments = path.split('/')
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "مسار الملف يحاول الخروج من مجلد المشروع: $path"
        }
        val reservedWindowsNames = setOf("CON", "PRN", "AUX", "NUL") +
            (1..9).flatMap { listOf("COM$it", "LPT$it") }
        val invalidPortableCharacters = "<>:" + 34.toChar() + "|?*"
        require(segments.none { segment ->
            segment.any { it.code < 32 || it in invalidPortableCharacters } ||
                segment.endsWith(' ') || segment.endsWith('.') ||
                segment.substringBefore('.').uppercase(Locale.ROOT) in reservedWindowsNames
        }) {
            "يحتوي المسار على اسم غير متوافق مع أنظمة الملفات الشائعة: $path"
        }
        require(segments.none { it.equals(".git", ignoreCase = true) }) {
            "لا يسمح بإنشاء ملفات داخل مجلد Git."
        }

        val allowedEnvFiles = setOf(".env.example", ".env.template", ".env.sample")
        val normalizedSegments = segments.map { it.lowercase(Locale.ROOT) }
        require(normalizedSegments.none { segment ->
            (segment.startsWith(".env") && segment !in allowedEnvFiles) ||
                segment.endsWith(".pem") || segment.endsWith(".key") ||
                segment.endsWith(".p12") || segment.endsWith(".pfx") ||
                segment.endsWith(".jks") || segment.endsWith(".keystore") ||
                segment in setOf(
                    "id_rsa", "id_ed25519", "credentials", "credentials.json",
                    "credentials.ini", "secrets.json", "secrets.yaml", "secrets.yml",
                    "service-account.json", "service_account.json",
                    "google-credentials.json", "google_credentials.json",
                    "application_default_credentials.json", ".git-credentials",
                    ".npmrc", ".pypirc", ".netrc", ".ssh", ".aws", ".azure", ".kube"
                ) || segment.matches(Regex("(?i).*(?:firebase-adminsdk|service[-_]?account).*\\\\.json$"))
        }) {
            "تم رفض ملف قد يحتوي على أسرار أو مفاتيح خاصة: $path"
        }
        require(path.matches(Regex("[A-Za-z0-9._@+/-]+"))) {
            "يحتوي مسار الملف على أحرف غير مدعومة: $path"
        }
    }}
