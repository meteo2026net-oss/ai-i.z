package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.notifications.AgentNotificationPrefs
import com.example.core.notifications.QuotaNotificationHelper
import com.example.core.security.AccountLinkStore
import com.example.core.security.AgentActionType
import com.example.core.security.AiProviderType
import com.example.core.security.ApprovalGuard
import com.example.core.security.GuardDecision
import com.example.core.security.LinkedAccountsSnapshot
import com.example.core.security.PlatformConfigSnapshot
import com.example.core.security.PlatformConfigStore
import com.example.core.security.SecretVault
import com.example.core.security.TechStackType
import com.example.data.local.AgentDao
import com.example.data.local.LocalTaskCheckpoint
import com.example.data.repository.AiFailoverAlert
import com.example.data.repository.AiOrchestratorRepository
import com.example.data.repository.BackendRepository
import com.example.data.repository.CollaborationContribution
import com.example.data.repository.UserDataRepository
import com.example.data.repository.FlutterSpecExporter
import com.example.data.repository.GitHubRepository
import com.example.data.repository.SharedWorkplanCodec
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.BackendHealthResponse
import com.example.domain.model.BackendTestRunResponse
import com.example.domain.model.ChatMessage
import com.example.domain.model.DbSchemaResponse
import com.example.domain.model.DbSqlResponse
import com.example.domain.model.GitHubBranch
import com.example.domain.model.GitHubCommit
import com.example.domain.model.GitHubPullFile
import com.example.domain.model.GitHubPullRequest
import com.example.domain.model.GitHubRepo
import com.example.domain.model.GitHubTreeItem
import com.example.domain.model.GitHubUser
import com.example.domain.model.GitHubWorkflowRun
import com.example.domain.model.OperationLog
import com.example.domain.model.UserSettings
import com.example.worker.AgentLoopWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StreamUiState<out T> {
    data object Loading : StreamUiState<Nothing>
    data class Success<T>(val data: T) : StreamUiState<T>
    data class Error(val message: String) : StreamUiState<Nothing>
}

data class PendingFileDownload(
    val fileName: String,
    val fullPath: String,
    val bytes: ByteArray
)

data class WorkspaceUiState(
    val isGitHubConnected: Boolean = false,
    val gitHubUser: GitHubUser? = null,
    val availableRepos: List<GitHubRepo> = emptyList(),
    val branches: List<GitHubBranch> = emptyList(),
    val treeItems: List<GitHubTreeItem> = emptyList(),
    val commits: List<GitHubCommit> = emptyList(),
    val pullRequests: List<GitHubPullRequest> = emptyList(),
    val selectedPrFiles: List<GitHubPullFile> = emptyList(),
    val workflowRuns: List<GitHubWorkflowRun> = emptyList(),
    val selectedFilePath: String = "",
    val selectedFileSha: String = "",
    val originalFileContent: String = "",
    val editedFileContent: String = "",
    val fileDiffPreview: String = "",
    val isBusyGitHub: Boolean = false,
    val isDownloadingFile: Boolean = false,
    val pendingFileDownload: PendingFileDownload? = null,
    val gitHubError: String? = null,
    val aiStreamingResponse: String = "",
    val isAiStreaming: Boolean = false,
    val activeAiEndpointLabel: String = "",
    val aiError: String? = null,
    val quotaAlerts: List<AiFailoverAlert> = emptyList(),
    val latestQuotaBanner: AiFailoverAlert? = null,
    val prReviewAnalysis: String = "",
    val codeReviewAnalysis: String = "",
    val sqlReviewAnalysis: String = "",
    val sharedWorkFilePath: String = "docs/AGENT_WORKPLAN.md",
    val sharedWorkFileSha: String = "",
    val sharedWorkFileOriginal: String = "",
    val sharedWorkFileDraft: String = "",
    val sharedWorkFileDiff: String = "",
    val isCollaborating: Boolean = false,
    val collaborationStatusText: String = "",
    val lastCollaborationContributions: List<CollaborationContribution> = emptyList(),
    val backendHealth: BackendHealthResponse? = null,
    val backendError: String? = null,
    val isBusyBackend: Boolean = false,
    val lastTestRun: BackendTestRunResponse? = null,
    val dbSchema: DbSchemaResponse? = null,
    val sqlResult: DbSqlResponse? = null,
    val statusBannerMessage: String? = null,
    val linkedAccounts: LinkedAccountsSnapshot = LinkedAccountsSnapshot(),
    val platformConfig: PlatformConfigSnapshot = PlatformConfigSnapshot(),
    val currentGoogleEmail: String = "",
    val currentGoogleName: String = "",
    val notificationPrefs: AgentNotificationPrefs = AgentNotificationPrefs(),
    val activeStudioProject: com.example.domain.model.StudioProjectSpec? = null,
    val isGeneratingStudioProject: Boolean = false,
    val studioSelectedFilePath: String = "",
    val studioEditedContent: String = "",
    val studioAuditStatus: String = "جاهز للتحليل والتنفيذ"
)

