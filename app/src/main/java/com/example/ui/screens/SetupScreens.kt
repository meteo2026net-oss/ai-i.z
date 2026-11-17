package com.example.ui.screens

import com.example.ui.i18n.AppUiStrings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.notifications.QuotaNotificationHelper
import com.example.core.security.AiEndpointConfig
import com.example.core.security.AiProviderType
import com.example.domain.model.UserSettings
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Screen 2: GitHub Connection & Repository Selector
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitHubSetupScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onConnectToken: (String) -> Unit,
    onSwitchGitHubAccount: (String) -> Unit,
    onOpenLinkedAccounts: () -> Unit,
    onSelectRepoAndBranch: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    val uiLang = settings.appLanguage
    var patInput by remember { mutableStateOf("") }
    var customRepoInput by remember(settings.activeRepo) { mutableStateOf(settings.activeRepo) }
    var customBranchInput by remember(settings.activeBranch) { mutableStateOf(settings.activeBranch) }

    val savedProjects = workspace.platformConfig.projects

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CommandCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(AppUiStrings.setupGithubAccountsTitle(uiLang), style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(
                        label = if (workspace.isGitHubConnected) {
                            workspace.gitHubUser?.login ?: "متصل"
                        } else "غير مربوط",
                        color = if (workspace.isGitHubConnected) EmeraldSuccess else AmberWarning
                    )
                }
                Text(
                    text = "يُحفظ GitHub Personal Access Token مشفراً محلياً عبر Android Keystore AES-GCM، ويمكنك إضافة عدة حسابات والتبديل بينها لكل مشروع.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (workspace.linkedAccounts.gitHubAccounts.size > 1) {
                    Text(AppUiStrings.setupGithubQuickSwitch(uiLang), style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workspace.linkedAccounts.gitHubAccounts.forEach { acc ->
                            FilterChip(
                                selected = acc.isActive,
                                onClick = { onSwitchGitHubAccount(acc.id) },
                                label = { Text("@${acc.login}") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = patInput,
                    onValueChange = { patInput = it },
                    label = { Text("GitHub Personal Access Token (PAT)") },
                    placeholder = { Text("ghp_... أو github_pat_...") },
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_pat_input")
                )
                if (!workspace.gitHubError.isNullOrBlank()) {
                    Text(
                        text = workspace.gitHubError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onConnectToken(patInput)
                            patInput = ""
                        },
                        enabled = !workspace.isBusyGitHub,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("github_connect_button")
                    ) {
                        if (workspace.isBusyGitHub) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(AppUiStrings.setupVerifyLink(uiLang))
                        }
                    }
                    if (workspace.isGitHubConnected) {
                        OutlinedButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("github_refresh_button")
                        ) {
                            Text(AppUiStrings.setupRefresh(uiLang))
                        }
                    }
                }
                OutlinedButton(
                    onClick = onOpenLinkedAccounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_linked_accounts_from_github")
                ) {
                    Text(AppUiStrings.setupManageProjectsMulti(uiLang))
                }
            }
        }

        item {
            CommandCard {
                Text(AppUiStrings.setupSavedProjectsActiveRepo(uiLang), style = MaterialTheme.typography.titleMedium)
                savedProjects.filter { it.repoSlug.isNotBlank() }.forEach { prj ->
                    val selected = settings.activeRepo.equals(prj.repoSlug, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                customRepoInput = prj.repoSlug
                                customBranchInput = prj.defaultBranch
                                onSelectRepoAndBranch(prj.repoSlug, prj.defaultBranch)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                customRepoInput = prj.repoSlug
                                customBranchInput = prj.defaultBranch
                                onSelectRepoAndBranch(prj.repoSlug, prj.defaultBranch)
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prj.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${prj.repoSlug} (${prj.defaultBranch})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                        }
                    }
                }
                OutlinedTextField(
                    value = customRepoInput,
                    onValueChange = { customRepoInput = it },
                    label = { Text(AppUiStrings.setupAnyGithubRepo(uiLang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customBranchInput,
                    onValueChange = { customBranchInput = it },
                    label = { Text(AppUiStrings.setupActiveBranch(uiLang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSelectRepoAndBranch(
                            customRepoInput.trim(),
                            customBranchInput.trim().ifBlank { "main" }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_active_repo_button")
                ) {
                    Text(AppUiStrings.setupAdoptRepoBranch(uiLang))
                }
            }
        }

        if (workspace.availableRepos.isNotEmpty()) {
            item {
                Text(
                    "مستودعات الحساب المتاحة (${workspace.availableRepos.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(workspace.availableRepos, key = { it.id }) { repo ->
                CommandCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                customRepoInput = repo.fullName
                                customBranchInput = repo.defaultBranch
                                onSelectRepoAndBranch(repo.fullName, repo.defaultBranch)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(repo.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${repo.language ?: "Code"} • الفرع الافتراضي: ${repo.defaultBranch}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.Source, contentDescription = null, tint = ElectricCyan)
                    }
                }
            }
        }
    }
}

/**
 * Screen 3: Multi-Account AI Failover Pool, Per-Project Priority & Backend Runner Settings
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiProviderScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    hasBackendSecret: Boolean,
    onAddAiEndpoint: (
        label: String,
        providerType: AiProviderType,
        modelName: String,
        apiKey: String,
        baseUrl: String,
        accountHint: String
    ) -> Unit,
    onToggleAiEndpoint: (String, Boolean) -> Unit,
    onResetEndpointQuota: (String) -> Unit,
    onResetAllQuotas: () -> Unit,
    onRemoveAiEndpoint: (String) -> Unit,
    onMovePriority: (String, Boolean) -> Unit,
    onToggleCollaborativeMember: (String, Boolean) -> Unit,
    onOpenSharedWorkspace: () -> Unit,
    onOpenAiConversations: () -> Unit,
    onSaveBackendConfig: (backendUrl: String, backendToken: String, aiProvider: String, aiModel: String) -> Unit,
    onTestBackend: (String) -> Unit
) {
    val uiLang = settings.appLanguage
    val context = LocalContext.current
    var notifPermissionGranted by remember {
        mutableStateOf(QuotaNotificationHelper.hasNotificationPermission(context))
    }
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted
    }

    var selectedProviderType by remember { mutableStateOf(AiProviderType.GEMINI_REST) }
    var endpointLabel by remember { mutableStateOf("") }
    var accountHint by remember { mutableStateOf("") }
    var modelName by remember(selectedProviderType) { mutableStateOf(selectedProviderType.defaultModel) }
    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember(selectedProviderType) { mutableStateOf(selectedProviderType.defaultBaseUrl) }

    var backendUrl by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }

    val pConfig = workspace.platformConfig
    val activeProject = pConfig.activeProject
    val endpointMap = pConfig.aiEndpoints.associateBy { it.id }
    val orderedEndpoints = buildList {
        activeProject?.aiPriorityOrder?.forEach { id ->
            endpointMap[id]?.let { add(it) }
        }
        pConfig.aiEndpoints.forEach { ep ->
            if (ep.id !in (activeProject?.aiPriorityOrder.orEmpty())) add(ep)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Permission + Quota Status Header
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(AppUiStrings.setupAiPoolTitle(uiLang), style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(
                        label = "${orderedEndpoints.count { it.isEnabled && it.quotaStatus != AiEndpointConfig.QUOTA_EXHAUSTED }} متاح",
                        color = EmeraldSuccess
                    )
                }
                Text(
                    text = "أضف عدة مفاتيح Google Gemini API (لكل حساب Google مفتاحه)، مع OpenAI وClaude وGroq وOpenRouter وDeepSeek. عند استنفاد أي حصة ينبهك التطبيق فوراً ويتابع العمل تلقائياً من التالي حسب أولوية المشروع.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifPermissionGranted) {
                        OutlinedButton(
                            onClick = {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                            Text(AppUiStrings.setupEnableQuotaNotifs(uiLang))
                        }
                    }
                    if (orderedEndpoints.any { it.quotaStatus != AiEndpointConfig.QUOTA_READY }) {
                        Button(
                            onClick = onResetAllQuotas,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_all_quotas_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Text(AppUiStrings.setupReenableAll(uiLang))
                        }
                    }
                }
                OutlinedButton(
                    onClick = onOpenAiConversations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_ai_conversations_screen_btn")
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null)
                    Text(AppUiStrings.setupViewAiConversations(uiLang))
                }
            }
        }

        // Quota Exhaustion History Card
        if (workspace.quotaAlerts.isNotEmpty()) {
            item {
                CommandCard(borderColor = AmberWarning.copy(alpha = 0.65f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AmberWarning)
                        Text(AppUiStrings.setupConsumptionAlerts(uiLang), style = MaterialTheme.typography.titleMedium)
                    }
                    workspace.quotaAlerts.take(5).forEach { alert ->
                        Text(
                            text = "• انتهى «${alert.exhaustedLabel}» -> ${alert.switchedToLabel ?: "بدون بديل"} (${alert.reason})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }
                }
            }
        }

        // Ordered Per-Project Priority Pool
        item {
            Text(
                text = "ترتيب الأولوية لمشروع: ${activeProject?.name ?: settings.activeRepo} (${orderedEndpoints.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(orderedEndpoints, key = { it.id }) { ep ->
            val rank = orderedEndpoints.indexOf(ep) + 1
            val type = AiProviderType.fromWireKey(ep.providerType)
            val statusColor = when {
                !ep.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                ep.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED -> CrimsonError
                ep.quotaStatus == AiEndpointConfig.QUOTA_ERROR -> AmberWarning
                else -> EmeraldSuccess
            }
            val statusLabel = when {
                !ep.isEnabled -> "معطل"
                ep.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED -> "انتهى الاستخدام"
                ep.quotaStatus == AiEndpointConfig.QUOTA_ERROR -> "خطأ مؤقت"
                else -> "متاح (#$rank)"
            }
            CommandCard(borderColor = statusColor.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#$rank • ${ep.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${type.arabicLabel} • ${ep.modelName}" +
                                (if (ep.accountHint.isNotBlank()) " • الحساب: ${ep.accountHint}" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "الطلبات الناجحة: ${ep.requestsCount}" +
                                (if (ep.lastError.isNotBlank()) " | ${ep.lastError}" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ep.lastError.isNotBlank()) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(label = statusLabel, color = statusColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMovePriority(ep.id, true) },
                            enabled = rank > 1,
                            modifier = Modifier.testTag("ai_up_${ep.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "رفع الأولوية")
                        }
                        IconButton(
                            onClick = { onMovePriority(ep.id, false) },
                            enabled = rank < orderedEndpoints.size,
                            modifier = Modifier.testTag("ai_down_${ep.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "خفض الأولوية")
                        }
                        if (ep.quotaStatus != AiEndpointConfig.QUOTA_READY) {
                            OutlinedButton(
                                onClick = { onResetEndpointQuota(ep.id) },
                                modifier = Modifier.testTag("reset_quota_${ep.id}")
                            ) {
                                Text(AppUiStrings.setupReenable(uiLang))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = ep.isEnabled,
                            onCheckedChange = { onToggleAiEndpoint(ep.id, it) }
                        )
                        if (orderedEndpoints.size > 1) {
                            IconButton(
                                onClick = { onRemoveAiEndpoint(ep.id) },
                                modifier = Modifier.testTag("remove_ai_${ep.id}")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف المزود", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Model Collaborative Team Card
        item {
            val selectedTeam = remember(activeProject?.collaborativeEndpointIds, orderedEndpoints) {
                val ids = activeProject?.collaborativeEndpointIds.orEmpty()
                if (ids.isNotEmpty()) ids.toSet() else orderedEndpoints.map { it.id }.toSet()
            }
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Text(AppUiStrings.setupCollabTeam(uiLang), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "اختر أكثر من حساب أو نموذج ليتعاونوا معاً (المخطط ← المنفذ ← المدقق) داخل ملف العمل المشترك، مع استمرار التحويل التلقائي إذا انتهت الحصة أو توقف نموذج:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderedEndpoints.forEach { ep ->
                        val checked = ep.id in selectedTeam
                        FilterChip(
                            selected = checked,
                            onClick = { onToggleCollaborativeMember(ep.id, !checked) },
                            label = { Text("${ep.label} (${ep.modelName})", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenSharedWorkspace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_open_shared_workspace_button")
                ) {
                    Text(AppUiStrings.setupOpenCollabRoom(uiLang))
                }
            }
        }

        // Add New AI Key / Agent Account Card
        item {
            CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                Text(AppUiStrings.setupAddAiAccount(uiLang), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiProviderType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedProviderType == type,
                            onClick = {
                                selectedProviderType = type
                                modelName = type.defaultModel
                                baseUrlInput = type.defaultBaseUrl
                            },
                            label = { Text(type.arabicLabel, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("provider_type_${type.wireKey}")
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedProviderType.suggestedModels.forEach { candidate ->
                        FilterChip(
                            selected = modelName == candidate,
                            onClick = { modelName = candidate },
                            label = { Text(candidate, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = endpointLabel,
                    onValueChange = { endpointLabel = it },
                    label = { Text(AppUiStrings.setupAiAccountName(uiLang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_endpoint_label_input")
                )
                OutlinedTextField(
                    value = accountHint,
                    onValueChange = { accountHint = it },
                    label = { Text("ملاحظة الحساب أو البريد المالك للمفتاح (اختياري)") },
                    placeholder = { Text("dev2@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("اسم النموذج (Model ID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selectedProviderType != AiProviderType.FIREBASE_GEMINI) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = {
                            Text(
                                if (selectedProviderType == AiProviderType.GEMINI_REST)
                                    "Google AI Studio Gemini API Key"
                                else "API Key (يُحفظ مشفراً محلياً)"
                            )
                        },
                        placeholder = {
                            Text(
                                when (selectedProviderType) {
                                    AiProviderType.GEMINI_REST -> "AIza..."
                                    AiProviderType.ANTHROPIC -> "sk-ant-..."
                                    else -> "sk-..."
                                }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_api_key_input")
                    )
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL (اختياري للتخصيص)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        onAddAiEndpoint(
                            endpointLabel,
                            selectedProviderType,
                            modelName,
                            apiKeyInput,
                            baseUrlInput,
                            accountHint
                        )
                        endpointLabel = ""
                        accountHint = ""
                        apiKeyInput = ""
                    },
                    enabled = !selectedProviderType.requiresApiKey || apiKeyInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_ai_endpoint_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("إضافة إلى مسبح الذكاء الاصطناعي وأولوية المشروع")
                }
            }
        }

        // Backend Runner Settings Card
        item {
            CommandCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Dns, contentDescription = null, tint = ElectricCyan)
                        Text("خادم التنفيذ (Backend Runner)", style = MaterialTheme.typography.titleMedium)
                    }
                    StatusPill(
                        label = if (workspace.backendHealth != null) "متصل" else "اختياري",
                        color = if (workspace.backendHealth != null) EmeraldSuccess else AmberWarning,
                        icon = if (workspace.backendHealth != null) Icons.Rounded.CloudDone else null
                    )
                }
                Text(
                    text = "لتشغيل الاختبارات البعيدة وفحص مخططات قواعد البيانات واستعلامات القراءة الآمنة، اربط خادم Backend الخاص بك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("رابط خادم Backend (HTTPS)") },
                    placeholder = { Text("https://runner.example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backend_url_input")
                )
                Text(
                    text = "تتم المصادقة تلقائياً بجلسة AI I.Z الحالية؛ لا حاجة إلى رمز Backend منفصل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!workspace.backendError.isNullOrBlank()) {
                    Text(
                        text = workspace.backendError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }
                workspace.backendHealth?.let { h ->
                    Text(
                        text = "الحالة: ${h.status} | .NET: ${h.dotnetAvailable} | Flutter: ${h.flutterAvailable} | القاعدة: ${h.activeDatabaseName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveBackendConfig(
                                backendUrl,
                                "",
                                selectedProviderType.wireKey,
                                modelName
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_ai_backend_button")
                    ) {
                        Text("حفظ إعدادات Backend")
                    }
                    OutlinedButton(
                        onClick = { onTestBackend(backendUrl) },
                        enabled = !workspace.isBusyBackend,
                        modifier = Modifier.testTag("test_backend_button")
                    ) {
                        Text("فحص الاتصال")
                    }
                }
            }
        }
    }
}
