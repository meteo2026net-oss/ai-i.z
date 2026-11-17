package com.example.ui.screens

import com.example.ui.i18n.AppUiStrings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.core.security.AiEndpointConfig
import com.example.core.security.AiProviderType
import com.example.domain.model.ChatMessage
import com.example.domain.model.UserSettings
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.WorkspaceUiState

/**
 * Screen for viewing all AI accounts (ChatGPT, Gemini, Anthropic, etc.)
 * and browsing their previous conversations, account info, and sync history.
 */
@Composable
fun AiAccountConversationsScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    chatMessages: List<ChatMessage>,
    onGoToAiSetup: () -> Unit
) {
    val uiLang = settings.appLanguage
    val context = LocalContext.current
    val endpoints = workspace.platformConfig.aiEndpoints
    var selectedEndpointId by remember(endpoints) {
        mutableStateOf(endpoints.firstOrNull()?.id.orEmpty())
    }
    var copiedNotice by remember { mutableStateOf(false) }

    val selectedEndpoint = endpoints.find { it.id == selectedEndpointId } ?: endpoints.firstOrNull()
    val providerType = selectedEndpoint?.let { AiProviderType.fromWireKey(it.providerType) } ?: AiProviderType.FIREBASE_GEMINI

    val filteredMessages = remember(chatMessages, selectedEndpoint) {
        chatMessages
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_account_conversations_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.55f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ElectricCyan)
                        Text(AppUiStrings.aiConvTitle(uiLang), style = MaterialTheme.typography.titleLarge)
                    }
                    StatusPill(
                        label = AppUiStrings.aiConvAccountsCount(uiLang, endpoints.size),
                        color = EmeraldSuccess
                    )
                }
                Text(
                    text = "عرض معلومات كل حساب (ChatGPT، Google Gemini، Claude، وغيرها) واستعراض سجل المحادثات والأسئلة السابقة التي تم إجراؤها عبر التطبيق والمحرك المدمج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onGoToAiSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conversations_goto_ai_setup_btn")
                ) {
                    Text(AppUiStrings.aiConvManageAccounts(uiLang))
                }
            }
        }

        // Account Selector Filter Chips
        item {
            CommandCard {
                Text(AppUiStrings.aiConvPickAccount(uiLang), style = MaterialTheme.typography.titleMedium)
                if (endpoints.isEmpty()) {
                    Text(AppUiStrings.aiConvNoAccounts(uiLang), style = MaterialTheme.typography.bodySmall, color = AmberWarning)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        endpoints.forEach { ep ->
                            val isSelected = ep.id == selectedEndpoint?.id
                            val type = AiProviderType.fromWireKey(ep.providerType)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedEndpointId = ep.id },
                                label = { Text("${ep.label} (${type.arabicLabel.take(12)})") },
                                leadingIcon = {
                                    Icon(
                                        if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 2.dp)
                                    )
                                },
                                modifier = Modifier.testTag("account_chip_${ep.id}")
                            )
                        }
                    }
                }
            }
        }

        // Selected Account Details Card
        if (selectedEndpoint != null) {
            item {
                val statusColor = when {
                    !selectedEndpoint.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    selectedEndpoint.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED -> CrimsonError
                    selectedEndpoint.quotaStatus == AiEndpointConfig.QUOTA_ERROR -> AmberWarning
                    else -> EmeraldSuccess
                }
                val quotaLabel = when {
                    !selectedEndpoint.isEnabled -> "معطل"
                    selectedEndpoint.quotaStatus == AiEndpointConfig.QUOTA_EXHAUSTED -> "انتهت الحصة (استنفاد)"
                    selectedEndpoint.quotaStatus == AiEndpointConfig.QUOTA_ERROR -> "خطأ مؤقت"
                    else -> "الحساب جاهز ومتاح"
                }

                CommandCard(borderColor = statusColor.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedEndpoint.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نوع المزود: ${providerType.arabicLabel} • النموذج: ${selectedEndpoint.modelName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(label = quotaLabel, color = statusColor)
                    }

                    if (selectedEndpoint.accountHint.isNotBlank()) {
                        Text(
                            text = "معلومات الحساب / البريد المالك: ${selectedEndpoint.accountHint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                    }
                    Text(
                        text = "الطلبات الناجحة المسجلة على هذا الحساب: ${selectedEndpoint.requestsCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (selectedEndpoint.lastError.isNotBlank()) {
                        Text(
                            text = "آخر خطأ أو استنفاد مسجل: ${selectedEndpoint.lastError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError
                        )
                    }
                }
            }
        }

        // Previous Conversations & Messages Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المحادثات السابقة (${filteredMessages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (filteredMessages.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val exportText = filteredMessages.joinToString("\n\n---\n\n") {
                                "[${it.role.uppercase()}]: ${it.content}"
                            }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("AI Conversations History", exportText))
                            copiedNotice = true
                        }
                    ) {
                        Icon(Icons.Rounded.IosShare, contentDescription = null)
                        Text(if (copiedNotice) AppUiStrings.aiConvCopied(uiLang) else AppUiStrings.aiConvCopy(uiLang))
                    }
                }
            }
        }

        if (filteredMessages.isEmpty()) {
            item {
                CommandCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null)
                        Text(AppUiStrings.aiConvEmpty(uiLang), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "ابدأ المحادثة مع الوكيل في شاشة «الوكيل» أو أطلق مهام برمجية لتظهر هنا سجلات وسياق المحادثات لجميع الحسابات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredMessages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                val isUser = msg.role == "user"
                CommandCard(
                    borderColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else ElectricCyan.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isUser) Icons.Rounded.Person else Icons.Rounded.Forum,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 2.dp),
                                tint = if (isUser) MaterialTheme.colorScheme.primary else ElectricCyan
                            )
                            Text(
                                text = if (isUser) "سؤال المستخدم" else "رد الوكيل / النموذج",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) MaterialTheme.colorScheme.primary else ElectricCyan
                            )
                        }
                        if (msg.repoContext.isNotBlank()) {
                            StatusPill(label = msg.repoContext, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
