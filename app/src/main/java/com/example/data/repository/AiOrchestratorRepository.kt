package com.example.data.repository

import android.util.Log
import com.example.core.security.AiEndpointConfig
import com.example.core.security.AiProviderType
import com.example.core.security.PlatformConfigStore
import com.example.core.security.ProjectProfileConfig
import com.example.core.security.SecretVault
import com.example.core.security.TechStackType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PlannedStep(
    val title: String,
    val tier: Int,
    val actionType: String,
    val rationale: String
)

data class StructuredAgentPlan(
    val summary: String,
    val steps: List<PlannedStep>
)

data class AiStreamUpdate(
    val text: String,
    val activeEndpointLabel: String,
    val failoverAlert: AiFailoverAlert? = null
)

data class CollaborationContribution(
    val roleTitle: String,
    val requestedEndpointLabel: String,
    val actualEndpointLabel: String,
    val actualModelName: String,
    val outputText: String,
    val failoverAlerts: List<AiFailoverAlert> = emptyList()
)

data class MultiModelCollaborationResult(
    val updatedFileMarkdown: String,
    val contributions: List<CollaborationContribution>,
    val failoverAlerts: List<AiFailoverAlert>,
    val summary: String
)

class AiOrchestratorRepository(private val vault: SecretVault) {

