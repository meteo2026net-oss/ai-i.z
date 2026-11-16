package com.example.ui.i18n

/**
 * Centralized UI copy for Arabic (default, RTL) and English (LTR).
 * Driven by UserSettings.appLanguage. Navigation and shared chrome use this first.
 */
object AppUiStrings {

    fun isEnglish(language: String?): Boolean =
        language.equals("en", ignoreCase = true)

    fun tabLabel(route: String, language: String?): String {
        val en = isEnglish(language)
        return when (route) {
            "dashboard" -> if (en) "Home" else "الرئيسية"
            "universal_studio" -> if (en) "Studio" else "الاستوديو"
            "chat" -> if (en) "Agent" else "الوكيل"
            "tasks" -> if (en) "Tasks" else "المهام"
            "files" -> if (en) "Repository" else "المستودع"
            "database" -> if (en) "Database" else "القاعدة"
            "linked_accounts" -> if (en) "Projects" else "المشاريع"
            "security" -> if (en) "Settings" else "الإعدادات"
            else -> route
        }
    }

    fun screenTitle(route: String, language: String?, projectTitle: String): String {
        val en = isEnglish(language)
        return when (route) {
            "dashboard" -> if (en) "Dashboard: $projectTitle" else "لوحة: $projectTitle"
            "universal_studio" -> if (en) "AI Studio Pro — development & model advisor" else "أستوديو التطوير ومرشح النماذج (AI Studio Pro)"
            "chat" -> if (en) "Agent chat" else "المحادثة مع الوكيل"
            "tasks" -> if (en) "Tasks & execution plan" else "المهام وخطة التنفيذ"
            "files" -> if (en) "File explorer & download" else "مستكشف وتنزيل الملفات"
            "code_editor" -> if (en) "Code editor & review" else "محرر ومراجع الأكواد"
            "branches" -> if (en) "Branches & commits" else "الفروع والـ Commits"
            "prs" -> if (en) "Pull Requests" else "Pull Requests"
            "actions" -> if (en) "GitHub Actions" else "GitHub Actions"
            "database" -> if (en) "Databases & secrets" else "قواعد البيانات والأسرار"
            "sql_review" -> if (en) "SQL review & security" else "مراجعة SQL والأمان"
            "tests" -> if (en) "Tests" else "الاختبارات"
            "logs" -> if (en) "Operation log & export" else "سجل العمليات والتصدير"
            "github_setup" -> if (en) "Connect GitHub" else "ربط GitHub"
            "ai_setup" -> if (en) "AI agents, priority & backend" else "وكلاء AI والأولوية وBackend"
            "ai_conversations" -> if (en) "ChatGPT & Gemini conversations" else "محادثات وحسابات ChatGPT وGemini"
            "security" -> if (en) "Security & permissions" else "إعدادات الأمان والصلاحيات"
            "linked_accounts" -> if (en) "Projects & linked accounts" else "المشاريع والحسابات المتعددة"
            "workspace_plan" -> if (en) "Collaboration room & shared workplan" else "غرفة التعاون وملف العمل الموحد"
            else -> "ai i.z"
        }
    }

    fun defaultProjectTitle(language: String?): String =
        if (isEnglish(language)) "Agent platform" else "منصة الوكيل"

    fun fileDownloadSaved(language: String?, fileName: String): String =
        if (isEnglish(language)) "Downloaded and saved: $fileName" else "تم تنزيل وحفظ الملف: $fileName"

    fun fileDownloadFailed(language: String?, detail: String?): String =
        if (isEnglish(language)) "Could not save the file: ${detail.orEmpty()}" else "تعذر حفظ الملف: ${detail.orEmpty()}"

    fun fileDownloadCancelled(language: String?): String =
        if (isEnglish(language)) "File download cancelled." else "تم إلغاء تنزيل الملف."

    fun signOut(language: String?): String =
        if (isEnglish(language)) "Sign out" else "تسجيل الخروج من الحساب"

    fun deleteAccount(language: String?): String =
        if (isEnglish(language)) "Permanently delete account from server" else "حذف الحساب نهائياً من الخادم"

    fun deleteAccountTitle(language: String?): String =
        if (isEnglish(language)) "Confirm account deletion" else "تأكيد حذف الحساب"

    fun deleteAccountBody(language: String?): String =
        if (isEnglish(language))
            "Your account and all server-stored data (projects, tasks, logs, approvals) will be permanently deleted. This cannot be undone."
        else
            "سيُحذف حسابك وكل بياناتك المخزّنة على خادم AI I.Z (المشاريع، المهام، السجلات، الموافقات) بشكل نهائي. لا يمكن التراجع عن هذا الإجراء."

    fun deleteAccountConfirm(language: String?): String =
        if (isEnglish(language)) "Delete permanently" else "حذف نهائي"

