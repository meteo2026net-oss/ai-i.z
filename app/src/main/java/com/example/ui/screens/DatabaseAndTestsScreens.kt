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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
 * Screen 12: Multi-Database Connections, Secrets & Schema Explorer
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatabaseScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onSaveDatabaseConnection: (
        label: String,
        engine: String,
        hostOrUrl: String,
        dbName: String,
        connSecret: String,
        apiSecret: String,
        readOnly: Boolean,
        protectedKeywordsCsv: String
    ) -> Unit,
    onActivateDatabaseConnection: (String) -> Unit,
    onRemoveDatabaseConnection: (String) -> Unit,
    onLoadSchema: () -> Unit,
    onGoToBackendSetup: () -> Unit,
    onGoToSqlReview: () -> Unit
) {
    val uiLang = settings.appLanguage
    val dbConnections = workspace.platformConfig.databaseConnections
    val activeProject = workspace.platformConfig.activeProject

    var labelInput by remember { mutableStateOf("") }
    var engineInput by remember { mutableStateOf("postgresql") }
    var hostInput by remember { mutableStateOf("") }
    var dbNameInput by remember { mutableStateOf("") }
    var connSecretInput by remember { mutableStateOf("") }
    var apiSecretInput by remember { mutableStateOf("") }
    var readOnlyMode by remember { mutableStateOf(true) }
    var protectedCsv by remember { mutableStateOf("metra_old, metra_legacy") }

    val engines = listOf(
        "postgresql" to "PostgreSQL",
        "supabase" to "Supabase",
        "mysql" to "MySQL",
        "sqlserver" to "SQL Server",
        "sqlite_remote" to "SQLite / LibSQL"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CommandCard(borderColor = CrimsonError.copy(alpha = 0.5f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = CrimsonError)
                    Text("العزل الأمني وإدارة أسرار قواعد البيانات", style = MaterialTheme.typography.titleMedium)
                }
                val protectedDisplay = (listOf("metra_old", "metra_legacy") +
                    activeProject?.protectedDbTargets.orEmpty()).distinct().joinToString("، ")
                Text(
                    text = "القواعد المعزولة المحظورة: $protectedDisplay. تُحفظ جميع سلاسل الاتصال والأسرار (Secrets) مشفرة عبر Android Keystore على جهازك فقط.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Saved DB Connections List
        if (dbConnections.isNotEmpty()) {
            item {
                Text(
                    "اتصالات قواعد البيانات المحفوظة (${dbConnections.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dbConnections, key = { it.id }) { conn ->
                CommandCard(
                    borderColor = if (conn.isActive) EmeraldSuccess.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conn.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${conn.engine} • ${conn.databaseName.ifBlank { conn.hostOrUrl }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = (if (conn.secretOrConnectionString.isNotBlank()) "سلسلة اتصال مشفرة ✓ " else "") +
                                    (if (conn.apiKeyOrServiceRole.isNotBlank()) "• Service/API Secret ✓ " else "") +
                                    (if (conn.readOnlyMode) "• وضع القراءة الآمن" else "• قراءة/كتابة بموافقة T3"),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        if (conn.isActive) {
                            StatusPill(label = "نشط", color = EmeraldSuccess, icon = Icons.Rounded.CheckCircle)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!conn.isActive) {
                            Button(
                                onClick = { onActivateDatabaseConnection(conn.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(AppUiStrings.actionActivate(uiLang))
                            }
                        }
                        OutlinedButton(
                            onClick = onGoToSqlReview,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(AppUiStrings.dbSqlCheck(uiLang))
                        }
                        IconButton(onClick = { onRemoveDatabaseConnection(conn.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = AppUiStrings.dbDeleteCd(uiLang), tint = CrimsonError)
                        }
                    }
                }
            }
        }

        // Add New DB Connection + Secret Card
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.45f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, tint = ElectricCyan)
                    Text(AppUiStrings.dbLinkNew(uiLang), style = MaterialTheme.typography.titleMedium)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    engines.forEach { (key, title) ->
                        FilterChip(
                            selected = engineInput == key,
                            onClick = { engineInput = key },
                            label = { Text(title) }
                        )
                    }
                }
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text(AppUiStrings.dbDisplayName(uiLang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_label_input")
                )
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Host / Supabase Project URL") },
                    placeholder = { Text("https://xyzcompany.supabase.co أو db.host:5432") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dbNameInput,
                    onValueChange = { dbNameInput = it },
                    label = { Text(AppUiStrings.dbNameLabel(uiLang)) },
                    placeholder = { Text("app_staging_db") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_name_input")
                )
                OutlinedTextField(
                    value = connSecretInput,
                    onValueChange = { connSecretInput = it },
                    label = { Text(AppUiStrings.dbConnectionSecret(uiLang)) },
                    placeholder = { Text("postgresql://readonly_user:secret@host:5432/db") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_conn_secret_input")
                )
                OutlinedTextField(
                    value = apiSecretInput,
                    onValueChange = { apiSecretInput = it },
                    label = { Text(AppUiStrings.dbOptionalToken(uiLang)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_api_secret_input")
                )
                OutlinedTextField(
                    value = protectedCsv,
                    onValueChange = { protectedCsv = it },
                    label = { Text(AppUiStrings.dbBlockedList(uiLang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppUiStrings.dbReadOnlyToggle(uiLang),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = readOnlyMode, onCheckedChange = { readOnlyMode = it })
                }
                Button(
                    onClick = {
                        onSaveDatabaseConnection(
                            labelInput,
                            engineInput,
                            hostInput,
                            dbNameInput,
                            connSecretInput,
                            apiSecretInput,
                            readOnlyMode,
                            protectedCsv
                        )
                        labelInput = ""
                        hostInput = ""
                        dbNameInput = ""
                        connSecretInput = ""
                        apiSecretInput = ""
                    },
                    enabled = labelInput.isNotBlank() || dbNameInput.isNotBlank() || hostInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_db_connection_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(AppUiStrings.dbSaveConnection(uiLang))
                }
            }
        }

        if (settings.backendUrl.isBlank()) {
            item {
                SetupRequiredBanner(
                    title = "Backend Schema Runner اختياري للجداول الحية",
                    description = "يمكنك استخدام مراجعة SQL بالذكاء الاصطناعي مباشرة الآن، أو ربط Backend Runner لجلب مخططات الجداول الحية وتنفيذ استعلامات SELECT.",
                    actionLabel = "إعداد خادم Backend",
                    onAction = onGoToBackendSetup,
                    testTag = "db_setup_backend_cta"
                )
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onLoadSchema,
                        enabled = !workspace.isBusyBackend,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_db_schema_button")
                    ) {
                        Icon(Icons.Rounded.Storage, contentDescription = null)
                        Text(AppUiStrings.dbFetchSchema(uiLang))
                    }
                    OutlinedButton(
                        onClick = onGoToSqlReview,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(AppUiStrings.dbSqlReview(uiLang))
                    }
                }
            }
        }

        if (!workspace.backendError.isNullOrBlank()) {
            item {
                CommandCard(borderColor = CrimsonError) {
                    Text(workspace.backendError, style = MaterialTheme.typography.bodySmall, color = CrimsonError)
                }
            }
        }

        workspace.dbSchema?.let { schema ->
            item {
                CommandCard(borderColor = EmeraldSuccess.copy(alpha = 0.5f)) {
                    Text("القاعدة: ${schema.databaseName}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "الإصدار: ${schema.engineVersion} • بيئة معزولة: ${schema.isIsolatedTarget}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSuccess
                    )
                }
            }
            items(schema.tables, key = { "${it.schema}.${it.name}" }) { table ->
                CommandCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${table.schema}.${table.name}", style = MaterialTheme.typography.titleMedium)
                        StatusPill(
                            label = if (table.rlsEnabled) AppUiStrings.dbRlsOn(uiLang) else AppUiStrings.dbRlsOff(uiLang),
                            color = if (table.rlsEnabled) EmeraldSuccess else AmberWarning
                        )
                    }
                    Text(
                        "تقدير الصفوف: ${table.rowEstimate} • الفهارس: ${table.indexes.size} • العلاقات: ${table.relations.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    table.columns.forEach { col ->
                        Text(
                            text = "• ${col.name}: ${col.dataType}" +
                                (if (col.isPrimaryKey) " [PK]" else "") +
                                (if (!col.isNullable) " NOT NULL" else ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 13: SQL & Security / Migration Review
 */
@Composable
fun SqlReviewScreen(
    workspace: WorkspaceUiState,
    onReviewOrExecuteSql: (sql: String, targetDatabase: String) -> Unit,
    appLanguage: String = "ar"
) {
    val uiLang = appLanguage
    val activeDbName = workspace.platformConfig.activeDatabase?.databaseName?.ifBlank {
        workspace.platformConfig.activeDatabase?.label
    } ?: "project_staging_db"
    var targetDb by remember(activeDbName) { mutableStateOf(activeDbName) }
    var sqlText by remember {
        mutableStateOf(
            "SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'public';"
        )
    }

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
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = AmberWarning)
                    Text(AppUiStrings.sqlReviewTitle(uiLang), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "تُنفّذ استعلامات القراءة SELECT فقط بصلاحيات قراءة منفصلة. أي DDL أو RLS أو Grants أو Migration يُحتجز تلقائياً لبوابة الموافقة الصريحة (المستوى 3).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetDb,
                    onValueChange = { targetDb = it },
                    label = { Text(AppUiStrings.sqlTargetDb(uiLang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_target_db_input")
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = sqlText,
                        onValueChange = { sqlText = it },
                        label = { Text("SQL / Migration Query") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        minLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sql_query_input")
                    )
                }
                Button(
                    onClick = { onReviewOrExecuteSql(sqlText, targetDb) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_review_execute_button")
                ) {
                    Text(AppUiStrings.sqlSecurityCheck(uiLang))
                }
            }
        }

        if (!workspace.backendError.isNullOrBlank()) {
            item {
                CommandCard(borderColor = CrimsonError) {
                    Text(workspace.backendError, style = MaterialTheme.typography.bodySmall, color = CrimsonError)
                }
            }
        }

        if (workspace.sqlReviewAnalysis.isNotBlank()) {
            item {
                CommandCard(borderColor = ElectricCyan) {
                    Text(AppUiStrings.sqlAiAnalysis(uiLang), style = MaterialTheme.typography.titleMedium)
                    Text(workspace.sqlReviewAnalysis, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        workspace.sqlResult?.let { res ->
            item {
                CommandCard(borderColor = EmeraldSuccess) {
                    Text("نتيجة التنفيذ (${res.rowsAffected} صف)", style = MaterialTheme.typography.titleMedium)
                    if (res.impactSummary.isNotBlank()) {
                        Text(res.impactSummary, style = MaterialTheme.typography.bodySmall)
                    }
                    val tablePreview = buildString {
                        if (res.columns.isNotEmpty()) appendLine(res.columns.joinToString(" | "))
                        res.rows.take(25).forEach { r -> appendLine(r.joinToString(" | ")) }
                    }
                    if (tablePreview.isNotBlank()) {
                        LtrCodeBox(code = tablePreview)
                    }
                }
            }
        }
    }
}

/**
 * Screen 14: Tests Runner
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestsScreen(
    settings: UserSettings,
    workspace: WorkspaceUiState,
    onRunTests: (String) -> Unit,
    onSendFailuresToAgent: (String) -> Unit,
    onGoToBackendSetup: () -> Unit,
    appLanguage: String = "ar"
) {
    val uiLang = appLanguage
    var selectedTarget by remember { mutableStateOf("all") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (settings.backendUrl.isBlank()) {
            item {
                SetupRequiredBanner(
                    title = "تشغيل الاختبارات يتطلب Backend Runner",
                    description = "اربط خادم Backend على جهاز التطوير أو السحابة لتشغيل اختبارات المشروع الفعلية.",
                    actionLabel = "إعداد خادم Backend",
                    onAction = onGoToBackendSetup,
                    testTag = "tests_setup_backend_cta"
                )
            }
        }

        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = ElectricCyan)
                    Text("تشغيل اختبارات المشروع: ${settings.activeRepo}", style = MaterialTheme.typography.titleMedium)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "all" to "الكل",
                        "flutter" to "Flutter",
                        "dotnet" to ".NET",
                        "android" to "Android/Gradle",
                        "node" to "Node/Web",
                        "db_isolated" to "DB معزولة"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedTarget == key,
                            onClick = { selectedTarget = key },
                            label = { Text(label) }
                        )
                    }
                }
                Button(
                    onClick = { onRunTests(selectedTarget) },
                    enabled = settings.backendUrl.isNotBlank() && !workspace.isBusyBackend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_tests_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text("بدء الاختبارات الفعلية")
                }
            }
        }

        workspace.lastTestRun?.let { run ->
            item {
                CommandCard(
                    borderColor = if (run.failedCount == 0) EmeraldSuccess else CrimsonError
                ) {
                    Text("ملخص النتائج (${run.durationMs} مللي ثانية)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusPill("ناجح: ${run.passedCount}", EmeraldSuccess)
                        StatusPill("فاشل: ${run.failedCount}", CrimsonError)
                        StatusPill("متخطى: ${run.skippedCount}", AmberWarning)
                    }
                    if (run.summary.isNotBlank()) {
                        Text(run.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (run.failedCount > 0) {
                        OutlinedButton(
                            onClick = {
                                val failures = run.cases.filter { !it.passed }
                                    .joinToString("\n") { "${it.name}: ${it.errorMessage}" }
                                onSendFailuresToAgent("أصلح أخطاء الاختبارات التالية:\n$failures")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إرسال الأخطاء للوكيل لإصلاحها")
                        }
                    }
                }
            }

            items(run.cases, key = { "${it.suite}_${it.name}" }) { testCase ->
                CommandCard(
                    borderColor = if (testCase.passed) EmeraldSuccess.copy(alpha = 0.4f) else CrimsonError.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(testCase.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusPill(
                            label = if (testCase.passed) "نجح" else "فشل",
                            color = if (testCase.passed) EmeraldSuccess else CrimsonError
                        )
                    }
                    if (testCase.errorMessage.isNotBlank()) {
                        LtrCodeBox(code = testCase.errorMessage)
                    }
                }
            }
        }
    }
}
