package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.domain.model.UserSettings
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.WorkspaceUiState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified Multi-Account Hub:
 * Link and switch multiple Google accounts, GitHub PATs, AI/Backend profiles,
 * and combined WorkspaceAccountBundles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkedAccountsScreen(
    currentUserId: String,
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onCreateProject: (
        name: String,
        repoSlug: String,
        branch: String,
        techStack: String,
        customTestCmd: String,
        linkedGhId: String,
        linkedDbId: String,
        protectedCsv: String,
        instructions: String,
        sharedWorkFilePath: String,
        projectRequirements: String,
        collaborativeMode: Boolean,
        continuousExecution: Boolean
    ) -> Unit,
    onActivateProject: (String) -> Unit,
    onRemoveProject: (String) -> Unit,
    onOpenSharedWorkspace: () -> Unit,
    onAddGitHubAccount: (token: String, label: String, defaultRepo: String) -> Unit,
    onSwitchGitHubAccount: (accountId: String) -> Unit,
    onRemoveGitHubAccount: (accountId: String) -> Unit,
    onSwitchBackendProfile: (profileId: String) -> Unit,
    onRemoveBackendProfile: (profileId: String) -> Unit,
    onCreateBundle: (name: String, gitHubAccountId: String, backendProfileId: String, repoSlug: String, branch: String) -> Unit,
    onActivateBundle: (bundleId: String) -> Unit,
    onRemoveBundle: (bundleId: String) -> Unit,
    onGoToAiSetup: () -> Unit,
    onGoogleAccountSwitched: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSwitchingGoogle by remember { mutableStateOf(false) }
    var googleSwitchError by remember { mutableStateOf<String?>(null) }

    var prjName by remember { mutableStateOf("") }
    var prjRepo by remember { mutableStateOf("") }
    var prjBranch by remember { mutableStateOf("main") }
    var prjStack by remember { mutableStateOf(com.example.core.security.TechStackType.AUTO.wireKey) }
    var prjTestCmd by remember { mutableStateOf("") }
    var prjProtectedCsv by remember { mutableStateOf("metra_old, metra_legacy") }
    var prjSharedFile by remember { mutableStateOf("docs/AGENT_WORKPLAN.md") }
    var prjRequirements by remember { mutableStateOf("") }
    var prjInstructions by remember { mutableStateOf("") }
    var prjCollabEnabled by remember { mutableStateOf(true) }
    var prjContinuousEnabled by remember { mutableStateOf(true) }
    var prjLinkedGhId by remember(workspace.linkedAccounts.gitHubAccounts) {
        mutableStateOf(workspace.linkedAccounts.gitHubAccounts.firstOrNull { it.isActive }?.id.orEmpty())
    }
    var prjLinkedDbId by remember(workspace.platformConfig.databaseConnections) {
        mutableStateOf(workspace.platformConfig.databaseConnections.firstOrNull { it.isActive }?.id.orEmpty())
    }

    var ghLabel by remember { mutableStateOf("") }
    var ghRepo by remember { mutableStateOf(settings.activeRepo) }
    var ghToken by remember { mutableStateOf("") }

    var bundleName by remember { mutableStateOf("") }
    var bundleRepo by remember(settings.activeRepo) { mutableStateOf(settings.activeRepo) }
    var bundleBranch by remember(settings.activeBranch) { mutableStateOf(settings.activeBranch) }
    var selectedGhId by remember(workspace.linkedAccounts.gitHubAccounts) {
        mutableStateOf(
            workspace.linkedAccounts.gitHubAccounts.firstOrNull { it.isActive }?.id
                ?: workspace.linkedAccounts.gitHubAccounts.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedBeId by remember(workspace.linkedAccounts.backendProfiles) {
        mutableStateOf(
            workspace.linkedAccounts.backendProfiles.firstOrNull { it.isActive }?.id
                ?: workspace.linkedAccounts.backendProfiles.firstOrNull()?.id.orEmpty()
        )
    }

    fun launchGoogleSwitch() {
        isSwitchingGoogle = true
        googleSwitchError = null
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {
                }
            }
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isSwitchingGoogle = false
                    onGoogleAccountSwitched()
                },
                onAuthError = { err ->
                    isSwitchingGoogle = false
                    googleSwitchError = err
                },
                scope = scope,
                onAuthCancelled = {
                    isSwitchingGoogle = false
                }
            )
        }
    }

    val snapshot = workspace.linkedAccounts
    val pConfig = workspace.platformConfig

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 0: Universal Multi-Project Profiles
        item {
            CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إدارة المشاريع البرمجية", style = MaterialTheme.typography.titleLarge)
                    StatusPill(label = "${pConfig.projects.size} مشاريع", color = ElectricCyan)
                }
                Text(
                    text = "أنشئ ملفاً لأي مشروع، واربطه بحساب GitHub وقاعدة بيانات وأولوية وكلاء AI الخاصة به.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                pConfig.projects.forEach { prj ->
                    val isActive = prj.id == pConfig.activeProject?.id
                    CommandCard(
                        borderColor = if (isActive) EmeraldSuccess.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prj.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${prj.repoSlug} (${prj.defaultBranch}) • ${com.example.core.security.TechStackType.fromWireKey(prj.techStack).arabicLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isActive) {
                                StatusPill(label = "نشط", color = EmeraldSuccess, icon = Icons.Rounded.CheckCircle)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                Button(
                                    onClick = { onActivateProject(prj.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تفعيل المشروع")
                                }
                            } else {
                                Button(
                                    onClick = onOpenSharedWorkspace,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ملف الخطة والتعاون")
                                }
                            }
                            OutlinedButton(
                                onClick = onGoToAiSetup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("أولوية AI")
                            }
                            if (pConfig.projects.size > 1) {
                                IconButton(onClick = { onRemoveProject(prj.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف المشروع", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = prjName,
                    onValueChange = { prjName = it },
                    label = { Text("اسم المشروع الجديد") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input")
                )
                OutlinedTextField(
                    value = prjRepo,
                    onValueChange = { prjRepo = it },
                    label = { Text("مستودع GitHub (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_repo_input")
                )
                OutlinedTextField(
                    value = prjBranch,
                    onValueChange = { prjBranch = it },
                    label = { Text("الفرع الافتراضي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.core.security.TechStackType.entries.forEach { stack ->
                        FilterChip(
                            selected = prjStack == stack.wireKey,
                            onClick = { prjStack = stack.wireKey },
                            label = { Text(stack.arabicLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (snapshot.gitHubAccounts.isNotEmpty()) {
                    Text("ربط بحساب GitHub:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        snapshot.gitHubAccounts.forEach { gh ->
                            FilterChip(
                                selected = prjLinkedGhId == gh.id,
                                onClick = { prjLinkedGhId = gh.id },
                                label = { Text("@${gh.login}") }
                            )
                        }
                    }
                }
                if (pConfig.databaseConnections.isNotEmpty()) {
                    Text("ربط بقاعدة بيانات:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pConfig.databaseConnections.forEach { db ->
                            FilterChip(
                                selected = prjLinkedDbId == db.id,
                                onClick = { prjLinkedDbId = db.id },
                                label = { Text(db.label) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = prjProtectedCsv,
                    onValueChange = { prjProtectedCsv = it },
                    label = { Text("قواعد بيانات محمية خاصة بهذا المشروع (CSV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prjSharedFile,
                    onValueChange = { prjSharedFile = it },
                    label = { Text("ملف العمل الواحد داخل مجلد المستودع") },
                    placeholder = { Text("docs/AGENT_WORKPLAN.md") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_shared_file_input")
                )
                OutlinedTextField(
                    value = prjRequirements,
                    onValueChange = { prjRequirements = it },
                    label = { Text("متطلبات العمل اللازمة للمشروع الجديد") },
                    placeholder = { Text("اذكر المزايا، الشاشات، الـ APIs، وشروط النجاح لهذا المشروع...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_requirements_input")
                )
                OutlinedTextField(
                    value = prjInstructions,
                    onValueChange = { prjInstructions = it },
                    label = { Text("تعليمات خاصة للوكيل والنماذج في هذا المشروع") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("العمل التعاوني لعدة نماذج معاً على الملف المشترك", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = prjCollabEnabled, onCheckedChange = { prjCollabEnabled = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("متابعة المهام للنهاية مع التحويل التلقائي للبدائل", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = prjContinuousEnabled, onCheckedChange = { prjContinuousEnabled = it })
                }
                Button(
                    onClick = {
                        onCreateProject(
                            prjName,
                            prjRepo,
                            prjBranch,
                            prjStack,
                            prjTestCmd,
                            prjLinkedGhId,
                            prjLinkedDbId,
                            prjProtectedCsv,
                            prjInstructions,
                            prjSharedFile,
                            prjRequirements,
                            prjCollabEnabled,
                            prjContinuousEnabled
                        )
                        prjName = ""
                        prjRepo = ""
                        prjRequirements = ""
                        prjInstructions = ""
                    },
                    enabled = prjName.isNotBlank() || prjRepo.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_project_button")
                ) {
                    Text("حفظ وتفعيل المشروع بمتطلباته")
                }
            }
        }

        // Section 1: Google Accounts & Switching
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
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = ElectricCyan)
                        Text("حسابات Google المرتبطة", style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(label = "${snapshot.googleAccounts.size.coerceAtLeast(1)} مسجل", color = ElectricCyan)
                }

                val activeEmail = workspace.currentGoogleEmail.ifBlank {
                    currentUserId
                }
                val activeName = workspace.currentGoogleName.ifBlank {
                    "حساب AI I.Z"
                }

                Text(
                    text = "الحساب النشط حالياً: $activeName ($activeEmail)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "بيانات المهام والمحادثات السحابية معزولة لكل حساب Google، بينما تبقى الحسابات المربوطة على هذا الجهاز مشفرة عبر Android Keystore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                snapshot.googleAccounts.forEach { gAccount ->
                    val isCurrent = gAccount.uid == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gAccount.displayName.ifBlank { "مستخدم Google" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = gAccount.email.ifBlank { gAccount.uid },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCurrent) {
                            StatusPill(label = "نشط", color = EmeraldSuccess, icon = Icons.Rounded.CheckCircle)
                        } else {
                            OutlinedButton(
                                onClick = { launchGoogleSwitch() },
                                enabled = !isSwitchingGoogle
                            ) {
                                Text("تبديل")
                            }
                        }
                    }
                }

                if (!googleSwitchError.isNullOrBlank()) {
                    Text(
                        text = googleSwitchError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }

                Button(
                    onClick = { launchGoogleSwitch() },
                    enabled = !isSwitchingGoogle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("switch_google_account_button")
                ) {
                    if (isSwitchingGoogle) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                        Text("تبديل أو ربط حساب Google آخر")
                    }
                }
            }
        }

        // Section 2: Multiple GitHub Accounts
        item {
            CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Source, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("حسابات GitHub المتعددة", style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(
                        label = "${snapshot.gitHubAccounts.size} مربوط",
                        color = if (snapshot.gitHubAccounts.isNotEmpty()) EmeraldSuccess else AmberWarning
                    )
                }
                Text(
                    text = "يمكنك ربط أكثر من حساب GitHub (شخصي، مؤسسة، عميل) وربط كل حساب بمستودع مفضل ليتم التبديل تلقائياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (snapshot.gitHubAccounts.isEmpty()) {
                    Text(
                        text = "لا توجد حسابات GitHub مضافة بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                } else {
                    snapshot.gitHubAccounts.forEach { gh ->
                        CommandCard(
                            borderColor = if (gh.isActive) EmeraldSuccess.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${gh.label} (@${gh.login})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (gh.defaultRepo.isNotBlank()) {
                                        Text(
                                            text = "المستودع المرتبط: ${gh.defaultRepo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                                if (gh.isActive) {
                                    StatusPill(label = "نشط", color = EmeraldSuccess, icon = Icons.Rounded.CheckCircle)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!gh.isActive) {
                                    Button(
                                        onClick = { onSwitchGitHubAccount(gh.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("switch_github_${gh.id}")
                                    ) {
                                        Text("تفعيل الحساب")
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveGitHubAccount(gh.id) },
                                    modifier = Modifier.testTag("remove_github_${gh.id}")
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "إزالة حساب GitHub", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = ghLabel,
                    onValueChange = { ghLabel = it },
                    label = { Text("اسم تعريفي للحساب (مثلاً: حساب METRA أو حساب العمل)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ghRepo,
                    onValueChange = { ghRepo = it },
                    label = { Text("ربط بمستودع تلقائياً (اختياري — owner/repo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ghToken,
                    onValueChange = { ghToken = it },
                    label = { Text("GitHub Personal Access Token (PAT)") },
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("linked_github_pat_input")
                )
                Button(
                    onClick = {
                        onAddGitHubAccount(ghToken, ghLabel, ghRepo)
                        ghToken = ""
                        ghLabel = ""
                    },
                    enabled = !workspace.isBusyGitHub && ghToken.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_github_account_button")
                ) {
                    if (workspace.isBusyGitHub) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AddLink, contentDescription = null)
                        Text("إضافة وربط حساب GitHub")
                    }
                }
            }
        }

        // Section 3: AI & Backend Runner Profiles
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
                        Text("بيئات الذكاء الاصطناعي وBackend", style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(label = "${snapshot.backendProfiles.size} محفوظة", color = ElectricCyan)
                }

                if (snapshot.backendProfiles.isEmpty()) {
                    Text(
                        text = "يمكنك حفظ عدة مفاتيح OpenAI أو روابط Backend (مثل بيئة Staging وبيئة Flutter) من إعدادات المزود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    snapshot.backendProfiles.forEach { profile ->
                        CommandCard(
                            borderColor = if (profile.isActive) EmeraldSuccess.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(profile.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${profile.aiProvider} (${profile.aiModel}) • ${profile.backendUrl.ifBlank { "بدون Backend" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (profile.isActive) {
                                    StatusPill(label = "نشط", color = EmeraldSuccess)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!profile.isActive) {
                                    Button(
                                        onClick = { onSwitchBackendProfile(profile.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تفعيل البيئة")
                                    }
                                }
                                IconButton(onClick = { onRemoveBackendProfile(profile.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف البيئة", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onGoToAiSetup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة أو تعديل بيئة AI / Backend جديدة")
                }
            }
        }

        // Section 4: Unified Cross-Account Bundles
        item {
            CommandCard(borderColor = EmeraldSuccess.copy(alpha = 0.45f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Hub, contentDescription = null, tint = EmeraldSuccess)
                    Text("حزم الربط الموحدة (ربط الحسابات معاً)", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "اربط حساب Google الحالي + حساب GitHub + بيئة AI/Backend + المستودع في حزمة واحدة لتبديل كامل بيئة العمل بضغطة واحدة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                snapshot.bundles.forEach { bundle ->
                    val needsGoogleSwitch = bundle.googleUid.isNotBlank() && bundle.googleUid != currentUserId
                    CommandCard(
                        borderColor = if (bundle.isActive) EmeraldSuccess.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bundle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (bundle.isActive) {
                                StatusPill(label = "حزمة نشطة", color = EmeraldSuccess, icon = Icons.Rounded.CheckCircle)
                            }
                        }
                        Text(
                            text = "Google: ${bundle.googleEmail.ifBlank { bundle.googleUid }} • GitHub: @${bundle.gitHubLogin.ifBlank { "غير محدد" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "المستودع: ${bundle.repoSlug} (${bundle.branch}) • البيئة: ${bundle.backendLabel.ifBlank { "الافتراضية" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (needsGoogleSwitch) {
                            Text(
                                text = "ملاحظة: هذه الحزمة مرتبطة بحساب Google مختلف (${bundle.googleEmail}).",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onActivateBundle(bundle.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("activate_bundle_${bundle.id}")
                            ) {
                                Text("تفعيل الحزمة")
                            }
                            if (needsGoogleSwitch) {
                                OutlinedButton(onClick = { launchGoogleSwitch() }) {
                                    Text("تبديل Google")
                                }
                            }
                            IconButton(onClick = { onRemoveBundle(bundle.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف الحزمة", tint = CrimsonError)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bundleName,
                    onValueChange = { bundleName = it },
                    label = { Text("اسم الحزمة الموحدة (مثلاً: حزمة METRA الأساسية)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bundle_name_input")
                )

                if (snapshot.gitHubAccounts.isNotEmpty()) {
                    Text("اختر حساب GitHub للحزمة:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snapshot.gitHubAccounts.forEach { gh ->
                            FilterChip(
                                selected = selectedGhId == gh.id,
                                onClick = {
                                    selectedGhId = gh.id
                                    if (gh.defaultRepo.isNotBlank()) bundleRepo = gh.defaultRepo
                                },
                                label = { Text("${gh.label} (@${gh.login})") }
                            )
                        }
                    }
                }

                if (snapshot.backendProfiles.isNotEmpty()) {
                    Text("اختر بيئة AI / Backend للحزمة:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snapshot.backendProfiles.forEach { be ->
                            FilterChip(
                                selected = selectedBeId == be.id,
                                onClick = { selectedBeId = be.id },
                                label = { Text(be.label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = bundleRepo,
                    onValueChange = { bundleRepo = it },
                    label = { Text("المستودع المرتبط (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bundleBranch,
                    onValueChange = { bundleBranch = it },
                    label = { Text("الفرع الافتراضي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onCreateBundle(
                            bundleName,
                            selectedGhId,
                            selectedBeId,
                            bundleRepo,
                            bundleBranch
                        )
                        bundleName = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_bundle_button")
                ) {
                    Icon(Icons.Rounded.Hub, contentDescription = null)
                    Text("حفظ وتفعيل حزمة الربط الموحدة")
                }
            }
        }
    }
}
