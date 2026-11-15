package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AgentTask
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.OperationLog
import com.example.domain.model.UserSettings
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.ApprovalGateCard
import com.example.ui.components.CommandCard
import com.example.ui.components.MetricsBarChart
import com.example.ui.components.SetupRequiredBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Screen 4: Main Command Dashboard
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    tasks: List<AgentTask>,
    approvals: List<ApprovalRequest>,
    logs: List<OperationLog>,
    onSwitchProject: (String) -> Unit,
    onResumeTask: (AgentTask) -> Unit,
    onApprove: (ApprovalRequest) -> Unit,
    onReject: (ApprovalRequest) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val activeTask = tasks.firstOrNull { it.status in listOf("running", "awaiting_approval", "queued", "paused") }
        ?: tasks.firstOrNull()
    val completedCount = tasks.count { it.status == "completed" }
    val remainingCount = tasks.count { it.status != "completed" }
    val overallProgress = if (tasks.isEmpty()) 0 else {
        tasks.map { it.progressPercent.toInt() }.average().toInt()
    }
    val pendingApprovals = approvals.filter { it.status == "pending" }
    val latestCommit = workspace.commits.firstOrNull()
    val latestError = tasks.firstOrNull { it.lastError.isNotBlank() }?.lastError
        ?: workspace.gitHubError
        ?: workspace.backendError
        ?: logs.firstOrNull { it.outcome == "error" }?.details
    val latestLog = logs.firstOrNull()

    val passedTests = workspace.lastTestRun?.passedCount ?: 0
    val failedTests = workspace.lastTestRun?.failedCount ?: 0
    val totalTests = (passedTests + failedTests).coerceAtLeast(1)

    val pConfig = workspace.platformConfig
    val activeProject = pConfig.activeProject
    val activeDb = pConfig.activeDatabase
    val activeAiName = workspace.activeAiEndpointLabel.ifBlank {
        activeProject?.aiPriorityOrder?.firstOrNull()
            ?.let { id -> pConfig.aiEndpoints.find { it.id == id }?.label }
            ?: pConfig.aiEndpoints.firstOrNull()?.label
            ?: settings.aiModel
    }
    val readyAiCount = pConfig.aiEndpoints.count {
        it.isEnabled && it.quotaStatus != com.example.core.security.AiEndpointConfig.QUOTA_EXHAUSTED
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Project Switcher Card
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المشاريع (${pConfig.projects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AssistChip(
                        onClick = { onNavigateRoute("linked_accounts") },
                        label = { Text("+ إدارة المشاريع") },
                        modifier = Modifier.testTag("dashboard_manage_projects_chip")
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pConfig.projects.forEach { prj ->
                        val isSelected = prj.id == activeProject?.id
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { onSwitchProject(prj.id) },
                            label = { Text(prj.name) },
                            modifier = Modifier.testTag("project_switch_${prj.id}")
                        )
                    }
                }
            }
        }

        // Universal AI Studio Pro Showcase Card
        item {
            CommandCard(borderColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = "أستوديو التطوير الشامل ومرشح النماذج (AI Studio Pro)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusPill(label = "كافة البيئات واللغات", color = ElectricCyan)
                }
                Text(
                    text = "إنشاء مشاريع متكاملة في كافة البيئات (أندرويد، Flutter، Next.js، FastAPI، Go، Rust) واقتراح النموذج الأقوى لكل مشروع.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onNavigateRoute("universal_studio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_open_studio_hero_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) {
                    Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح أستوديو التطوير وإنشاء مشروع جديد", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!workspace.isGitHubConnected) {
            item {
                SetupRequiredBanner(
                    title = "ربط GitHub مطلوب للبيانات الحية",
                    description = "اربط حساب GitHub لعرض الفروع، الملفات، تنزيل الأكواد، الـ commits، وActions الحقيقية لمشروع ${activeProject?.name ?: settings.activeRepo}.",
                    actionLabel = "الانتقال إلى ربط GitHub",
                    onAction = { onNavigateRoute("github_setup") },
                    testTag = "dashboard_connect_github_cta"
                )
            }
        }

        // Hero Project & Agent Status Card
        item {
            CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedProgressRing(progressPercent = activeTask?.progressPercent?.toInt() ?: overallProgress)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = activeProject?.name ?: settings.activeRepo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${settings.activeRepo} (${settings.activeBranch})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "الأولوية #1: $activeAiName • ($readyAiCount حساب AI جاهز)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        val agentStatusLabel = when (activeTask?.status) {
                            "running" -> "متابعة تلقائية قيد التنفيذ"
                            "awaiting_approval" -> "بانتظار موافقتك للاستمرار"
                            "paused" -> "توقف مؤقت (سيستأنف تلقائياً)"
                            "failed" -> "توقف بسبب خطأ"
                            "completed" -> "مكتمل"
                            "queued" -> "جاهز للاستئناف"
                            else -> "جاهز للأوامر"
                        }
                        val statusColor = when (activeTask?.status) {
                            "running" -> ElectricCyan
                            "awaiting_approval" -> AmberWarning
                            "failed" -> CrimsonError
                            "completed" -> EmeraldSuccess
                            else -> EmeraldSuccess
                        }
                        StatusPill(label = "حالة الوكيل: $agentStatusLabel", color = statusColor)
                    }
                }

                if (activeTask != null) {
                    Text(
                        text = "المهمة الحالية: ${activeTask.title}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (activeTask.lastCompletedStep.isNotBlank()) {
                        Text(
                            text = "آخر خطوة مكتملة: ${activeTask.lastCompletedStep}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeTask.status in listOf("queued", "paused", "failed")) {
                        Button(
                            onClick = { onResumeTask(activeTask) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_resume_task_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text("استئناف المهمة في الخلفية")
                        }
                    }
                }
            }
        }

        // System Matrix + Metrics Charts
        item {
            CommandCard {
                Text("حالة المنظومة والمقاييس", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = if (workspace.isGitHubConnected) "GitHub متصل" else "GitHub غير مربوط",
                        color = if (workspace.isGitHubConnected) EmeraldSuccess else AmberWarning
                    )
                    StatusPill(
                        label = "AI: $readyAiCount متاح",
                        color = if (readyAiCount > 0) EmeraldSuccess else CrimsonError
                    )
                    StatusPill(
                        label = activeDb?.let { "DB: ${it.label}" }
                            ?: if (workspace.backendHealth?.postgresConnected == true) "PostgreSQL متصل" else "DB غير محددة",
                        color = if (activeDb != null || workspace.backendHealth?.postgresConnected == true) EmeraldSuccess else AmberWarning
                    )
                    StatusPill(
                        label = if (workspace.backendHealth != null) "Backend متصل" else "Backend اختياري",
                        color = if (workspace.backendHealth != null) EmeraldSuccess else ElectricCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniBox("مهام مكتملة", "$completedCount", EmeraldSuccess, Modifier.weight(1f))
                    MetricMiniBox("مهام متبقية", "$remainingCount", ElectricCyan, Modifier.weight(1f))
                    MetricMiniBox("اختبارات ناجحة", "$passedTests", EmeraldSuccess, Modifier.weight(1f))
                    MetricMiniBox("اختبارات فاشلة", "$failedTests", CrimsonError, Modifier.weight(1f))
                }

                MetricsBarChart(
                    items = listOf(
                        "إنجاز المهام" to (overallProgress / 100f to ElectricCyan),
                        "نجاح الاختبارات" to ((if (workspace.lastTestRun == null) 0f else passedTests.toFloat() / totalTests) to EmeraldSuccess),
                        "موافقات معلقة" to ((pendingApprovals.size.coerceAtMost(5) / 5f) to AmberWarning)
                    )
                )
            }
        }

        // Pending Approvals Alert
        if (pendingApprovals.isNotEmpty()) {
            item {
                Text(
                    "طلبات الموافقة المعلقة (${pendingApprovals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = AmberWarning
                )
            }
            item {
                val firstApproval = pendingApprovals.first()
                ApprovalGateCard(
                    approval = firstApproval,
                    onApprove = { onApprove(firstApproval) },
                    onReject = { onReject(firstApproval) }
                )
            }
        }

        // Latest Commit, Error & Activity
        item {
            CommandCard {
                Text("آخر المستجدات الفعلية", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "آخر Commit: ${latestCommit?.let { "${it.sha.take(7)} — ${it.commit.message.lines().firstOrNull().orEmpty()}" } ?: "لا توجد commits محملة بعد"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "آخر خطأ: ${latestError ?: "لا توجد أخطاء مسجلة"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (latestError != null) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "آخر نشاط: ${latestLog?.let { "${it.action} (${it.outcome})" } ?: "لا يوجد نشاط مسجل بعد"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Hub Grid for all 16 Screens
        item {
            CommandCard {
                Text("الوصول السريع لجميع الشاشات", style = MaterialTheme.typography.titleMedium)
                val shortcuts = listOf(
                    Triple("محادثات وحسابات AI", "ai_conversations", Icons.Rounded.Forum),
                    Triple("ملف العمل والتعاون", "workspace_plan", Icons.Rounded.Groups),
                    Triple("المشاريع والحسابات", "linked_accounts", Icons.Rounded.VerifiedUser),
                    Triple("ربط GitHub", "github_setup", Icons.Rounded.AccountTree),
                    Triple("وكلاء AI وBackend", "ai_setup", Icons.Rounded.Settings),
                    Triple("المحادثة", "chat", Icons.Rounded.PlayCircle),
                    Triple("المهام", "tasks", Icons.Rounded.VerifiedUser),
                    Triple("الملفات", "files", Icons.Rounded.FolderOpen),
                    Triple("محرر الأكواد", "code_editor", Icons.Rounded.Code),
                    Triple("الفروع والـ Commits", "branches", Icons.Rounded.AccountTree),
                    Triple("Pull Requests", "prs", Icons.Rounded.MergeType),
                    Triple("GitHub Actions", "actions", Icons.Rounded.PlayArrow),
                    Triple("قاعدة البيانات", "database", Icons.Rounded.Storage),
                    Triple("SQL والأمان", "sql_review", Icons.Rounded.Security),
                    Triple("الاختبارات", "tests", Icons.Rounded.BugReport),
                    Triple("سجل العمليات", "logs", Icons.Rounded.History),
                    Triple("الأمان والصلاحيات", "security", Icons.Rounded.Security)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fullShortcuts = listOf(
                        Triple("أستوديو التطوير الشامل", "universal_studio", Icons.Rounded.Code)
                    ) + shortcuts
                    fullShortcuts.forEach { (label, route, icon) ->
                        AssistChip(
                            onClick = { onNavigateRoute(route) },
                            label = { Text(label) },
                            leadingIcon = { Icon(icon, contentDescription = null) },
                            modifier = Modifier.testTag("shortcut_$route")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    CommandCard(
        modifier = modifier.clickable {},
        borderColor = accent.copy(alpha = 0.35f)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