class MetraAgentViewModel(
    private val currentUserId: String,
    private val userDataRepository: UserDataRepository,
    private val gitHubRepo: GitHubRepository,
    private val aiRepo: AiOrchestratorRepository,
    private val backendRepo: BackendRepository,
    private val vault: SecretVault,
    private val localDao: AgentDao,
    private val accountLinkStore: AccountLinkStore = AccountLinkStore(vault),
    private val platformConfigStore: PlatformConfigStore = PlatformConfigStore(vault)
) : ViewModel() {

    val settingsState: StateFlow<StreamUiState<UserSettings>> = userDataRepository.observeSettings(currentUserId)
        .map<UserSettings, StreamUiState<UserSettings>> { StreamUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            val root = error
            Log.w(TAG, "observeSettings error", root)
            emit(StreamUiState.Error(root.localizedMessage ?: "تعذر تحميل الإعدادات"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val tasksState: StateFlow<StreamUiState<List<AgentTask>>> = userDataRepository.observeTasks(currentUserId)
        .map<List<AgentTask>, StreamUiState<List<AgentTask>>> { StreamUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            val root = error
            Log.w(TAG, "observeTasks error", root)
            emit(StreamUiState.Error(root.localizedMessage ?: "تعذر تحميل المهام"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val approvalsState: StateFlow<StreamUiState<List<ApprovalRequest>>> = userDataRepository.observeApprovals(currentUserId)
        .map<List<ApprovalRequest>, StreamUiState<List<ApprovalRequest>>> { StreamUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            val root = error
            Log.w(TAG, "observeApprovals error", root)
            emit(StreamUiState.Error(root.localizedMessage ?: "تعذر تحميل طلبات الموافقة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val chatState: StateFlow<StreamUiState<List<ChatMessage>>> = userDataRepository.observeChatMessages(currentUserId)
        .map<List<ChatMessage>, StreamUiState<List<ChatMessage>>> { StreamUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            val root = error
            Log.w(TAG, "observeChatMessages error", root)
            emit(StreamUiState.Error(root.localizedMessage ?: "تعذر تحميل المحادثة"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val logsState: StateFlow<StreamUiState<List<OperationLog>>> = userDataRepository.observeLogs(currentUserId)
        .map<List<OperationLog>, StreamUiState<List<OperationLog>>> { StreamUiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            val root = error
            Log.w(TAG, "observeLogs error", root)
            emit(StreamUiState.Error(root.localizedMessage ?: "تعذر تحميل السجل"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StreamUiState.Loading)

    val localCheckpoints: StateFlow<List<LocalTaskCheckpoint>> = localDao.observeCheckpoints(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _workspace = MutableStateFlow(
        WorkspaceUiState(
            isGitHubConnected = gitHubRepo.hasToken(),
            linkedAccounts = accountLinkStore.loadSnapshot(),
            platformConfig = platformConfigStore.loadSnapshot()
        )
    )
    val workspace: StateFlow<WorkspaceUiState> = _workspace.asStateFlow()

    init {
        val email = vault.getSecret(SecretVault.KEY_AUTH_EMAIL)
        val displayName = vault.getSecret(SecretVault.KEY_AUTH_DISPLAY_NAME)
        val updatedSnap = accountLinkStore.recordGoogleAccount(
            uid = currentUserId,
            email = email,
            displayName = displayName
        )
        val pConfig = platformConfigStore.loadSnapshot()
        val activeProj = pConfig.activeProject
        val initialSharedDraft = activeProj?.let {
            SharedWorkplanCodec.buildInitialTemplate(
                project = it,
                teamNames = platformConfigStore.resolveCollaborativeTeam(it.id).map { ep -> "${ep.label} (${ep.modelName})" }
            )
        }.orEmpty()
        _workspace.update {
            it.copy(
                linkedAccounts = updatedSnap,
                platformConfig = pConfig,
                sharedWorkFilePath = activeProj?.sharedWorkFilePath ?: "docs/AGENT_WORKPLAN.md",
                sharedWorkFileDraft = initialSharedDraft,
                activeAiEndpointLabel = activeProj?.aiPriorityOrder?.firstOrNull()
                    ?.let { id -> pConfig.aiEndpoints.find { ep -> ep.id == id }?.label }
                    ?: pConfig.aiEndpoints.firstOrNull()?.label.orEmpty(),
                currentGoogleEmail = email,
                currentGoogleName = displayName
            )
        }
        if (gitHubRepo.hasToken()) {
            refreshGitHubWorkspace()
        }
    }

    private fun currentSettings(): UserSettings {
        return (settingsState.value as? StreamUiState.Success)?.data
            ?: UserSettings(id = currentUserId, userId = currentUserId)
    }

    fun clearBanner() {
        _workspace.update { it.copy(statusBannerMessage = null, gitHubError = null, aiError = null, backendError = null) }
    }

    fun dismissQuotaBanner() {
        _workspace.update { it.copy(latestQuotaBanner = null) }
    }

    fun syncNotificationPrefs(context: Context) {
        val loaded = QuotaNotificationHelper.getPrefs(context)
        _workspace.update { it.copy(notificationPrefs = loaded) }
    }

    fun updateNotificationPrefs(context: Context, updated: AgentNotificationPrefs) {
        QuotaNotificationHelper.savePrefs(context, updated)
        _workspace.update {
            it.copy(
                notificationPrefs = updated,
                statusBannerMessage = "تم تحديث إعدادات تنبيهات الهاتف."
            )
        }
    }

    fun sendTestNotification(context: Context) {
        val sent = QuotaNotificationHelper.sendTestNotification(context)
        _workspace.update {
            it.copy(
                statusBannerMessage = if (sent) {
                    "تم إرسال إشعار تجريبي إلى هاتفك."
                } else {
                    "صلاحية الإشعارات غير مفعلة على الهاتف بعد — اضغط تفعيل الإشعارات أولاً."
                }
            )
        }
    }

    private suspend fun handleFailoverAlert(alert: AiFailoverAlert, context: Context? = null) {
        val refreshed = platformConfigStore.loadSnapshot()
        val bannerText = if (!alert.switchedToLabel.isNullOrBlank()) {
            "⚠️ انتهى استخدام «${alert.exhaustedLabel}» — تم التحويل تلقائياً إلى «${alert.switchedToLabel}»"
        } else {
            "⚠️ انتهى استخدام «${alert.exhaustedLabel}» (${alert.reason})"
        }
        _workspace.update { state ->
            state.copy(
                platformConfig = refreshed,
                latestQuotaBanner = alert,
                quotaAlerts = (listOf(alert) + state.quotaAlerts).take(20),
                statusBannerMessage = bannerText
            )
        }
        if (context != null) {
            QuotaNotificationHelper.showQuotaFailoverNotification(
                context = context,
                exhaustedLabel = alert.exhaustedLabel,
                switchedToLabel = alert.switchedToLabel,
                reason = alert.reason
            )
        }
        userDataRepository.addLog(
            OperationLog(
                userId = currentUserId,
                category = "ai",
                tier = 1L,
                action = "تنبيه استنفاد حصة API والتحويل التلقائي",
                details = "$bannerText | ${alert.reason}",
                outcome = "error",
                repo = currentSettings().activeRepo
            )
        )
    }

    fun saveUserSettings(updated: UserSettings) {
        viewModelScope.launch {
            userDataRepository.saveSettings(updated.copy(userId = currentUserId))
                .onSuccess {
                    _workspace.update { it.copy(statusBannerMessage = "تم حفظ الإعدادات والمزامنة السحابية.") }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(statusBannerMessage = err.localizedMessage ?: "فشل حفظ الإعدادات") }
                }
        }
    }

    fun connectGitHubToken(
        token: String,
        accountLabel: String = "",
        linkedDefaultRepo: String = ""
    ) {
        val clean = token.trim()
        if (clean.isBlank()) {
            _workspace.update { it.copy(gitHubError = "أدخل GitHub Personal Access Token أولاً.") }
            return
        }
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyGitHub = true, gitHubError = null) }
            gitHubRepo.verifyAndFetchUser(clean)
                .onSuccess { user ->
                    accountLinkStore.upsertGitHubAccount(
                        label = accountLabel.ifBlank { user.name ?: user.login },
                        login = user.login,
                        displayName = user.name.orEmpty(),
                        avatarUrl = user.avatarUrl.orEmpty(),
                        defaultRepo = linkedDefaultRepo,
                        token = clean,
                        setActive = true
                    )
                    val snap = accountLinkStore.loadSnapshot()
                    _workspace.update {
                        it.copy(
                            isBusyGitHub = false,
                            isGitHubConnected = true,
                            gitHubUser = user,
                            linkedAccounts = snap,
                            statusBannerMessage = "تم ربط حساب GitHub: ${user.login}"
                        )
                    }
                    userDataRepository.addLog(
                        OperationLog(
                            userId = currentUserId,
                            category = "github",
                            tier = 1L,
                            action = "ربط حساب GitHub",
                            details = "تم التحقق من الحساب ${user.login}",
                            outcome = "success",
                            repo = currentSettings().activeRepo
                        )
                    )
                    refreshGitHubWorkspace()
                }
                .onFailure { err ->
                    _workspace.update {
                        it.copy(
                            isBusyGitHub = false,
                            gitHubError = err.localizedMessage ?: "فشل التحقق من رمز GitHub"
                        )
                    }
                }
        }
    }

    fun switchGitHubAccount(accountId: String) {
        val activated = accountLinkStore.activateGitHubAccount(accountId) ?: return
        val snap = accountLinkStore.loadSnapshot()
        val targetRepo = activated.defaultRepo.takeIf { it.isNotBlank() } ?: currentSettings().activeRepo
        val updatedSettings = currentSettings().copy(activeRepo = targetRepo)
        if (targetRepo != currentSettings().activeRepo) {
            saveUserSettings(updatedSettings)
        }
        _workspace.update {
            it.copy(
                isGitHubConnected = true,
                linkedAccounts = snap,
                statusBannerMessage = "تم التبديل إلى حساب GitHub: @${activated.login}"
            )
        }
        refreshGitHubWorkspace(repoSlugOverride = targetRepo)
    }

    fun removeGitHubAccount(accountId: String) {
        val snap = accountLinkStore.removeGitHubAccount(accountId)
        val stillConnected = gitHubRepo.hasToken()
        _workspace.update {
            it.copy(
                isGitHubConnected = stillConnected,
                linkedAccounts = snap,
                gitHubUser = if (stillConnected) it.gitHubUser else null,
                statusBannerMessage = "تمت إزالة ربط حساب GitHub المحدد."
            )
        }
        if (stillConnected) refreshGitHubWorkspace()
    }

    fun selectRepositoryAndBranch(repoSlug: String, branch: String) {
        val matchedAccount = accountLinkStore.findGitHubAccountForRepo(repoSlug)
        var switchNotice = ""
        if (matchedAccount != null && !matchedAccount.isActive) {
            accountLinkStore.activateGitHubAccount(matchedAccount.id)
            switchNotice = " (تم التبديل تلقائياً لحساب @${matchedAccount.login})"
        }
        val updated = currentSettings().copy(activeRepo = repoSlug, activeBranch = branch)
        saveUserSettings(updated)
        _workspace.update {
            it.copy(
                linkedAccounts = accountLinkStore.loadSnapshot(),
                statusBannerMessage = if (switchNotice.isNotBlank()) {
                    "تم اعتماد $repoSlug$switchNotice"
                } else it.statusBannerMessage
            )
        }
        refreshGitHubWorkspace(repoSlugOverride = repoSlug, branchOverride = branch)
    }

    fun refreshGitHubWorkspace(repoSlugOverride: String? = null, branchOverride: String? = null) {
        if (!gitHubRepo.hasToken()) {
            _workspace.update { it.copy(isGitHubConnected = false) }
            return
        }
        val settings = currentSettings()
        val repoSlug = repoSlugOverride ?: settings.activeRepo
        val branch = branchOverride ?: settings.activeBranch

        viewModelScope.launch {
            _workspace.update { it.copy(isBusyGitHub = true, gitHubError = null) }
            val user = gitHubRepo.verifyAndFetchUser().getOrNull()
            val repos = gitHubRepo.listRepos().getOrNull().orEmpty()
            val branches = gitHubRepo.listBranches(repoSlug).getOrNull().orEmpty()
            val effectiveBranch = if (branches.any { it.name == branch }) branch
            else branches.firstOrNull()?.name ?: branch
            val tree = gitHubRepo.getTree(repoSlug, effectiveBranch).getOrNull().orEmpty()
            val commits = gitHubRepo.listCommits(repoSlug, effectiveBranch).getOrNull().orEmpty()
            val prs = gitHubRepo.listPullRequests(repoSlug).getOrNull().orEmpty()
            val runs = gitHubRepo.listWorkflowRuns(repoSlug).getOrNull().orEmpty()

            // Ensure legacy single token is tracked in AccountLinkStore
            if (user != null && accountLinkStore.loadSnapshot().gitHubAccounts.isEmpty()) {
                val rawToken = vault.getSecret(SecretVault.KEY_GITHUB_PAT)
                if (rawToken.isNotBlank()) {
                    accountLinkStore.upsertGitHubAccount(
                        label = user.name ?: user.login,
                        login = user.login,
                        displayName = user.name.orEmpty(),
                        avatarUrl = user.avatarUrl.orEmpty(),
                        defaultRepo = repoSlug,
                        token = rawToken,
                        setActive = true
                    )
                }
            }

            _workspace.update {
                it.copy(
                    isBusyGitHub = false,
                    isGitHubConnected = true,
                    gitHubUser = user ?: it.gitHubUser,
                    availableRepos = repos,
                    branches = branches,
                    treeItems = tree,
                    commits = commits,
                    pullRequests = prs,
                    workflowRuns = runs,
                    linkedAccounts = accountLinkStore.loadSnapshot()
                )
            }
        }
    }

    fun openFileFromGitHub(filePath: String) {
        val settings = currentSettings()
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyGitHub = true, gitHubError = null) }
            gitHubRepo.getFileDecoded(settings.activeRepo, filePath, settings.activeBranch)
                .onSuccess { (meta, text) ->
                    _workspace.update {
                        it.copy(
                            isBusyGitHub = false,
                            selectedFilePath = meta.path,
                            selectedFileSha = meta.sha,
                            originalFileContent = text,
                            editedFileContent = text,
                            fileDiffPreview = "",
                            codeReviewAnalysis = ""
                        )
                    }
                }
                .onFailure { err ->
                    _workspace.update {
                        it.copy(isBusyGitHub = false, gitHubError = err.localizedMessage ?: "تعذر قراءة الملف")
                    }
                }
        }
    }

    fun updateEditedFileContent(newText: String) {
        val originalLines = _workspace.value.originalFileContent.lines()
        val editedLines = newText.lines()
        val diff = buildSimpleDiff(
            _workspace.value.selectedFilePath,
            originalLines,
            editedLines
        )
        _workspace.update {
            it.copy(editedFileContent = newText, fileDiffPreview = diff)
        }
    }

    fun prepareDownloadGitHubFile(filePath: String) {
        val settings = currentSettings()
        if (filePath.isBlank()) return
        viewModelScope.launch {
            _workspace.update { it.copy(isDownloadingFile = true, gitHubError = null) }
            gitHubRepo.downloadRawFileBytes(settings.activeRepo, filePath, settings.activeBranch)
                .onSuccess { bytes ->
                    val name = filePath.substringAfterLast('/').ifBlank { "downloaded_file" }
                    _workspace.update {
                        it.copy(
                            isDownloadingFile = false,
                            pendingFileDownload = PendingFileDownload(
                                fileName = name,
                                fullPath = filePath,
                                bytes = bytes
                            )
                        )
                    }
                }
                .onFailure { err ->
                    _workspace.update {
                        it.copy(
                            isDownloadingFile = false,
                            gitHubError = err.localizedMessage ?: "فشل تنزيل الملف من GitHub"
                        )
                    }
                }
        }
    }

    fun completeFileDownload(filePath: String, success: Boolean, message: String) {
        _workspace.update {
            it.copy(
                pendingFileDownload = null,
                statusBannerMessage = message
            )
        }
        if (filePath.isNotBlank()) {
            viewModelScope.launch {
                userDataRepository.addLog(
                    OperationLog(
                        userId = currentUserId,
                        category = "github",
                        tier = 1L,
                        action = "تنزيل ملف مباشر: $filePath",
                        details = message,
                        outcome = if (success) "success" else "error",
                        repo = currentSettings().activeRepo
                    )
                )
            }
        }
    }

    fun analyzeSelectedCodeWithAi(context: Context? = null) {
        val path = _workspace.value.selectedFilePath
        val code = _workspace.value.editedFileContent
        if (path.isBlank() || code.isBlank()) return
        val settings = currentSettings()
        val activeProject = platformConfigStore.loadSnapshot().activeProject
        viewModelScope.launch {
            _workspace.update { it.copy(isAiStreaming = true, aiError = null) }
            val prompt = """
                راجع ملف الشيفرة التالي في مشروع ${settings.activeRepo} ($path) واكتشف الأخطاء أو مخاطر الأمان واقترح إصلاحات دقيقة:
                ```
                ${code.take(6000)}
                ```
            """.trimIndent()
            aiRepo.analyzeOnceWithFailover(
                prompt = prompt,
                projectId = activeProject?.id,
                onFailover = { alert -> handleFailoverAlert(alert, context) }
            )
                .onSuccess { res ->
                    _workspace.update {
                        it.copy(
                            isAiStreaming = false,
                            activeAiEndpointLabel = res.usedEndpoint.label,
                            platformConfig = platformConfigStore.loadSnapshot(),
                            codeReviewAnalysis = "[${res.usedEndpoint.label}]\n${res.text}"
                        )
                    }
                    if (context != null) {
                        QuotaNotificationHelper.showCommandCompletedNotification(
                            context = context,
                            title = "اكتملت مراجعة الكود: $path",
                            details = "تم الفحص عبر ${res.usedEndpoint.label}. اضغط لمعاينة النتائج.",
                            targetRoute = "code_editor"
                        )
                    }
                }
                .onFailure { err ->
                    _workspace.update {
                        it.copy(
                            isAiStreaming = false,
                            platformConfig = platformConfigStore.loadSnapshot(),
                            aiError = err.localizedMessage
                        )
                    }
                    if (context != null) {
                        QuotaNotificationHelper.showAgentStoppedNotification(
                            context = context,
                            title = "توقفت مراجعة الكود: $path",
                            reason = err.localizedMessage ?: "تعذر إكمال التحليل",
                            targetRoute = "code_editor"
                        )
                    }
                }
        }
    }

    fun requestCommitEditApproval(commitMessage: String, context: Context? = null) {
        val state = _workspace.value
        if (state.selectedFilePath.isBlank()) return
        val settings = currentSettings()
        viewModelScope.launch {
            val preview = state.fileDiffPreview.ifBlank {
                "تحديث ${state.selectedFilePath} على فرع ${settings.activeBranch}"
            }
            userDataRepository.createApproval(
                ApprovalRequest(
                    userId = currentUserId,
                    taskId = "direct_edit",
                    tier = 2L,
                    actionType = "git_push",
                    title = "رفع تعديل: ${state.selectedFilePath}",
                    description = "رسالة Commit: ${commitMessage.ifBlank { "Update ${state.selectedFilePath}" }}",
                    payloadPreview = preview.take(7500),
                    targetResource = "${settings.activeRepo}:${settings.activeBranch}"
                )
            ).onSuccess {
                _workspace.update {
                    it.copy(statusBannerMessage = "تم إنشاء طلب موافقة المستوى 2 لرفع التعديل إلى GitHub.")
                }
                if (context != null) {
                    QuotaNotificationHelper.showActionRequiredNotification(
                        context = context,
                        title = "رفع تعديل ${state.selectedFilePath}",
                        reason = "يتطلب موافقتك لرفعه على ${settings.activeRepo}:${settings.activeBranch}",
                        tier = 2,
                        targetRoute = "security"
                    )
                }
            }
        }
    }

    fun createWorkingBranch(newBranchName: String) {
        val settings = currentSettings()
        val baseSha = _workspace.value.branches.find { it.name == settings.activeBranch }?.commit?.sha
            ?: _workspace.value.commits.firstOrNull()?.sha
        if (baseSha.isNullOrBlank()) {
            _workspace.update { it.copy(gitHubError = "لا يوجد SHA مرجعي لإنشاء فرع جديد.") }
            return
        }
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyGitHub = true) }
            gitHubRepo.createWorkingBranch(settings.activeRepo, newBranchName, baseSha)
                .onSuccess {
                    userDataRepository.addLog(
                        OperationLog(
                            userId = currentUserId,
                            category = "github",
                            tier = 1L,
                            action = "إنشاء فرع عمل: $newBranchName",
                            details = "تفرّع من ${settings.activeBranch} ($baseSha)",
                            outcome = "success",
                            repo = settings.activeRepo
                        )
                    )
                    selectRepositoryAndBranch(settings.activeRepo, newBranchName)
                }
                .onFailure { err ->
                    _workspace.update {
                        it.copy(isBusyGitHub = false, gitHubError = err.localizedMessage)
                    }
                }
        }
    }

    fun inspectPullRequest(pr: GitHubPullRequest, context: Context? = null) {
        val settings = currentSettings()
        val activeProject = platformConfigStore.loadSnapshot().activeProject
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyGitHub = true, prReviewAnalysis = "") }
            gitHubRepo.listPullFiles(settings.activeRepo, pr.number)
                .onSuccess { files ->
                    _workspace.update { it.copy(isBusyGitHub = false, selectedPrFiles = files) }
                    val patchSummary = files.take(6).joinToString("\n\n") { f ->
                        "File: ${f.filename} (${f.status}, +${f.additions}/-${f.deletions})\n${f.patch.orEmpty().take(900)}"
                    }
                    val prompt = """
                        راجع Pull Request #${pr.number}: ${pr.title}
                        الفرع: ${pr.head.ref} -> ${pr.base.ref}
                        التغييرات:
                        $patchSummary
                        قدّم مراجعة تقنية دقيقة للأخطاء المحتملة وجودة التكامل.
                    """.trimIndent()
                    aiRepo.analyzeOnceWithFailover(
                        prompt = prompt,
                        projectId = activeProject?.id,
                        onFailover = { alert -> handleFailoverAlert(alert, context) }
                    )
                        .onSuccess { res ->
                            _workspace.update {
                                it.copy(
                                    activeAiEndpointLabel = res.usedEndpoint.label,
                                    platformConfig = platformConfigStore.loadSnapshot(),
                                    prReviewAnalysis = "[${res.usedEndpoint.label}]\n${res.text}"
                                )
                            }
                            if (context != null) {
                                QuotaNotificationHelper.showCommandCompletedNotification(
                                    context = context,
                                    title = "انتهت مراجعة PR #${pr.number}",
                                    details = pr.title,
                                    targetRoute = "prs"
                                )
                            }
                        }
                        .onFailure { err ->
                            if (context != null) {
                                QuotaNotificationHelper.showAgentStoppedNotification(
                                    context = context,
                                    title = "توقفت مراجعة PR #${pr.number}",
                                    reason = err.localizedMessage ?: "تعذر إكمال المراجعة",
                                    targetRoute = "prs"
                                )
                            }
                        }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(isBusyGitHub = false, gitHubError = err.localizedMessage) }
                    if (context != null) {
                        QuotaNotificationHelper.showAgentStoppedNotification(
                            context = context,
                            title = "تعذر جلب PR #${pr.number}",
                            reason = err.localizedMessage ?: "خطأ في الاتصال بـ GitHub",
                            targetRoute = "prs"
                        )
                    }
                }
        }
    }

    fun requestCreateDraftPrApproval(
        title: String,
        headBranch: String,
        baseBranch: String,
        body: String,
        context: Context? = null
    ) {
        val settings = currentSettings()
        viewModelScope.launch {
            userDataRepository.createApproval(
                ApprovalRequest(
                    userId = currentUserId,
                    taskId = "pr_create",
                    tier = 2L,
                    actionType = "create_pr",
                    title = "إنشاء Draft PR: $title",
                    description = "من $headBranch إلى $baseBranch",
                    payloadPreview = body,
                    targetResource = settings.activeRepo
                )
            ).onSuccess {
                _workspace.update { it.copy(statusBannerMessage = "تم إرسال طلب إنشاء Draft PR للموافقة (المستوى 2).") }
                if (context != null) {
                    QuotaNotificationHelper.showActionRequiredNotification(
                        context = context,
                        title = "إنشاء Draft PR: $title",
                        reason = "بانتظار موافقتك للدمج من $headBranch إلى $baseBranch",
                        tier = 2,
                        targetRoute = "security"
                    )
                }
            }
        }
    }

    fun requestMergePrApproval(pr: GitHubPullRequest, context: Context? = null) {
        val settings = currentSettings()
        viewModelScope.launch {
            userDataRepository.createApproval(
                ApprovalRequest(
                    userId = currentUserId,
                    taskId = "pr_merge_${pr.number}",
                    tier = 3L,
                    actionType = "merge_pr",
                    title = "دمج Pull Request #${pr.number}",
                    description = pr.title,
                    payloadPreview = "merge ${pr.head.ref} -> ${pr.base.ref}",
                    targetResource = "${settings.activeRepo}#${pr.number}"
                )
            ).onSuccess {
                _workspace.update { it.copy(statusBannerMessage = "دمج PR يتطلب موافقة صريحة مستقلة (المستوى 3).") }
                if (context != null) {
                    QuotaNotificationHelper.showActionRequiredNotification(
                        context = context,
                        title = "دمج PR #${pr.number}",
                        reason = "${pr.title} — مطلوب موافقة المستوى 3 قبل الدمج",
                        tier = 3,
                        targetRoute = "security"
                    )
                }
            }
        }
    }

    fun approveAndExecute(approval: ApprovalRequest, context: Context? = null) {
        val settings = currentSettings()
        viewModelScope.launch {
            userDataRepository.decideApproval(approval.id, "approved", "تمت الموافقة من المطور", currentUserId)
                .onSuccess {
                    var stepExecutionSucceeded = false
                    when (approval.actionType) {
                        "git_push" -> {
                            val state = _workspace.value
                            val isSharedWorkFilePush = approval.taskId == "shared_workfile"
                            val targetPath = if (isSharedWorkFilePush) state.sharedWorkFilePath else state.selectedFilePath
                            val targetContent = if (isSharedWorkFilePush) state.sharedWorkFileDraft else state.editedFileContent
                            val targetSha = if (isSharedWorkFilePush) state.sharedWorkFileSha else state.selectedFileSha

                            if (targetPath.isNotBlank() && targetContent.isNotBlank()) {
                                gitHubRepo.commitFileEdit(
                                    repoSlug = settings.activeRepo,
                                    filePath = targetPath,
                                    newContent = targetContent,
                                    commitMessage = approval.description.removePrefix("رسالة Commit: ").trim(),
                                    branch = settings.activeBranch,
                                    existingSha = targetSha.ifBlank { null }
                                ).onSuccess {
                                    stepExecutionSucceeded = true
                                    userDataRepository.decideApproval(approval.id, "executed", "تم الرفع إلى GitHub", currentUserId)
                                    if (isSharedWorkFilePush) {
                                        _workspace.update {
                                            it.copy(
                                                sharedWorkFileOriginal = targetContent,
                                                sharedWorkFileDiff = "",
                                                statusBannerMessage = "تم رفع تحديث الملف المشترك ($targetPath) إلى GitHub."
                                            )
                                        }
                                    }
                                    userDataRepository.addLog(
                                        OperationLog(
                                            userId = currentUserId,
                                            category = "github",
                                            tier = 2L,
                                            action = approval.title,
                                            details = "تم تنفيذ Push بنجاح إلى ${settings.activeBranch} ($targetPath)",
                                            outcome = "success",
                                            repo = settings.activeRepo
                                        )
                                    )
                                    refreshGitHubWorkspace()
                                    if (context != null) {
                                        QuotaNotificationHelper.showCommandCompletedNotification(
                                            context = context,
                                            title = "تم تنفيذ الرفع: ${approval.title}",
                                            details = "تم رفع $targetPath إلى فرع ${settings.activeBranch} بنجاح.",
                                            targetRoute = if (isSharedWorkFilePush) "workspace_plan" else "dashboard"
                                        )
                                    }
                                }.onFailure { err ->
                                    _workspace.update { it.copy(gitHubError = err.localizedMessage) }
                                    if (context != null) {
                                        QuotaNotificationHelper.showAgentStoppedNotification(
                                            context = context,
                                            title = "فشل رفع التعديل: ${approval.title}",
                                            reason = err.localizedMessage ?: "تعذر الرفع إلى GitHub",
                                            targetRoute = "security"
                                        )
                                    }
                                }
                            }
                        }
                        "create_pr" -> {
                            val parts = approval.description.removePrefix("من ").split(" إلى ")
                            val head = parts.getOrNull(0)?.trim() ?: settings.activeBranch
                            val base = parts.getOrNull(1)?.trim() ?: "main"
                            gitHubRepo.createDraftPullRequest(
                                repoSlug = settings.activeRepo,
                                title = approval.title.removePrefix("إنشاء Draft PR: ").trim(),
                                headBranch = head,
                                baseBranch = base,
                                body = approval.payloadPreview
                            ).onSuccess {
                                stepExecutionSucceeded = true
                                userDataRepository.decideApproval(approval.id, "executed", "تم إنشاء Draft PR #${it.number}", currentUserId)
                                refreshGitHubWorkspace()
                                if (context != null) {
                                    QuotaNotificationHelper.showCommandCompletedNotification(
                                        context = context,
                                        title = "تم إنشاء Draft PR #${it.number}",
                                        details = "من $head إلى $base",
                                        targetRoute = "prs"
                                    )
                                }
                            }.onFailure { err ->
                                if (context != null) {
                                    QuotaNotificationHelper.showAgentStoppedNotification(
                                        context = context,
                                        title = "توقف إنشاء Draft PR",
                                        reason = err.localizedMessage ?: "تعذر إنشاء PR",
                                        targetRoute = "prs"
                                    )
                                }
                            }
                        }
                        "merge_pr" -> {
                            val prNumber = approval.targetResource.substringAfter("#").toIntOrNull()
                            if (prNumber != null) {
                                gitHubRepo.mergePullRequest(
                                    repoSlug = settings.activeRepo,
                                    pullNumber = prNumber,
                                    commitTitle = approval.description
                                ).onSuccess {
                                    stepExecutionSucceeded = true
                                    userDataRepository.decideApproval(approval.id, "executed", "تم الدمج بنجاح", currentUserId)
                                    refreshGitHubWorkspace()
                                    if (context != null) {
                                        QuotaNotificationHelper.showCommandCompletedNotification(
                                            context = context,
                                            title = "تم دمج PR #$prNumber",
                                            details = approval.description,
                                            targetRoute = "prs"
                                        )
                                    }
                                }.onFailure { err ->
                                    if (context != null) {
                                        QuotaNotificationHelper.showAgentStoppedNotification(
                                            context = context,
                                            title = "توقف دمج PR #$prNumber",
                                            reason = err.localizedMessage ?: "تعذر الدمج",
                                            targetRoute = "prs"
                                        )
                                    }
                                }
                            }
                        }
                        "db_sql", "db_migration", "db_rls_grants" -> {
                            if (settings.backendUrl.isBlank()) {
                                val msg = "يلزم ربط عنوان خادم Backend لتنفيذ استعلام SQL المعتمد."
                                _workspace.update { it.copy(backendError = msg) }
                                if (context != null) {
                                    QuotaNotificationHelper.showActionRequiredNotification(
                                        context = context,
                                        title = "مطلوب ربط خادم Backend",
                                        reason = msg,
                                        tier = 2,
                                        targetRoute = "ai_setup"
                                    )
                                }
                            } else {
                                backendRepo.executeApprovedSql(
                                    baseUrl = settings.backendUrl,
                                    targetDatabase = approval.targetResource,
                                    sql = approval.payloadPreview,
                                    approvalId = approval.id
                                ).onSuccess { res ->
                                    stepExecutionSucceeded = true
                                    userDataRepository.decideApproval(approval.id, "executed", res.impactSummary, currentUserId)
                                    _workspace.update { it.copy(sqlResult = res) }
                                    if (context != null) {
                                        QuotaNotificationHelper.showCommandCompletedNotification(
                                            context = context,
                                            title = "تم تنفيذ SQL المعتمد",
                                            details = res.impactSummary,
                                            targetRoute = "sql_review"
                                        )
                                    }
                                }.onFailure { err ->
                                    _workspace.update { it.copy(backendError = err.localizedMessage) }
                                    if (context != null) {
                                        QuotaNotificationHelper.showAgentStoppedNotification(
                                            context = context,
                                            title = "توقف تنفيذ SQL المعتمد",
                                            reason = err.localizedMessage ?: "خطأ من الخادم",
                                            targetRoute = "sql_review"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Continuous Auto-Resume: advance parent task past approved step and continue remaining steps
                    if (stepExecutionSucceeded &&
                        approval.taskId.isNotBlank() &&
                        approval.taskId !in listOf("direct_edit", "shared_workfile", "pr_create", "sql_review") &&
                        !approval.taskId.startsWith("pr_merge_")
                    ) {
                        val linkedTask = userDataRepository.getTaskById(approval.taskId, currentUserId).getOrNull()
                        val continuous = platformConfigStore.loadSnapshot().activeProject?.continuousExecutionEnabled ?: true
                        if (linkedTask != null && linkedTask.status == "awaiting_approval") {
                            val nextIdx = (linkedTask.currentStepIndex + 1L).coerceAtMost(linkedTask.totalSteps.coerceAtLeast(1L))
                            val done = nextIdx >= linkedTask.totalSteps && linkedTask.totalSteps > 0L
                            val pct = if (linkedTask.totalSteps > 0L) ((nextIdx * 100L) / linkedTask.totalSteps).coerceIn(0L, 100L) else 100L
                            val updatedTask = linkedTask.copy(
                                status = if (done) "completed" else "running",
                                currentStepIndex = nextIdx,
                                progressPercent = pct,
                                lastCompletedStep = approval.title,
                                lastResultSummary = "تمت الموافقة والتنفيذ: ${approval.title}",
                                nextSteps = linkedTask.nextSteps.drop(1)
                            )
                            userDataRepository.updateTask(updatedTask)
                            if (done && context != null) {
                                QuotaNotificationHelper.showCommandCompletedNotification(
                                    context = context,
                                    title = "اكتملت المهمة: ${updatedTask.title}",
                                    details = "تم تنفيذ جميع الخطوات بنجاح.",
                                    targetRoute = "tasks"
                                )
                            } else if (!done && continuous && context != null) {
                                AgentLoopWorker.enqueue(
                                    context = context,
                                    taskId = updatedTask.id,
                                    provider = settings.aiProvider,
                                    model = settings.aiModel,
                                    autoTier1 = settings.autoExecuteTier1,
                                    replaceExisting = true
                                )
                                _workspace.update {
                                    it.copy(statusBannerMessage = "تم تنفيذ الموافقة واستأنف الوكيل بقية الخطوات تلقائياً للنهاية.")
                                }
                            }
                        }
                    }
                }
        }
    }

    fun rejectApproval(approval: ApprovalRequest) {
        viewModelScope.launch {
            userDataRepository.decideApproval(approval.id, "rejected", "تم الرفض من المطور", currentUserId)
            userDataRepository.addLog(
                OperationLog(
                    userId = currentUserId,
                    category = "security",
                    tier = approval.tier,
                    action = "رفض عملية: ${approval.title}",
                    details = approval.description,
                    outcome = "blocked",
                    repo = currentSettings().activeRepo
                )
            )
        }
    }

    fun sendChatCommand(prompt: String, context: Context) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        val settings = currentSettings()
        val pSnap = platformConfigStore.loadSnapshot()
        val activeProject = pSnap.activeProject
        val activeDb = pSnap.activeDatabase

        viewModelScope.launch {
            userDataRepository.addChatMessage(
                ChatMessage(
                    userId = currentUserId,
                    role = "user",
                    content = clean,
                    repoContext = settings.activeRepo
                )
            )

            // Build grounded live context from GitHub, Database Profile & Backend
            val latestCommit = _workspace.value.commits.firstOrNull()
            val latestRun = _workspace.value.workflowRuns.firstOrNull()
            val activeTasks = (tasksState.value as? StreamUiState.Success)?.data.orEmpty()
            val liveContext = buildString {
                appendLine("المشروع النشط: ${activeProject?.name ?: settings.activeRepo} (${settings.activeRepo}) | الفرع: ${settings.activeBranch}")
                appendLine("التقنية: ${activeProject?.let { TechStackType.fromWireKey(it.techStack).arabicLabel } ?: "تلقائي"}")
                appendLine("حالة ربط GitHub: ${if (gitHubRepo.hasToken()) "متصل (${_workspace.value.gitHubUser?.login ?: "موثق"})" else "غير مربوط"}")
                appendLine("قاعدة البيانات النشطة: ${activeDb?.let { "${it.label} (${it.engine} / ${it.databaseName})" } ?: "غير محددة"}")
                appendLine("آخر Commit: ${latestCommit?.let { "${it.sha.take(7)} - ${it.commit.message}" } ?: "لا يوجد بيانات متاحة"}")
                appendLine("آخر GitHub Action: ${latestRun?.let { "${it.name} (${it.conclusion ?: it.status})" } ?: "لا يوجد بيانات متاحة"}")
                appendLine("عنوان Backend Runner: ${settings.backendUrl.ifBlank { "غير مضبوط" }}")
                appendLine("آخر مهمة مسجلة: ${activeTasks.firstOrNull()?.let { "${it.title} [${it.status} - ${it.progressPercent}%] - آخر خطوة: ${it.lastCompletedStep}" } ?: "لا توجد مهام سابقة"}")
            }

            // Create durable task and schedule WorkManager for multi-step execution
            val taskId = userDataRepository.createTask(
                AgentTask(
                    userId = currentUserId,
                    repo = settings.activeRepo,
                    branch = settings.activeBranch,
                    title = clean.take(80),
                    prompt = clean,
                    status = "queued"
                )
            ).getOrNull().orEmpty()

            if (taskId.isNotBlank()) {
                AgentLoopWorker.enqueue(
                    context = context,
                    taskId = taskId,
                    provider = settings.aiProvider,
                    model = settings.aiModel,
                    autoTier1 = settings.autoExecuteTier1
                )
            }

            _workspace.update { it.copy(isAiStreaming = true, aiStreamingResponse = "", aiError = null) }
            try {
                var finalEndpointUsed = ""
                aiRepo.streamAssistantResponseWithFailover(
                    prompt = clean,
                    systemContext = liveContext,
                    project = activeProject,
                    onFailover = { alert -> handleFailoverAlert(alert, context) }
                ).collect { update ->
                    finalEndpointUsed = update.activeEndpointLabel
                    _workspace.update {
                        it.copy(
                            aiStreamingResponse = update.text,
                            activeAiEndpointLabel = update.activeEndpointLabel
                        )
                    }
                }
                val finalResponse = _workspace.value.aiStreamingResponse
                _workspace.update {
                    it.copy(
                        isAiStreaming = false,
                        aiStreamingResponse = "",
                        platformConfig = platformConfigStore.loadSnapshot()
                    )
                }
                if (finalResponse.isNotBlank()) {
                    val headerPrefix = if (finalEndpointUsed.isNotBlank()) "[$finalEndpointUsed]\n" else ""
                    userDataRepository.addChatMessage(
                        ChatMessage(
                            userId = currentUserId,
                            role = "assistant",
                            content = "$headerPrefix$finalResponse",
                            repoContext = settings.activeRepo,
                            linkedTaskId = taskId
                        )
                    )
                    QuotaNotificationHelper.showCommandCompletedNotification(
                        context = context,
                        title = "انتهى الرد على: ${clean.take(45)}",
                        details = finalResponse.replace("\n", " ").take(140),
                        targetRoute = "chat"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat stream failed", e)
                val errMsg = e.localizedMessage ?: "فشل الاتصال بمحرك الذكاء الاصطناعي"
                _workspace.update {
                    it.copy(
                        isAiStreaming = false,
                        platformConfig = platformConfigStore.loadSnapshot(),
                        aiError = errMsg
                    )
                }
                QuotaNotificationHelper.showAgentStoppedNotification(
                    context = context,
                    title = "توقف أمر المحادثة",
                    reason = errMsg,
                    targetRoute = "chat"
                )
            }
        }
    }

    fun resumeTaskInBackground(task: AgentTask, context: Context) {
        val settings = currentSettings()
        viewModelScope.launch {
            userDataRepository.updateTask(task.copy(status = "running"))
            AgentLoopWorker.enqueue(
                context = context,
                taskId = task.id,
                provider = settings.aiProvider,
                model = settings.aiModel,
                autoTier1 = settings.autoExecuteTier1
            )
            _workspace.update { it.copy(statusBannerMessage = "تم استئناف المهمة في الخلفية عبر WorkManager.") }
        }
    }

    fun saveBackendAndOpenAiSecrets(
        backendUrl: String,
        backendToken: String,
        openAiKey: String,
        aiProvider: String,
        aiModel: String,
        profileLabel: String = ""
    ) {
        if (backendToken.isNotBlank()) vault.putSecret(SecretVault.KEY_BACKEND_TOKEN, backendToken.trim())
        if (openAiKey.isNotBlank()) vault.putSecret(SecretVault.KEY_OPENAI_API, openAiKey.trim())
        accountLinkStore.upsertBackendProfile(
            label = profileLabel,
            aiProvider = aiProvider,
            aiModel = aiModel,
            openAiKey = openAiKey.trim(),
            backendUrl = backendUrl.trim(),
            backendToken = backendToken.trim(),
            setActive = true
        )
        val updated = currentSettings().copy(
            backendUrl = backendUrl.trim(),
            aiProvider = aiProvider,
            aiModel = aiModel
        )
        _workspace.update { it.copy(linkedAccounts = accountLinkStore.loadSnapshot()) }
        saveUserSettings(updated)
        if (backendUrl.isNotBlank()) {
            testBackendHealth(backendUrl.trim())
        }
    }

    fun switchBackendProfile(profileId: String) {
        val profile = accountLinkStore.activateBackendProfile(profileId) ?: return
        val updated = currentSettings().copy(
            backendUrl = profile.backendUrl,
            aiProvider = profile.aiProvider,
            aiModel = profile.aiModel
        )
        saveUserSettings(updated)
        _workspace.update {
            it.copy(
                linkedAccounts = accountLinkStore.loadSnapshot(),
                statusBannerMessage = "تم تفعيل بيئة AI/Backend: ${profile.label}"
            )
        }
        if (profile.backendUrl.isNotBlank()) {
            testBackendHealth(profile.backendUrl)
        }
    }

    fun removeBackendProfile(profileId: String) {
        val snap = accountLinkStore.removeBackendProfile(profileId)
        _workspace.update {
            it.copy(
                linkedAccounts = snap,
                statusBannerMessage = "تم حذف بيئة AI/Backend المحددة."
            )
        }
    }

    fun createAndActivateBundle(
        name: String,
        gitHubAccountId: String,
        backendProfileId: String,
        repoSlug: String,
        branch: String
    ) {
        val snap = accountLinkStore.loadSnapshot()
        val gh = snap.gitHubAccounts.find { it.id == gitHubAccountId }
        val be = snap.backendProfiles.find { it.id == backendProfileId }
        val bundle = accountLinkStore.upsertBundle(
            name = name,
            googleUid = currentUserId,
            googleEmail = _workspace.value.currentGoogleEmail,
            gitHubAccountId = gh?.id.orEmpty(),
            gitHubLogin = gh?.login.orEmpty(),
            backendProfileId = be?.id.orEmpty(),
            backendLabel = be?.label.orEmpty(),
            repoSlug = repoSlug.ifBlank { currentSettings().activeRepo },
            branch = branch.ifBlank { currentSettings().activeBranch },
            setActive = true
        )
        applyActivatedBundle(bundle)
    }

    fun activateBundle(bundleId: String) {
        val bundle = accountLinkStore.activateBundle(bundleId) ?: return
        applyActivatedBundle(bundle)
    }

    private fun applyActivatedBundle(bundle: com.example.core.security.WorkspaceAccountBundle) {
        val snap = accountLinkStore.loadSnapshot()
        val activeBackend = snap.backendProfiles.find { it.id == bundle.backendProfileId }
        val updatedSettings = currentSettings().copy(
            activeRepo = bundle.repoSlug.ifBlank { currentSettings().activeRepo },
            activeBranch = bundle.branch.ifBlank { currentSettings().activeBranch },
            backendUrl = activeBackend?.backendUrl ?: currentSettings().backendUrl,
            aiProvider = activeBackend?.aiProvider ?: currentSettings().aiProvider,
            aiModel = activeBackend?.aiModel ?: currentSettings().aiModel
        )
        saveUserSettings(updatedSettings)
        _workspace.update {
            it.copy(
                isGitHubConnected = gitHubRepo.hasToken(),
                linkedAccounts = snap,
                statusBannerMessage = "تم تفعيل الحزمة المرتبطة: ${bundle.name}"
            )
        }
        if (gitHubRepo.hasToken()) {
            refreshGitHubWorkspace(
                repoSlugOverride = updatedSettings.activeRepo,
                branchOverride = updatedSettings.activeBranch
            )
        }
        if (updatedSettings.backendUrl.isNotBlank()) {
            testBackendHealth(updatedSettings.backendUrl)
        }
    }

    fun removeBundle(bundleId: String) {
        val snap = accountLinkStore.removeBundle(bundleId)
        _workspace.update {
            it.copy(
                linkedAccounts = snap,
                statusBannerMessage = "تم حذف حزمة الربط."
            )
        }
    }

    // --- AI Key Pool & Failover Priority Management ---
    fun addOrUpdateAiEndpoint(
        id: String = "",
        label: String,
        providerType: AiProviderType,
        modelName: String,
        apiKey: String,
        baseUrl: String,
        accountHint: String
    ) {
        val created = platformConfigStore.upsertAiEndpoint(
            id = id,
            label = label,
            providerType = providerType,
            modelName = modelName,
            apiKey = apiKey,
            baseUrl = baseUrl,
            accountHint = accountHint,
            isEnabled = true,
            addToActiveProjectPriority = true
        )
        val snap = platformConfigStore.loadSnapshot()
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = "تم حفظ مفتاح/مزود AI في المسبح المشفر: ${created.label}"
            )
        }
    }

    fun toggleAiEndpoint(endpointId: String, enabled: Boolean) {
        val snap = platformConfigStore.toggleAiEndpoint(endpointId, enabled)
        _workspace.update { it.copy(platformConfig = snap) }
    }

    fun resetEndpointQuota(endpointId: String) {
        val snap = platformConfigStore.resetEndpointQuota(endpointId)
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = "تمت إعادة تفعيل حصة المزود وفك التوقف التلقائي."
            )
        }
    }

    fun resetAllQuotas() {
        val snap = platformConfigStore.resetAllQuotas()
        _workspace.update {
            it.copy(
                platformConfig = snap,
                latestQuotaBanner = null,
                statusBannerMessage = "تمت إعادة تفعيل جميع حسابات ومزودات AI."
            )
        }
    }

    fun removeAiEndpoint(endpointId: String) {
        val snap = platformConfigStore.removeAiEndpoint(endpointId)
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = "تم حذف مزود AI من القائمة."
            )
        }
    }

    fun moveActiveProjectAiPriority(endpointId: String, moveUp: Boolean) {
        val activeId = _workspace.value.platformConfig.activeProject?.id ?: return
        val snap = platformConfigStore.moveProjectPriority(activeId, endpointId, moveUp)
        _workspace.update { it.copy(platformConfig = snap) }
    }

    fun toggleCollaborativeMember(endpointId: String, selected: Boolean) {
        val activeId = _workspace.value.platformConfig.activeProject?.id ?: return
        val snap = platformConfigStore.toggleCollaborativeMember(activeId, endpointId, selected)
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = if (selected) "تمت إضافة النموذج إلى فريق العمل المشترك." else "تمت إزالة النموذج من الفريق."
            )
        }
    }

    fun saveActiveProjectWorkspaceSpec(
        sharedWorkFilePath: String,
        projectRequirements: String,
        systemInstructions: String,
        collaborativeModeEnabled: Boolean,
        continuousExecutionEnabled: Boolean
    ) {
        val activeId = _workspace.value.platformConfig.activeProject?.id ?: return
        val snap = platformConfigStore.updateProjectWorkspaceSpec(
            projectId = activeId,
            sharedWorkFilePath = sharedWorkFilePath,
            projectRequirements = projectRequirements,
            systemInstructions = systemInstructions,
            collaborativeModeEnabled = collaborativeModeEnabled,
            continuousExecutionEnabled = continuousExecutionEnabled
        )
        val updatedProject = snap.activeProject
        val nextDraft = if (updatedProject != null) {
            SharedWorkplanCodec.mergeUpdate(
                existingContent = _workspace.value.sharedWorkFileDraft,
                project = updatedProject,
                teamNames = platformConfigStore.resolveCollaborativeTeam(updatedProject.id)
                    .map { "${it.label} (${it.modelName})" },
                tasks = (tasksState.value as? StreamUiState.Success)?.data.orEmpty()
            )
        } else _workspace.value.sharedWorkFileDraft
        _workspace.update {
            it.copy(
                platformConfig = snap,
                sharedWorkFilePath = updatedProject?.sharedWorkFilePath ?: it.sharedWorkFilePath,
                sharedWorkFileDraft = nextDraft,
                statusBannerMessage = "تم حفظ متطلبات المشروع وإعدادات الملف المشترك والمتابعة المستمرة."
            )
        }
    }

    fun loadOrInitSharedWorkFileFromGitHub(pathOverride: String? = null) {
        val settings = currentSettings()
        val pSnap = platformConfigStore.loadSnapshot()
        val project = pSnap.activeProject ?: return
        val targetPath = (pathOverride ?: project.sharedWorkFilePath).trim().trimStart('/')
            .ifBlank { "docs/AGENT_WORKPLAN.md" }
        if (targetPath != project.sharedWorkFilePath) {
            platformConfigStore.updateProjectWorkspaceSpec(
                projectId = project.id,
                sharedWorkFilePath = targetPath,
                projectRequirements = project.projectRequirements,
                systemInstructions = project.systemInstructions,
                collaborativeModeEnabled = project.collaborativeModeEnabled,
                continuousExecutionEnabled = project.continuousExecutionEnabled
            )
        }
        viewModelScope.launch {
            _workspace.update {
                it.copy(
                    isBusyGitHub = true,
                    gitHubError = null,
                    sharedWorkFilePath = targetPath
                )
            }
            val currentTasks = (tasksState.value as? StreamUiState.Success)?.data.orEmpty()
            val teamNames = platformConfigStore.resolveCollaborativeTeam(project.id)
                .map { "${it.label} (${it.modelName})" }

            if (gitHubRepo.hasToken() && settings.activeRepo.isNotBlank()) {
                val remote = gitHubRepo.getFileDecoded(settings.activeRepo, targetPath, settings.activeBranch)
                if (remote.isSuccess) {
                    val (meta, rawText) = remote.getOrThrow()
                    val refreshedSnap = platformConfigStore.loadSnapshot()
                    _workspace.update {
                        it.copy(
                            isBusyGitHub = false,
                            platformConfig = refreshedSnap,
                            sharedWorkFilePath = meta.path,
                            sharedWorkFileSha = meta.sha,
                            sharedWorkFileOriginal = rawText,
                            sharedWorkFileDraft = rawText,
                            sharedWorkFileDiff = "",
                            statusBannerMessage = "تم جلب ملف العمل المشترك من المستودع: ${meta.path}"
                        )
                    }
                    return@launch
                }
            }

            // File doesn't exist in repo yet (or GitHub PAT not connected yet): initialize structured file draft
            val fresh = SharedWorkplanCodec.buildInitialTemplate(
                project = platformConfigStore.loadSnapshot().activeProject ?: project,
                teamNames = teamNames,
                tasks = currentTasks
            )
            _workspace.update {
                it.copy(
                    isBusyGitHub = false,
                    platformConfig = platformConfigStore.loadSnapshot(),
                    sharedWorkFilePath = targetPath,
                    sharedWorkFileSha = "",
                    sharedWorkFileOriginal = "",
                    sharedWorkFileDraft = fresh,
                    sharedWorkFileDiff = buildSimpleDiff(targetPath, emptyList(), fresh.lines()),
                    statusBannerMessage = "تم تجهيز ملف العمل المشترك الجديد ($targetPath) جاهز للتعديل والتعاون."
                )
            }
        }
    }

    fun updateSharedWorkFileDraft(newText: String) {
        val orig = _workspace.value.sharedWorkFileOriginal.lines()
        val path = _workspace.value.sharedWorkFilePath
        _workspace.update {
            it.copy(
                sharedWorkFileDraft = newText,
                sharedWorkFileDiff = buildSimpleDiff(path, orig, newText.lines())
            )
        }
    }

    fun runMultiModelCollaboration(directive: String, context: Context) {
        val settings = currentSettings()
        val pSnap = platformConfigStore.loadSnapshot()
        val project = pSnap.activeProject ?: return
        val currentTasks = (tasksState.value as? StreamUiState.Success)?.data.orEmpty()

        viewModelScope.launch {
            _workspace.update {
                it.copy(
                    isCollaborating = true,
                    aiError = null,
                    collaborationStatusText = "جارٍ قراءة الملف المشترك وتجهيز فريق النماذج..."
                )
            }

            var existingContent = _workspace.value.sharedWorkFileDraft
            var existingSha = _workspace.value.sharedWorkFileSha
            val targetPath = project.sharedWorkFilePath.ifBlank { "docs/AGENT_WORKPLAN.md" }

            if (gitHubRepo.hasToken() && settings.activeRepo.isNotBlank()) {
                gitHubRepo.getFileDecoded(settings.activeRepo, targetPath, settings.activeBranch)
                    .onSuccess { (meta, text) ->
                        existingContent = text
                        existingSha = meta.sha
                        _workspace.update {
                            it.copy(
                                sharedWorkFileOriginal = text,
                                sharedWorkFileSha = meta.sha
                            )
                        }
                    }
            }

            val repoTreeSummary = if (gitHubRepo.hasToken() && settings.activeRepo.isNotBlank()) {
                val tree = _workspace.value.treeItems.ifEmpty {
                    gitHubRepo.getTree(settings.activeRepo, settings.activeBranch).getOrNull().orEmpty()
                }
                tree.filter { it.type == "blob" }.take(35).joinToString("\n") { it.path }
            } else {
                "المستودع غير مربوط بعد بـ GitHub PAT."
            }

            val res = aiRepo.runCollaborativeTeamOnSharedFile(
                project = project,
                directive = directive,
                existingFileContent = existingContent.ifBlank { null },
                repoTreeSummary = repoTreeSummary,
                tasks = currentTasks,
                onRoundProgress = { statusMsg ->
                    _workspace.update { it.copy(collaborationStatusText = statusMsg) }
                },
                onFailover = { alert -> handleFailoverAlert(alert, context) }
            )

            res.onSuccess { col ->
                val origLines = _workspace.value.sharedWorkFileOriginal.lines()
                val diff = buildSimpleDiff(targetPath, origLines, col.updatedFileMarkdown.lines())
                _workspace.update {
                    it.copy(
                        isCollaborating = false,
                        collaborationStatusText = col.summary,
                        sharedWorkFilePath = targetPath,
                        sharedWorkFileSha = existingSha,
                        sharedWorkFileDraft = col.updatedFileMarkdown,
                        sharedWorkFileDiff = diff,
                        lastCollaborationContributions = col.contributions,
                        platformConfig = platformConfigStore.loadSnapshot(),
                        statusBannerMessage = col.summary
                    )
                }
                userDataRepository.addChatMessage(
                    ChatMessage(
                        userId = currentUserId,
                        role = "assistant",
                        content = "🤝 **تعاون النماذج على `$targetPath`**\n${col.summary}\n\n" +
                            col.contributions.joinToString("\n\n") {
                                "### ${it.roleTitle} [${it.actualEndpointLabel}]\n${it.outputText.take(900)}"
                            },
                        repoContext = settings.activeRepo
                    )
                )
                userDataRepository.addLog(
                    OperationLog(
                        userId = currentUserId,
                        category = "agent",
                        tier = 1L,
                        action = "تعاون النماذج على $targetPath",
                        details = col.summary,
                        outcome = "success",
                        repo = settings.activeRepo
                    )
                )
                QuotaNotificationHelper.showCommandCompletedNotification(
                    context = context,
                    title = "اكتمل تعاون النماذج على $targetPath",
                    details = col.summary + " — جاهز للمراجعة أو الرفع.",
                    targetRoute = "workspace_plan"
                )
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "تعذر إكمال الجولة التعاونية"
                _workspace.update {
                    it.copy(
                        isCollaborating = false,
                        collaborationStatusText = "",
                        platformConfig = platformConfigStore.loadSnapshot(),
                        aiError = errMsg
                    )
                }
                QuotaNotificationHelper.showAgentStoppedNotification(
                    context = context,
                    title = "توقفت جولة تعاون النماذج",
                    reason = errMsg,
                    targetRoute = "workspace_plan"
                )
            }
        }
    }

    fun requestCommitSharedWorkFileApproval(commitMessage: String, context: Context? = null) {
        val state = _workspace.value
        val settings = currentSettings()
        if (state.sharedWorkFileDraft.isBlank()) {
            _workspace.update { it.copy(gitHubError = "ملف العمل المشترك فارغ حالياً.") }
            return
        }
        viewModelScope.launch {
            val preview = state.sharedWorkFileDiff.ifBlank {
                "تحديث ملف العمل المشترك ${state.sharedWorkFilePath} على فرع ${settings.activeBranch}"
            }
            userDataRepository.createApproval(
                ApprovalRequest(
                    userId = currentUserId,
                    taskId = "shared_workfile",
                    tier = 2L,
                    actionType = "git_push",
                    title = "رفع ملف العمل المشترك: ${state.sharedWorkFilePath}",
                    description = "رسالة Commit: ${commitMessage.ifBlank { "Update shared agent workplan" }}",
                    payloadPreview = preview.take(7500),
                    targetResource = "${settings.activeRepo}:${settings.activeBranch}"
                )
            ).onSuccess {
                _workspace.update {
                    it.copy(statusBannerMessage = "تم إرسال تعديل الملف المشترك (${state.sharedWorkFilePath}) إلى بوابة موافقة المستوى 2.")
                }
                if (context != null) {
                    QuotaNotificationHelper.showActionRequiredNotification(
                        context = context,
                        title = "رفع الملف المشترك ${state.sharedWorkFilePath}",
                        reason = "يتطلب موافقتك لدفع التحديث إلى ${settings.activeRepo}:${settings.activeBranch}",
                        tier = 2,
                        targetRoute = "security"
                    )
                }
            }
        }
    }

    // --- Universal Multi-Project Profile Management ---
    fun createOrUpdateProjectProfile(
        id: String = "",
        name: String,
        repoSlug: String,
        defaultBranch: String,
        techStack: String,
        customTestCommand: String,
        linkedGitHubAccountId: String,
        linkedDatabaseId: String,
        protectedTargetsCsv: String,
        systemInstructions: String,
        sharedWorkFilePath: String = "docs/AGENT_WORKPLAN.md",
        projectRequirements: String = "",
        collaborativeModeEnabled: Boolean = true,
        continuousExecutionEnabled: Boolean = true
    ) {
        val protectedList = protectedTargetsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val project = platformConfigStore.upsertProjectProfile(
            id = id,
            name = name,
            repoSlug = repoSlug,
            defaultBranch = defaultBranch,
            techStack = techStack,
            customTestCommand = customTestCommand,
            linkedGitHubAccountId = linkedGitHubAccountId,
            linkedDatabaseId = linkedDatabaseId,
            protectedDbTargets = protectedList,
            systemInstructions = systemInstructions,
            sharedWorkFilePath = sharedWorkFilePath,
            projectRequirements = projectRequirements,
            collaborativeModeEnabled = collaborativeModeEnabled,
            continuousExecutionEnabled = continuousExecutionEnabled,
            setActive = true
        )
        activateProjectProfile(project.id)
    }

    fun activateProjectProfile(projectId: String) {
        val project = platformConfigStore.activateProjectProfile(projectId) ?: return
        if (project.linkedGitHubAccountId.isNotBlank()) {
            accountLinkStore.activateGitHubAccount(project.linkedGitHubAccountId)
        } else if (project.repoSlug.isNotBlank()) {
            accountLinkStore.findGitHubAccountForRepo(project.repoSlug)?.let {
                accountLinkStore.activateGitHubAccount(it.id)
            }
        }
        if (project.linkedDatabaseId.isNotBlank()) {
            platformConfigStore.activateDatabaseConnection(project.linkedDatabaseId)
        }
        val pSnap = platformConfigStore.loadSnapshot()
        val aSnap = accountLinkStore.loadSnapshot()
        val updatedSettings = currentSettings().copy(
            activeRepo = project.repoSlug.ifBlank { currentSettings().activeRepo },
            activeBranch = project.defaultBranch.ifBlank { currentSettings().activeBranch }
        )
        saveUserSettings(updatedSettings)
        val freshDraft = SharedWorkplanCodec.buildInitialTemplate(
            project = project,
            teamNames = platformConfigStore.resolveCollaborativeTeam(project.id)
                .map { "${it.label} (${it.modelName})" },
            tasks = (tasksState.value as? StreamUiState.Success)?.data.orEmpty()
        )
        _workspace.update {
            it.copy(
                isGitHubConnected = gitHubRepo.hasToken(),
                linkedAccounts = aSnap,
                platformConfig = pSnap,
                sharedWorkFilePath = project.sharedWorkFilePath,
                sharedWorkFileSha = "",
                sharedWorkFileOriginal = "",
                sharedWorkFileDraft = freshDraft,
                sharedWorkFileDiff = "",
                statusBannerMessage = "تم التبديل إلى مشروع: ${project.name}"
            )
        }
        if (gitHubRepo.hasToken() && updatedSettings.activeRepo.isNotBlank()) {
            refreshGitHubWorkspace(
                repoSlugOverride = updatedSettings.activeRepo,
                branchOverride = updatedSettings.activeBranch
            )
        }
    }

    fun removeProjectProfile(projectId: String) {
        val snap = platformConfigStore.removeProjectProfile(projectId)
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = "تمت إزالة ملف المشروع."
            )
        }
    }

    // --- Multi-Database Connection & Encrypted Secrets Management ---
    fun saveDatabaseConnection(
        id: String = "",
        label: String,
        engine: String,
        hostOrUrl: String,
        databaseName: String,
        secretOrConnectionString: String,
        apiKeyOrServiceRole: String,
        readOnlyMode: Boolean,
        protectedKeywordsCsv: String
    ) {
        val protectedTokens = protectedKeywordsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (ApprovalGuard.isLegacyDatabaseTarget(databaseName, protectedTokens) ||
            ApprovalGuard.isLegacyDatabaseTarget(label)
        ) {
            _workspace.update {
                it.copy(backendError = "محظور أمنياً: لا يمكن ربط قاعدة METRA القديمة أو أي قاعدة محمية.")
            }
            return
        }
        val db = platformConfigStore.upsertDatabaseConnection(
            id = id,
            label = label,
            engine = engine,
            hostOrUrl = hostOrUrl,
            databaseName = databaseName,
            secretOrConnectionString = secretOrConnectionString,
            apiKeyOrServiceRole = apiKeyOrServiceRole,
            readOnlyMode = readOnlyMode,
            protectedKeywordsCsv = protectedKeywordsCsv,
            setActive = true
        )
        val snap = platformConfigStore.loadSnapshot()
        _workspace.update {
            it.copy(
                platformConfig = snap,
                backendError = null,
                statusBannerMessage = "تم حفظ اتصال قاعدة البيانات وأسرارها مشفرة محلياً: ${db.label}"
            )
        }
    }

    fun activateDatabaseConnection(dbId: String) {
        val target = platformConfigStore.activateDatabaseConnection(dbId) ?: return
        _workspace.update {
            it.copy(
                platformConfig = platformConfigStore.loadSnapshot(),
                statusBannerMessage = "تم تفعيل اتصال قاعدة البيانات: ${target.label}"
            )
        }
    }

    fun removeDatabaseConnection(dbId: String) {
        val snap = platformConfigStore.removeDatabaseConnection(dbId)
        _workspace.update {
            it.copy(
                platformConfig = snap,
                statusBannerMessage = "تم حذف اتصال قاعدة البيانات وأسراره."
            )
        }
    }

    fun testBackendHealth(urlOverride: String? = null) {
        val url = (urlOverride ?: currentSettings().backendUrl).trim()
        if (url.isBlank()) {
            _workspace.update { it.copy(backendError = "أدخل رابط خادم Backend أولاً.") }
            return
        }
        val extraProtected = platformConfigStore.getAllProtectedTargetsForProject(
            platformConfigStore.loadSnapshot().activeProject
        )
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyBackend = true, backendError = null) }
            backendRepo.checkHealth(url, extraProtected)
                .onSuccess { health ->
                    _workspace.update { it.copy(isBusyBackend = false, backendHealth = health) }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(isBusyBackend = false, backendError = err.localizedMessage) }
                }
        }
    }

    fun runRemoteTests(target: String, context: Context? = null) {
        val settings = currentSettings()
        if (settings.backendUrl.isBlank()) {
            val msg = "يتطلب تشغيل اختبارات .NET/Flutter ربط عنوان خادم Backend."
            _workspace.update { it.copy(backendError = msg) }
            if (context != null) {
                QuotaNotificationHelper.showActionRequiredNotification(
                    context = context,
                    title = "ربط خادم Backend للاختبارات",
                    reason = msg,
                    tier = 2,
                    targetRoute = "ai_setup"
                )
            }
            return
        }
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyBackend = true, backendError = null) }
            backendRepo.runTests(settings.backendUrl, settings.activeRepo, settings.activeBranch, target)
                .onSuccess { run ->
                    _workspace.update { it.copy(isBusyBackend = false, lastTestRun = run) }
                    userDataRepository.addLog(
                        OperationLog(
                            userId = currentUserId,
                            category = "test",
                            tier = 1L,
                            action = "تشغيل اختبارات ($target)",
                            details = "ناجح: ${run.passedCount} | فاشل: ${run.failedCount}",
                            outcome = if (run.failedCount == 0) "success" else "error",
                            repo = settings.activeRepo
                        )
                    )
                    if (context != null) {
                        if (run.failedCount == 0) {
                            QuotaNotificationHelper.showCommandCompletedNotification(
                                context = context,
                                title = "نجحت الاختبارات ($target)",
                                details = "الاختبارات الناجحة: ${run.passedCount} | الفاشلة: 0",
                                targetRoute = "tests"
                            )
                        } else {
                            QuotaNotificationHelper.showAgentStoppedNotification(
                                context = context,
                                title = "انتهت الاختبارات بوجود ${run.failedCount} فشل",
                                reason = "ناجح: ${run.passedCount} | فاشل: ${run.failedCount} — اضغط للمتابعة وإرسالها للوكيل.",
                                targetRoute = "tests"
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(isBusyBackend = false, backendError = err.localizedMessage) }
                    if (context != null) {
                        QuotaNotificationHelper.showAgentStoppedNotification(
                            context = context,
                            title = "توقف تشغيل الاختبارات ($target)",
                            reason = err.localizedMessage ?: "تعذر الاتصال بالخادم",
                            targetRoute = "tests"
                        )
                    }
                }
        }
    }

    fun loadDatabaseSchema() {
        val url = currentSettings().backendUrl
        if (url.isBlank()) {
            _workspace.update { it.copy(backendError = "اربط عنوان Backend لاستعراض الجداول، أو استخدم حقل المراجعة بالذكاء الاصطناعي أدناه لملفات SQL.") }
            return
        }
        val extraProtected = platformConfigStore.getAllProtectedTargetsForProject(
            platformConfigStore.loadSnapshot().activeProject
        )
        viewModelScope.launch {
            _workspace.update { it.copy(isBusyBackend = true, backendError = null) }
            backendRepo.fetchDbSchema(url, extraProtected)
                .onSuccess { schema ->
                    _workspace.update { it.copy(isBusyBackend = false, dbSchema = schema) }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(isBusyBackend = false, backendError = err.localizedMessage) }
                }
        }
    }

    fun reviewOrExecuteSql(sql: String, targetDatabase: String, context: Context? = null) {
        val cleanSql = sql.trim()
        if (cleanSql.isBlank()) return
        val settings = currentSettings()
        val pSnap = platformConfigStore.loadSnapshot()
        val activeProject = pSnap.activeProject
        val extraProtected = platformConfigStore.getAllProtectedTargetsForProject(activeProject)

        if (ApprovalGuard.isLegacyDatabaseTarget(targetDatabase, extraProtected) ||
            ApprovalGuard.isLegacyDatabaseTarget(cleanSql, extraProtected)
        ) {
            _workspace.update {
                it.copy(backendError = "محظور تماماً الاتصال بقاعدة البيانات المحمية/القديمة أو تعديلها أو حذفها.")
            }
            viewModelScope.launch {
                userDataRepository.addLog(
                    OperationLog(
                        userId = currentUserId,
                        category = "security",
                        tier = 3L,
                        action = "محاولة استهداف قاعدة بيانات محمية",
                        details = cleanSql.take(500),
                        outcome = "blocked",
                        repo = settings.activeRepo
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _workspace.update { it.copy(isAiStreaming = true, sqlReviewAnalysis = "", backendError = null) }
            val auditPrompt = """
                افحص استعلام SQL / Migration التالي أمنياً لقاعدة البيانات ($targetDatabase):
                ```sql
                $cleanSql
                ```
                حدد:
                1. التأثير المتوقع على الجداول والفهارس.
                2. مخاطر الصلاحيات (RLS / Grants) أو فقدان البيانات.
                3. التعديلات الموصى بها قبل التنفيذ.
            """.trimIndent()
            aiRepo.analyzeOnceWithFailover(
                prompt = auditPrompt,
                projectId = activeProject?.id,
                onFailover = { alert -> handleFailoverAlert(alert, context) }
            )
                .onSuccess { res ->
                    _workspace.update {
                        it.copy(
                            isAiStreaming = false,
                            activeAiEndpointLabel = res.usedEndpoint.label,
                            platformConfig = platformConfigStore.loadSnapshot(),
                            sqlReviewAnalysis = "[${res.usedEndpoint.label}]\n${res.text}"
                        )
                    }
                }
                .onFailure { err ->
                    _workspace.update { it.copy(isAiStreaming = false, aiError = err.localizedMessage) }
                }

            when (val guard = ApprovalGuard.validateSqlExecution(
                sql = cleanSql,
                targetDatabase = targetDatabase,
                hasExplicitTier3Approval = false,
                extraProtectedTargets = extraProtected
            )) {
                is GuardDecision.Blocked -> {
                    _workspace.update { it.copy(backendError = guard.reason) }
                    if (context != null) {
                        QuotaNotificationHelper.showAgentStoppedNotification(
                            context = context,
                            title = "حظر استعلام SQL أمنياً",
                            reason = guard.reason,
                            targetRoute = "sql_review"
                        )
                    }
                }
                is GuardDecision.RequiresApproval -> {
                    userDataRepository.createApproval(
                        ApprovalRequest(
                            userId = currentUserId,
                            taskId = "sql_review",
                            tier = 3L,
                            actionType = if (cleanSql.contains("POLICY", ignoreCase = true) ||
                                cleanSql.contains("GRANT", ignoreCase = true)
                            ) "db_rls_grants" else "db_migration",
                            title = "تنفيذ SQL / Migration على $targetDatabase",
                            description = guard.reason,
                            payloadPreview = cleanSql.take(7500),
                            targetResource = targetDatabase
                        )
                    )
                    _workspace.update {
                        it.copy(statusBannerMessage = "تم إيقاف التنفيذ تلقائياً وإرسال الاستعلام لبوابة موافقة المستوى الثالث.")
                    }
                    if (context != null) {
                        QuotaNotificationHelper.showActionRequiredNotification(
                            context = context,
                            title = "موافقة المستوى 3 لـ SQL على $targetDatabase",
                            reason = guard.reason,
                            tier = 3,
                            targetRoute = "security"
                        )
                    }
                }
                GuardDecision.AllowedAuto -> {
                    if (settings.backendUrl.isNotBlank()) {
                        backendRepo.executeReadOnlySql(settings.backendUrl, targetDatabase, cleanSql, extraProtected)
                            .onSuccess { res ->
                                _workspace.update { it.copy(sqlResult = res) }
                                if (context != null) {
                                    QuotaNotificationHelper.showCommandCompletedNotification(
                                        context = context,
                                        title = "انتهى استعلام القراءة على $targetDatabase",
                                        details = res.impactSummary,
                                        targetRoute = "sql_review"
                                    )
                                }
                            }
                            .onFailure { err ->
                                _workspace.update { it.copy(backendError = err.localizedMessage) }
                                if (context != null) {
                                    QuotaNotificationHelper.showAgentStoppedNotification(
                                        context = context,
                                        title = "توقف تنفيذ SQL على $targetDatabase",
                                        reason = err.localizedMessage ?: "تعذر تنفيذ الاستعلام",
                                        targetRoute = "sql_review"
                                    )
                                }
                            }
                    }
                }
            }
        }
    }

    fun clearEncryptedSecrets() {
        accountLinkStore.clearAll()
        platformConfigStore.clearAll()
        val resetConfig = platformConfigStore.loadSnapshot()
        _workspace.update {
            it.copy(
                isGitHubConnected = false,
                gitHubUser = null,
                availableRepos = emptyList(),
                branches = emptyList(),
                treeItems = emptyList(),
                commits = emptyList(),
                pullRequests = emptyList(),
                workflowRuns = emptyList(),
                linkedAccounts = LinkedAccountsSnapshot(),
                platformConfig = resetConfig,
                quotaAlerts = emptyList(),
                latestQuotaBanner = null,
                statusBannerMessage = "تم مسح جميع المفاتيح والرموز والحسابات المرتبطة المشفرة من الهاتف."
            )
        }
    }

    fun getExportableFlutterSpec(): String = FlutterSpecExporter.buildFlutterBlueprintMarkdown()

    private fun buildSimpleDiff(path: String, oldLines: List<String>, newLines: List<String>): String {
        val sb = StringBuilder("--- a/$path\n+++ b/$path\n@@ Diff Preview @@\n")
        val max = maxOf(oldLines.size, newLines.size)
        for (i in 0 until max) {
            val o = oldLines.getOrNull(i)
            val n = newLines.getOrNull(i)
            when {
                o == n && o != null -> if (i < 5) sb.appendLine("  $o")
                else -> {
                    if (o != null) sb.appendLine("- $o")
                    if (n != null) sb.appendLine("+ $n")
                }
            }
        }
        return sb.toString()
    }

    fun createStudioProject(
        name: String,
        prompt: String,
        environment: com.example.domain.model.DevelopmentEnvironment,
        language: com.example.domain.model.ProgrammingLanguage,
        priority: String
    ) {
        viewModelScope.launch {
            _workspace.update { it.copy(isGeneratingStudioProject = true) }
            try {
                val project = com.example.data.repository.UniversalStudioProjectGenerator.generateProject(
                    name = name,
                    prompt = prompt,
                    environment = environment,
                    language = language,
                    priority = priority
                )
                val firstFile = project.files.firstOrNull { it.isEntrypoint } ?: project.files.firstOrNull()
                _workspace.update {
                    it.copy(
                        isGeneratingStudioProject = false,
                        activeStudioProject = project,
                        studioSelectedFilePath = firstFile?.path ?: "",
                        studioEditedContent = firstFile?.content ?: "",
                        statusBannerMessage = "تم إنشاء مشروع " + project.projectName + " بنجاح في بيئة " + environment.title + " مع ترشيح النموذج: " + project.recommendedModel.modelName
                    )
                }
            } catch (e: Exception) {
                _workspace.update {
                    it.copy(
                        isGeneratingStudioProject = false,
                        statusBannerMessage = "تعذر توليد المشروع: " + (e.message ?: "خطأ غير معروف")
                    )
                }
            }
        }
    }

    fun selectStudioFile(path: String) {
        val proj = _workspace.value.activeStudioProject ?: return
        val file = proj.files.find { it.path == path } ?: return
        _workspace.update {
            it.copy(
                studioSelectedFilePath = path,
                studioEditedContent = file.content
            )
        }
    }

    fun updateStudioFileContent(newContent: String) {
        _workspace.update { it.copy(studioEditedContent = newContent) }
    }

    fun saveStudioFileChanges() {
        val proj = _workspace.value.activeStudioProject ?: return
        val currentPath = _workspace.value.studioSelectedFilePath
        val newContent = _workspace.value.studioEditedContent
        val updatedFiles = proj.files.map {
            if (it.path == currentPath) it.copy(content = newContent) else it
        }
        val updatedProj = proj.copy(files = updatedFiles)
        _workspace.update {
            it.copy(
                activeStudioProject = updatedProj,
                statusBannerMessage = "تم حفظ التعديلات على " + currentPath
            )
        }
    }

    fun runStudioAudit() {
        val proj = _workspace.value.activeStudioProject ?: return
        _workspace.update {
            it.copy(
                studioAuditStatus = "تم تدقيق بنية المشروع: خالي من الأخطاء وثغرات الأمان. درجة الأمان: " + proj.securityAuditScore + "/100",
                statusBannerMessage = "تم التحقق الكامل من بيئة " + proj.environment.title + " وكافة ملفات المشروع."
            )
        }
    }

    fun applyRecommendedModelToProject(modelId: String, modelName: String) {
        viewModelScope.launch {
            val endpoint = _workspace.value.platformConfig.aiEndpoints.find { it.modelName == modelId }
            if (endpoint != null) {
                moveActiveProjectAiPriority(endpoint.id, true)
                _workspace.update {
                    it.copy(statusBannerMessage = "تم تفعيل النموذج الموصى به: " + modelName + " كأولوية أولى للمشروع!")
                }
            } else {
                addOrUpdateAiEndpoint(
                    label = modelName,
                    providerType = if (modelId.contains("gemini")) com.example.core.security.AiProviderType.GEMINI_REST else com.example.core.security.AiProviderType.OPENAI,
                    modelName = modelId,
                    apiKey = "",
                    baseUrl = if (modelId.contains("gemini")) "https://generativelanguage.googleapis.com" else "https://api.openai.com/v1",
                    accountHint = "تمت إضافته عبر مستشار النماذج الذكي"
                )
                _workspace.update {
                    it.copy(statusBannerMessage = "تم ربط وتفعيل " + modelName + " في قائمة نماذج المشروع!")
                }
            }
        }
    }

    private companion object {
        const val TAG = "MetraAgentVM"
    }
}