    fun deleteAccountInProgress(language: String?): String =
        if (isEnglish(language)) "Deleting…" else "جاري الحذف…"

    fun cancel(language: String?): String =
        if (isEnglish(language)) "Cancel" else "إلغاء"

    fun darkThemeLabel(language: String?): String =
        if (isEnglish(language)) "Elegant dark mode" else "الوضع الداكن الأنيق"

    fun languageSectionTitle(language: String?): String =
        if (isEnglish(language)) "Interface language" else "لغة الواجهة / Interface language"

    fun languageSectionHint(language: String?): String =
        if (isEnglish(language))
            "Arabic (RTL) or English (LTR). Layout direction updates after save."
        else
            "العربية (RTL) أو English (LTR). يتغير اتجاه التخطيط فوراً بعد الحفظ."

    fun googleSignIn(language: String?): String =
        if (isEnglish(language)) "Sign in with Google" else "تسجيل الدخول باستخدام Google"

    fun googleOauthMissing(language: String?): String =
        if (isEnglish(language))
            "Google OAuth is not configured. Set GOOGLE_WEB_CLIENT_ID."
        else
            "إعداد Google OAuth غير مكتمل. يلزم ضبط GOOGLE_WEB_CLIENT_ID."

    fun studioLibraryTitle(language: String?): String =
        if (isEnglish(language)) "AI I.Z project library" else "مكتبة مشاريع AI I.Z"

    fun studioLibraryEmpty(language: String?): String =
        if (isEnglish(language)) "No saved projects in your account yet." else "لا توجد مشاريع محفوظة في حسابك بعد."

    fun studioDeleteTitle(language: String?): String =
        if (isEnglish(language)) "Delete saved project?" else "حذف المشروع المحفوظ؟"

    fun studioDeleteBody(language: String?, name: String?): String =
        if (isEnglish(language))
            "${name.orEmpty()} will be removed from your cloud library. This cannot be undone."
        else
            "سيُحذف ${name.orEmpty()} من مكتبتك السحابية. لا يمكن التراجع عن هذا الإجراء."

    fun studioDeleteConfirm(language: String?): String =
        if (isEnglish(language)) "Delete" else "حذف"

    fun studioProjectNameLabel(language: String?): String =
        if (isEnglish(language)) "Project name (e.g. my-fintech-app)" else "اسم المشروع (مثال: my-fintech-app)"

    fun studioRequirementsLabel(language: String?): String =
        if (isEnglish(language))
            "Requirements and instructions (features, rules, APIs, design…)"
        else
            "المتطلبات والتعليمات (الميزات، القواعد، الـ APIs، التصميم...)"

    fun studioRequirementsPlaceholder(language: String?): String =
        if (isEnglish(language))
            "Example: full e-commerce app with cart, JWT auth, and a payment gateway…"
        else
            "مثال: تطبيق متجر إلكتروني متكامل مع سلة مشتريات وتوثيق JWT وبوابة دفع..."

    fun studioBuilding(language: String?): String =
        if (isEnglish(language)) "Building project structure and generating code…" else "جاري بناء هيكل المشروع وتوليد الأكواد..."

    fun studioCreateAnalyze(language: String?): String =
        if (isEnglish(language)) "Create project and analyze via model advisor" else "إنشاء المشروع وتحليله عبر مستشار النماذج"

    fun studioAdoptModel(language: String?): String =
        if (isEnglish(language)) "Adopt this model for the project in the AI pool" else "اعتماد هذا النموذج للمشروع في حوض AI"

    fun studioNoOpenProject(language: String?): String =
        if (isEnglish(language)) "No project is open" else "لا يوجد مشروع مفتوح حالياً"

    fun studioNoOpenProjectHint(language: String?): String =
        if (isEnglish(language))
            "Open the project builder tab to create your first app in the environment you choose."
        else
            "توجه إلى تبويب 'منشئ المشاريع' لإنشاء أول تطبيق في البيئة التي تختارها."

    fun studioNeedProjectForRun(language: String?): String =
        if (isEnglish(language))
            "Create or open a project first to view run commands and audits."
        else
            "يرجى إنشاء أو فتح مشروع أولاً لعرض أوامر التشغيل والتدقيق."

    fun studioSecurityScan(language: String?): String =
        if (isEnglish(language)) "Run an immediate scan of all project files" else "إجراء فحص فوري لكافة ملفات المشروع"

    fun aiAgentLabel(language: String?): String =
        if (isEnglish(language)) "AI agent" else "وكيل الذكاء"


