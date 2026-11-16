package com.example.ui.screens

import com.example.ui.i18n.AppUiStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.LocalTaskCheckpoint
import com.example.domain.model.AgentTask
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess

/**
 * Screen 6: Tasks & Execution Plan (with WorkManager background checkpointing)
 */
@Composable
fun TasksScreen(
    tasks: List<AgentTask>,
    localCheckpoints: List<LocalTaskCheckpoint>,
    sharedWorkFilePath: String,
    onCreateTaskAndRun: (String) -> Unit,
    onResumeTask: (AgentTask) -> Unit,
    onOpenSharedWorkspace: () -> Unit,
    appLanguage: String = "ar"
) {
    val uiLang = appLanguage
    var newTaskPrompt by remember { mutableStateOf("") }

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
                    Icon(Icons.Rounded.AddTask, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(AppUiStrings.tasksBackgroundTitle(uiLang), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "يتابع الوكيل الخطوات تلقائياً عبر جميع الحسابات والنماذج المتاحة وعند أي حصة منتهية يتحوّل للبديل ويعاود العمل من آخر نقطة محفوظة في Room وFirestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newTaskPrompt,
                    onValueChange = { newTaskPrompt = it },
                    label = { Text(AppUiStrings.tasksDescriptionLabel(uiLang)) },
                    placeholder = { Text(AppUiStrings.tasksDescriptionPlaceholder(uiLang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_task_prompt_input")
                )
                Button(
                    onClick = {
                        if (newTaskPrompt.isNotBlank()) {
                            onCreateTaskAndRun(newTaskPrompt)
                            newTaskPrompt = ""
                        }
                    },
                    enabled = newTaskPrompt.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_task_button")
                ) {
                    Text(AppUiStrings.tasksLaunch(uiLang))
                }
                OutlinedButton(
                    onClick = onOpenSharedWorkspace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks_open_shared_workspace_button")
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null)
                    Text(AppUiStrings.tasksOpenSharedPlan(uiLang, sharedWorkFilePath))
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                CommandCard {
                    Text(AppUiStrings.tasksEmpty(uiLang), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "أطلق مهمة من الأعلى أو من غرفة التعاون لتبدأ دورة Agent Loop المستمرة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(tasks, key = { it.id.ifBlank { it.hashCode().toString() } }) { task ->
            val statusColor = when (task.status) {
                "completed" -> EmeraldSuccess
                "awaiting_approval", "paused" -> AmberWarning
                "failed" -> CrimsonError
                else -> ElectricCyan
            }
            val localCp = localCheckpoints.firstOrNull { it.taskId == task.id }

            CommandCard(borderColor = statusColor.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(label = "${task.progressPercent}%", color = statusColor)
                }
                LinearProgressIndicator(
                    progress = { (task.progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "المستودع: ${task.repo} (${task.branch}) • الخطوة ${task.currentStepIndex}/${task.totalSteps}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.lastCompletedStep.isNotBlank()) {
                    Text(
                        text = "آخر خطوة مكتملة: ${task.lastCompletedStep}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (task.lastResultSummary.isNotBlank()) {
                    Text(
                        text = "النتيجة: ${task.lastResultSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.modifiedFiles.isNotEmpty()) {
                    Text(
                        text = "ملفات متأثرة: ${task.modifiedFiles.joinToString("، ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
                if (task.nextSteps.isNotEmpty()) {
                    Text(
                        text = "الخطوات التالية: ${task.nextSteps.joinToString(" ◀ ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.lastError.isNotBlank()) {
                    Text(
                        text = "خطأ: ${task.lastError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }
                if (localCp != null) {
                    Text(
                        text = "نقطة استئناف محلية محفوظة (Room)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess
                    )
                }
                if (task.status != "completed") {
                    Button(
                        onClick = { onResumeTask(task) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resume_task_${task.id}")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(AppUiStrings.tasksResumeNow(uiLang))
                    }
                }
            }
        }
    }
}
