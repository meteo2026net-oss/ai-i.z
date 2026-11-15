package com.example.data.repository

import com.example.core.security.AiEndpointConfig
import com.example.core.security.AiProviderType
import com.example.core.security.ApprovalGuard
import com.example.core.security.GuardDecision
import com.example.core.security.PlatformConfigStore
import com.example.core.security.SecretStorage
import com.example.core.security.TechStackType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiFailoverPoolTest {

    private class FakeSecretStorage : SecretStorage {
        private val map = mutableMapOf<String, String>()
        override fun putSecret(key: String, value: String) {
            if (value.isBlank()) map.remove(key) else map[key] = value
        }
        override fun getSecret(key: String): String = map[key].orEmpty()
        override fun hasSecret(key: String): Boolean = !map[key].isNullOrBlank()
        override fun clearAll() = map.clear()
    }

    @Test
    fun priorityOrderingAndFailover_switchesAutomaticallyOnQuota429() = runTest {
        val store = PlatformConfigStore(FakeSecretStorage())
        val initial = store.loadSnapshot()
        val projectId = initial.activeProjectId

        val gemini1 = store.upsertAiEndpoint(
            label = "Gemini Account 1",
            providerType = AiProviderType.GEMINI_REST,
            modelName = "gemini-2.5-flash",
            apiKey = "AIza_account_1",
            accountHint = "dev1@gmail.com"
        )
        val gemini2 = store.upsertAiEndpoint(
            label = "Gemini Account 2",
            providerType = AiProviderType.GEMINI_REST,
            modelName = "gemini-2.5-flash",
            apiKey = "AIza_account_2",
            accountHint = "dev2@gmail.com"
        )
        val openai = store.upsertAiEndpoint(
            label = "OpenAI Backup",
            providerType = AiProviderType.OPENAI,
            modelName = "gpt-4o-mini",
            apiKey = "sk-backup"
        )

        // Set explicit project priority: Gemini 1 -> Gemini 2 -> OpenAI
        store.setProjectAiPriorityOrder(projectId, listOf(gemini1.id, gemini2.id, openai.id))
        // Move OpenAI up ahead of Gemini 2, then back down to verify reordering
        store.moveProjectPriority(projectId, openai.id, moveUp = true)
        var ordered = store.resolveOrderedAiEndpoints(projectId)
        assertEquals(listOf(gemini1.id, openai.id, gemini2.id, PlatformConfigStore.BUILT_IN_GEMINI_ID), ordered.map { it.id })

        store.moveProjectPriority(projectId, openai.id, moveUp = false)
        ordered = store.resolveOrderedAiEndpoints(projectId)
        assertEquals(listOf(gemini1.id, gemini2.id, openai.id, PlatformConfigStore.BUILT_IN_GEMINI_ID), ordered.map { it.id })

        val attemptedIds = mutableListOf<String>()
        val emittedAlerts = mutableListOf<AiFailoverAlert>()

        val transport = object : AiRemoteTransport {
            override suspend fun complete(
                endpoint: AiEndpointConfig,
                systemInstruction: String,
                userPrompt: String,
                expectJson: Boolean
            ): String {
                attemptedIds.add(endpoint.id)
                if (endpoint.id == gemini1.id) {
                    throw AiApiCallException(
                        httpCode = 429,
                        rawBody = """{"error":{"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded"}}""",
                        message = "HTTP 429 RESOURCE_EXHAUSTED"
                    )
                }
                return "تم التنفيذ عبر ${endpoint.label}"
            }
        }

        val engine = AiFailoverEngine(store, transport)
        val result = engine.executeWithFailover(
            projectId = projectId,
            systemInstruction = "Test sys",
            userPrompt = "Analyze repo",
            onFailover = { emittedAlerts.add(it) }
        )

        assertTrue(result.isSuccess)
        val success = result.getOrThrow()
        assertEquals(listOf(gemini1.id, gemini2.id), attemptedIds)
        assertEquals(gemini2.id, success.usedEndpoint.id)
        assertEquals("تم التنفيذ عبر Gemini Account 2", success.text)

        assertEquals(1, emittedAlerts.size)
        assertEquals("Gemini Account 1", emittedAlerts.first().exhaustedLabel)
        assertEquals("Gemini Account 2", emittedAlerts.first().switchedToLabel)

        val snapAfter = store.loadSnapshot()
        val g1After = snapAfter.aiEndpoints.first { it.id == gemini1.id }
        val g2After = snapAfter.aiEndpoints.first { it.id == gemini2.id }
        assertEquals(AiEndpointConfig.QUOTA_EXHAUSTED, g1After.quotaStatus)
        assertEquals(AiEndpointConfig.QUOTA_READY, g2After.quotaStatus)
        assertEquals(1L, g2After.requestsCount)
    }

    @Test
    fun allExhausted_returnsFailureAndResetRestoresPool() = runTest {
        val store = PlatformConfigStore(FakeSecretStorage())
        val initial = store.loadSnapshot()
        val projectId = initial.activeProjectId
        // Disable built-in slot for strict single-key test
        store.toggleAiEndpoint(PlatformConfigStore.BUILT_IN_GEMINI_ID, false)

        val gemini1 = store.upsertAiEndpoint(
            label = "Gemini Solo",
            providerType = AiProviderType.GEMINI_REST,
            modelName = "gemini-2.5-flash",
            apiKey = "AIza_solo"
        )
        store.setProjectAiPriorityOrder(projectId, listOf(gemini1.id))

        val failingTransport = object : AiRemoteTransport {
            override suspend fun complete(
                endpoint: AiEndpointConfig,
                systemInstruction: String,
                userPrompt: String,
                expectJson: Boolean
            ): String {
                throw AiApiCallException(
                    httpCode = 429,
                    rawBody = "insufficient_quota",
                    message = "insufficient_quota"
                )
            }
        }

        val engine = AiFailoverEngine(store, failingTransport)
        val firstRun = engine.executeWithFailover(
            projectId = projectId,
            systemInstruction = "Sys",
            userPrompt = "Hello"
        )
        assertTrue(firstRun.isFailure)
        assertTrue(store.resolveOrderedAiEndpoints(projectId).isEmpty())

        // Second run immediately sees no ready endpoints
        val emptyRun = engine.executeWithFailover(
            projectId = projectId,
            systemInstruction = "Sys",
            userPrompt = "Hello"
        )
        assertTrue(emptyRun.isFailure)

        // Reset restores availability
        store.resetEndpointQuota(gemini1.id)
        val restored = store.resolveOrderedAiEndpoints(projectId)
        assertEquals(1, restored.size)
        assertEquals(gemini1.id, restored.first().id)
    }

    @Test
    fun databaseConnectionsAndPerProjectProtectedTargets_guardBlocksUnsafeSql() {
        val store = PlatformConfigStore(FakeSecretStorage())
        val db = store.upsertDatabaseConnection(
            label = "Supabase New App",
            engine = "supabase",
            hostOrUrl = "https://xyz.supabase.co",
            databaseName = "app_prod",
            secretOrConnectionString = "postgres://user:pass@db.xyz.supabase.co:5432/app_prod",
            apiKeyOrServiceRole = "sbp_secret_123",
            readOnlyMode = true,
            protectedKeywordsCsv = "billing_vault, legacy_users",
            setActive = true
        )

        val project = store.upsertProjectProfile(
            name = "Universal Shop",
            repoSlug = "org/universal-shop",
            techStack = TechStackType.NODEJS_WEB.wireKey,
            linkedDatabaseId = db.id,
            protectedDbTargets = listOf("audit_archive"),
            setActive = true
        )

        val snap = store.loadSnapshot()
        assertNotNull(snap.activeDatabase)
        assertEquals(db.id, snap.activeDatabase!!.id)
        assertEquals("sbp_secret_123", snap.activeDatabase!!.apiKeyOrServiceRole)

        val extraTargets = store.getAllProtectedTargetsForProject(project)
        assertTrue(extraTargets.containsAll(listOf("audit_archive", "billing_vault", "legacy_users")))

        // Protected keyword on custom project should be blocked even with Tier 3 approval
        val blockedCustom = ApprovalGuard.validateSqlExecution(
            sql = "SELECT * FROM billing_vault",
            targetDatabase = db.databaseName,
            hasExplicitTier3Approval = true,
            extraProtectedTargets = extraTargets
        )
        assertTrue(blockedCustom is GuardDecision.Blocked)

        // METRA legacy protection must remain unconditionally blocked
        val blockedMetra = ApprovalGuard.validateSqlExecution(
            sql = "SELECT * FROM metra_old.users",
            targetDatabase = "metra_old",
            hasExplicitTier3Approval = true,
            extraProtectedTargets = extraTargets
        )
        assertTrue(blockedMetra is GuardDecision.Blocked)

        // Safe SELECT on non-protected table allowed
        val allowedSelect = ApprovalGuard.validateSqlExecution(
            sql = "SELECT id, name FROM products LIMIT 10",
            targetDatabase = db.databaseName,
            hasExplicitTier3Approval = false,
            extraProtectedTargets = extraTargets
        )
        assertEquals(GuardDecision.AllowedAuto, allowedSelect)

        // Removing DB clears project link cleanly
        val afterRemove = store.removeDatabaseConnection(db.id)
        assertNull(afterRemove.activeDatabase)
        assertEquals("", afterRemove.projects.first { it.id == project.id }.linkedDatabaseId)
        assertFalse(afterRemove.projects.isEmpty())
    }

    @Test
    fun modelStopAndQuotaChain_failsOverAcrossQuotaAccountAndModel() = runTest {
        val store = PlatformConfigStore(FakeSecretStorage())
        val projectId = store.loadSnapshot().activeProjectId

        val quota1 = store.upsertAiEndpoint(
            label = "حصة 1",
            providerType = AiProviderType.GEMINI_REST,
            modelName = "gemini-2.5-flash",
            apiKey = "AIza_1"
        )
        val stoppedModel = store.upsertAiEndpoint(
            label = "نموذج متوقف",
            providerType = AiProviderType.ANTHROPIC,
            modelName = "claude-3-5-sonnet-latest",
            apiKey = "sk-ant-1"
        )
        val backupModel = store.upsertAiEndpoint(
            label = "النموذج البديل",
            providerType = AiProviderType.OPENAI,
            modelName = "gpt-4o-mini",
            apiKey = "sk-openai-1"
        )

        store.setProjectAiPriorityOrder(projectId, listOf(quota1.id, stoppedModel.id, backupModel.id))

        val engine = AiFailoverEngine(
            configStore = store,
            transport = object : AiRemoteTransport {
                override suspend fun complete(
                    endpoint: AiEndpointConfig,
                    systemInstruction: String,
                    userPrompt: String,
                    expectJson: Boolean
                ): String {
                    return when (endpoint.id) {
                        quota1.id -> throw AiApiCallException(429, "RESOURCE_EXHAUSTED", "quota")
                        stoppedModel.id -> throw AiApiCallException(503, "model unavailable", "503")
                        else -> "استمر العمل بنجاح على ${endpoint.label}"
                    }
                }
            }
        )

        val result = engine.executeWithFailover(
            projectId = projectId,
            systemInstruction = "Sys",
            userPrompt = "Continue to end",
            excludedEndpointIds = setOf(PlatformConfigStore.BUILT_IN_GEMINI_ID)
        )
        assertTrue(result.isSuccess)
        val exec = result.getOrThrow()
        assertEquals(backupModel.id, exec.usedEndpoint.id)
        assertEquals(2, exec.failoverAlerts.size)
        assertEquals("نموذج متوقف", exec.failoverAlerts[0].switchedToLabel)
        assertEquals("النموذج البديل", exec.failoverAlerts[1].switchedToLabel)
    }

    @Test
    fun collaborativeSharedWorkplan_mergesRequirementsPlanInstructionsAndRounds() {
        val store = PlatformConfigStore(FakeSecretStorage())
        val g1 = store.upsertAiEndpoint(
            label = "Gemini Architect",
            providerType = AiProviderType.GEMINI_REST,
            modelName = "gemini-2.5-pro",
            apiKey = "AIza_arch"
        )
        val o1 = store.upsertAiEndpoint(
            label = "OpenAI Coder",
            providerType = AiProviderType.OPENAI,
            modelName = "gpt-4o",
            apiKey = "sk-coder"
        )
        val project = store.upsertProjectProfile(
            name = "Smart Commerce",
            repoSlug = "dev/smart-commerce",
            techStack = TechStackType.FLUTTER.wireKey,
            sharedWorkFilePath = "docs/TEAM_PLAN.md",
            projectRequirements = "1. سلة مشتريات\n2. دفع إلكتروني آمن",
            systemInstructions = "اكتب اختبارات لكل وحدة قبل الإغلاق.",
            collaborativeModeEnabled = true,
            collaborativeEndpointIds = listOf(g1.id, o1.id),
            continuousExecutionEnabled = true
        )

        val team = store.resolveCollaborativeTeam(project.id)
        assertEquals(listOf(g1.id, o1.id), team.map { it.id })

        val initialDoc = SharedWorkplanCodec.buildInitialTemplate(
            project = project,
            teamNames = team.map { it.label }
        )
        assertTrue(initialDoc.contains("docs/TEAM_PLAN.md"))
        assertTrue(initialDoc.contains("1. سلة مشتريات"))
        assertTrue(initialDoc.contains("اكتب اختبارات لكل وحدة قبل الإغلاق."))

        val updatedDoc = SharedWorkplanCodec.mergeUpdate(
            existingContent = initialDoc,
            project = project,
            teamNames = team.map { it.label },
            tasks = emptyList(),
            newPlanMarkdown = "- [ ] تصميم قاعدة الطلبات\n- [ ] بناء واجهة الدفع",
            newRoundEntries = listOf(
                WorkplanRoundEntry("1. المخطط", "Gemini Architect", "gemini-2.5-pro", "تم وضع البنية الأساسية"),
                WorkplanRoundEntry("2. المنفذ", "OpenAI Coder", "gpt-4o", "تم اقتراح ملف CartBloc.dart")
            ),
            directiveTitle = "إطلاق خطة الإصدار الأول"
        )
        assertTrue(updatedDoc.contains("تصميم قاعدة الطلبات"))
        assertTrue(updatedDoc.contains("Gemini Architect"))
        assertTrue(updatedDoc.contains("OpenAI Coder"))
        assertTrue(updatedDoc.contains("تم اقتراح ملف CartBloc.dart"))
    }
}
