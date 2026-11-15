package com.example.data.repository

import com.example.core.security.ProjectProfileConfig
import com.example.core.security.TechStackType
import com.example.domain.model.AgentTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkplanRoundEntry(
    val roleTitle: String,
    val endpointLabel: String,
    val modelName: String,
    val summaryContent: String
)

/**
 * Pure Kotlin builder & updater for a single shared Markdown workspace file
 * inside any repository (e.g. `docs/AGENT_WORKPLAN.md`) where multiple AI models
 * read, collaborate, maintain the project requirements, execution plan, and
 * technical instructions without losing prior history.
 */
object SharedWorkplanCodec {

    private const val MARKER_REQUIREMENTS_START = "<!-- METRA_AGENT_REQUIREMENTS_START -->"
    private const val MARKER_REQUIREMENTS_END = "<!-- METRA_AGENT_REQUIREMENTS_END -->"
    private const val MARKER_INSTRUCTIONS_START = "<!-- METRA_AGENT_INSTRUCTIONS_START -->"
    private const val MARKER_INSTRUCTIONS_END = "<!-- METRA_AGENT_INSTRUCTIONS_END -->"
    private const val MARKER_PLAN_START = "<!-- METRA_AGENT_PLAN_START -->"
    private const val MARKER_PLAN_END = "<!-- METRA_AGENT_PLAN_END -->"
    private const val MARKER_COLLAB_START = "<!-- METRA_AGENT_COLLAB_LOG_START -->"
    private const val MARKER_COLLAB_END = "<!-- METRA_AGENT_COLLAB_LOG_END -->"

    fun buildInitialTemplate(
        project: ProjectProfileConfig,
        teamNames: List<String>,
        tasks: List<AgentTask> = emptyList()
    ): String {
        val stack = TechStackType.fromWireKey(project.techStack).arabicLabel
        val reqs = project.projectRequirements.ifBlank {
            "- تحديد متطلبات المشروع الأساسية هنا ليقرأها فريق النماذج المتعاونة."
        }
        val instr = project.systemInstructions.ifBlank {
            "- التزم بنظام الموافقات الثلاثي، واقرأ هذا الملف قبل أي تعديل، وحدّث حالة كل خطوة بعد إنجازها."
        }
        val planSection = formatTasksAsChecklist(tasks)
        val teamText = if (teamNames.isNotEmpty()) teamNames.joinToString(" | ") else "افتراضي"

        return buildString {
            appendLine("# ملف العمل الموحد للمشروع: ${project.name}")
            appendLine()
            appendLine("> ملف مشترك داخل المستودع لتنسيق العمل بين عدة نماذج ذكاء اصطناعي ومتابعة التنفيذ حتى النهاية.")
            appendLine()
            appendLine("## 1. هوية المشروع والإعدادات")
            appendLine("- **المستودع:** `${project.repoSlug}` (الفرع: `${project.defaultBranch}`)")
            appendLine("- **المسار المشترك:** `${project.sharedWorkFilePath}`")
            appendLine("- **التقنية:** $stack")
            appendLine("- **وضع المتابعة المستمرة:** ${if (project.continuousExecutionEnabled) "مفعّل (تبديل تلقائي عند انتهاء الحصة/الحساب/النموذج)" else "يدوي"}")
            appendLine("- **النماذج المتعاونة:** $teamText")
            appendLine()
            appendLine("## 2. متطلبات العمل المحددة")
            appendLine(MARKER_REQUIREMENTS_START)
            appendLine(reqs)
            appendLine(MARKER_REQUIREMENTS_END)
            appendLine()
            appendLine("## 3. التعليمات البرمجية للوكيل")
            appendLine(MARKER_INSTRUCTIONS_START)
            appendLine(instr)
            appendLine(MARKER_INSTRUCTIONS_END)
            appendLine()
            appendLine("## 4. خطة العمل والخطوات التنفيذية")
            appendLine(MARKER_PLAN_START)
            appendLine(planSection)
            appendLine(MARKER_PLAN_END)
            appendLine()
            appendLine("## 5. سجل التعاون والتعديلات بين النماذج")
            appendLine(MARKER_COLLAB_START)
            appendLine("_بانتظار أول جولة تعاونية..._")
            appendLine(MARKER_COLLAB_END)
        }
    }

