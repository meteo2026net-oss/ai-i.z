package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.core.security.SecretVault
import com.example.data.repository.AuthSessionRepository
import com.example.core.notifications.AgentNotificationPrefs
import com.example.core.notifications.QuotaNotificationHelper
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.OperationLog
import com.example.domain.model.UserSettings
import com.example.ui.components.ApprovalGateCard
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.components.TierBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen 15: Operation Logs & Flutter Spec / Final Report Export
 */
@Composable
fun LogsScreen(
    logs: List<OperationLog>,
    flutterSpecText: String
) {
    val context = LocalContext.current
    var categoryFilter by remember { mutableStateOf("all") }
    var copiedNotice by remember { mutableStateOf(false) }

    val filtered = remember(logs, categoryFilter) {
        if (categoryFilter == "all") logs else logs.filter { it.category == categoryFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.IosShare, contentDescription = null, tint = ElectricCyan)
                    Text("تصدير مواصفات Flutter وBackend والتقرير", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "يمكنك مشاركة أو نسخ مواصفات Flutter الكاملة وعقد Backend Runner ومقترح PostgreSQL Migration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, flutterSpecText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "تصدير مواصفات METRA Flutter"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_flutter_spec_button")
                    ) {
                        Text("مشاركة المواصفات")
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("METRA Flutter Spec", flutterSpecText))
                            copiedNotice = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (copiedNotice) "تم النسخ ✓" else "نسخ للحافظة")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "الكل",
                    "agent" to "الوكيل",
                    "github" to "GitHub",
                    "database" to "القاعدة",
                    "test" to "الاختبارات",
                    "security" to "الأمان"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = categoryFilter == key,
                        onClick = { categoryFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                CommandCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null)
                        Text("لا توجد عمليات مسجلة في هذا التصنيف بعد", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        items(filtered, key = { it.id.ifBlank { it.hashCode().toString() } }) { log ->
            val color = when (log.outcome) {
                "success" -> EmeraldSuccess
                "blocked", "error" -> CrimsonError
                else -> AmberWarning
            }
            CommandCard(borderColor = color.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TierBadge(tier = log.tier.toInt())
                    StatusPill(label = log.outcome, color = color)
                }
                Text(log.action, style = MaterialTheme.typography.titleMedium)
                if (log.details.isNotBlank()) {
                    Text(
                        log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (log.repo.isNotBlank()) {
                    Text(
                        "المستودع: ${log.repo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}

/**
 * Screen 16: Security, Permissions & Theme Settings
 */
@Composable
fun SecuritySettingsScreen(
    settings: UserSettings,
    approvals: List<ApprovalRequest>,
    notificationPrefs: AgentNotificationPrefs,
    onUpdateNotificationPrefs: (AgentNotificationPrefs) -> Unit,
    onSendTestNotification: () -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onApprove: (ApprovalRequest) -> Unit,
    onReject: (ApprovalRequest) -> Unit,
    onClearSecrets: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }
    var deleteAccountInProgress by remember { mutableStateOf(false) }
    var notifGranted by remember {
        mutableStateOf(QuotaNotificationHelper.hasNotificationPermission(context))
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldSuccess)
                    Text("إعدادات الأمان والصلاحيات ثلاثية المستويات", style = MaterialTheme.typography.titleLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("التنفيذ التلقائي للمستوى الأول", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "قراءة الملفات، التحليل، التعديل داخل فرع عمل، تشغيل الاختبارات، وإنشاء التقارير.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoExecuteTier1,
                        onCheckedChange = { onUpdateSettings(settings.copy(autoExecuteTier1 = it)) },
                        modifier = Modifier.testTag("tier1_auto_switch")
                    )
                }

                TierBadge(tier = 2)
                Text(
                    "المستوى الثاني (موافقة إلزامية قبل التنفيذ): رفع commits، إنشاء Pull Request، تعديل staging، أو إعدادات الخدمات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TierBadge(tier = 3)
                Text(
                    "المستوى الثالث (موافقة صريحة مستقلة): تنفيذ SQL على قاعدة مستضافة، تعديل RLS/Grants، تطبيق migrations، دمج PR، والنشر للإنتاج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = ElectricCyan)
                        Text("إشعارات الانتباه والتنبيهات الفورية", style = MaterialTheme.typography.titleMedium)
                    }
                    StatusPill(
                        label = if (notifGranted) "مفعّلة" else "غير مصرح",
                        color = if (notifGranted) EmeraldSuccess else AmberWarning
                    )
                }
                Text(
                    text = "ينبهك التطبيق بصوت واهتزاز مع فتح الشاشة مباشرة عند الضغط على الإشعار في الحالات التالية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                    Button(
                        onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_grant_notifications_button")
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                        Text("منح صلاحية الإشعارات على الهاتف")
                    }
                }
                NotificationToggleRow(
                    title = "عند إنهاء أي أمر أو مهمة أو جولة تعاون",
                    checked = notificationPrefs.notifyOnCompletion,
                    onCheckedChange = {
                        onUpdateNotificationPrefs(notificationPrefs.copy(notifyOnCompletion = it))
                    },
                    testTag = "notif_completion_switch"
                )
                NotificationToggleRow(
                    title = "عند التوقف المؤقت أو الخطأ أو الحظر الأمني",
                    checked = notificationPrefs.notifyOnStopOrError,
                    onCheckedChange = {
                        onUpdateNotificationPrefs(notificationPrefs.copy(notifyOnStopOrError = it))
                    },
                    testTag = "notif_stop_switch"
                )
                NotificationToggleRow(
                    title = "عند الحاجة لإجراء أو موافقة (المستوى 2 و3)",
                    checked = notificationPrefs.notifyOnActionRequired,
                    onCheckedChange = {
                        onUpdateNotificationPrefs(notificationPrefs.copy(notifyOnActionRequired = it))
                    },
                    testTag = "notif_action_switch"
                )
                NotificationToggleRow(
                    title = "عند انتهاء حصة API والتحويل للبديل",
                    checked = notificationPrefs.notifyOnQuotaFailover,
                    onCheckedChange = {
                        onUpdateNotificationPrefs(notificationPrefs.copy(notifyOnQuotaFailover = it))
                    },
                    testTag = "notif_quota_switch"
                )
                OutlinedButton(
                    onClick = onSendTestNotification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_test_notification_button")
                ) {
                    Text("إرسال إشعار تجريبي للتأكد من الصوت والاهتزاز")
                }
            }
        }

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
                        Icon(Icons.Rounded.DarkMode, contentDescription = null)
                        Text("الوضع الداكن الأنيق", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = settings.darkTheme,
                        onCheckedChange = { onUpdateSettings(settings.copy(darkTheme = it)) },
                        modifier = Modifier.testTag("dark_theme_switch")
                    )
                }
            }
        }

        if (approvals.isNotEmpty()) {
            item {
                Text("سجل الموافقات (${approvals.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(approvals, key = { it.id.ifBlank { it.hashCode().toString() } }) { approval ->
                ApprovalGateCard(
                    approval = approval,
                    onApprove = { onApprove(approval) },
                    onReject = { onReject(approval) }
                )
            }
        }

        item {
            CommandCard(borderColor = CrimsonError.copy(alpha = 0.5f)) {
                Text("إدارة الأسرار والجلسة", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onClearSecrets,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_vault_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CrimsonError)
                    Text("مسح GitHub PAT ومفاتيح API المشفرة محلياً")
                }
                Button(
                    onClick = {
                        scope.launch {
                            withContext(NonCancellable) {
                                try {
                                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: Exception) {
                                } finally {
                                    AuthSessionRepository(SecretVault(context)).signOut()
                                    onSignedOut()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Text("تسجيل الخروج من الحساب")
                }
                OutlinedButton(
                    onClick = {
                        deleteAccountError = null
                        showDeleteAccountDialog = true
                    },
                    enabled = !deleteAccountInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CrimsonError)
                    Text("حذف الحساب نهائياً من الخادم")
                }
                deleteAccountError?.let { err ->
                    Text(err, color = CrimsonError, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldSuccess)
                    Text("حقوق الملكية الفكرية وحماية التطبيق", style = MaterialTheme.typography.titleMedium)
                }
                Text("اسم التطبيق: ai i.z", style = MaterialTheme.typography.bodyMedium)
                Text("المطور والمالك: إبراهيم زينب (Ibrahim Zeinab)", style = MaterialTheme.typography.bodyMedium)
                Text("جميع الحقوق محفوظة © 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الأمان والتشفير: التطبيق محمي بالكامل عبر خوارزميات التشفير المتقدمة وضغط وإخفاء الأكواد (ProGuard/R8) لمنع تفكيكه أو سرقة أكواده.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!deleteAccountInProgress) showDeleteAccountDialog = false
                },
                title = { Text("تأكيد حذف الحساب") },
                text = {
                    Text(
                        "سيُحذف حسابك وكل بياناتك المخزّنة على خادم AI I.Z (المشاريع، المهام، السجلات، الموافقات) بشكل نهائي. لا يمكن التراجع عن هذا الإجراء."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                deleteAccountInProgress = true
                                deleteAccountError = null
                                val result = withContext(NonCancellable) {
                                    AuthSessionRepository(SecretVault(context)).deleteAccount()
                                }
                                deleteAccountInProgress = false
                                result.fold(
                                    onSuccess = {
                                        showDeleteAccountDialog = false
                                        runCatching {
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        }
                                        onSignedOut()
                                    },
                                    onFailure = { error ->
                                        deleteAccountError = error.message
                                            ?: "تعذر حذف الحساب. تحقق من الاتصال وحاول مجدداً."
                                        showDeleteAccountDialog = false
                                    }
                                )
                            }
                        },
                        enabled = !deleteAccountInProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
                    ) {
                        Text(if (deleteAccountInProgress) "جاري الحذف…" else "حذف نهائي")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteAccountDialog = false },
                        enabled = !deleteAccountInProgress
                    ) {
                        Text("إلغاء")
                    }
                }
            )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