    private val configStore = PlatformConfigStore(vault)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(65, TimeUnit.SECONDS)
            .build()
    }

    private val defaultGeminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val flashLiteGeminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    private val argonGeminiModel by lazy {
        Firebase.ai.generativeModel("gemini-4-argon")
    }

    private val planSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "ملخص موجز للخطة باللغة العربية"),
                "steps" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "عنوان الخطوة التنفيذية بالعربية"),
                            "tier" to Schema.integer(description = "مستوى الصلاحية: 1 أو 2 أو 3"),
                            "actionType" to Schema.string(description = "نوع العملية البرمجية"),
                            "rationale" to Schema.string(description = "سبب الخطوة وتأثيرها المتوقع")
                        )
                    ),
                    description = "خطوات التنفيذ المتتابعة"
                )
            )
        )
    }

    private val plannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            }
        )
    }

    private fun selectFirebaseGeminiModel(modelName: String) = when (modelName) {
        "gemini-4-argon" -> argonGeminiModel
        "gemini-3.5-flash-lite" -> flashLiteGeminiModel
        else -> defaultGeminiModel
    }

    private val remoteTransport = object : AiRemoteTransport {
        override suspend fun complete(
            endpoint: AiEndpointConfig,
            systemInstruction: String,
            userPrompt: String,
            expectJson: Boolean
        ): String {
            return when (AiProviderType.fromWireKey(endpoint.providerType)) {
                AiProviderType.FIREBASE_GEMINI -> {
                    val merged = if (systemInstruction.isNotBlank()) {
                        "$systemInstruction\n\n$userPrompt"
                    } else userPrompt
                    if (expectJson) {
                        val res = withTimeout(60_000L) {
                            plannerModel.generateContent(merged)
                        }
                        res.text ?: throw IllegalStateException("استجابة فارغة من Firebase Gemini Planner.")
                    } else {
                        val res = withTimeout(60_000L) {
                            selectFirebaseGeminiModel(endpoint.modelName).generateContent(merged)
                        }
                        res.text ?: throw IllegalStateException("استجابة فارغة من Firebase Gemini.")
                    }
                }
                AiProviderType.GEMINI_REST -> callDirectGeminiRest(
                    endpoint = endpoint,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    expectJson = expectJson
                )
                AiProviderType.ANTHROPIC -> callAnthropicRest(
                    endpoint = endpoint,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt
                )
                AiProviderType.OPENAI,
                AiProviderType.GROQ,
                AiProviderType.OPENROUTER,
                AiProviderType.DEEPSEEK,
                AiProviderType.CUSTOM_OPENAI -> callOpenAiCompatibleRest(
                    endpoint = endpoint,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    expectJson = expectJson
                )
            }
        }
    }

    private val failoverEngine = AiFailoverEngine(configStore, remoteTransport)

    fun buildProjectSystemPrompt(project: ProjectProfileConfig?): String {
        val projectTitle = project?.name?.ifBlank { project.repoSlug } ?: "المشروع البرمجي النشط"
        val repoLine = project?.repoSlug?.takeIf { it.isNotBlank() } ?: "غير محدد"
        val stackLabel = project?.let { TechStackType.fromWireKey(it.techStack).arabicLabel } ?: "تلقائي"
        val reqNote = project?.projectRequirements?.takeIf { it.isNotBlank() }
            ?.let { "\nمتطلبات المشروع المعتمدة:\n$it" }
            .orEmpty()
        val customNote = project?.systemInstructions?.takeIf { it.isNotBlank() }
            ?.let { "\nتعليمات المشروع الخاصة: $it" }
            .orEmpty()
        val sharedFileNote = project?.sharedWorkFilePath?.takeIf { it.isNotBlank() }
            ?.let { "\nملف العمل المشترك داخل المستودع: $it" }
            .orEmpty()
        val protectedDbs = (listOf("metra_old", "metra_legacy") + project?.protectedDbTargets.orEmpty())
            .distinct()
            .joinToString(", ")

        return """
            أنت وكيل الذكاء الاصطناعي البرمجي المستقل لمنصة التطوير الشاملة، وتعمل حالياً على مشروع: $projectTitle ($repoLine).
            نوع التقنية: $stackLabel$sharedFileNote$reqNote$customNote
            القواعد الإلزامية:
            1. أجب باللغة العربية بوضوح واحترافية هندسية، واستند فقط إلى البيانات الحقيقية المرفقة في السياق.
            2. لا تدّعِ نجاح أي عملية لم تُنفذ فعلياً، وإذا كانت هناك بيانات ناقصة اذكر ذلك صراحة.
            3. التزم بنظام الموافقات الثلاثي (المستوى 1 تلقائي، المستوى 2 موافقة، المستوى 3 موافقة صريحة مستقلة).
            4. قواعد البيانات المحمية التالية معزولة ومحظورة تماماً: $protectedDbs
        """.trimIndent()
    }

    fun streamAssistantResponseWithFailover(
        prompt: String,
        systemContext: String,
        project: ProjectProfileConfig? = null,
        onFailover: suspend (AiFailoverAlert) -> Unit = {}
    ): Flow<AiStreamUpdate> = flow {
        val systemPrompt = buildProjectSystemPrompt(project)
        val userFullPrompt = """
            السياق الحي للمشروع:
            $systemContext

            طلب المطور:
            $prompt
        """.trimIndent()

        val ordered = configStore.resolveOrderedAiEndpoints(project?.id)
        if (ordered.isEmpty()) {
            throw IllegalStateException(
                "جميع حسابات ومزودات AI استنفدت الحد المسموح أو معطلة. افتح إعدادات AI لإعادة التفعيل أو إضافة مفتاح جديد."
            )
        }

        var lastError: Throwable? = null
        val triedIds = mutableSetOf<String>()
        for (index in ordered.indices) {
            val endpoint = ordered[index]
            if (endpoint.id in triedIds) continue
            val providerType = AiProviderType.fromWireKey(endpoint.providerType)
            if (providerType == AiProviderType.FIREBASE_GEMINI) {
                triedIds.add(endpoint.id)
                var emittedAny = false
                try {
                    val model = selectFirebaseGeminiModel(endpoint.modelName)
                    val accumulated = StringBuilder()
                    val merged = "$systemPrompt\n\n$userFullPrompt"
                    withTimeout(60_000L) {
                        model.generateContentStream(merged).collect { chunk ->
                            val chunkText = chunk.text.orEmpty()
                            if (chunkText.isNotEmpty()) {
                                emittedAny = true
                                accumulated.append(chunkText)
                                emit(
                                    AiStreamUpdate(
                                        text = accumulated.toString(),
                                        activeEndpointLabel = endpoint.label
                                    )
                                )
                            }
                        }
                    }
                    if (accumulated.isBlank()) {
                        throw IllegalStateException("أرجع Firebase Gemini استجابة فارغة.")
                    }
                    configStore.incrementEndpointSuccess(endpoint.id)
                    return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    val classified = AiFailoverEngine.classifyAiFailure(e)
                    if (classified.isQuotaExhausted) {
                        configStore.markQuotaExhausted(endpoint.id, classified.arabicReason)
                    } else {
                        configStore.markEndpointError(endpoint.id, classified.arabicReason)
                    }
                    val nextCandidate = ordered.drop(index + 1).firstOrNull { it.id !in triedIds }
                    val alert = AiFailoverAlert(
                        exhaustedEndpointId = endpoint.id,
                        exhaustedLabel = endpoint.label,
                        exhaustedProvider = providerType.arabicLabel,
                        switchedToLabel = nextCandidate?.label,
                        reason = classified.arabicReason
                    )
                    onFailover(alert)
                    if (emittedAny && nextCandidate != null) {
                        emit(
                            AiStreamUpdate(
                                text = "\n⚠️ انتقل العمل تلقائياً من «${endpoint.label}» إلى «${nextCandidate.label}» للمتابعة بدون توقف...",
                                activeEndpointLabel = nextCandidate.label,
                                failoverAlert = alert
                            )
                        )
                    }
                    if (nextCandidate == null) break
                }
            } else {
                // REST failover through all remaining non-tried endpoints without duplication
                val subResult = failoverEngine.executeWithFailover(
                    projectId = project?.id,
                    systemInstruction = systemPrompt,
                    userPrompt = userFullPrompt,
                    preferredEndpointOverride = endpoint,
                    excludedEndpointIds = triedIds,
                    onFailover = onFailover
                )
                subResult.onSuccess { res ->
                    emit(
                        AiStreamUpdate(
                            text = res.text,
                            activeEndpointLabel = res.usedEndpoint.label,
                            failoverAlert = res.failoverAlerts.lastOrNull()
                        )
                    )
                    return@flow
                }.onFailure { err ->
                    lastError = err
                }
                break
            }
        }
        throw lastError ?: IllegalStateException("فشلت جميع مزودات الذكاء الاصطناعي في أولوية هذا المشروع.")
    }

    /** Backward-compatible streaming signature using project priority pool */
    fun streamAssistantResponse(
        prompt: String,
        systemContext: String,
        provider: String,
        modelName: String
    ): Flow<String> = flow {
        val activeProject = configStore.loadSnapshot().activeProject
        streamAssistantResponseWithFailover(
            prompt = prompt,
            systemContext = systemContext,
            project = activeProject
        ).collect { update ->
            emit(update.text)
        }
    }

    suspend fun generateStructuredPlan(
        taskPrompt: String,
        repoContext: String,
        provider: String,
        modelName: String,
        project: ProjectProfileConfig? = configStore.loadSnapshot().activeProject,
        onFailover: suspend (AiFailoverAlert) -> Unit = {}
    ): Result<StructuredAgentPlan> = try {
        val projectName = project?.name?.ifBlank { project.repoSlug } ?: "المشروع البرمجي"
        val prompt = """
            حلّل مهمة التطوير التالية لمشروع $projectName وقسّمها إلى 3 إلى 5 خطوات تنفيذية ذرية مرتبة.
            صنّف كل خطوة بدقة حسب مستوى الصلاحية:
            - tier = 1: read_files, analyze_code, stage_branch_edit, run_tests, generate_report
            - tier = 2: git_push, create_pr, staging_deploy, service_config
            - tier = 3: db_sql, db_rls_grants, db_migration, merge_pr, prod_deploy

            السياق الحالي:
            $repoContext

            المهمة:
            $taskPrompt
        """.trimIndent()

        val systemInstr = "أجب بـ JSON فقط بالمفاتيح: summary, steps:[{title,tier,actionType,rationale}]."
        val execResult = failoverEngine.executeWithFailover(
            projectId = project?.id,
            systemInstruction = systemInstr,
            userPrompt = prompt,
            expectJson = true,
            onFailover = onFailover
        )
        execResult.map { res ->
            val cleaned = res.text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            parsePlanJson(cleaned)
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Plan generation timed out", e)
        Result.failure(IllegalStateException("انتهت مهلة توليد الخطة (60 ثانية). يُرجى المحاولة مجدداً."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Plan generation failed", e)
        Result.failure(e)
    }

    suspend fun analyzeOnceWithFailover(
        prompt: String,
        projectId: String? = configStore.loadSnapshot().activeProjectId,
        onFailover: suspend (AiFailoverAlert) -> Unit = {}
    ): Result<AiExecutionResult> = try {
        val project = configStore.loadSnapshot().projects.find { it.id == projectId }
        failoverEngine.executeWithFailover(
            projectId = projectId,
            systemInstruction = buildProjectSystemPrompt(project),
            userPrompt = prompt,
            expectJson = false,
            onFailover = onFailover
        )
    } catch (e: TimeoutCancellationException) {
        Result.failure(IllegalStateException("انتهت مهلة التحليل (60 ثانية)."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun analyzeOnce(
        prompt: String,
        provider: String,
        modelName: String
    ): Result<String> = analyzeOnceWithFailover(prompt).map { it.text }

    /**
     * Multi-Model Collaborative Pipeline working together on a single shared file inside the repo:
     * Role 1: Planner & Requirements Analyst -> produces/refines execution checklist
     * Role 2: Implementer & Instructions Author -> technical implementation details & edits
     * Role 3: Security & QA Reviewer -> validates, checks edge cases & finalizes shared file updates
     *
     * Every round automatically fails over to the next available quota/account/model if a model stops.
     */
    suspend fun runCollaborativeTeamOnSharedFile(
        project: ProjectProfileConfig,
        directive: String,
        existingFileContent: String?,
        repoTreeSummary: String,
        tasks: List<com.example.domain.model.AgentTask>,
        onRoundProgress: suspend (String) -> Unit = {},
        onFailover: suspend (AiFailoverAlert) -> Unit = {}
    ): Result<MultiModelCollaborationResult> {
        return try {
            val team = configStore.resolveCollaborativeTeam(project.id)
            if (team.isEmpty()) {
                return Result.failure(
                    IllegalStateException("لا توجد نماذج متاحة للتعاون حالياً. فعّل مفتاحاً أو أعد تفعيل الحصة من إعدادات AI.")
                )
            }

        val roleDefinitions = listOf(
            "1. المخطط ومحلل المتطلبات (Planner & Architect)" to
                "حلّل متطلبات المشروع وملف العمل الحالي واكتب خطة تنفيذية دقيقة على شكل قائمة مهام `- [ ] ...` مع توضيح الملفات والتبعيات.",
            "2. المنفذ البرمجي وكاتب التعليمات (Implementer & Engineer)" to
                "استناداً إلى خطة المخطط أعلاه ومتطلبات المشروع، اكتب التعليمات البرمجية الدقيقة وخطوات التعديل الفعلية على الكود وقاعدة البيانات.",
            "3. المدقق الأمني ومراجع الجودة (Reviewer & QA)" to
                "راجع ما كتبه المخطط والمنفذ لاكتشاف أي ثغرات أو مخاطر أو أخطاء محتملة، وأضف قائمة تحقق الاختبارات والخطوة التالية الآمنة."
        )

        val contributions = mutableListOf<CollaborationContribution>()
        val allAlerts = mutableListOf<AiFailoverAlert>()
        val sysPrompt = buildProjectSystemPrompt(project)
        val priorNotes = StringBuilder()
        var generatedPlanChecklist = ""

        val activeRounds = if (team.size == 1 && !project.collaborativeModeEnabled) {
            roleDefinitions.take(1)
        } else {
            roleDefinitions
        }

        for ((idx, rolePair) in activeRounds.withIndex()) {
            val (roleTitle, roleInstruction) = rolePair
            val assignedEndpoint = team[idx % team.size]
            onRoundProgress("جارٍ تنفيذ دور ($roleTitle) عبر [${assignedEndpoint.label}]...")

            val roundPrompt = """
                المهمة / التوجيه المطلوب:
                ${directive.ifBlank { "تحليل المتطلبات وتحديث خطة العمل للمشروع" }}

                المسار الموحد في المستودع: ${project.sharedWorkFilePath}
                المحتوى الحالي للملف المشترك:
                ```markdown
                ${(existingFileContent ?: "ملف جديد لم يُنشأ بعد في المستودع").take(4500)}
                ```

                شجرة الملفات الحقيقية للمستودع:
                ${repoTreeSummary.take(1800)}

                ${if (priorNotes.isNotBlank()) "مساهمات النماذج السابقة في هذه الجولة:\n$priorNotes" else ""}

                دورك الآن: $roleTitle
                التعليمات: $roleInstruction
            """.trimIndent()

            val roundResult = failoverEngine.executeWithFailover(
                projectId = project.id,
                systemInstruction = sysPrompt,
                userPrompt = roundPrompt,
                preferredEndpointOverride = assignedEndpoint,
                onFailover = { alert ->
                    allAlerts.add(alert)
                    onFailover(alert)
                }
            )

            val exec = roundResult.getOrElse { err ->
                return Result.failure(err)
            }

            if (idx == 0) {
                val checklistLines = exec.text.lines()
                    .filter { it.trimStart().startsWith("- [") || it.trimStart().startsWith("1.") || it.trimStart().startsWith("- ") }
                    .take(12)
                if (checklistLines.isNotEmpty()) {
                    generatedPlanChecklist = checklistLines.joinToString("\n") { line ->
                        val trimmed = line.trimStart()
                        if (trimmed.startsWith("- [")) line else "- [ ] ${trimmed.removePrefix("- ").trim()}"
                    }
                }
            }

            contributions.add(
                CollaborationContribution(
                    roleTitle = roleTitle,
                    requestedEndpointLabel = assignedEndpoint.label,
                    actualEndpointLabel = exec.usedEndpoint.label,
                    actualModelName = exec.usedEndpoint.modelName,
                    outputText = exec.text,
                    failoverAlerts = exec.failoverAlerts
                )
            )

            priorNotes.appendLine("=== $roleTitle (${exec.usedEndpoint.label}) ===")
            priorNotes.appendLine(exec.text.take(1600))
            priorNotes.appendLine()
        }

        val entries = contributions.map {
            WorkplanRoundEntry(
                roleTitle = it.roleTitle,
                endpointLabel = it.actualEndpointLabel,
                modelName = it.actualModelName,
                summaryContent = it.outputText
            )
        }
        val mergedMarkdown = SharedWorkplanCodec.mergeUpdate(
            existingContent = existingFileContent,
            project = project,
            teamNames = contributions.map { "${it.actualEndpointLabel} (${it.actualModelName})" }.distinct(),
            tasks = tasks,
            newPlanMarkdown = generatedPlanChecklist.ifBlank { null },
            newRoundEntries = entries,
            directiveTitle = directive.take(90)
        )

        val summary = buildString {
            append("تمت الجولة التعاونية بمشاركة ${contributions.size} أدوار عبر: ")
            append(contributions.joinToString(" ← ") { it.actualEndpointLabel })
            if (allAlerts.isNotEmpty()) {
                append(" (تم تفعيل التحويل التلقائي ${allAlerts.size} مرة بدون توقف)")
            }
        }
        Result.success(
            MultiModelCollaborationResult(
                updatedFileMarkdown = mergedMarkdown,
                contributions = contributions,
                failoverAlerts = allAlerts,
                summary = summary
            )
        )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callDirectGeminiRest(
        endpoint: AiEndpointConfig,
        systemInstruction: String,
        userPrompt: String,
        expectJson: Boolean
    ): String = withContext(Dispatchers.IO) {
        val base = endpoint.baseUrl.ifBlank { AiProviderType.GEMINI_REST.defaultBaseUrl }.trimEnd('/')
        val model = endpoint.modelName.ifBlank { AiProviderType.GEMINI_REST.defaultModel }
        val url = "$base/v1beta/models/$model:generateContent?key=${endpoint.apiKey.trim()}"
        val bodyJson = JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            }
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                if (expectJson) put("responseMimeType", "application/json")
            })
        }
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw AiApiCallException(resp.code, raw, "Gemini REST HTTP ${resp.code}: $raw")
            }
            val parsed = JSONObject(raw)
            val candidates = parsed.optJSONArray("candidates") ?: throw IllegalStateException("لا توجد استجابة من Gemini REST.")
            val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
            if (text.isBlank()) throw IllegalStateException("أرجع Gemini REST نصاً فارغاً.")
            text
        }
    }

    private suspend fun callOpenAiCompatibleRest(
        endpoint: AiEndpointConfig,
        systemInstruction: String,
        userPrompt: String,
        expectJson: Boolean
    ): String = withContext(Dispatchers.IO) {
        val type = AiProviderType.fromWireKey(endpoint.providerType)
        val rawUrl = endpoint.baseUrl.ifBlank { type.defaultBaseUrl }.trim()
        val finalUrl = if (rawUrl.endsWith("/chat/completions")) rawUrl
        else rawUrl.trimEnd('/') + "/v1/chat/completions"
        val model = endpoint.modelName.ifBlank { type.defaultModel }

        val messages = JSONArray().apply {
            if (systemInstruction.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", systemInstruction))
            }
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.2)
            if (expectJson && type == AiProviderType.OPENAI) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
        }
        val reqBuilder = Request.Builder()
            .url(finalUrl)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (endpoint.apiKey.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer ${endpoint.apiKey.trim()}")
        }
        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw AiApiCallException(resp.code, raw, "${type.arabicLabel} HTTP ${resp.code}: $raw")
            }
            val parsed = JSONObject(raw)
            val text = parsed.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            if (text.isBlank()) throw IllegalStateException("أرجع ${type.arabicLabel} استجابة فارغة.")
            text
        }
    }

    private suspend fun callAnthropicRest(
        endpoint: AiEndpointConfig,
        systemInstruction: String,
        userPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val url = endpoint.baseUrl.ifBlank { AiProviderType.ANTHROPIC.defaultBaseUrl }.trim()
        val model = endpoint.modelName.ifBlank { AiProviderType.ANTHROPIC.defaultModel }
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            if (systemInstruction.isNotBlank()) {
                put("system", systemInstruction)
            }
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", userPrompt))
            })
        }
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", endpoint.apiKey.trim())
            .header("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw AiApiCallException(resp.code, raw, "Claude HTTP ${resp.code}: $raw")
            }
            val parsed = JSONObject(raw)
            val text = parsed.optJSONArray("content")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()
            if (text.isBlank()) throw IllegalStateException("أرجع Anthropic Claude استجابة فارغة.")
            text
        }
    }

    private fun parsePlanJson(rawJson: String): StructuredAgentPlan {
        val obj = JSONObject(rawJson)
        val summary = obj.optString("summary", "خطة تنفيذ المهمة")
        val arr = obj.optJSONArray("steps")
        val steps = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        PlannedStep(
                            title = item.optString("title", "خطوة ${i + 1}"),
                            tier = item.optInt("tier", 1).coerceIn(1, 3),
                            actionType = item.optString("actionType", "analyze_code"),
                            rationale = item.optString("rationale", "")
                        )
                    )
                }
            }
        }
        return StructuredAgentPlan(summary = summary, steps = steps)
    }

    private companion object {
        const val TAG = "AiOrchestrator"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
