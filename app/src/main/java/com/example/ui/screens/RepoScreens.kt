package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.GitHubPullRequest
import com.example.domain.model.UserSettings
import com.example.ui.components.CommandCard
import com.example.ui.components.LtrCodeBox
import com.example.ui.components.SetupRequiredBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Screen 7: Project File Explorer
 */
@Composable
fun FileExplorerScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onOpenFile: (String) -> Unit,
    onDownloadFile: (String) -> Unit,
    onGoToGitHubSetup: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(workspace.treeItems, searchQuery) {
        workspace.treeItems.filter {
            searchQuery.isBlank() || it.path.contains(searchQuery, ignoreCase = true)
        }.take(200)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!workspace.isGitHubConnected) {
            item {
                SetupRequiredBanner(
                    title = "مستكشف الملفات غير مربوط",
                    description = "اربط GitHub PAT لعرض وتنزيل ملفات مستودع ${settings.activeRepo}.",
                    actionLabel = "ربط GitHub الآن",
                    onAction = onGoToGitHubSetup,
                    testTag = "files_connect_github"
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث في ملفات ${settings.activeRepo} (${settings.activeBranch})") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_search_input")
            )
        }

        items(filtered, key = { "${it.type}_${it.path}" }) { item ->
            val isTree = item.type == "tree"
            CommandCard(
                modifier = Modifier.clickable(enabled = !isTree) { onOpenFile(item.path) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isTree) Icons.Rounded.Folder else Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (isTree) AmberWarning else ElectricCyan
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.path, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (isTree) "مجلد" else "ملف • ${item.size ?: 0} بايت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isTree) {
                        IconButton(
                            onClick = { onDownloadFile(item.path) },
                            enabled = !workspace.isDownloadingFile,
                            modifier = Modifier.testTag("download_file_${item.path}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "تنزيل الملف إلى الهاتف",
                                tint = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 8: Code Viewer / Editor & Before/After Diff Reviewer
 */
@Composable
fun CodeEditorScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onEditContent: (String) -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onDownloadFile: (String) -> Unit,
    onRequestCommitApproval: (String) -> Unit,
    onGoToFiles: () -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }
    var showDiffOnly by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (workspace.selectedFilePath.isBlank()) {
            item {
                CommandCard {
                    Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("لم يتم اختيار ملف بعد", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "اختر ملفاً من مستكشف الملفات لعرض محتواه، تحريره، تنزيله على الهاتف، ومعاينة الفروقات قبل/بعد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onGoToFiles, modifier = Modifier.fillMaxWidth()) {
                        Text("فتح مستكشف الملفات")
                    }
                }
            }
        } else {
            item {
                CommandCard {
                    Text(workspace.selectedFilePath, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "الفرع: ${settings.activeBranch} • SHA: ${workspace.selectedFileSha.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAnalyzeWithAi,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analyze_code_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Text("مراجعة AI")
                        }
                        OutlinedButton(
                            onClick = { showDiffOnly = !showDiffOnly },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (showDiffOnly) "عرض المحرر" else "مقارنة قبل/بعد")
                        }
                    }
                    OutlinedButton(
                        onClick = { onDownloadFile(workspace.selectedFilePath) },
                        enabled = !workspace.isDownloadingFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_current_file_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Text(if (workspace.isDownloadingFile) "جارٍ التحميل..." else "تنزيل الملف مباشرة إلى الهاتف")
                    }
                }
            }

            if (workspace.codeReviewAnalysis.isNotBlank()) {
                item {
                    CommandCard(borderColor = ElectricCyan.copy(alpha = 0.5f)) {
                        Text("نتيجة مراجعة الذكاء الاصطناعي", style = MaterialTheme.typography.titleMedium)
                        Text(workspace.codeReviewAnalysis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                if (showDiffOnly) {
                    LtrCodeBox(
                        code = workspace.fileDiffPreview.ifBlank { "لا توجد تعديلات مقارنة بالملف الأصلي." },
                        highlightDiff = true
                    )
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = workspace.editedFileContent,
                            onValueChange = onEditContent,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("code_editor_textfield")
                        )
                    }
                }
            }

            item {
                CommandCard {
                    Text("رفع التعديل (المستوى 2 — يتطلب موافقة)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("رسالة Commit") },
                        placeholder = { Text("fix: resolve issue in ${workspace.selectedFilePath}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onRequestCommitApproval(commitMessage) },
                        enabled = workspace.editedFileContent != workspace.originalFileContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_commit_approval_button")
                    ) {
                        Text("طلب موافقة Push إلى GitHub")
                    }
                }
            }
        }
    }
}

/**
 * Screen 9: GitHub Branches & Commits
 */
@Composable
fun BranchesCommitsScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onSelectBranch: (String) -> Unit,
    onCreateBranch: (String) -> Unit,
    onGoToGitHubSetup: () -> Unit
) {
    var newBranchName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!workspace.isGitHubConnected) {
            item {
                SetupRequiredBanner(
                    title = "ربط GitHub مطلوب",
                    description = "اربط حساب GitHub لعرض الفروع وسجل الـ Commits الحقيقي.",
                    actionLabel = "إعداد GitHub",
                    onAction = onGoToGitHubSetup,
                    testTag = "branches_connect_github"
                )
            }
        }

        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = ElectricCyan)
                    Text("إنشاء فرع عمل جديد (المستوى 1)", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = newBranchName,
                    onValueChange = { newBranchName = it },
                    label = { Text("اسم الفرع الجديد (تفرع من ${settings.activeBranch})") },
                    placeholder = { Text("agent/fix-metra-tests") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newBranchName.isNotBlank()) {
                            onCreateBranch(newBranchName)
                            newBranchName = ""
                        }
                    },
                    enabled = workspace.isGitHubConnected && newBranchName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_branch_button")
                ) {
                    Text("إنشاء الفرع")
                }
            }
        }

        item {
            Text("الفروع (${workspace.branches.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(workspace.branches, key = { it.name }) { branch ->
            val selected = branch.name == settings.activeBranch
            CommandCard(
                modifier = Modifier.clickable { onSelectBranch(branch.name) },
                borderColor = if (selected) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(branch.name, style = MaterialTheme.typography.titleMedium)
                    StatusPill(
                        label = if (selected) "نشط" else branch.commit.sha.take(7),
                        color = if (selected) EmeraldSuccess else ElectricCyan
                    )
                }
            }
        }

        item {
            Text("أحدث Commits (${workspace.commits.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(workspace.commits, key = { it.sha }) { commit ->
            CommandCard {
                Text(
                    text = commit.commit.message.lines().firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${commit.sha.take(7)} • ${commit.commit.author.name} • ${commit.commit.author.date.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Screen 10: Pull Requests & Review
 */
@Composable
fun PullRequestsScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onInspectPr: (GitHubPullRequest) -> Unit,
    onRequestCreateDraftPr: (title: String, head: String, base: String, body: String) -> Unit,
    onRequestMergePr: (GitHubPullRequest) -> Unit
) {
    var prTitle by remember { mutableStateOf("") }
    var prHead by remember(settings.activeBranch) { mutableStateOf(settings.activeBranch) }
    var prBase by remember { mutableStateOf("main") }
    var prBody by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.MergeType, contentDescription = null, tint = ElectricCyan)
                    Text("إنشاء Draft Pull Request (المستوى 2)", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = prTitle,
                    onValueChange = { prTitle = it },
                    label = { Text("عنوان PR") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prHead,
                        onValueChange = { prHead = it },
                        label = { Text("من فرع") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prBase,
                        onValueChange = { prBase = it },
                        label = { Text("إلى فرع") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = prBody,
                    onValueChange = { prBody = it },
                    label = { Text("ملخص التغييرات") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onRequestCreateDraftPr(prTitle, prHead, prBase, prBody)
                        prTitle = ""
                        prBody = ""
                    },
                    enabled = prTitle.isNotBlank() && workspace.isGitHubConnected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("طلب موافقة إنشاء Draft PR")
                }
            }
        }

        if (workspace.prReviewAnalysis.isNotBlank()) {
            item {
                CommandCard(borderColor = ElectricCyan) {
                    Text("مراجعة الذكاء الاصطناعي لـ Pull Request", style = MaterialTheme.typography.titleMedium)
                    Text(workspace.prReviewAnalysis, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (workspace.selectedPrFiles.isNotEmpty()) {
            item {
                Text("ملفات التغيير في PR المحدد", style = MaterialTheme.typography.titleMedium)
            }
            items(workspace.selectedPrFiles, key = { it.filename }) { file ->
                CommandCard {
                    Text("${file.filename} (+${file.additions} / -${file.deletions})", style = MaterialTheme.typography.labelLarge)
                    if (!file.patch.isNullOrBlank()) {
                        LtrCodeBox(code = file.patch, highlightDiff = true)
                    }
                }
            }
        }

        items(workspace.pullRequests, key = { it.id }) { pr ->
            CommandCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#${pr.number} ${pr.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusPill(
                        label = if (pr.draft) "Draft" else pr.state,
                        color = if (pr.state == "open") EmeraldSuccess else ElectricCyan
                    )
                }
                Text(
                    text = "${pr.head.ref} ◀ ${pr.base.ref} • بواسطة ${pr.user.login}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onInspectPr(pr) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مراجعة الملفات وAI")
                    }
                    if (pr.state == "open") {
                        Button(
                            onClick = { onRequestMergePr(pr) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("طلب دمج (T3)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 11: GitHub Actions Runs
 */
@Composable
fun ActionsScreen(
    workspace: WorkspaceUiState,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = ElectricCyan)
                    Text("سجل GitHub Actions", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("تحديث")
                }
            }
        }

        if (workspace.workflowRuns.isEmpty()) {
            item {
                CommandCard {
                    Text("لا توجد تشغيلات Actions متاحة حالياً", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "تأكد من ربط GitHub ووجود ملفات Workflow داخل المستودع.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(workspace.workflowRuns, key = { it.id }) { run ->
            val color = when (run.conclusion) {
                "success" -> EmeraldSuccess
                "failure", "timed_out" -> CrimsonError
                else -> AmberWarning
            }
            CommandCard(borderColor = color.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = run.name ?: "Workflow #${run.runNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        label = run.conclusion ?: run.status ?: "running",
                        color = color
                    )
                }
                Text(
                    text = "الفرع: ${run.headBranch ?: "-"} • رقم التشغيل: #${run.runNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
