package com.example.ui.screens

import com.example.ui.i18n.AppUiStrings

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.AiEndpointConfig
import com.example.domain.model.UserSettings
import com.example.ui.components.CommandCard
import com.example.ui.components.LtrCodeBox
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Shared Repository Workplan & Multi-Model Collaboration Workspace:
 * Multiple AI models/accounts read, collaborate, update plan + instructions + requirements
 * inside a single file in the repository folder, with automatic failover and continuous execution.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedWorkspaceScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onSaveProjectSpec: (
        sharedWorkFilePath: String,
        requirements: String,
        instructions: String,
        collaborativeMode: Boolean,
        continuousExecution: Boolean
    ) -> Unit,
    onToggleTeamMember: (endpointId: String, selected: Boolean) -> Unit,
    onLoadOrInitSharedFile: (String) -> Unit,
    onEditSharedFileDraft: (String) -> Unit,
    onRunCollaboration: (String) -> Unit,
    onRequestCommitSharedFile: (String) -> Unit,
    onGoToAiSetup: () -> Unit
) {
    val uiLang = settings.appLanguage
    val activeProject = workspace.platformConfig.activeProject
    val endpoints = workspace.platformConfig.aiEndpoints
    val selectedTeamIds = remember(activeProject?.collaborativeEndpointIds, endpoints) {
        val raw = activeProject?.collaborativeEndpointIds.orEmpty()
        if (raw.isNotEmpty()) raw.toSet()
        else activeProject?.aiPriorityOrder?.toSet() ?: endpoints.map { it.id }.toSet()
    }

    var filePathInput by remember(activeProject?.id, workspace.sharedWorkFilePath) {
        mutableStateOf(activeProject?.sharedWorkFilePath ?: workspace.sharedWorkFilePath)
    }
    var requirementsInput by remember(activeProject?.id, activeProject?.projectRequirements) {
        mutableStateOf(activeProject?.projectRequirements.orEmpty())
    }
    var instructionsInput by remember(activeProject?.id, activeProject?.systemInstructions) {
        mutableStateOf(activeProject?.systemInstructions.orEmpty())
    }
    var collaborativeEnabled by remember(activeProject?.id, activeProject?.collaborativeModeEnabled) {
        mutableStateOf(activeProject?.collaborativeModeEnabled ?: true)
    }
    var continuousEnabled by remember(activeProject?.id, activeProject?.continuousExecutionEnabled) {
        mutableStateOf(activeProject?.continuousExecutionEnabled ?: true)
    }

    var collaborationDirective by remember { mutableStateOf("") }
    var commitMessage by remember { mutableStateOf("Update collaborative workplan and instructions") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Shared Repo File & Continuous Mode Spec
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = ElectricCyan)
                        Text(AppUiStrings.workspaceTitle(uiLang), style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(
                        label = if (continuousEnabled) AppUiStrings.workspaceContinuousOn(uiLang) else AppUiStrings.workspaceContinuousOff(uiLang),
                        color = if (continuousEnabled) EmeraldSuccess else AmberWarning
                    )
                }
                Text(
                    text = "تعمل عدة نماذج معاً على ملف واحد داخل مجلد المستودع؛ تضع المتطلبات وخطة العمل والتعليمات ويُعدّل كل نموذج على نفس الملف مع التحويل التلقائي عند انتهاء أي حصة أو توقف أي نموذج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = filePathInput,
                    onValueChange = { filePathInput = it },
                    label = { Text(AppUiStrings.workspaceFilePath(uiLang)) },
                    placeholder = { Text("docs/AGENT_WORKPLAN.md") },
                    leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shared_work_file_path_input")
                )

                OutlinedTextField(
                    value = requirementsInput,
                    onValueChange = { requirementsInput = it },
                    label = { Text(AppUiStrings.workspaceRequirements(uiLang)) },
                    placeholder = { Text(AppUiStrings.workspaceRequirementsPh(uiLang)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_requirements_input")
                )

                OutlinedTextField(
                    value = instructionsInput,
                    onValueChange = { instructionsInput = it },
                    label = { Text(AppUiStrings.workspaceAgentInstructions(uiLang)) },
                    placeholder = { Text(AppUiStrings.workspaceAgentPlaceholder(uiLang)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_instructions_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppUiStrings.workspaceCollabMulti(uiLang), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "المخطط ← المنفذ ← المدقق على نفس الملف المشترك",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = collaborativeEnabled,
                        onCheckedChange = { collaborativeEnabled = it },
                        modifier = Modifier.testTag("toggle_collaborative_mode")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppUiStrings.workspaceContinuousFollow(uiLang), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "تجاوز انتهاء الحصة/الحساب/النموذج واستئناف المهام بعد أي موافقة تلقائياً",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = continuousEnabled,
                        onCheckedChange = { continuousEnabled = it },
                        modifier = Modifier.testTag("toggle_continuous_mode")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveProjectSpec(
                                filePathInput,
                                requirementsInput,
                                instructionsInput,
                                collaborativeEnabled,
                                continuousEnabled
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_workspace_spec_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Text(AppUiStrings.workspaceSaveSpecs(uiLang))
                    }
                    OutlinedButton(
                        onClick = { onLoadOrInitSharedFile(filePathInput) },
                        enabled = !workspace.isBusyGitHub,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_shared_file_button")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Text(AppUiStrings.workspaceFetchGithub(uiLang))
                    }
                }
            }
        }

        // Team Selection Card
        item {
            CommandCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppUiStrings.workspaceTeamModels(uiLang, selectedTeamIds.size), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onGoToAiSetup) {
                        Text(AppUiStrings.workspaceAddModels(uiLang))
                    }
                }
                Text(
                    text = "اختر نموذجين أو أكثر ليعملوا بالتسلسل والتعاون على نفس الملف، وإذا توقف أي نموذج أو انتهت حصته يتحول دوره تلقائياً للتالي:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    endpoints.forEach { ep ->
                        val isSelected = ep.id in selectedTeamIds
                        val isExhausted = ep.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleTeamMember(ep.id, !isSelected) },
                            label = {
                                Text(
                                    text = "${ep.label} (${ep.modelName})" + if (isExhausted) " [حصة منتهية]" else "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("collab_member_${ep.id}")
                        )
                    }
                }
            }
        }

        // Run Multi-Model Collaboration Card
        item {
            CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)) {
                Text(AppUiStrings.workspaceRunRoundNow(uiLang), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = collaborationDirective,
                    onValueChange = { collaborationDirective = it },
                    label = { Text(AppUiStrings.workspaceRoundGoal(uiLang)) },
                    placeholder = { Text(AppUiStrings.workspaceRoundGoalPh(uiLang)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collab_directive_input")
                )
                if (workspace.collaborationStatusText.isNotBlank()) {
                    Text(
                        text = workspace.collaborationStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan
                    )
                }
                if (!workspace.aiError.isNullOrBlank()) {
                    Text(
                        text = workspace.aiError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }
                Button(
                    onClick = {
                        onSaveProjectSpec(
                            filePathInput,
                            requirementsInput,
                            instructionsInput,
                            collaborativeEnabled,
                            continuousEnabled
                        )
                        onRunCollaboration(collaborationDirective)
                    },
                    enabled = !workspace.isCollaborating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_collaboration_button")
                ) {
                    if (workspace.isCollaborating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text(AppUiStrings.workspaceStartCollab(uiLang))
                    }
                }
            }
        }

        // Last Round Breakdown
        if (workspace.lastCollaborationContributions.isNotEmpty()) {
            item {
                CommandCard(borderColor = EmeraldSuccess.copy(alpha = 0.5f)) {
                    Text(AppUiStrings.workspaceModelContributions(uiLang), style = MaterialTheme.typography.titleMedium)
                    workspace.lastCollaborationContributions.forEach { contrib ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val failoverTag = if (contrib.requestedEndpointLabel != contrib.actualEndpointLabel) {
                                " ⇄ تحوّل تلقائياً إلى ${contrib.actualEndpointLabel}"
                            } else ""
                            Text(
                                text = "${contrib.roleTitle} • [${contrib.actualEndpointLabel} / ${contrib.actualModelName}]$failoverTag",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Text(
                                text = contrib.outputText.take(650),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Editable Shared Markdown Content + Diff + Commit Approval
        item {
            CommandCard {
                Text(
                    text = "محتوى الملف المشترك: ${workspace.sharedWorkFilePath}",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = workspace.sharedWorkFileDraft,
                    onValueChange = onEditSharedFileDraft,
                    label = { Text(AppUiStrings.workspaceDirectEdit(uiLang)) },
                    minLines = 10,
                    maxLines = 20,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shared_work_file_editor")
                )

                if (workspace.sharedWorkFileDiff.isNotBlank()) {
                    Text(AppUiStrings.workspacePreviewChanges(uiLang), style = MaterialTheme.typography.labelLarge)
                    LtrCodeBox(
                        code = workspace.sharedWorkFileDiff.take(2500),
                        highlightDiff = true
                    )
                }

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text(AppUiStrings.workspaceCommitMsg(uiLang, settings.activeRepo, settings.activeBranch)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onRequestCommitSharedFile(commitMessage) },
                    enabled = workspace.isGitHubConnected && workspace.sharedWorkFileDraft.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("commit_shared_work_file_button")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Text(AppUiStrings.workspaceRequestPush(uiLang))
                }
                if (!workspace.isGitHubConnected) {
                    Text(
                        text = "ملاحظة: اربط GitHub PAT لحفظ هذا الملف داخل المستودع مباشرة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
            }
        }
    }
}
