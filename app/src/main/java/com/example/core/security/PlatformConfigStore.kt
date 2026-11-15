package com.example.core.security

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AiProviderType(
    val wireKey: String,
    val arabicLabel: String,
    val defaultModel: String,
    val defaultBaseUrl: String,
    val requiresApiKey: Boolean,
    val suggestedModels: List<String>
) {
    FIREBASE_GEMINI(
        wireKey = "firebase_gemini",
        arabicLabel = "Firebase Gemini (مدمج بدون مفتاح)",
        defaultModel = "gemini-3.8-flash",
        defaultBaseUrl = "",
        requiresApiKey = false,
        suggestedModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon")
    ),
    GEMINI_REST(
        wireKey = "gemini_rest",
        arabicLabel = "Google Gemini API (مفتاح حساب)",
        defaultModel = "gemini-2.5-flash",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        requiresApiKey = true,
        suggestedModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash")
    ),
    OPENAI(
        wireKey = "openai",
        arabicLabel = "OpenAI API",
        defaultModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        requiresApiKey = true,
        suggestedModels = listOf("gpt-4o-mini", "gpt-4o", "o3-mini")
    ),
    ANTHROPIC(
        wireKey = "anthropic",
        arabicLabel = "Anthropic Claude",
        defaultModel = "claude-3-5-sonnet-latest",
        defaultBaseUrl = "https://api.anthropic.com/v1/messages",
        requiresApiKey = true,
        suggestedModels = listOf("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest")
    ),
    GROQ(
        wireKey = "groq",
        arabicLabel = "Groq (سريع جداً)",
        defaultModel = "llama-3.3-70b-versatile",
        defaultBaseUrl = "https://api.groq.com/openai/v1/chat/completions",
        requiresApiKey = true,
        suggestedModels = listOf("llama-3.3-70b-versatile", "qwen-2.5-coder-32b")
    ),
    OPENROUTER(
        wireKey = "openrouter",
        arabicLabel = "OpenRouter (نماذج متعددة)",
        defaultModel = "google/gemini-2.5-flash",
        defaultBaseUrl = "https://openrouter.ai/api/v1/chat/completions",
        requiresApiKey = true,
        suggestedModels = listOf(
            "google/gemini-2.5-flash",
            "anthropic/claude-3.5-sonnet",
            "deepseek/deepseek-chat-v3-0324"
        )
    ),
    DEEPSEEK(
        wireKey = "deepseek",
        arabicLabel = "DeepSeek API",
        defaultModel = "deepseek-chat",
        defaultBaseUrl = "https://api.deepseek.com/chat/completions",
        requiresApiKey = true,
        suggestedModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    CUSTOM_OPENAI(
        wireKey = "custom_openai",
        arabicLabel = "خادم AI مخصص (OpenAI-Compatible)",
        defaultModel = "qwen2.5-coder:32b",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        requiresApiKey = false,
        suggestedModels = listOf("qwen2.5-coder:32b", "codellama:latest")
    );

    companion object {
        fun fromWireKey(key: String): AiProviderType {
            return when (key.lowercase().trim()) {
                "gemini", "firebase_gemini" -> FIREBASE_GEMINI
                "gemini_rest", "gemini_api" -> GEMINI_REST
                "openai" -> OPENAI
                "anthropic", "claude" -> ANTHROPIC
                "groq" -> GROQ
                "openrouter" -> OPENROUTER
                "deepseek" -> DEEPSEEK
                "custom_openai", "custom" -> CUSTOM_OPENAI
                else -> FIREBASE_GEMINI
            }
        }
    }
}

enum class TechStackType(val wireKey: String, val arabicLabel: String) {
    AUTO("auto", "كشف تلقائي"),
    ANDROID_KOTLIN("android_kotlin", "Android (Kotlin / Compose)"),
    FLUTTER("flutter", "Flutter / Dart"),
    DOTNET("dotnet", ".NET / C#"),
    NODEJS_WEB("nodejs_web", "Web / Node.js / React"),
    PYTHON("python", "Python / FastAPI"),
    GO("go", "Go"),
    OTHER("other", "مشروع عام");

    companion object {
        fun fromWireKey(key: String): TechStackType =
            entries.find { it.wireKey == key.lowercase().trim() } ?: AUTO
    }
}

data class AiEndpointConfig(
    val id: String,
    val label: String,
    val providerType: String = AiProviderType.FIREBASE_GEMINI.wireKey,
    val modelName: String = "gemini-3.8-flash",
    val apiKey: String = "",
    val baseUrl: String = "",
    val accountHint: String = "",
    val isEnabled: Boolean = true,
    val quotaStatus: String = QUOTA_READY, // "ready" | "exhausted" | "error"
    val lastError: String = "",
    val exhaustedAtEpochMs: Long = 0L,
    val requestsCount: Long = 0L
) {
    companion object {
        const val QUOTA_READY = "ready"
        const val QUOTA_EXHAUSTED = "exhausted"
        const val QUOTA_ERROR = "error"
    }
}

data class DatabaseConnectionConfig(
    val id: String,
    val label: String,
    val engine: String = "postgresql", // postgresql, supabase, mysql, sqlserver, sqlite_remote
    val hostOrUrl: String = "",
    val databaseName: String = "",
    val secretOrConnectionString: String = "",
    val apiKeyOrServiceRole: String = "",
    val readOnlyMode: Boolean = true,
    val protectedKeywordsCsv: String = "",
    val isActive: Boolean = false
)

data class ProjectProfileConfig(
    val id: String,
    val name: String,
    val repoSlug: String,
    val defaultBranch: String = "main",
    val techStack: String = TechStackType.AUTO.wireKey,
    val customTestCommand: String = "",
    val linkedGitHubAccountId: String = "",
    val linkedDatabaseId: String = "",
    val aiPriorityOrder: List<String> = emptyList(),
    val protectedDbTargets: List<String> = emptyList(),
    val systemInstructions: String = "",
    val sharedWorkFilePath: String = "docs/AGENT_WORKPLAN.md",
    val projectRequirements: String = "",
    val collaborativeModeEnabled: Boolean = true,
    val collaborativeEndpointIds: List<String> = emptyList(),
    val continuousExecutionEnabled: Boolean = true
)

data class PlatformConfigSnapshot(
    val aiEndpoints: List<AiEndpointConfig> = emptyList(),
    val databaseConnections: List<DatabaseConnectionConfig> = emptyList(),
    val projects: List<ProjectProfileConfig> = emptyList(),
    val activeProjectId: String = ""
) {
    val activeProject: ProjectProfileConfig?
        get() = projects.find { it.id == activeProjectId } ?: projects.firstOrNull()

    val activeDatabase: DatabaseConnectionConfig?
        get() = databaseConnections.find { it.isActive } ?: databaseConnections.firstOrNull()
}

/**
 * Encrypted local configuration store for:
 * - Multi-account AI Key & Provider Pool (with quota tracking & cooldown resets)
 * - Per-project AI Failover Priority chains
 * - Database Connections & per-DB secrets (connection string / service keys)
 * - Multi-project Developer Profiles
 */
class PlatformConfigStore(private val storage: SecretStorage) {

    fun loadSnapshot(): PlatformConfigSnapshot {
        val raw = storage.getSecret(SecretVault.KEY_PLATFORM_CONFIG_JSON)
        if (raw.isBlank()) {
            return bootstrapDefaults()
        }
        return try {
            val root = JSONObject(raw)
            val endpoints = parseArray(root.optJSONArray("aiEndpoints")) { o ->
                AiEndpointConfig(
                    id = o.optString("id"),
                    label = o.optString("label"),
                    providerType = o.optString("providerType", AiProviderType.FIREBASE_GEMINI.wireKey),
                    modelName = o.optString("modelName", "gemini-3.8-flash"),
                    apiKey = o.optString("apiKey"),
                    baseUrl = o.optString("baseUrl"),
                    accountHint = o.optString("accountHint"),
                    isEnabled = o.optBoolean("isEnabled", true),
                    quotaStatus = o.optString("quotaStatus", AiEndpointConfig.QUOTA_READY),
                    lastError = o.optString("lastError"),
                    exhaustedAtEpochMs = o.optLong("exhaustedAtEpochMs", 0L),
                    requestsCount = o.optLong("requestsCount", 0L)
                )
            }.filter { it.id.isNotBlank() }

            val dbs = parseArray(root.optJSONArray("databaseConnections")) { o ->
                DatabaseConnectionConfig(
                    id = o.optString("id"),
                    label = o.optString("label"),
                    engine = o.optString("engine", "postgresql"),
                    hostOrUrl = o.optString("hostOrUrl"),
                    databaseName = o.optString("databaseName"),
                    secretOrConnectionString = o.optString("secretOrConnectionString"),
                    apiKeyOrServiceRole = o.optString("apiKeyOrServiceRole"),
                    readOnlyMode = o.optBoolean("readOnlyMode", true),
                    protectedKeywordsCsv = o.optString("protectedKeywordsCsv"),
                    isActive = o.optBoolean("isActive", false)
                )
            }.filter { it.id.isNotBlank() }

            val projects = parseArray(root.optJSONArray("projects")) { o ->
                ProjectProfileConfig(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    repoSlug = o.optString("repoSlug"),
                    defaultBranch = o.optString("defaultBranch", "main"),
                    techStack = o.optString("techStack", TechStackType.AUTO.wireKey),
                    customTestCommand = o.optString("customTestCommand"),
                    linkedGitHubAccountId = o.optString("linkedGitHubAccountId"),
                    linkedDatabaseId = o.optString("linkedDatabaseId"),
                    aiPriorityOrder = parseStringArray(o.optJSONArray("aiPriorityOrder")),
                    protectedDbTargets = parseStringArray(o.optJSONArray("protectedDbTargets")),
                    systemInstructions = o.optString("systemInstructions"),
                    sharedWorkFilePath = o.optString("sharedWorkFilePath", "docs/AGENT_WORKPLAN.md")
                        .ifBlank { "docs/AGENT_WORKPLAN.md" },
                    projectRequirements = o.optString("projectRequirements"),
                    collaborativeModeEnabled = o.optBoolean("collaborativeModeEnabled", true),
                    collaborativeEndpointIds = parseStringArray(o.optJSONArray("collaborativeEndpointIds")),
                    continuousExecutionEnabled = o.optBoolean("continuousExecutionEnabled", true)
                )
            }.filter { it.id.isNotBlank() }

            val activeProjectId = root.optString("activeProjectId")
            val finalEndpoints = endpoints.ifEmpty { listOf(defaultBuiltInGeminiEndpoint()) }
            val finalProjects = projects.ifEmpty { defaultStarterProjects(finalEndpoints.map { it.id }) }
            val resolvedActiveId = finalProjects.find { it.id == activeProjectId }?.id
                ?: finalProjects.first().id

            PlatformConfigSnapshot(
                aiEndpoints = finalEndpoints,
                databaseConnections = dbs,
                projects = finalProjects,
                activeProjectId = resolvedActiveId
            )
        } catch (_: Exception) {
            bootstrapDefaults()
        }
    }

    private fun bootstrapDefaults(): PlatformConfigSnapshot {
        val defaultAi = defaultBuiltInGeminiEndpoint()
        val starterProjects = defaultStarterProjects(listOf(defaultAi.id))
        val snap = PlatformConfigSnapshot(
            aiEndpoints = listOf(defaultAi),
            databaseConnections = emptyList(),
            projects = starterProjects,
            activeProjectId = starterProjects.first().id
        )
        saveSnapshot(snap)
        return snap
    }

    private fun saveSnapshot(snapshot: PlatformConfigSnapshot) {
        val root = JSONObject().apply {
            put("activeProjectId", snapshot.activeProjectId)
            put("aiEndpoints", JSONArray().apply {
                snapshot.aiEndpoints.forEach { ep ->
                    put(JSONObject().apply {
                        put("id", ep.id)
                        put("label", ep.label)
                        put("providerType", ep.providerType)
                        put("modelName", ep.modelName)
                        put("apiKey", ep.apiKey)
                        put("baseUrl", ep.baseUrl)
                        put("accountHint", ep.accountHint)
                        put("isEnabled", ep.isEnabled)
                        put("quotaStatus", ep.quotaStatus)
                        put("lastError", ep.lastError)
                        put("exhaustedAtEpochMs", ep.exhaustedAtEpochMs)
                        put("requestsCount", ep.requestsCount)
                    })
                }
            })
            put("databaseConnections", JSONArray().apply {
                snapshot.databaseConnections.forEach { db ->
                    put(JSONObject().apply {
                        put("id", db.id)
                        put("label", db.label)
                        put("engine", db.engine)
                        put("hostOrUrl", db.hostOrUrl)
                        put("databaseName", db.databaseName)
                        put("secretOrConnectionString", db.secretOrConnectionString)
                        put("apiKeyOrServiceRole", db.apiKeyOrServiceRole)
                        put("readOnlyMode", db.readOnlyMode)
                        put("protectedKeywordsCsv", db.protectedKeywordsCsv)
                        put("isActive", db.isActive)
                    })
                }
            })
            put("projects", JSONArray().apply {
                snapshot.projects.forEach { prj ->
                    put(JSONObject().apply {
                        put("id", prj.id)
                        put("name", prj.name)
                        put("repoSlug", prj.repoSlug)
                        put("defaultBranch", prj.defaultBranch)
                        put("techStack", prj.techStack)
                        put("customTestCommand", prj.customTestCommand)
                        put("linkedGitHubAccountId", prj.linkedGitHubAccountId)
                        put("linkedDatabaseId", prj.linkedDatabaseId)
                        put("aiPriorityOrder", JSONArray(prj.aiPriorityOrder))
                        put("protectedDbTargets", JSONArray(prj.protectedDbTargets))
                        put("systemInstructions", prj.systemInstructions)
                        put("sharedWorkFilePath", prj.sharedWorkFilePath)
                        put("projectRequirements", prj.projectRequirements)
                        put("collaborativeModeEnabled", prj.collaborativeModeEnabled)
                        put("collaborativeEndpointIds", JSONArray(prj.collaborativeEndpointIds))
                        put("continuousExecutionEnabled", prj.continuousExecutionEnabled)
                    })
                }
            })
        }
        storage.putSecret(SecretVault.KEY_PLATFORM_CONFIG_JSON, root.toString())
    }

    fun upsertAiEndpoint(
        id: String = "",
        label: String,
        providerType: AiProviderType,
        modelName: String,
        apiKey: String,
        baseUrl: String = "",
        accountHint: String = "",
        isEnabled: Boolean = true,
        addToActiveProjectPriority: Boolean = true
    ): AiEndpointConfig {
        val snap = loadSnapshot()
        val existing = snap.aiEndpoints.find { it.id == id && id.isNotBlank() }
        val finalId = existing?.id ?: "ai_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val resolvedKey = apiKey.trim().ifBlank { existing?.apiKey.orEmpty() }
        val resolvedUrl = baseUrl.trim().ifBlank { existing?.baseUrl ?: providerType.defaultBaseUrl }
        val resolvedModel = modelName.trim().ifBlank { providerType.defaultModel }
        val resolvedLabel = label.trim().ifBlank {
            val hintPart = accountHint.trim().takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            "${providerType.arabicLabel}$hintPart"
        }

        val entry = AiEndpointConfig(
            id = finalId,
            label = resolvedLabel,
            providerType = providerType.wireKey,
            modelName = resolvedModel,
            apiKey = resolvedKey,
            baseUrl = resolvedUrl,
            accountHint = accountHint.trim(),
            isEnabled = isEnabled,
            quotaStatus = AiEndpointConfig.QUOTA_READY,
            lastError = "",
            exhaustedAtEpochMs = 0L,
            requestsCount = existing?.requestsCount ?: 0L
        )

        val updatedEndpoints = snap.aiEndpoints.filterNot { it.id == finalId } + entry
        val updatedProjects = if (addToActiveProjectPriority && existing == null) {
            snap.projects.map { p ->
                if (p.id == snap.activeProjectId && finalId !in p.aiPriorityOrder) {
                    p.copy(aiPriorityOrder = p.aiPriorityOrder + finalId)
                } else p
            }
        } else snap.projects

        if (providerType == AiProviderType.OPENAI && resolvedKey.isNotBlank()) {
            storage.putSecret(SecretVault.KEY_OPENAI_API, resolvedKey)
        }
        saveSnapshot(snap.copy(aiEndpoints = updatedEndpoints, projects = updatedProjects))
        return entry
    }

    fun toggleAiEndpoint(endpointId: String, enabled: Boolean): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map {
            if (it.id == endpointId) it.copy(isEnabled = enabled) else it
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    fun removeAiEndpoint(endpointId: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val remaining = snap.aiEndpoints.filterNot { it.id == endpointId }
            .ifEmpty { listOf(defaultBuiltInGeminiEndpoint()) }
        val validIds = remaining.map { it.id }.toSet()
        val cleanedProjects = snap.projects.map { p ->
            p.copy(
                aiPriorityOrder = p.aiPriorityOrder.filter { it in validIds },
                collaborativeEndpointIds = p.collaborativeEndpointIds.filter { it in validIds }
            )
        }
        val next = snap.copy(aiEndpoints = remaining, projects = cleanedProjects)
        saveSnapshot(next)
        return next
    }

    fun markQuotaExhausted(
        endpointId: String,
        reason: String,
        nowMs: Long = System.currentTimeMillis()
    ): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map { ep ->
            if (ep.id == endpointId) {
                ep.copy(
                    quotaStatus = AiEndpointConfig.QUOTA_EXHAUSTED,
                    lastError = reason.take(250),
                    exhaustedAtEpochMs = nowMs
                )
            } else ep
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    fun markEndpointError(endpointId: String, reason: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map { ep ->
            if (ep.id == endpointId) {
                ep.copy(
                    quotaStatus = AiEndpointConfig.QUOTA_ERROR,
                    lastError = reason.take(250)
                )
            } else ep
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    fun incrementEndpointSuccess(endpointId: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map { ep ->
            if (ep.id == endpointId) {
                ep.copy(
                    quotaStatus = AiEndpointConfig.QUOTA_READY,
                    lastError = "",
                    requestsCount = ep.requestsCount + 1L
                )
            } else ep
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    fun resetEndpointQuota(endpointId: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map { ep ->
            if (ep.id == endpointId) {
                ep.copy(
                    quotaStatus = AiEndpointConfig.QUOTA_READY,
                    lastError = "",
                    exhaustedAtEpochMs = 0L
                )
            } else ep
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    fun resetAllQuotas(): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val updated = snap.aiEndpoints.map { ep ->
            ep.copy(
                quotaStatus = AiEndpointConfig.QUOTA_READY,
                lastError = "",
                exhaustedAtEpochMs = 0L
            )
        }
        val next = snap.copy(aiEndpoints = updated)
        saveSnapshot(next)
        return next
    }

    /**
     * Resolves enabled AI endpoints in strict priority order for the given project:
     * 1. Ready endpoints listed in project.aiPriorityOrder (in order)
     * 2. Remaining ready endpoints in the pool (for automatic failover resilience)
     * 3. Any exhausted endpoints whose cooldown (> 15 mins) elapsed are placed last if nothing else is ready
     */
    fun resolveOrderedAiEndpoints(
        projectId: String? = null,
        nowMs: Long = System.currentTimeMillis(),
        cooldownMs: Long = DEFAULT_COOLDOWN_MS
    ): List<AiEndpointConfig> {
        val snap = loadSnapshot()
        val project = snap.projects.find { it.id == (projectId ?: snap.activeProjectId) }
            ?: snap.activeProject
        val enabledMap = snap.aiEndpoints.filter { ep ->
            if (!ep.isEnabled) return@filter false
            val type = AiProviderType.fromWireKey(ep.providerType)
            !type.requiresApiKey || ep.apiKey.isNotBlank()
        }.associateBy { it.id }

        val orderedIds = buildList {
            project?.aiPriorityOrder?.forEach { id ->
                if (id in enabledMap && id !in this) add(id)
            }
            enabledMap.keys.forEach { id ->
                if (id !in this) add(id)
            }
        }
        val orderedEndpoints = orderedIds.mapNotNull { enabledMap[it] }
        val readyNow = orderedEndpoints.filter { ep ->
            ep.quotaStatus != AiEndpointConfig.QUOTA_EXHAUSTED
        }
        val cooledDown = orderedEndpoints.filter { ep ->
            ep.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED &&
                ep.exhaustedAtEpochMs > 0L &&
                (nowMs - ep.exhaustedAtEpochMs) >= cooldownMs
        }
        return readyNow + cooledDown
    }

    fun moveProjectPriority(projectId: String, endpointId: String, moveUp: Boolean): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val target = snap.projects.find { it.id == projectId } ?: return snap
        val allIds = snap.aiEndpoints.map { it.id }
        val currentOrder = (target.aiPriorityOrder.filter { it in allIds } +
            allIds.filter { it !in target.aiPriorityOrder }).toMutableList()
        val idx = currentOrder.indexOf(endpointId)
        if (idx == -1) return snap
        val swapIdx = if (moveUp) idx - 1 else idx + 1
        if (swapIdx !in currentOrder.indices) return snap
        val tmp = currentOrder[idx]
        currentOrder[idx] = currentOrder[swapIdx]
        currentOrder[swapIdx] = tmp

        val updatedProjects = snap.projects.map {
            if (it.id == projectId) it.copy(aiPriorityOrder = currentOrder) else it
        }
        val next = snap.copy(projects = updatedProjects)
        saveSnapshot(next)
        return next
    }

    fun setProjectAiPriorityOrder(projectId: String, orderedIds: List<String>): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val validIds = snap.aiEndpoints.map { it.id }.toSet()
        val cleaned = orderedIds.filter { it in validIds }.distinct()
        val updatedProjects = snap.projects.map {
            if (it.id == projectId) it.copy(aiPriorityOrder = cleaned) else it
        }
        val next = snap.copy(projects = updatedProjects)
        saveSnapshot(next)
        return next
    }

    /**
     * Resolves the collaborative AI team for a project.
     * Uses explicit [ProjectProfileConfig.collaborativeEndpointIds] if set,
     * otherwise falls back to enabled priority endpoints for the project.
     */
    fun resolveCollaborativeTeam(projectId: String? = null): List<AiEndpointConfig> {
        val snap = loadSnapshot()
        val project = snap.projects.find { it.id == (projectId ?: snap.activeProjectId) }
            ?: snap.activeProject
        val orderedReady = resolveOrderedAiEndpoints(projectId)
        val selectedIds = project?.collaborativeEndpointIds.orEmpty()
        if (selectedIds.isEmpty()) {
            return orderedReady
        }
        val readyMap = orderedReady.associateBy { it.id }
        val team = selectedIds.mapNotNull { readyMap[it] }
        return team.ifEmpty { orderedReady }
    }

    fun toggleCollaborativeMember(
        projectId: String,
        endpointId: String,
        selected: Boolean
    ): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val validIds = snap.aiEndpoints.map { it.id }.toSet()
        if (endpointId !in validIds) return snap
        val updatedProjects = snap.projects.map { prj ->
            if (prj.id != projectId) prj
            else {
                val baseList = prj.collaborativeEndpointIds.ifEmpty {
                    prj.aiPriorityOrder.ifEmpty { snap.aiEndpoints.map { it.id } }
                }
                val nextTeam = if (selected) {
                    (baseList + endpointId).distinct()
                } else {
                    baseList.filterNot { it == endpointId }
                }
                prj.copy(collaborativeEndpointIds = nextTeam)
            }
        }
        val next = snap.copy(projects = updatedProjects)
        saveSnapshot(next)
        return next
    }

    fun updateProjectWorkspaceSpec(
        projectId: String,
        sharedWorkFilePath: String,
        projectRequirements: String,
        systemInstructions: String,
        collaborativeModeEnabled: Boolean,
        continuousExecutionEnabled: Boolean
    ): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val cleanPath = sharedWorkFilePath.trim().trimStart('/').ifBlank { "docs/AGENT_WORKPLAN.md" }
        val updatedProjects = snap.projects.map { prj ->
            if (prj.id == projectId) {
                prj.copy(
                    sharedWorkFilePath = cleanPath,
                    projectRequirements = projectRequirements.trim(),
                    systemInstructions = systemInstructions.trim(),
                    collaborativeModeEnabled = collaborativeModeEnabled,
                    continuousExecutionEnabled = continuousExecutionEnabled
                )
            } else prj
        }
        val next = snap.copy(projects = updatedProjects)
        saveSnapshot(next)
        return next
    }

    fun upsertDatabaseConnection(
        id: String = "",
        label: String,
        engine: String,
        hostOrUrl: String,
        databaseName: String,
        secretOrConnectionString: String,
        apiKeyOrServiceRole: String,
        readOnlyMode: Boolean = true,
        protectedKeywordsCsv: String = "",
        setActive: Boolean = true
    ): DatabaseConnectionConfig {
        val snap = loadSnapshot()
        val existing = snap.databaseConnections.find { it.id == id && id.isNotBlank() }
        val finalId = existing?.id ?: "db_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val resolvedConnSecret = secretOrConnectionString.trim().ifBlank { existing?.secretOrConnectionString.orEmpty() }
        val resolvedApiSecret = apiKeyOrServiceRole.trim().ifBlank { existing?.apiKeyOrServiceRole.orEmpty() }
        val resolvedLabel = label.trim().ifBlank {
            databaseName.trim().ifBlank { "قاعدة بيانات $engine" }
        }
        val entry = DatabaseConnectionConfig(
            id = finalId,
            label = resolvedLabel,
            engine = engine.trim().ifBlank { "postgresql" },
            hostOrUrl = hostOrUrl.trim(),
            databaseName = databaseName.trim(),
            secretOrConnectionString = resolvedConnSecret,
            apiKeyOrServiceRole = resolvedApiSecret,
            readOnlyMode = readOnlyMode,
            protectedKeywordsCsv = protectedKeywordsCsv.trim(),
            isActive = setActive || snap.databaseConnections.none { it.isActive }
        )
        val updatedDbs = snap.databaseConnections
            .filterNot { it.id == finalId }
            .map { if (entry.isActive) it.copy(isActive = false) else it } + entry
        saveSnapshot(snap.copy(databaseConnections = updatedDbs))
        return entry
    }

    fun activateDatabaseConnection(dbId: String): DatabaseConnectionConfig? {
        val snap = loadSnapshot()
        val target = snap.databaseConnections.find { it.id == dbId } ?: return null
        val updated = snap.databaseConnections.map { it.copy(isActive = it.id == dbId) }
        saveSnapshot(snap.copy(databaseConnections = updated))
        return target.copy(isActive = true)
    }

    fun removeDatabaseConnection(dbId: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val remaining = snap.databaseConnections.filterNot { it.id == dbId }
        val nextActive = remaining.firstOrNull { it.isActive } ?: remaining.firstOrNull()
        val normalized = if (nextActive != null) {
            remaining.map { it.copy(isActive = it.id == nextActive.id) }
        } else emptyList()
        val updatedProjects = snap.projects.map { p ->
            if (p.linkedDatabaseId == dbId) p.copy(linkedDatabaseId = "") else p
        }
        val next = snap.copy(databaseConnections = normalized, projects = updatedProjects)
        saveSnapshot(next)
        return next
    }

    fun upsertProjectProfile(
        id: String = "",
        name: String,
        repoSlug: String,
        defaultBranch: String = "main",
        techStack: String = TechStackType.AUTO.wireKey,
        customTestCommand: String = "",
        linkedGitHubAccountId: String = "",
        linkedDatabaseId: String = "",
        aiPriorityOrder: List<String> = emptyList(),
        protectedDbTargets: List<String> = emptyList(),
        systemInstructions: String = "",
        sharedWorkFilePath: String = "docs/AGENT_WORKPLAN.md",
        projectRequirements: String = "",
        collaborativeModeEnabled: Boolean = true,
        collaborativeEndpointIds: List<String> = emptyList(),
        continuousExecutionEnabled: Boolean = true,
        setActive: Boolean = true
    ): ProjectProfileConfig {
        val snap = loadSnapshot()
        val existing = snap.projects.find { it.id == id && id.isNotBlank() }
            ?: snap.projects.find {
                repoSlug.isNotBlank() && it.repoSlug.equals(repoSlug.trim(), ignoreCase = true)
            }
        val finalId = existing?.id ?: "prj_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val resolvedPriority = aiPriorityOrder.ifEmpty {
            existing?.aiPriorityOrder?.ifEmpty { snap.aiEndpoints.map { it.id } }
                ?: snap.aiEndpoints.map { it.id }
        }
        val resolvedTeam = collaborativeEndpointIds.ifEmpty {
            existing?.collaborativeEndpointIds.orEmpty()
        }
        val cleanWorkPath = sharedWorkFilePath.trim().trimStart('/').ifBlank {
            existing?.sharedWorkFilePath?.ifBlank { "docs/AGENT_WORKPLAN.md" } ?: "docs/AGENT_WORKPLAN.md"
        }
        val project = ProjectProfileConfig(
            id = finalId,
            name = name.trim().ifBlank { repoSlug.substringAfter("/").ifBlank { "مشروع جديد" } },
            repoSlug = repoSlug.trim(),
            defaultBranch = defaultBranch.trim().ifBlank { "main" },
            techStack = techStack.ifBlank { TechStackType.AUTO.wireKey },
            customTestCommand = customTestCommand.trim(),
            linkedGitHubAccountId = linkedGitHubAccountId,
            linkedDatabaseId = linkedDatabaseId,
            aiPriorityOrder = resolvedPriority,
            protectedDbTargets = protectedDbTargets.map { it.trim() }.filter { it.isNotBlank() },
            systemInstructions = systemInstructions.trim().ifBlank { existing?.systemInstructions.orEmpty() },
            sharedWorkFilePath = cleanWorkPath,
            projectRequirements = projectRequirements.trim().ifBlank { existing?.projectRequirements.orEmpty() },
            collaborativeModeEnabled = collaborativeModeEnabled,
            collaborativeEndpointIds = resolvedTeam,
            continuousExecutionEnabled = continuousExecutionEnabled
        )
        val updatedProjects = snap.projects.filterNot { it.id == finalId } + project
        val activeId = if (setActive) finalId else snap.activeProjectId.ifBlank { finalId }
        saveSnapshot(snap.copy(projects = updatedProjects, activeProjectId = activeId))
        return project
    }

    fun activateProjectProfile(projectId: String): ProjectProfileConfig? {
        val snap = loadSnapshot()
        val target = snap.projects.find { it.id == projectId } ?: return null
        saveSnapshot(snap.copy(activeProjectId = projectId))
        return target
    }

    fun removeProjectProfile(projectId: String): PlatformConfigSnapshot {
        val snap = loadSnapshot()
        val remaining = snap.projects.filterNot { it.id == projectId }
            .ifEmpty { defaultStarterProjects(snap.aiEndpoints.map { it.id }) }
        val nextActiveId = if (snap.activeProjectId == projectId) remaining.first().id else snap.activeProjectId
        val next = snap.copy(projects = remaining, activeProjectId = nextActiveId)
        saveSnapshot(next)
        return next
    }

    fun getAllProtectedTargetsForProject(project: ProjectProfileConfig?): List<String> {
        val snap = loadSnapshot()
        val projectTargets = project?.protectedDbTargets.orEmpty()
        val activeDbTargets = snap.databaseConnections
            .find { it.id == project?.linkedDatabaseId }
            ?.protectedKeywordsCsv
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return (projectTargets + activeDbTargets).distinct()
    }

    fun clearAll() {
        storage.clearAll()
    }

    private fun defaultBuiltInGeminiEndpoint() = AiEndpointConfig(
        id = BUILT_IN_GEMINI_ID,
        label = "Firebase Gemini المدمج",
        providerType = AiProviderType.FIREBASE_GEMINI.wireKey,
        modelName = "gemini-3.8-flash",
        apiKey = "",
        baseUrl = "",
        accountHint = "مشروع Firebase المربوط",
        isEnabled = true,
        quotaStatus = AiEndpointConfig.QUOTA_READY
    )

    private fun defaultStarterProjects(defaultAiOrder: List<String>) = listOf(
        ProjectProfileConfig(
            id = "prj_metra_main",
            name = "METRA 2027 الأساسي",
            repoSlug = "gfa437926-tech/metra2027",
            defaultBranch = "main",
            techStack = TechStackType.DOTNET.wireKey,
            aiPriorityOrder = defaultAiOrder,
            protectedDbTargets = listOf("metra_old", "metra_legacy"),
            systemInstructions = "حافظ على عزل قاعدة METRA القديمة وركز على استقرار البنية والاختبارات."
        ),
        ProjectProfileConfig(
            id = "prj_metra_flutter",
            name = "METRA Flutter المستقل",
            repoSlug = "gfa437926-tech/metra-weather-intelligence",
            defaultBranch = "main",
            techStack = TechStackType.FLUTTER.wireKey,
            aiPriorityOrder = defaultAiOrder,
            protectedDbTargets = listOf("metra_old", "metra_legacy"),
            systemInstructions = "تطبيق Flutter متعدد المنصات للتكامل مع منظومة METRA."
        )
    )

    private inline fun <T> parseArray(array: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(mapper(obj))
        }
        return result
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val s = array.optString(i)
            if (s.isNotBlank()) result.add(s)
        }
        return result
    }

    companion object {
        const val BUILT_IN_GEMINI_ID = "default_firebase_gemini"
        const val DEFAULT_COOLDOWN_MS = 15 * 60 * 1000L
    }
}