    fun mergeUpdate(
        existingContent: String?,
        project: ProjectProfileConfig,
        teamNames: List<String>,
        tasks: List<AgentTask>,
        newPlanMarkdown: String? = null,
        newRoundEntries: List<WorkplanRoundEntry> = emptyList(),
        directiveTitle: String = ""
    ): String {
        if (existingContent.isNullOrBlank() ||
            !existingContent.contains(MARKER_COLLAB_START)
        ) {
            val base = buildInitialTemplate(project, teamNames, tasks)
            return mergeUpdate(
                existingContent = base,
                project = project,
                teamNames = teamNames,
                tasks = tasks,
                newPlanMarkdown = newPlanMarkdown,
                newRoundEntries = newRoundEntries,
                directiveTitle = directiveTitle
            )
        }

        var updated = existingContent
        if (project.projectRequirements.isNotBlank()) {
            updated = replaceSection(
                source = updated,
                startMarker = MARKER_REQUIREMENTS_START,
                endMarker = MARKER_REQUIREMENTS_END,
                newInner = project.projectRequirements.trim()
            )
        }
        if (project.systemInstructions.isNotBlank()) {
            updated = replaceSection(
                source = updated,
                startMarker = MARKER_INSTRUCTIONS_START,
                endMarker = MARKER_INSTRUCTIONS_END,
                newInner = project.systemInstructions.trim()
            )
        }

        val planBody = newPlanMarkdown?.takeIf { it.isNotBlank() } ?: formatTasksAsChecklist(tasks)
        updated = replaceSection(
            source = updated,
            startMarker = MARKER_PLAN_START,
            endMarker = MARKER_PLAN_END,
            newInner = planBody
        )

        if (newRoundEntries.isNotEmpty()) {
            val existingCollab = extractSection(updated, MARKER_COLLAB_START, MARKER_COLLAB_END)
                .replace("_بانتظار أول جولة تعاونية..._", "")
                .trim()
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val appendedBlock = buildString {
                appendLine("### جولة تعاونية ($stamp)" + if (directiveTitle.isNotBlank()) " — $directiveTitle" else "")
                newRoundEntries.forEach { entry ->
                    appendLine("#### ${entry.roleTitle} • [${entry.endpointLabel} / `${entry.modelName}`]")
                    appendLine(entry.summaryContent.trim())
                    appendLine()
                }
            }.trim()

            val combinedCollab = listOf(existingCollab, appendedBlock)
                .filter { it.isNotBlank() }
                .joinToString("\n\n---\n\n")
                // Keep shared file cleanly bounded under ~24KB so GitHub API & AI prompts stay fast
                .takeLast(24_000)

            updated = replaceSection(
                source = updated,
                startMarker = MARKER_COLLAB_START,
                endMarker = MARKER_COLLAB_END,
                newInner = combinedCollab
            )
        }
        return updated
    }

    private fun formatTasksAsChecklist(tasks: List<AgentTask>): String {
        if (tasks.isEmpty()) {
            return "- [ ] بانتظار جدولة أو توليد خطة التنفيذ الأولية للمشروع."
        }
        return buildString {
            tasks.take(15).forEach { task ->
                val done = task.status == "completed"
                val mark = if (done) "x" else " "
                appendLine("- [$mark] **${task.title}** (`${task.status}` • ${task.progressPercent}%)")
                if (task.lastCompletedStep.isNotBlank()) {
                    appendLine("  - آخر خطوة مكتملة: ${task.lastCompletedStep}")
                }
                if (task.nextSteps.isNotEmpty() && !done) {
                    appendLine("  - الخطوات المتبقية: ${task.nextSteps.joinToString(" ◀ ")}")
                }
            }
        }.trimEnd()
    }

    private fun extractSection(source: String, startMarker: String, endMarker: String): String {
        val s = source.indexOf(startMarker)
        val e = source.indexOf(endMarker)
        if (s == -1 || e <= s) return ""
        return source.substring(s + startMarker.length, e).trim()
    }

    private fun replaceSection(
        source: String,
        startMarker: String,
        endMarker: String,
        newInner: String
    ): String {
        val s = source.indexOf(startMarker)
        val e = source.indexOf(endMarker)
        if (s == -1 || e <= s) return source
        return buildString {
            append(source.substring(0, s + startMarker.length))
            appendLine()
            appendLine(newInner.trim())
            append(source.substring(e))
        }
    }
}
