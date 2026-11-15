package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.notifications.QuotaNotificationHelper
import com.example.core.security.AgentActionType
import com.example.core.security.ApprovalGuard
import com.example.core.security.GuardDecision
import com.example.core.security.PlatformConfigStore
import com.example.core.security.SecretVault
import com.example.data.local.LocalTaskCheckpoint
import com.example.data.local.MetraLocalDatabase
import com.example.data.repository.AiFailoverAlert
import com.example.data.repository.AiOrchestratorRepository
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.UserDataRepository
import com.example.data.repository.GitHubRepository
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.OperationLog
import org.json.JSONArray
import org.json.JSONObject

class AgentLoopWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val vault = SecretVault(applicationContext)
        val uid = AuthSessionRepository(vault).restoreSession().getOrNull() ?: return Result.retry()
        val dataRepo = UserDataRepository(applicationContext)
        val dao = MetraLocalDatabase.getInstance(applicationContext).agentDao()
        val configStore = PlatformConfigStore(vault)
        val gitHubRepo = GitHubRepository(vault)
        val aiRepo = AiOrchestratorRepository(vault)

        val remoteTask = dataRepo.getTaskById(taskId, uid).getOrNull()
        val checkpoint = dao.getCheckpoint(taskId)
        val task = remoteTask ?: checkpoint?.toAgentTask() ?: return Result.failure()

        if (task.status == "completed" || task.status == "awaiting_approval") {
            return Result.success()
        }

        val activeProject = configStore.loadSnapshot().projects.find {
            it.repoSlug.equals(task.repo, ignoreCase = true)
        } ?: configStore.loadSnapshot().activeProject

        val onFailover: suspend (AiFailoverAlert) -> Unit = { alert ->
            QuotaNotificationHelper.showQuotaFailoverNotification(
                context = applicationContext,
                exhaustedLabel = alert.exhaustedLabel,
                switchedToLabel = alert.switchedToLabel,
                reason = alert.reason
            )
            dataRepo.addLog(
                OperationLog(
                    userId = uid,
                    category = "ai",
                    tier = 1L,
                    action = "تنبيه استنفاد API والتحويل التلقائي",
                    details = "انتهت حصة ${alert.exhaustedLabel} (${alert.reason}) -> التحويل إلى: ${alert.switchedToLabel ?: "لا يوجد بديل"}",
                    outcome = "error",
                    repo = task.repo
                )
            )
        }

        val steps = parseStepsJson(checkpoint?.stepsJson)
        if (steps.isEmpty()) {
            val sharedPath = activeProject?.sharedWorkFilePath?.ifBlank { "docs/AGENT_WORKPLAN.md" }
                ?: "docs/AGENT_WORKPLAN.md"
            val repoSummary = if (gitHubRepo.hasToken()) {
                val commits = gitHubRepo.listCommits(task.repo, task.branch).getOrNull()?.take(3)
                    ?.joinToString("\n") { "- ${it.sha.take(7)}: ${it.commit.message}" }
                    ?: "تعذر جلب الـ commits"
                val sharedFileText = gitHubRepo.getFileDecoded(task.repo, sharedPath, task.branch)
                    .getOrNull()?.second?.take(1800)
                buildString {
                    appendLine("المستودع: ${task.repo} (${task.branch})")
                    appendLine("الملف المشترك: $sharedPath")
                    if (!sharedFileText.isNullOrBlank()) {
                        appendLine("محتوى ملف العمل المشترك الحالي:\n$sharedFileText")
                    }
                    if (!activeProject?.projectRequirements.isNullOrBlank()) {
                        appendLine("متطلبات المشروع:\n${activeProject?.projectRequirements}")
                    }
                    appendLine("آخر التغييرات:\n$commits")
                }
            } else {
                buildString {
                    appendLine("المستودع: ${task.repo} (${task.branch}) — GitHub Token غير مربوط بعد.")
                    if (!activeProject?.projectRequirements.isNullOrBlank()) {
                        appendLine("متطلبات المشروع:\n${activeProject?.projectRequirements}")
                    }
                }
            }

            val planResult = aiRepo.generateStructuredPlan(
                taskPrompt = task.prompt,
                repoContext = repoSummary,
                provider = inputData.getString(KEY_PROVIDER) ?: "gemini",
                modelName = inputData.getString(KEY_MODEL) ?: "gemini-3.8-flash",
                project = activeProject,
                onFailover = onFailover
            )
            val plan = planResult.getOrElse { err ->
                val isTemporaryQuotaOrNet = isQuotaOrTransientError(err)
                val reason = err.localizedMessage ?: "فشل تحليل المهمة"
                val nextTaskState = task.copy(
                    status = if (isTemporaryQuotaOrNet) "paused" else "failed",
                    lastError = reason
                )
                dataRepo.updateTask(nextTaskState)
                dao.upsertCheckpoint(nextTaskState.toCheckpoint("[]"))
                QuotaNotificationHelper.showAgentStoppedNotification(
                    context = applicationContext,
                    title = if (isTemporaryQuotaOrNet) {
                        "توقف مؤقت للمهمة: ${task.title}"
                    } else {
                        "توقفت المهمة: ${task.title}"
                    },
                    reason = reason,
                    isPausedWillRetry = isTemporaryQuotaOrNet,
                    targetRoute = "tasks"
                )
                return if (isTemporaryQuotaOrNet) Result.retry() else Result.failure()
            }

            val jsonArr = JSONArray().apply {
                plan.steps.forEach { step ->
                    put(JSONObject().apply {
                        put("title", step.title)
                        put("tier", step.tier)
                        put("actionType", step.actionType)
                        put("rationale", step.rationale)
                    })
                }
            }
            val updated = task.copy(
                status = "running",
                totalSteps = plan.steps.size.toLong(),
                nextSteps = plan.steps.map { it.title },
                lastResultSummary = plan.summary
            )
            dataRepo.updateTask(updated)
            dao.upsertCheckpoint(updated.toCheckpoint(jsonArr.toString()))
            return runStepsFrom(
                updated,
                plan.steps.map { StepSpec(it.title, it.tier, it.actionType, it.rationale) },
                jsonArr.toString(),
                uid,
                dataRepo,
                dao,
                gitHubRepo,
                aiRepo,
                configStore,
                activeProject?.id,
                configStore.getAllProtectedTargetsForProject(activeProject),
                onFailover
            )
        }

        return runStepsFrom(
            task,
            steps,
            checkpoint?.stepsJson ?: "[]",
            uid,
            dataRepo,
            dao,
            gitHubRepo,
            aiRepo,
            configStore,
            activeProject?.id,
            configStore.getAllProtectedTargetsForProject(activeProject),
            onFailover
        )
    }

    private suspend fun runStepsFrom(
        initialTask: AgentTask,
        steps: List<StepSpec>,
        stepsJson: String,
        uid: String,
        dataRepo: UserDataRepository,
        dao: com.example.data.local.AgentDao,
        gitHubRepo: GitHubRepository,
        aiRepo: AiOrchestratorRepository,
        configStore: PlatformConfigStore,
        projectId: String?,
        extraProtectedTargets: List<String>,
        onFailover: suspend (AiFailoverAlert) -> Unit
    ): Result {
        var currentTask = initialTask
        val startIndex = currentTask.currentStepIndex.toInt().coerceAtLeast(0)
        val autoTier1 = inputData.getBoolean(KEY_AUTO_TIER1, true)

        for (index in startIndex until steps.size) {
            val step = steps[index]
            val action = AgentActionType.fromWireKey(step.actionType) ?: AgentActionType.ANALYZE_CODE
            when (val guard = ApprovalGuard.evaluateAction(
                action = action,
                autoExecuteTier1 = autoTier1,
                hasExplicitApproval = false,
                targetResource = currentTask.repo,
                extraProtectedTargets = extraProtectedTargets
            )) {
                is GuardDecision.Blocked -> {
                    val blocked = currentTask.copy(
                        status = "failed",
                        lastError = guard.reason
                    )
                    dataRepo.updateTask(blocked)
                    dao.upsertCheckpoint(blocked.toCheckpoint(stepsJson))
                    dataRepo.addLog(
                        OperationLog(
                            userId = uid,
                            category = "security",
                            tier = 3L,
                            action = step.title,
                            details = guard.reason,
                            outcome = "blocked",
                            repo = currentTask.repo
                        )
                    )
                    QuotaNotificationHelper.showAgentStoppedNotification(
                        context = applicationContext,
                        title = "توقفت المهمة بسبب حظر أمني: ${currentTask.title}",
                        reason = "${step.title}: ${guard.reason}",
                        isPausedWillRetry = false,
                        targetRoute = "tasks"
                    )
                    return Result.failure()
                }
                is GuardDecision.RequiresApproval -> {
                    val apprAction = when (action) {
                        AgentActionType.GIT_PUSH -> "git_push"
                        AgentActionType.CREATE_PR -> "create_pr"
                        AgentActionType.STAGING_DEPLOY -> "staging_deploy"
                        AgentActionType.SERVICE_CONFIG -> "service_config"
                        AgentActionType.DB_SQL -> "db_sql"
                        AgentActionType.DB_RLS_GRANTS -> "db_rls_grants"
                        AgentActionType.DB_MIGRATION -> "db_migration"
                        AgentActionType.MERGE_PR -> "merge_pr"
                        AgentActionType.PROD_DEPLOY -> "prod_deploy"
                        else -> "git_push"
                    }
                    dataRepo.createApproval(
                        ApprovalRequest(
                            userId = uid,
                            taskId = currentTask.id,
                            tier = guard.tier.level.toLong(),
                            actionType = apprAction,
                            title = step.title,
                            description = guard.reason,
                            payloadPreview = step.rationale,
                            targetResource = "${currentTask.repo}:${currentTask.branch}"
                        )
                    )
                    val waitingTask = currentTask.copy(
                        status = "awaiting_approval",
                        currentStepIndex = index.toLong(),
                        nextSteps = steps.drop(index).map { it.title },
                        lastResultSummary = "توقف الوكيل بانتظار موافقتك على: ${step.title}"
                    )
                    dataRepo.updateTask(waitingTask)
                    dao.upsertCheckpoint(waitingTask.toCheckpoint(stepsJson))
                    dataRepo.addLog(
                        OperationLog(
                            userId = uid,
                            category = "agent",
                            tier = guard.tier.level.toLong(),
                            action = "طلب موافقة: ${step.title}",
                            details = guard.reason,
                            outcome = "pending",
                            repo = currentTask.repo
                        )
                    )
                    QuotaNotificationHelper.showActionRequiredNotification(
                        context = applicationContext,
                        title = step.title,
                        reason = "المهمة «${currentTask.title}» متوقفة بانتظار موافقتك: ${guard.reason}",
                        tier = guard.tier.level,
                        targetRoute = "security"
                    )
                    return Result.success()
                }
                GuardDecision.AllowedAuto -> {
                    val stepResultObj = executeTier1Step(
                        step = step,
                        task = currentTask,
                        gitHubRepo = gitHubRepo,
                        aiRepo = aiRepo,
                        configStore = configStore,
                        projectId = projectId,
                        onFailover = onFailover
                    )
                    if (stepResultObj.shouldRetryLater) {
                        val pausedTask = currentTask.copy(
                            status = "paused",
                            lastError = stepResultObj.text
                        )
                        dataRepo.updateTask(pausedTask)
                        dao.upsertCheckpoint(pausedTask.toCheckpoint(stepsJson))
                        QuotaNotificationHelper.showAgentStoppedNotification(
                            context = applicationContext,
                            title = "توقف مؤقت للمهمة: ${currentTask.title}",
                            reason = stepResultObj.text,
                            isPausedWillRetry = true,
                            targetRoute = "tasks"
                        )
                        return Result.retry()
                    }
                    val completedCount = index + 1
                    val pct = ((completedCount * 100) / steps.size.coerceAtLeast(1)).coerceIn(0, 100)
                    val sharedFile = projectId?.let { id ->
                        configStore.loadSnapshot().projects.find { it.id == id }?.sharedWorkFilePath
                    }.orEmpty()
                    val updatedFiles = if (sharedFile.isNotBlank() && sharedFile !in currentTask.modifiedFiles) {
                        (currentTask.modifiedFiles + sharedFile).take(30)
                    } else currentTask.modifiedFiles

                    currentTask = currentTask.copy(
                        status = if (completedCount >= steps.size) "completed" else "running",
                        currentStepIndex = completedCount.toLong(),
                        totalSteps = steps.size.toLong(),
                        progressPercent = pct.toLong(),
                        lastCompletedStep = step.title,
                        lastResultSummary = stepResultObj.text.take(1900),
                        lastError = "",
                        modifiedFiles = updatedFiles,
                        nextSteps = steps.drop(completedCount).map { it.title }
                    )
                    dataRepo.updateTask(currentTask)
                    dao.upsertCheckpoint(currentTask.toCheckpoint(stepsJson))
                    dataRepo.addLog(
                        OperationLog(
                            userId = uid,
                            category = "agent",
                            tier = 1L,
                            action = step.title,
                            details = stepResultObj.text.take(2800),
                            outcome = "success",
                            repo = currentTask.repo
                        )
                    )
                    if (completedCount >= steps.size) {
                        QuotaNotificationHelper.showCommandCompletedNotification(
                            context = applicationContext,
                            title = "اكتملت المهمة: ${currentTask.title}",
                            details = "أنهى الوكيل جميع الخطوات (${steps.size}/${steps.size}) في ${currentTask.repo}. آخر خطوة: ${step.title}",
                            targetRoute = "tasks"
                        )
                    }
                }
            }
        }
        return Result.success()
    }

    private data class StepExecutionOutput(
        val text: String,
        val shouldRetryLater: Boolean = false
    )

    private suspend fun executeTier1Step(
        step: StepSpec,
        task: AgentTask,
        gitHubRepo: GitHubRepository,
        aiRepo: AiOrchestratorRepository,
        configStore: PlatformConfigStore,
        projectId: String?,
        onFailover: suspend (AiFailoverAlert) -> Unit
    ): StepExecutionOutput {
        val project = configStore.loadSnapshot().projects.find { it.id == projectId }
        val liveTreeInfo = if (gitHubRepo.hasToken()) {
            val tree = gitHubRepo.getTree(task.repo, task.branch).getOrNull()
            val sampleFiles = tree?.filter { it.type == "blob" }?.take(25)?.joinToString("\n") { it.path }
            sampleFiles ?: "تعذر قراءة شجرة الملفات."
        } else {
            "تنبيه: GitHub PAT غير مضبوط، تم تنفيذ تحليل وصفي فقط بدون قراءة فعلية للشيفرة."
        }
        val analysisPrompt = """
            نفّذ الخطوة التالية من المستوى الأول لمشروع ${task.repo} (فرع ${task.branch}):
            الخطوة: ${step.title}
            السبب: ${step.rationale}
            ${if (!project?.projectRequirements.isNullOrBlank()) "متطلبات المشروع:\n${project?.projectRequirements}\n" else ""}
            عينة الملفات الحقيقية:
            $liveTreeInfo
            أعطِ نتيجة مختصرة وواضحة بما تم فحصه فعلياً.
        """.trimIndent()
        return aiRepo.analyzeOnceWithFailover(
            prompt = analysisPrompt,
            projectId = projectId,
            onFailover = onFailover
        ).fold(
            onSuccess = { StepExecutionOutput(text = "[${it.usedEndpoint.label}]\n${it.text}") },
            onFailure = { err ->
                if (isQuotaOrTransientError(err)) {
                    StepExecutionOutput(
                        text = "استنفدت جميع الحصص مؤقتاً — سيعاود الوكيل المتابعة تلقائياً فور توفر حصة.",
                        shouldRetryLater = true
                    )
                } else {
                    StepExecutionOutput(text = "تم تسجيل الخطوة مع ملاحظة: ${err.localizedMessage}")
                }
            }
        )
    }

    private fun isQuotaOrTransientError(err: Throwable): Boolean {
        val msg = err.message.orEmpty().lowercase()
        return msg.contains("حصة") ||
            msg.contains("quota") ||
            msg.contains("429") ||
            msg.contains("مستنفد") ||
            msg.contains("timeout") ||
            msg.contains("unavailable")
    }

    private fun parseStepsJson(raw: String?): List<StepSpec> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                StepSpec(
                    title = o.optString("title"),
                    tier = o.optInt("tier", 1),
                    actionType = o.optString("actionType", "analyze_code"),
                    rationale = o.optString("rationale")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun AgentTask.toCheckpoint(stepsJson: String) = LocalTaskCheckpoint(
        taskId = id,
        userId = userId,
        repo = repo,
        branch = branch,
        title = title,
        prompt = prompt,
        status = status,
        currentStepIndex = currentStepIndex.toInt(),
        totalSteps = totalSteps.toInt(),
        progressPercent = progressPercent.toInt(),
        lastCompletedStep = lastCompletedStep,
        lastResultSummary = lastResultSummary,
        lastError = lastError,
        modifiedFilesCsv = modifiedFiles.joinToString("|"),
        nextStepsCsv = nextSteps.joinToString("|"),
        stepsJson = stepsJson
    )

    private fun LocalTaskCheckpoint.toAgentTask() = AgentTask(
        id = taskId,
        userId = userId,
        repo = repo,
        branch = branch,
        title = title,
        prompt = prompt,
        status = status,
        currentStepIndex = currentStepIndex.toLong(),
        totalSteps = totalSteps.toLong(),
        progressPercent = progressPercent.toLong(),
        lastCompletedStep = lastCompletedStep,
        lastResultSummary = lastResultSummary,
        lastError = lastError,
        modifiedFiles = modifiedFilesCsv.split("|").filter { it.isNotBlank() },
        nextSteps = nextStepsCsv.split("|").filter { it.isNotBlank() }
    )

    data class StepSpec(
        val title: String,
        val tier: Int,
        val actionType: String,
        val rationale: String
    )

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_MODEL = "ai_model"
        const val KEY_AUTO_TIER1 = "auto_tier1"

        fun enqueue(
            context: Context,
            taskId: String,
            provider: String,
            model: String,
            autoTier1: Boolean,
            replaceExisting: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<AgentLoopWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        KEY_TASK_ID to taskId,
                        KEY_PROVIDER to provider,
                        KEY_MODEL to model,
                        KEY_AUTO_TIER1 to autoTier1
                    )
                )
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    "metra_agent_task_$taskId",
                    if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}
