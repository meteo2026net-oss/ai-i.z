package com.example.ui.screens

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DevelopmentEnvironment
import com.example.domain.model.ModelAdvisorEngine
import com.example.domain.model.ProgrammingLanguage
import com.example.domain.model.StudioProjectSpec
import com.example.data.repository.StudioProjectArchiveExporter
import com.example.ui.components.CommandCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

/**
 * Universal AI Studio Screen (ai i.z)
 * A supercharged Google AI Studio alternative supporting ALL development environments,
 * all programming languages, and an intelligent model advisor.
 * Developed by Ibrahim Zeinab (إبراهيم زينب).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalStudioScreen(
    activeProject: StudioProjectSpec?,
    isGenerating: Boolean,
    selectedFilePath: String,
    editedFileContent: String,
    auditStatus: String,
    onCreateProject: (name: String, prompt: String, env: DevelopmentEnvironment, lang: ProgrammingLanguage, priority: String) -> Unit,
    onSelectFile: (path: String) -> Unit,
    onUpdateFileContent: (String) -> Unit,
    onSaveFileChanges: () -> Unit,
    onRunAudit: () -> Unit,
    onApplyModel: (modelId: String, modelName: String) -> Unit,
    onOpenChat: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("منشئ المشاريع", "مستشار النماذج", "مستكشف الأكواد", "التشغيل والتدقيق")

    // Project creation state
    var projectNameInput by remember { mutableStateOf("") }
    var projectPromptInput by remember { mutableStateOf("") }
    var selectedEnv by remember { mutableStateOf(DevelopmentEnvironment.MOBILE_ANDROID) }
    var selectedLang by remember(selectedEnv) { mutableStateOf(selectedEnv.defaultLanguage) }
    var selectedPriority by remember { mutableStateOf("balanced") }

    val recommendedModels = remember(selectedEnv, selectedLang, selectedPriority) {
        ModelAdvisorEngine.recommendModels(selectedEnv, selectedLang, selectedPriority)
    }

    LaunchedEffect(activeProject) {
        if (activeProject != null && selectedTab == 0 && !isGenerating) {
            // Auto switch to workspace or model tab when project created
            selectedTab = 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("universal_studio_screen")
    ) {
        // Studio Banner / Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ai i.z Universal Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مستودع البرمجة الشامل | المطور إبراهيم زينب",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AssistChip(
                        onClick = onOpenChat,
                        label = { Text("وكيل الذكاء") },
                        leadingIcon = {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("studio_open_chat_chip")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Studio Sub-Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("studio_tab_$index")
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> ProjectCreatorTab(
                    projectName = projectNameInput,
                    onProjectNameChange = { projectNameInput = it },
                    prompt = projectPromptInput,
                    onPromptChange = { projectPromptInput = it },
                    selectedEnv = selectedEnv,
                    onSelectEnv = {
                        selectedEnv = it
                        selectedLang = it.defaultLanguage
                    },
                    selectedLang = selectedLang,
                    onSelectLang = { selectedLang = it },
                    selectedPriority = selectedPriority,
                    onSelectPriority = { selectedPriority = it },
                    isGenerating = isGenerating,
                    onCreate = {
                        onCreateProject(projectNameInput, projectPromptInput, selectedEnv, selectedLang, selectedPriority)
                    }
                )

                1 -> ModelAdvisorTab(
                    activeProject = activeProject,
                    models = if (activeProject != null) listOf(activeProject.recommendedModel) + activeProject.alternateModels else recommendedModels,
                    selectedEnv = activeProject?.environment ?: selectedEnv,
                    onApplyModel = onApplyModel
                )

                2 -> CodeExplorerTab(
                    project = activeProject,
                    selectedFilePath = selectedFilePath,
                    editedContent = editedFileContent,
                    onSelectFile = onSelectFile,
                    onContentChange = onUpdateFileContent,
                    onSave = onSaveFileChanges,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(editedFileContent))
                        Toast.makeText(context, "تم نسخ الكود للحافظة", Toast.LENGTH_SHORT).show()
                    },
                    onExport = {
                        activeProject?.let { shareStudioProjectArchive(context, it) }
                    }
                )

                3 -> ExecutionAndAuditTab(
                    project = activeProject,
                    auditStatus = auditStatus,
                    onRunAudit = onRunAudit
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCreatorTab(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedEnv: DevelopmentEnvironment,
    onSelectEnv: (DevelopmentEnvironment) -> Unit,
    selectedLang: ProgrammingLanguage,
    onSelectLang: (ProgrammingLanguage) -> Unit,
    selectedPriority: String,
    onSelectPriority: (String) -> Unit,
    isGenerating: Boolean,
    onCreate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.5f)) {
                Text(
                    text = "إنشاء تطبيق جديد فائق الذكاء (Universal Prompt-to-App)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اكتب فكرة تطبيقك أو مشروعه، وسيقوم أستوديو ai i.z بتوليد الهيكل، الأكواد، ملفات الإعداد، واقتراح أقوى نموذج ذكاء اصطناعي له.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = onProjectNameChange,
                    label = { Text("اسم المشروع (مثال: my-fintech-app)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_project_name_input")
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    label = { Text("المتطلبات والتعليمات (الميزات، القواعد، الـ APIs، التصميم...)") },
                    placeholder = { Text("مثال: تطبيق متجر إلكتروني متكامل مع سلة مشتريات وتوثيق JWT وبوابة دفع...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_prompt_input")
                )
            }
        }

        // Environment Selector
        item {
            CommandCard {
                Text(
                    text = "1. اختر بيئة التطوير المستهدفة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ندعم كافة بيئات العمل من الهواتف والويب إلى الخوادم السحابية والأنظمة المصغرة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DevelopmentEnvironment.entries.forEach { env ->
                        val isSelected = env == selectedEnv
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectEnv(env) },
                            label = { Text(env.title, fontSize = 12.sp) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier.testTag("env_chip_${env.name}")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "حزمة البيئة المختارة: ${selectedEnv.subtitle}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricCyan
                        )
                        Text(
                            text = "أداة البناء الافتراضية: ${selectedEnv.buildTool} | التصنيف: ${selectedEnv.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Language & Priority Selector
        item {
            CommandCard {
                Text(
                    text = "2. لغة البرمجة الأساسية ومستوى الأولوية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "اللغة الأساسية:",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ProgrammingLanguage.entries) { lang ->
                        val isSelected = lang == selectedLang
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectLang(lang) },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("lang_chip_${lang.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "معيار المفاضلة والأولوية للذكاء الاصطناعي:",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priorities = listOf(
                        "balanced" to "المتوازن والأشمل (Recommended)",
                        "accuracy" to "أعلى دقة ومنطق معماري",
                        "huge_context" to "أكبر سياق (Gemini 2M Tokens)",
                        "speed" to "سرعة فائقة وإنشاء فوري",
                        "open_source" to "نماذج مفتوحة المصدر (DeepSeek/Qwen)"
                    )
                    priorities.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPriority == key,
                            onClick = { onSelectPriority(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("priority_chip_$key")
                        )
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = onCreate,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_studio_project_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("جاري بناء هيكل المشروع وتوليد الأكواد...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنشاء المشروع وتحليله عبر مستشار النماذج", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ModelAdvisorTab(
    activeProject: StudioProjectSpec?,
    models: List<com.example.domain.model.AiModelRecommendation>,
    selectedEnv: DevelopmentEnvironment,
    onApplyModel: (modelId: String, modelName: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CommandCard(borderColor = EmeraldSuccess.copy(alpha = 0.5f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = EmeraldSuccess)
                    Text(
                        text = "مستشار النماذج الأقوى والأذكى (AI Studio Model Matcher)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "يقوم النظام بدراسة طبيعة المشروع (${selectedEnv.title})، واختيار النموذج أو فريق النماذج الأكثر تفوقاً في العالم لكتابة وتنقيح الأكواد بأعلى دقة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(models) { model ->
            ModelRecommendationCard(
                recommendation = model,
                onApply = { onApplyModel(model.modelId, model.modelName) }
            )
        }

        // Multi-Model Collaboration Team Architecture
        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, tint = AmberWarning)
                    Text(
                        text = "تكوين فريق العمل متعدد النماذج (Multi-Model Team)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "في هذا المشروع، يوصى بتوزيع المهام بين أقوى النماذج للحصول على نتيجة بدون أي ثغرات برمجية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TeamRoleRow("المهندس المعماري (Architect)", "Gemini 1.5 Pro / Claude 3.7", "تحديد بنية الملفات ومخطط البيانات")
                    TeamRoleRow("المبرمج التنفيذي (Core Coder)", "Claude 3.7 / Gemini 2.0 Flash", "كتابة الدوال والواجهات والمكونات")
                    TeamRoleRow("مدقق الأمان والمنطق (Security & Logic)", "DeepSeek-R1 / o3-mini", "اكتشاف الثغرات والتحقق من صحة الخوارزميات")
                    TeamRoleRow("مهندس الاختبارات (QA Tester)", "GPT-4o / Claude Sonnet", "كتابة اختبارات الوحدة وفحص الأخطاء")
                }
            }
        }
    }
}


private fun shareStudioProjectArchive(
    context: android.content.Context,
    project: StudioProjectSpec
) {
    try {
        val exportDirectory = File(context.cacheDir, "studio-exports")
        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
            throw IllegalStateException("تعذر إنشاء مجلد التصدير.")
        }
        val safeName = project.projectName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.')
            .take(64)
            .ifBlank { "ai-iz-project" }
        val archive = File(exportDirectory, "$safeName.zip")
        FileOutputStream(archive).use { output ->
            StudioProjectArchiveExporter.writeZip(project, output)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archive
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, archive.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "تصدير مشروع ${project.projectName}"))
    } catch (error: Exception) {
        Toast.makeText(
            context,
            "تعذر تصدير المشروع: ${error.localizedMessage ?: "خطأ غير معروف"}",
            Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
private fun ModelRecommendationCard(
    recommendation: com.example.domain.model.AiModelRecommendation,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("model_rec_${recommendation.modelId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = if (recommendation.isPrimaryPick) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = recommendation.modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (recommendation.isPrimaryPick) {
                            StatusPill(label = "الخيار الأقوى 🏆", color = ElectricCyan)
                        }
                    }
                    Text(
                        text = "المطور: ${recommendation.provider} | نافذة السياق: ${recommendation.contextWindow}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (recommendation.matchScore >= 95) EmeraldSuccess.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${recommendation.matchScore}% تطابق",
                        fontWeight = FontWeight.Bold,
                        color = if (recommendation.matchScore >= 95) EmeraldSuccess else ElectricCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🌟 ميزة القوة: ${recommendation.strengthBadge}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmberWarning,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = recommendation.rationale,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "الدور في المشروع: ${recommendation.teamRole}",
                style = MaterialTheme.typography.bodySmall,
                color = ElectricCyan,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_model_${recommendation.modelId}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recommendation.isPrimaryPick) ElectricCyan else MaterialTheme.colorScheme.primary,
                    contentColor = if (recommendation.isPrimaryPick) Color.Black else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("اعتماد هذا النموذج للمشروع في حوض AI")
            }
        }
    }
}

@Composable
private fun TeamRoleRow(role: String, model: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = role, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = model, style = MaterialTheme.typography.bodySmall, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CodeExplorerTab(
    project: StudioProjectSpec?,
    selectedFilePath: String,
    editedContent: String,
    onSelectFile: (path: String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit
) {
    if (project == null || project.files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Rounded.FolderOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("لا يوجد مشروع مفتوح حالياً", style = MaterialTheme.typography.titleMedium)
                Text("توجه إلى تبويب 'منشئ المشاريع' لإنشاء أول تطبيق في البيئة التي تختارها.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    val selectedFile = project.files.find { it.path == selectedFilePath } ?: project.files.first()

    Column(modifier = Modifier.fillMaxSize()) {
        // Files horizontal scroll selector
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.files) { file ->
                    val isSelected = file.path == selectedFile.path
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFile(file.path) },
                        label = { Text(file.path.substringAfterLast("/"), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (file.isEntrypoint) Icons.Rounded.PlayCircle else Icons.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag("file_tab_${file.path.replace('/', '_')}")
                    )
                }
            }
        }

        // File info bar & Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedFile.path,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = selectedFile.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = "تصدير المشروع كملف ZIP", modifier = Modifier.size(18.dp), tint = ElectricCyan)
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "نسخ الكود", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Save, contentDescription = "حفظ التعديلات", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                }
            }
        }

        Divider()

        // Code Editor View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(8.dp)
        ) {
            SelectionContainer {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("studio_code_editor_field"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )
            }
        }
    }
}

@Composable
private fun ExecutionAndAuditTab(
    project: StudioProjectSpec?,
    auditStatus: String,
    onRunAudit: () -> Unit
) {
    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("يرجى إنشاء أو فتح مشروع أولاً لعرض أوامر التشغيل والتدقيق.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Project Overview
        item {
            CommandCard(borderColor = ElectricCyan.copy(alpha = 0.5f)) {
                Text(
                    text = "بيانات تشغيل مشروع: ${project.projectName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "البيئة: ${project.environment.title} | اللغة: ${project.primaryLanguage.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
                Text(
                    text = project.securityAuditScore?.let { "نتيجة الفحص الأمني الموثق: $it/100" }
                        ?: "لم يُجرَ فحص أمني آلي موثّق لهذا المشروع بعد",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (project.securityAuditScore != null) EmeraldSuccess else AmberWarning
                )
            }
        }

        // Instructions
        item {
            CommandCard {
                Text(text = "أوامر البناء والتهيئة (Build):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.buildInstructions,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "أمر التشغيل المباشر (Run / Start):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.runInstructions,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldSuccess,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "أمر تشغيل الاختبارات الآلية (Test):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = project.testInstructions,
                        fontFamily = FontFamily.Monospace,
                        color = AmberWarning,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Audit & Quality Check
        item {
            CommandCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldSuccess)
                    Text(
                        text = "فحص الجودة والسلامة البرمجية (Security & Lint Audit)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = auditStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRunAudit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_studio_audit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إجراء فحص فوري لكافة ملفات المشروع")
                }
            }
        }
    }
}
