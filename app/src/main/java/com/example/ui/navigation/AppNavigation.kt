package com.example.ui.navigation

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.notifications.QuotaNotificationHelper
import com.example.core.security.SecretVault
import com.example.data.local.MetraLocalDatabase
import com.example.data.repository.AiOrchestratorRepository
import com.example.data.repository.AuthSessionRepository
import com.example.data.repository.BackendRepository
import com.example.data.repository.FirestoreAgentRepository
import com.example.data.repository.GitHubRepository
import com.example.domain.model.UserSettings
import com.example.ui.screens.ActionsScreen
import com.example.ui.screens.AgentChatScreen
import com.example.ui.screens.AiAccountConversationsScreen
import com.example.ui.screens.UniversalStudioScreen
import androidx.compose.material.icons.rounded.Code
import com.example.ui.screens.AiProviderScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BranchesCommitsScreen
import com.example.ui.screens.CodeEditorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.FileExplorerScreen
import com.example.ui.screens.GitHubSetupScreen
import com.example.ui.screens.LinkedAccountsScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.PullRequestsScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.SharedWorkspaceScreen
import com.example.ui.screens.SqlReviewScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TestsScreen
import com.example.ui.components.QuotaFailoverBanner
import com.example.ui.theme.MetraAgentTheme
import com.example.ui.viewmodel.MetraAgentViewModel
import com.example.ui.viewmodel.StreamUiState
import com.google.firebase.firestore.FirebaseFirestore