    // --- Dashboard ---
    fun dashboardManageProjects(language: String?): String =
        if (isEnglish(language)) "+ Manage projects" else "+ إدارة المشاريع"
    fun dashboardOpenStudio(language: String?): String =
        if (isEnglish(language)) "Open development studio and create a new project" else "فتح أستوديو التطوير وإنشاء مشروع جديد"
    fun dashboardResumeBackgroundTask(language: String?): String =
        if (isEnglish(language)) "Resume background task" else "استئناف المهمة في الخلفية"
    fun dashboardSystemStatus(language: String?): String =
        if (isEnglish(language)) "System status & metrics" else "حالة المنظومة والمقاييس"
    fun dashboardRecentUpdates(language: String?): String =
        if (isEnglish(language)) "Latest real activity" else "آخر المستجدات الفعلية"
    fun dashboardQuickAccess(language: String?): String =
        if (isEnglish(language)) "Quick access to all screens" else "الوصول السريع لجميع الشاشات"

    // --- Tasks ---
    fun tasksBackgroundTitle(language: String?): String =
        if (isEnglish(language)) "Background-connected task (run to completion)" else "مهمة متصلة بالخلفية (حتى النهاية)"
    fun tasksDescriptionLabel(language: String?): String =
        if (isEnglish(language)) "Task description" else "وصف المهمة"
    fun tasksDescriptionPlaceholder(language: String?): String =
        if (isEnglish(language))
            "Example: execute the work plan and keep fixing until tests pass"
        else
            "مثال: نفّذ خطة العمل وتابع الإصلاح حتى نجاح الاختبارات"
    fun tasksLaunch(language: String?): String =
        if (isEnglish(language)) "Launch task and follow to completion" else "إطلاق المهمة والمتابعة للنهاية"
    fun tasksOpenSharedPlan(language: String?, path: String): String =
        if (isEnglish(language)) "Open shared workplan and multi-model collaboration ($path)"
        else "فتح ملف الخطة المشترك وتعاون النماذج ($path)"
    fun tasksEmpty(language: String?): String =
        if (isEnglish(language)) "No tasks recorded yet" else "لا توجد مهام مسجلة بعد"
    fun tasksResumeNow(language: String?): String =
        if (isEnglish(language)) "Continue / resume now" else "متابعة / استئناف الآن"

    // --- Chat ---
    fun chatAiAccounts(language: String?): String =
        if (isEnglish(language)) "AI accounts & conversations" else "حسابات ومحادثات AI"
    fun chatWorkFile(language: String?): String =
        if (isEnglish(language)) "Work file" else "ملف العمل"
    fun chatRunCollaboration(language: String?): String =
        if (isEnglish(language)) "Run multi-model collaboration on the shared file" else "تشغيل تعاون النماذج على الملف المشترك"
    fun chatAgentReady(language: String?, project: String): String =
        if (isEnglish(language)) "Smart agent ready for project: $project" else "الوكيل الذكي جاهز لمشروع: $project"
    fun chatAnalyzing(language: String?): String =
        if (isEnglish(language)) "Analyzing and executing…" else "جارٍ التحليل والتنفيذ..."
    fun chatPlaceholder(language: String?): String =
        if (isEnglish(language)) "Type a command for the agent (e.g. continue METRA development)…" else "اكتب أمراً للوكيل (مثال: تابع تطوير METRA)..."

    // --- Security / settings extras ---
    fun logsExportSpecs(language: String?): String =
        if (isEnglish(language)) "Export Flutter & Backend specs and report" else "تصدير مواصفات Flutter وBackend والتقرير"
    fun logsShareSpecs(language: String?): String =
        if (isEnglish(language)) "Share specs" else "مشاركة المواصفات"
    fun logsEmptyCategory(language: String?): String =
        if (isEnglish(language)) "No operations in this category yet" else "لا توجد عمليات مسجلة في هذا التصنيف بعد"
    fun securityTitle(language: String?): String =
        if (isEnglish(language)) "Security settings & three-tier permissions" else "إعدادات الأمان والصلاحيات ثلاثية المستويات"
    fun securityTier1Auto(language: String?): String =
        if (isEnglish(language)) "Auto-execute tier-1 actions" else "التنفيذ التلقائي للمستوى الأول"
    fun securityNotificationsTitle(language: String?): String =
        if (isEnglish(language)) "Attention alerts & instant notifications" else "إشعارات الانتباه والتنبيهات الفورية"
    fun securityNotifEnabled(language: String?): String =
        if (isEnglish(language)) "Enabled" else "مفعّلة"
    fun securityNotifDenied(language: String?): String =
        if (isEnglish(language)) "Not permitted" else "غير مصرح"
    fun securityClearSecrets(language: String?): String =
        if (isEnglish(language)) "Clear encrypted local GitHub PAT and API keys" else "مسح GitHub PAT ومفاتيح API المشفرة محلياً"
    fun securityIpTitle(language: String?): String =
        if (isEnglish(language)) "Intellectual property & app protection" else "حقوق الملكية الفكرية وحماية التطبيق"
    fun securitySendTestNotif(language: String?): String =
        if (isEnglish(language)) "Send a test notification to verify sound and vibration" else "إرسال إشعار تجريبي للتأكد من الصوت والاهتزاز"

}
