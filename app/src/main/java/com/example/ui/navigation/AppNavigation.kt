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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        MetraAgentTheme(darkTheme = true) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedMetraShell(
                currentUserId = user.uid,
                pendingNotificationRoute = pendingNotificationRoute,
                onNotificationRouteConsumed = onNotificationRouteConsumed,
                onSignedOut = { currentUser = null }
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
    // The five mobile destinations follow the approved Figma information architecture.
    // Advanced tools remain accessible from dashboard shortcuts and the desktop navigation rail.
    val mobileTabs = listOf(
        TopNavTab("dashboard", "الرئيسية", Icons.Rounded.Dashboard),
        TopNavTab("chat", "الوكيل", Icons.Rounded.AutoAwesome),
        TopNavTab("tasks", "المهام", Icons.Rounded.Checklist),
        TopNavTab("linked_accounts", "المشاريع", Icons.Rounded.FolderOpen),
        TopNavTab("security", "الإعدادات", Icons.Rounded.Settings)
    )
    val isTopTab = primaryTabs.any { it.route == currentRoute }

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
            if (primaryTabs.any { it.route == route }) {
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
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تفعيل إشعارات حدود استخدام AI", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "لتلقي تنبيه عند قرب انتهاء حصة الحساب.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    androidx.compose.material3.TextButton(
                                        onClick = { notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                                    ) { Text("تفعيل") }
                                    androidx.compose.material3.TextButton(
                                        onClick = { dismissedNotifBanner = true }
                                    ) { Text("لاحقاً") }
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            QuotaFailoverBanner(
                                workspace = workspace,
                                onOpenAiSetup = { navigateTo("ai_setup") },
                                onOpenAiConversations = { navigateTo("ai_conversations") }
                            )

                            NavHost(
                                navController = navController,
                                startDestination = "dashboard",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        settings = settings,
                                        workspace = workspace,
                                        tasks = tasks,
                                        approvals = approvals,
                                        logs = logs,
                                        onSwitchProject = viewModel::switchActiveProject,
                                        onResumeTask = viewModel::resumeTask,
                                        onApprove = viewModel::approve,
                                        onReject = viewModel::reject,
                                        onNavigateRoute = ::navigateTo
                                    )
                                }
                                composable("universal_studio") {
                                    UniversalStudioScreen(
                                    ...