private data class TopNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MetraRootApp(
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val vault = remember(context) { SecretVault(context) }
    val authScope = androidx.compose.runtime.rememberCoroutineScope()
    var currentUserId by remember(vault) {
        mutableStateOf(vault.getSecret(SecretVault.KEY_AUTH_USER_ID).ifBlank { null })
    }

    val userId = currentUserId
    if (userId == null) {
        MetraAgentTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = { authenticatedUserId -> currentUserId = authenticatedUserId })
        }
    } else {
        key(userId) {
            AuthenticatedMetraShell(
                currentUserId = userId,
                pendingNotificationRoute = pendingNotificationRoute,
                onNotificationRouteConsumed = onNotificationRouteConsumed,
                onSignedOut = {
                    authScope.launch {
                        AuthSessionRepository(vault).signOut()
                        currentUserId = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedMetraShell(
    currentUserId: String,
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val vault = remember(context) { SecretVault(context) }

    val viewModel: MetraAgentViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                val localDb = MetraLocalDatabase.getInstance(app)
                val appVault = SecretVault(app)
                MetraAgentViewModel(
                    currentUserId = currentUserId,
                    firestoreRepo = FirestoreAgentRepository(firestore),
                    gitHubRepo = GitHubRepository(appVault),
                    aiRepo = AiOrchestratorRepository(appVault),
                    backendRepo = BackendRepository(appVault),
                    vault = appVault,
                    localDao = localDb.agentDao()
                )
            }
        }
    )

    val settingsStream by viewModel.settingsState.collectAsStateWithLifecycle()
    val tasksStream by viewModel.tasksState.collectAsStateWithLifecycle()
    val approvalsStream by viewModel.approvalsState.collectAsStateWithLifecycle()
    val chatStream by viewModel.chatState.collectAsStateWithLifecycle()
    val logsStream by viewModel.logsState.collectAsStateWithLifecycle()
    val localCheckpoints by viewModel.localCheckpoints.collectAsStateWithLifecycle()
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()

    val settings = (settingsStream as? StreamUiState.Success)?.data
        ?: UserSettings(id = currentUserId, userId = currentUserId)
    val tasks = (tasksStream as? StreamUiState.Success)?.data.orEmpty()
    val approvals = (approvalsStream as? StreamUiState.Success)?.data.orEmpty()
    val messages = (chatStream as? StreamUiState.Success)?.data.orEmpty()
    val logs = (logsStream as? StreamUiState.Success)?.data.orEmpty()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(workspace.statusBannerMessage) {
        val msg = workspace.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val pending = workspace.pendingFileDownload
        if (uri != null && pending != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pending.bytes)
                }
                viewModel.completeFileDownload(
                    filePath = pending.fullPath,
                    success = true,
                    message = "تم تنزيل وحفظ الملف: ${pending.fileName}"
                )
            } catch (e: Exception) {
                viewModel.completeFileDownload(
                    filePath = pending.fullPath,
                    success = false,
                    message = "تعذر حفظ الملف: ${e.localizedMessage}"
                )
            }
        } else {
            viewModel.completeFileDownload(
                filePath = pending?.fullPath.orEmpty(),
                success = false,
                message = "تم إلغاء تنزيل الملف."
            )
        }
    }

    LaunchedEffect(workspace.pendingFileDownload) {
        val pending = workspace.pendingFileDownload
        if (pending != null) {
            saveFileLauncher.launch(pending.fileName)
        }
    }

    val activeProjectTitle = workspace.platformConfig.activeProject?.name?.ifBlank {
        settings.activeRepo.substringAfter("/")
    } ?: settings.activeRepo.substringAfter("/").ifBlank { "منصة الوكيل" }

    val primaryTabs = listOf(
        TopNavTab("dashboard", "الرئيسية", Icons.Rounded.Dashboard),
        TopNavTab("universal_studio", "الاستوديو", Icons.Rounded.Code),
        TopNavTab("chat", "الوكيل", Icons.Rounded.AutoAwesome),
        TopNavTab("tasks", "المهام", Icons.Rounded.Checklist),
        TopNavTab("files", "المستودع", Icons.Rounded.FolderOpen),
        TopNavTab("database", "القاعدة", Icons.Rounded.Storage)
    )
    // Five mobile destinations follow the approved Figma information architecture.
    // Advanced tools remain accessible from dashboard shortcuts and the desktop navigation rail.
    val mobileTabs = listOf(
        TopNavTab("dashboard", "الرئيسية", Icons.Rounded.Dashboard),
        TopNavTab("chat", "الوكيل", Icons.Rounded.AutoAwesome),
        TopNavTab("tasks", "المهام", Icons.Rounded.Checklist),
        TopNavTab("linked_accounts", "المشاريع", Icons.Rounded.FolderOpen),
        TopNavTab("security", "الإعدادات", Icons.Rounded.Settings)
    )
    val isTopTab = primaryTabs.any { it.route == currentRoute } ||
        mobileTabs.any { it.route == currentRoute }

    val screenTitles = mapOf(
        "dashboard" to "لوحة: $activeProjectTitle",
        "universal_studio" to "أستوديو التطوير ومرشح النماذج (AI Studio Pro)",
        "chat" to "المحادثة مع الوكيل",
        "tasks" to "المهام وخطة التنفيذ",
        "files" to "مستكشف وتنزيل الملفات",
        "code_editor" to "محرر ومراجع الأكواد",
        "branches" to "الفروع والـ Commits",
        "prs" to "Pull Requests",
        "actions" to "GitHub Actions",
        "database" to "قواعد البيانات والأسرار",
        "sql_review" to "مراجعة SQL والأمان",
        "tests" to "الاختبارات",
        "logs" to "سجل العمليات والتصدير",
        "github_setup" to "ربط GitHub",
        "ai_setup" to "وكلاء AI والأولوية وBackend",
        "ai_conversations" to "محادثات وحسابات ChatGPT وGemini",
        "security" to "إعدادات الأمان والصلاحيات",
        "linked_accounts" to "المشاريع والحسابات المتعددة",
        "workspace_plan" to "غرفة التعاون وملف العمل الموحد"
    )

    var hasNotificationPermission by remember {
        mutableStateOf(QuotaNotificationHelper.hasNotificationPermission(context))
    }
    var dismissedNotifBanner by remember { mutableStateOf(false) }
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        viewModel.syncNotificationPrefs(context)
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            if (primaryTabs.any { it.route == route } || mobileTabs.any { it.route == route }) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            } else {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingNotificationRoute) {
        val target = pendingNotificationRoute
        if (!target.isNullOrBlank()) {
            navigateTo(target)
            onNotificationRouteConsumed()
        }
    }

    MetraAgentTheme(darkTheme = settings.darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = screenTitles[currentRoute] ?: "ai i.z",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            if (!isTopTab) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("nav_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "رجوع"
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navigateTo("linked_accounts") },
                                modifier = Modifier.testTag("top_linked_accounts_button")
                            ) {
                                Icon(Icons.Rounded.Hub, contentDescription = "الحسابات المرتبطة")
                            }
                            IconButton(
                                onClick = { navigateTo("security") },
                                modifier = Modifier.testTag("top_security_button")
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = "الأمان والصلاحيات")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            mobileTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateTo(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("bottom_nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            primaryTabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateTo(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !hasNotificationPermission &&
                            !dismissedNotifBanner
                        ) {
                            com.example.ui.components.CommandCard(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                borderColor = MaterialTheme.colorScheme.primary
                            ) {
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "فعّل إشعارات التنبيه والانتباه على الهاتف",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "لتصلك إشعارات فورية بصوت واهتزاز عند انتهاء أي أمر أو توقف الوكيل أو طلب إجراء/موافقة.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("enable_notifications_banner_btn")
                                    ) {
                                        Text("تفعيل الإشعارات الآن")
                                    }
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { dismissedNotifBanner = true }
                                    ) {
                                        Text("لاحقاً")
                                    }
                                }
                            }
                        }

                        workspace.latestQuotaBanner?.let { banner ->
                            QuotaFailoverBanner(
                                exhaustedLabel = banner.exhaustedLabel,
                                switchedToLabel = banner.switchedToLabel,
                                reason = banner.reason,
                                onDismiss = { viewModel.dismissQuotaBanner() },
                                onManageAi = {
                                    viewModel.dismissQuotaBanner()
                                    navigateTo("ai_setup")
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NavHost(
                                navController = navController,
                                startDestination = "dashboard"
                            ) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        tasks = tasks,
                                        approvals = approvals,
                                        logs = logs,
                                        onSwitchProject = { viewModel.activateProjectProfile(it) },
                                        onResumeTask = { viewModel.resumeTaskInBackground(it, context) },
                                        onApprove = { viewModel.approveAndExecute(it, context) },
                                        onReject = { viewModel.rejectApproval(it) },
                                        onNavigateRoute = { navigateTo(it) }
                                    )
                                }
                                composable("chat") {
                                    AgentChatScreen(
                                        messages = messages,
                                        pendingApprovals = approvals.filter { it.status == "pending" },
                                        workspace = workspace,
                                        onSendCommand = { viewModel.sendChatCommand(it, context) },
                                        onRunTeamCollaboration = { directive ->
                                            viewModel.runMultiModelCollaboration(directive, context)
                                            navigateTo("workspace_plan")
                                        },
                                        onOpenSharedWorkspace = { navigateTo("workspace_plan") },
                                        onApprove = { viewModel.approveAndExecute(it, context) },
                                        onReject = { viewModel.rejectApproval(it) },
                                        onOpenAiConversations = { navigateTo("ai_conversations") }
                                    )
                                }
                                composable("tasks") {
                                    TasksScreen(
                                        tasks = tasks,
                                        localCheckpoints = localCheckpoints,
                                        sharedWorkFilePath = workspace.sharedWorkFilePath,
                                        onCreateTaskAndRun = { viewModel.sendChatCommand(it, context) },
                                        onResumeTask = { viewModel.resumeTaskInBackground(it, context) },
                                        onOpenSharedWorkspace = { navigateTo("workspace_plan") }
                                    )
                                }
                                composable("workspace_plan") {
                                    SharedWorkspaceScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onSaveProjectSpec = { path, reqs, instr, collab, continuous ->
                                            viewModel.saveActiveProjectWorkspaceSpec(
                                                sharedWorkFilePath = path,
                                                projectRequirements = reqs,
                                                systemInstructions = instr,
                                                collaborativeModeEnabled = collab,
                                                continuousExecutionEnabled = continuous
                                            )
                                        },
                                        onToggleTeamMember = { epId, sel ->
                                            viewModel.toggleCollaborativeMember(epId, sel)
                                        },
                                        onLoadOrInitSharedFile = { path ->
                                            viewModel.loadOrInitSharedWorkFileFromGitHub(path)
                                        },
                                        onEditSharedFileDraft = { viewModel.updateSharedWorkFileDraft(it) },
                                        onRunCollaboration = { directive ->
                                            viewModel.runMultiModelCollaboration(directive, context)
                                        },
                                        onRequestCommitSharedFile = { msg ->
                                            viewModel.requestCommitSharedWorkFileApproval(msg, context)
                                        },
                                        onGoToAiSetup = { navigateTo("ai_setup") }
                                    )
                                }
                                composable("files") {
                                    FileExplorerScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onOpenFile = { path ->
                                            viewModel.openFileFromGitHub(path)
                                            navigateTo("code_editor")
                                        },
                                        onDownloadFile = { path ->
                                            viewModel.prepareDownloadGitHubFile(path)
                                        },
                                        onGoToGitHubSetup = { navigateTo("github_setup") }
                                    )
                                }
                                composable("code_editor") {
                                    CodeEditorScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onEditContent = { viewModel.updateEditedFileContent(it) },
                                        onAnalyzeWithAi = { viewModel.analyzeSelectedCodeWithAi(context) },
                                        onDownloadFile = { viewModel.prepareDownloadGitHubFile(it) },
                                        onRequestCommitApproval = { viewModel.requestCommitEditApproval(it, context) },
                                        onGoToFiles = { navigateTo("files") }
                                    )
                                }
                                composable("branches") {
                                    BranchesCommitsScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onSelectBranch = { viewModel.selectRepositoryAndBranch(settings.activeRepo, it) },
                                        onCreateBranch = { viewModel.createWorkingBranch(it) },
                                        onGoToGitHubSetup = { navigateTo("github_setup") }
                                    )
                                }
                                composable("prs") {
                                    PullRequestsScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onInspectPr = { viewModel.inspectPullRequest(it, context) },
                                        onRequestCreateDraftPr = { t, h, b, body ->
                                            viewModel.requestCreateDraftPrApproval(t, h, b, body, context)
                                        },
                                        onRequestMergePr = { viewModel.requestMergePrApproval(it, context) }
                                    )
                                }
                                composable("actions") {
                                    ActionsScreen(
                                        workspace = workspace,
                                        onRefresh = { viewModel.refreshGitHubWorkspace() }
                                    )
                                }
                                composable("database") {
                                    DatabaseScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onSaveDatabaseConnection = { label, engine, host, dbName, connSec, apiSec, ro, prot ->
                                            viewModel.saveDatabaseConnection(
                                                label = label,
                                                engine = engine,
                                                hostOrUrl = host,
                                                databaseName = dbName,
                                                secretOrConnectionString = connSec,
                                                apiKeyOrServiceRole = apiSec,
                                                readOnlyMode = ro,
                                                protectedKeywordsCsv = prot
                                            )
                                        },
                                        onActivateDatabaseConnection = { viewModel.activateDatabaseConnection(it) },
                                        onRemoveDatabaseConnection = { viewModel.removeDatabaseConnection(it) },
                                        onLoadSchema = { viewModel.loadDatabaseSchema() },
                                        onGoToBackendSetup = { navigateTo("ai_setup") },
                                        onGoToSqlReview = { navigateTo("sql_review") }
                                    )
                                }
                                composable("sql_review") {
                                    SqlReviewScreen(
                                        workspace = workspace,
                                        onReviewOrExecuteSql = { sql, targetDb ->
                                            viewModel.reviewOrExecuteSql(sql, targetDb, context)
                                        }
                                    )
                                }
                                composable("tests") {
                                    TestsScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onRunTests = { viewModel.runRemoteTests(it, context) },
                                        onSendFailuresToAgent = { prompt ->
                                            viewModel.sendChatCommand(prompt, context)
                                            navigateTo("chat")
                                        },
                                        onGoToBackendSetup = { navigateTo("ai_setup") }
                                    )
                                }
                                composable("logs") {
                                    LogsScreen(
                                        logs = logs,
                                        flutterSpecText = viewModel.getExportableFlutterSpec()
                                    )
                                }
                                composable("github_setup") {
                                    GitHubSetupScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        onConnectToken = { viewModel.connectGitHubToken(it) },
                                        onSwitchGitHubAccount = { viewModel.switchGitHubAccount(it) },
                                        onOpenLinkedAccounts = { navigateTo("linked_accounts") },
                                        onSelectRepoAndBranch = { repo, branch ->
                                            viewModel.selectRepositoryAndBranch(repo, branch)
                                        },
                                        onRefresh = { viewModel.refreshGitHubWorkspace() }
                                    )
                                }
                                composable("linked_accounts") {
                                    LinkedAccountsScreen(
                                        currentUserId = currentUserId,
                                        settings = settings,
                                        workspace = workspace,
                                        onCreateProject = { name, repo, branch, stack, cmd, ghId, dbId, prot, instr, sharedPath, reqs, collab, cont ->
                                            viewModel.createOrUpdateProjectProfile(
                                                name = name,
                                                repoSlug = repo,
                                                defaultBranch = branch,
                                                techStack = stack,
                                                customTestCommand = cmd,
                                                linkedGitHubAccountId = ghId,
                                                linkedDatabaseId = dbId,
                                                protectedTargetsCsv = prot,
                                                systemInstructions = instr,
                                                sharedWorkFilePath = sharedPath,
                                                projectRequirements = reqs,
                                                collaborativeModeEnabled = collab,
                                                continuousExecutionEnabled = cont
                                            )
                                        },
                                        onActivateProject = { viewModel.activateProjectProfile(it) },
                                        onRemoveProject = { viewModel.removeProjectProfile(it) },
                                        onOpenSharedWorkspace = { navigateTo("workspace_plan") },
                                        onAddGitHubAccount = { token, label, defaultRepo ->
                                            viewModel.connectGitHubToken(token, label, defaultRepo)
                                        },
                                        onSwitchGitHubAccount = { viewModel.switchGitHubAccount(it) },
                                        onRemoveGitHubAccount = { viewModel.removeGitHubAccount(it) },
                                        onSwitchBackendProfile = { viewModel.switchBackendProfile(it) },
                                        onRemoveBackendProfile = { viewModel.removeBackendProfile(it) },
                                        onCreateBundle = { name, ghId, beId, repo, branch ->
                                            viewModel.createAndActivateBundle(name, ghId, beId, repo, branch)
                                        },
                                        onActivateBundle = { viewModel.activateBundle(it) },
                                        onRemoveBundle = { viewModel.removeBundle(it) },
                                        onGoToAiSetup = { navigateTo("ai_setup") },
                                        onGoogleAccountSwitched = { navigateTo("dashboard") }
                                    )
                                }
                                composable("ai_setup") {
                                    AiProviderScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        hasBackendSecret = vault.hasSecret(SecretVault.KEY_BACKEND_TOKEN),
                                        onAddAiEndpoint = { label, type, model, key, url, hint ->
                                            viewModel.addOrUpdateAiEndpoint(
                                                label = label,
                                                providerType = type,
                                                modelName = model,
                                                apiKey = key,
                                                baseUrl = url,
                                                accountHint = hint
                                            )
                                        },
                                        onToggleAiEndpoint = { id, enabled -> viewModel.toggleAiEndpoint(id, enabled) },
                                        onResetEndpointQuota = { viewModel.resetEndpointQuota(it) },
                                        onResetAllQuotas = { viewModel.resetAllQuotas() },
                                        onRemoveAiEndpoint = { viewModel.removeAiEndpoint(it) },
                                        onMovePriority = { id, up -> viewModel.moveActiveProjectAiPriority(id, up) },
                                        onToggleCollaborativeMember = { id, sel -> viewModel.toggleCollaborativeMember(id, sel) },
                                        onOpenSharedWorkspace = { navigateTo("workspace_plan") },
                                        onSaveBackendConfig = { url, token, prov, mod ->
                                            viewModel.saveBackendAndOpenAiSecrets(url, token, "", prov, mod)
                                        },
                                        onTestBackend = { viewModel.testBackendHealth(it) },
                                         onOpenAiConversations = { navigateTo("ai_conversations") }
                                    )
                                }
                                composable("security") {
                                    SecuritySettingsScreen(
                                        settings = settings,
                                        approvals = approvals,
                                        notificationPrefs = workspace.notificationPrefs,
                                        onUpdateNotificationPrefs = {
                                            viewModel.updateNotificationPrefs(context, it)
                                        },
                                        onSendTestNotification = {
                                            viewModel.sendTestNotification(context)
                                        },
                                        onUpdateSettings = { viewModel.saveUserSettings(it) },
                                        onApprove = { viewModel.approveAndExecute(it, context) },
                                        onReject = { viewModel.rejectApproval(it) },
                                        onClearSecrets = { viewModel.clearEncryptedSecrets() },
                                        onSignedOut = onSignedOut
                                    )
                                }
                                composable("universal_studio") {
                                    UniversalStudioScreen(
                                        activeProject = workspace.activeStudioProject,
                                        isGenerating = workspace.isGeneratingStudioProject,
                                        selectedFilePath = workspace.studioSelectedFilePath,
                                        editedFileContent = workspace.studioEditedContent,
                                        auditStatus = workspace.studioAuditStatus,
                                        onCreateProject = { name, prompt, env, lang, priority ->
                                            viewModel.createStudioProject(name, prompt, env, lang, priority)
                                        },
                                        onSelectFile = { viewModel.selectStudioFile(it) },
                                        onUpdateFileContent = { viewModel.updateStudioFileContent(it) },
                                        onSaveFileChanges = { viewModel.saveStudioFileChanges() },
                                        onRunAudit = { viewModel.runStudioAudit() },
                                        onApplyModel = { id, name -> viewModel.applyRecommendedModelToProject(id, name) },
                                        onOpenChat = { navigateTo("chat") }
                                    )
                                }
                                composable("ai_conversations") {
                                    AiAccountConversationsScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        chatMessages = messages,
                                        onGoToAiSetup = { navigateTo("ai_setup") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
