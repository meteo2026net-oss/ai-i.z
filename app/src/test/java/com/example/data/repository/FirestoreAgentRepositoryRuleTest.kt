package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.OperationLog
import com.example.domain.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreAgentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreAgentRepository(firestore)
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                AgentTask(
                    userId = uid,
                    repo = "gfa437926-tech/metra2027",
                    branch = "main",
                    title = "Analyze METRA Architecture",
                    prompt = "Inspect repo files and CI status",
                    status = "running",
                    currentStepIndex = 1L,
                    totalSteps = 4L,
                    progressPercent = 25L,
                    lastCompletedStep = "Tree scan",
                    lastResultSummary = "Indexed files",
                    modifiedFiles = listOf("README.md"),
                    nextSteps = listOf("Check tests")
                ),
                explicitId = taskId
            )
        }
        assertTrue(createResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emitted.any { it.id == taskId && it.title == "Analyze METRA Architecture" })
    }

    @Test
    fun saveSettings_andObserveSettings_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreAgentRepository(firestore)

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSettings(
                UserSettings(
                    userId = uid,
                    activeRepo = "gfa437926-tech/metra-weather-intelligence",
                    activeBranch = "develop",
                    aiProvider = "gemini",
                    aiModel = "gemini-3.8-flash",
                    backendUrl = "https://runner.metra.example",
                    darkTheme = true,
                    autoExecuteTier1 = true
                )
            )
        }
        assertTrue(saveRes.isSuccess)

        val settings = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(uid).first {
                it.activeRepo == "gfa437926-tech/metra-weather-intelligence"
            }
        }
        assertEquals("develop", settings.activeBranch)
    }

    @Test
    fun approvalLifecycle_andLogAppend_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreAgentRepository(firestore)

        val apprId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createApproval(
                ApprovalRequest(
                    userId = uid,
                    taskId = "task_123",
                    tier = 2L,
                    actionType = "create_pr",
                    title = "Create Draft PR",
                    description = "Open draft PR for bugfix branch",
                    payloadPreview = "head: fix/weather-api -> base: main",
                    targetResource = "gfa437926-tech/metra2027"
                )
            ).getOrThrow()
        }

        val approveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.decideApproval(apprId, "approved", "Approved by owner", uid)
        }
        assertTrue(approveRes.isSuccess)

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addLog(
                OperationLog(
                    userId = uid,
                    category = "github",
                    tier = 2L,
                    action = "Approved Draft PR",
                    details = "PR ready to create",
                    outcome = "success",
                    repo = "gfa437926-tech/metra2027"
                )
            )
        }
        assertTrue(logRes.isSuccess)
    }

    @Test
    fun crossUserTaskAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreAgentRepository(firestore)
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createTask(
                AgentTask(
                    userId = aliceUid,
                    title = "Alice Private Task",
                    prompt = "Private prompt"
                ),
                explicitId = taskId
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.getTaskById(taskId, userId = aliceUid).getOrThrow()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull(captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreAgentRepository(firestore)

        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTasks("some_other_uid").first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull(captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_metra@test.com"
        const val BOB_EMAIL = "bob_metra@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
