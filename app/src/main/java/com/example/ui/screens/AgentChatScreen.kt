package com.example.ui.screens

import com.example.ui.i18n.AppUiStrings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ChatMessage
import com.example.ui.components.ApprovalGateCard
import com.example.ui.components.CommandCard
import com.example.ui.theme.CrimsonError
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Screen 5: Agent Chat & Command Center
 */
@Composable
fun AgentChatScreen(
    messages: List<ChatMessage>,
    pendingApprovals: List<ApprovalRequest>,
    workspace: WorkspaceUiState,
    onSendCommand: (String) -> Unit,
    onRunTeamCollaboration: (String) -> Unit,
    onOpenSharedWorkspace: () -> Unit,
    onOpenAiConversations: () -> Unit,
    onApprove: (ApprovalRequest) -> Unit,
    onReject: (ApprovalRequest) -> Unit,
    appLanguage: String = "ar"
) {
    val uiLang = appLanguage
    var inputText by remember { mutableStateOf("") }
    val activeProject = workspace.platformConfig.activeProject
    val projectLabel = activeProject?.name?.ifBlank { activeProject.repoSlug }
        ?: (if (AppUiStrings.isEnglish(uiLang)) "Active project" else "المشروع النشط")
    val continuousLabel = if (activeProject?.continuousExecutionEnabled != false) "متابعة مستمرة للنهاية" else "عادي"

    val quickCommands = remember(projectLabel) {
        listOf(
            "تابع تطوير $projectLabel حتى إغلاق الأخطاء",
            "افحص مشروع $projectLabel كاملاً",
            "أصلح جميع أخطاء الاختبارات",
            "راجع آخر Pull Request",
            "افحص قاعدة البيانات وبنية الجداول",
            "راجع أمان الصلاحيات والثغرات",
            "أنشئ تقريراً هندسياً نهائياً",
            "تابع من آخر نقطة توقفت عندها"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المشروع: $projectLabel • ${workspace.activeAiEndpointLabel.ifBlank { "AI" }} • $continuousLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = onOpenAiConversations,
                    label = { Text(AppUiStrings.chatAiAccounts(uiLang), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chat_open_ai_conversations")
                )
                AssistChip(
                    onClick = onOpenSharedWorkspace,
                    leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(AppUiStrings.chatWorkFile(uiLang), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("chat_open_shared_workspace")
                )
            }
        }
        // Quick Commands Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onRunTeamCollaboration(inputText.ifBlank { "تحديث خطة العمل وتنفيذ متطلبات $projectLabel" }) },
                label = { Text(AppUiStrings.chatRunCollaboration(uiLang)) },
                leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = null) },
                modifier = Modifier.testTag("quick_cmd_team_collab")
            )
            quickCommands.forEachIndexed { idx, cmd ->
                AssistChip(
                    onClick = { onSendCommand(cmd) },
                    label = { Text(cmd) },
                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.testTag("quick_cmd_$idx")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && !workspace.isAiStreaming) {
                item {
                    CommandCard {
                        Text(AppUiStrings.chatAgentReady(uiLang, projectLabel), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "أرسل مهمة برمجية أو اختر أحد الأوامر السريعة أعلاه. يدعم الوكيل التحويل التلقائي بين حسابات AI عند انتهاء الحصة والعمل الخلفي مع التوقف عند أي موافقة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                val isUser = msg.role == "user"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = if (isUser) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isUser) "أنت" else "الوكيل البرمجي",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            if (workspace.isAiStreaming) {
                item {
                    CommandCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(AppUiStrings.chatAnalyzing(uiLang), style = MaterialTheme.typography.labelMedium)
                        }
                        if (workspace.aiStreamingResponse.isNotBlank()) {
                            Text(
                                text = workspace.aiStreamingResponse,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (!workspace.aiError.isNullOrBlank()) {
                item {
                    CommandCard(borderColor = CrimsonError) {
                        Text(
                            text = workspace.aiError,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError
                        )
                    }
                }
            }

            if (pendingApprovals.isNotEmpty()) {
                items(pendingApprovals, key = { "chat_appr_${it.id}" }) { approval ->
                    ApprovalGateCard(
                        approval = approval,
                        onApprove = { onApprove(approval) },
                        onReject = { onReject(approval) }
                    )
                }
            }
        }

        // Message Composer
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(AppUiStrings.chatPlaceholder(uiLang)) },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_prompt_input")
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendCommand(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !workspace.isAiStreaming,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال")
                }
            }
        }
    }
}
