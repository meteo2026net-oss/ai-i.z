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


    // --- Linked accounts / projects ---
    fun projectsManageTitle(language: String?): String =
        if (isEnglish(language)) "Manage software projects" else "إدارة المشاريع البرمجية"
    fun projectsCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n projects" else "$n مشاريع"
    fun projectsIntro(language: String?): String =
        if (isEnglish(language))
            "Create a profile for any project and link GitHub, a database, and AI agent priority."
        else
            "أنشئ ملفاً لأي مشروع، واربطه بحساب GitHub وقاعدة بيانات وأولوية وكلاء AI الخاصة به."
    fun statusActive(language: String?): String =
        if (isEnglish(language)) "Active" else "نشط"
    fun projectActivate(language: String?): String =
        if (isEnglish(language)) "Activate project" else "تفعيل المشروع"
    fun projectWorkplanFile(language: String?): String =
        if (isEnglish(language)) "Plan & collaboration file" else "ملف الخطة والتعاون"
    fun projectAiPriority(language: String?): String =
        if (isEnglish(language)) "AI priority" else "أولوية AI"
    fun projectDeleteCd(language: String?): String =
        if (isEnglish(language)) "Delete project" else "حذف المشروع"
    fun projectNewName(language: String?): String =
        if (isEnglish(language)) "New project name" else "اسم المشروع الجديد"
    fun projectGithubRepo(language: String?): String =
        if (isEnglish(language)) "GitHub repository (owner/repo)" else "مستودع GitHub (owner/repo)"
    fun projectDefaultBranch(language: String?): String =
        if (isEnglish(language)) "Default branch" else "الفرع الافتراضي"
    fun projectLinkGithub(language: String?): String =
        if (isEnglish(language)) "Link GitHub account:" else "ربط بحساب GitHub:"
    fun projectLinkDatabase(language: String?): String =
        if (isEnglish(language)) "Link database:" else "ربط بقاعدة بيانات:"
    fun projectProtectedDbs(language: String?): String =
        if (isEnglish(language)) "Protected databases for this project (CSV)" else "قواعد بيانات محمية خاصة بهذا المشروع (CSV)"
    fun projectWorkFileInRepo(language: String?): String =
        if (isEnglish(language)) "Single work file inside the repo folder" else "ملف العمل الواحد داخل مجلد المستودع"
    fun projectRequirements(language: String?): String =
        if (isEnglish(language)) "Work requirements for the new project" else "متطلبات العمل اللازمة للمشروع الجديد"
    fun projectRequirementsPh(language: String?): String =
        if (isEnglish(language))
            "List features, screens, APIs, and success criteria for this project…"
        else
            "اذكر المزايا، الشاشات، الـ APIs، وشروط النجاح لهذا المشروع..."
    fun projectAgentInstructions(language: String?): String =
        if (isEnglish(language)) "Special instructions for the agent and models on this project" else "تعليمات خاصة للوكيل والنماذج في هذا المشروع"
    fun projectMultiModelCollab(language: String?): String =
        if (isEnglish(language)) "Multi-model collaboration on the shared file" else "العمل التعاوني لعدة نماذج معاً على الملف المشترك"
    fun projectContinuousTasks(language: String?): String =
        if (isEnglish(language)) "Follow tasks to completion with automatic failover" else "متابعة المهام للنهاية مع التحويل التلقائي للبدائل"
    fun projectSaveActivate(language: String?): String =
        if (isEnglish(language)) "Save and activate project with its requirements" else "حفظ وتفعيل المشروع بمتطلباته"
    fun googleLinkedAccounts(language: String?): String =
        if (isEnglish(language)) "Linked Google accounts" else "حسابات Google المرتبطة"
    fun googleRegisteredCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n registered" else "$n مسجل"
    fun googleAiIzAccount(language: String?): String =
        if (isEnglish(language)) "AI I.Z account" else "حساب AI I.Z"
    fun googleActiveAccount(language: String?, name: String, email: String): String =
        if (isEnglish(language)) "Currently active account: $name ($email)" else "الحساب النشط حالياً: $name ($email)"
    fun googleUserFallback(language: String?): String =
        if (isEnglish(language)) "Google user" else "مستخدم Google"
    fun actionSwitch(language: String?): String =
        if (isEnglish(language)) "Switch" else "تبديل"
    fun googleSwitchOrLink(language: String?): String =
        if (isEnglish(language)) "Switch or link another Google account" else "تبديل أو ربط حساب Google آخر"
    fun githubMultiAccounts(language: String?): String =
        if (isEnglish(language)) "Multiple GitHub accounts" else "حسابات GitHub المتعددة"
    fun githubLinkedCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n linked" else "$n مربوط"
    fun githubMultiIntro(language: String?): String =
        if (isEnglish(language))
            "Link more than one GitHub account (personal, org, client) and attach a preferred repo for automatic switching."
        else
            "يمكنك ربط أكثر من حساب GitHub (شخصي، مؤسسة، عميل) وربط كل حساب بمستودع مفضل ليتم التبديل تلقائياً."
    fun githubNoneYet(language: String?): String =
        if (isEnglish(language)) "No GitHub accounts added yet." else "لا توجد حسابات GitHub مضافة بعد."
    fun githubLinkedRepo(language: String?, repo: String): String =
        if (isEnglish(language)) "Linked repository: $repo" else "المستودع المرتبط: $repo"
    fun githubActivateAccount(language: String?): String =
        if (isEnglish(language)) "Activate account" else "تفعيل الحساب"
    fun githubAccountLabel(language: String?): String =
        if (isEnglish(language)) "Account label (e.g. METRA or work)" else "اسم تعريفي للحساب (مثلاً: حساب METRA أو حساب العمل)"

    // --- Database ---
    fun dbIsolatedBlocked(language: String?, list: String): String =
        if (isEnglish(language)) "Blocked isolated databases: $list. Connection strings and secrets stay encrypted via Android Keystore."
        else "القواعد المعزولة المحظورة: $list. تُحفظ جميع سلاسل الاتصال والأسرار (Secrets) مشفرة عبر Android Keystore."
    fun dbSavedConnections(language: String?, n: Int): String =
        if (isEnglish(language)) "Saved database connections ($n)" else "اتصالات قواعد البيانات المحفوظة ($n)"
    fun dbEncryptedChain(language: String?): String =
        if (isEnglish(language)) "Encrypted connection string ✓ " else "سلسلة اتصال مشفرة ✓ "
    fun dbReadOnlyMode(language: String?): String =
        if (isEnglish(language)) "• Safe read-only mode" else "• وضع القراءة الآمن"
    fun dbReadWriteT3(language: String?): String =
        if (isEnglish(language)) "• Read/write with T3 approval" else "• قراءة/كتابة بموافقة T3"
    fun actionActivate(language: String?): String =
        if (isEnglish(language)) "Activate" else "تفعيل"
    fun dbSqlCheck(language: String?): String =
        if (isEnglish(language)) "SQL check" else "فحص SQL"
    fun dbDeleteCd(language: String?): String =
        if (isEnglish(language)) "Delete database" else "حذف القاعدة"
    fun dbLinkNew(language: String?): String =
        if (isEnglish(language)) "Link a new database and enter its secret" else "ربط قاعدة بيانات جديدة وإدخال Secret الخاص بها"
    fun dbDisplayName(language: String?): String =
        if (isEnglish(language)) "Display name (e.g. Supabase Staging)" else "الاسم التعريفي (مثلاً: Supabase Staging)"
    fun dbNameLabel(language: String?): String =
        if (isEnglish(language)) "Database name" else "اسم قاعدة البيانات"
    fun dbConnectionSecret(language: String?): String =
        if (isEnglish(language)) "Connection string / DB secret (encrypted locally)" else "Connection String / DB Secret (يُشفر محلياً)"
    fun dbOptionalToken(language: String?): String =
        if (isEnglish(language)) "Supabase key / DB token secret (optional)" else "Supabase Key / DB Token Secret (اختياري)"
    fun dbBlockedList(language: String?): String =
        if (isEnglish(language)) "Blocked protected databases (comma-separated)" else "قواعد بيانات محظورة ومحمية (مفصولة بفاصلة)"
    fun dbReadOnlyToggle(language: String?): String =
        if (isEnglish(language)) "Isolate as read-only permissions" else "عزل صلاحيات القراءة فقط (Read-Only)"
    fun dbSaveConnection(language: String?): String =
        if (isEnglish(language)) "Save database connection and secret" else "حفظ اتصال قاعدة البيانات والـ Secret"
    fun dbFetchSchema(language: String?): String =
        if (isEnglish(language)) "Fetch table structure" else "جلب بنية الجداول"
    fun dbSqlReview(language: String?): String =
        if (isEnglish(language)) "SQL review & security" else "مراجعة SQL والأمان"
    fun dbDatabaseLabel(language: String?, name: String): String =
        if (isEnglish(language)) "Database: $name" else "القاعدة: $name"
    fun dbRlsOn(language: String?): String =
        if (isEnglish(language)) "RLS on" else "RLS مفعّل"
    fun dbRlsOff(language: String?): String =
        if (isEnglish(language)) "No RLS" else "بدون RLS"
    fun sqlReviewTitle(language: String?): String =
        if (isEnglish(language)) "SQL review, migrations & security" else "مراجعة SQL وملفات Migrations والأمان"
    fun sqlTargetDb(language: String?): String =
        if (isEnglish(language)) "Target database" else "قاعدة البيانات المستهدفة"
    fun sqlSecurityCheck(language: String?): String =
        if (isEnglish(language)) "Security impact check / safe run" else "فحص الأثر الأمني / تنفيذ آمن"
    fun sqlAiAnalysis(language: String?): String =
        if (isEnglish(language)) "Security analysis & expected impact (AI)" else "تحليل الأمان والأثر المتوقع (AI)"
    fun sqlResultRows(language: String?, n: Int): String =
        if (isEnglish(language)) "Execution result ($n rows)" else "نتيجة التنفيذ ($n صف)"

    // --- Setup / repo common ---
    fun setupGithubTitle(language: String?): String =
        if (isEnglish(language)) "Connect GitHub" else "ربط GitHub"
    fun setupAiTitle(language: String?): String =
        if (isEnglish(language)) "AI agents, priority & backend" else "وكلاء AI والأولوية وBackend"
    fun repoBranchesTitle(language: String?): String =
        if (isEnglish(language)) "Branches & commits" else "الفروع والـ Commits"
    fun repoFilesTitle(language: String?): String =
        if (isEnglish(language)) "File explorer" else "مستكشف الملفات"
    fun loadingGeneric(language: String?): String =
        if (isEnglish(language)) "Loading…" else "جاري التحميل…"
    fun emptyGeneric(language: String?): String =
        if (isEnglish(language)) "Nothing to show yet." else "لا يوجد محتوى للعرض بعد."
    fun errorGeneric(language: String?): String =
        if (isEnglish(language)) "Something went wrong. Try again." else "حدث خطأ. حاول مجدداً."
    fun retryAction(language: String?): String =
        if (isEnglish(language)) "Retry" else "إعادة المحاولة"
    fun saveAction(language: String?): String =
        if (isEnglish(language)) "Save" else "حفظ"
    fun confirmAction(language: String?): String =
        if (isEnglish(language)) "Confirm" else "تأكيد"


    fun setupGithubAccountsTitle(language: String?): String =
        if (isEnglish(language)) "Link real GitHub accounts" else "ربط حسابات GitHub الحقيقية"
    fun setupGithubQuickSwitch(language: String?): String =
        if (isEnglish(language)) "Quick switch between linked GitHub accounts:" else "التبديل السريع بين حسابات GitHub المرتبطة:"
    fun setupVerifyLink(language: String?): String =
        if (isEnglish(language)) "Verify & link" else "تحقق وربط"
    fun setupRefresh(language: String?): String =
        if (isEnglish(language)) "Refresh" else "تحديث"
    fun setupManageProjectsMulti(language: String?): String =
        if (isEnglish(language)) "Manage projects and multi-account links" else "إدارة المشاريع وربط الحسابات المتعددة"
    fun setupSavedProjectsActiveRepo(language: String?): String =
        if (isEnglish(language)) "Saved projects and active repository" else "المشاريع المحفوظة والمستودع النشط"
    fun setupAnyGithubRepo(language: String?): String =
        if (isEnglish(language)) "Any GitHub repository (owner/repo)" else "أي مستودع GitHub (owner/repo)"
    fun setupActiveBranch(language: String?): String =
        if (isEnglish(language)) "Active branch" else "الفرع النشط"
    fun setupAdoptRepoBranch(language: String?): String =
        if (isEnglish(language)) "Adopt repository and branch" else "اعتماد المستودع والفرع"
    fun setupAiPoolTitle(language: String?): String =
        if (isEnglish(language)) "AI pool and automatic failover" else "مسبح الذكاء الاصطناعي والتحويل التلقائي"
    fun setupEnableQuotaNotifs(language: String?): String =
        if (isEnglish(language)) "Enable quota exhaustion notifications" else "تفعيل إشعارات انتهاء الحصة"
    fun setupReenableAll(language: String?): String =
        if (isEnglish(language)) "Re-enable all" else "إعادة تفعيل الكل"
    fun setupViewAiConversations(language: String?): String =
        if (isEnglish(language)) "View past ChatGPT and Gemini conversations and accounts" else "عرض محادثات ومعلومات حسابات ChatGPT وGemini السابقة"
    fun setupConsumptionAlerts(language: String?): String =
        if (isEnglish(language)) "Recent consumption and failover alerts" else "تنبيهات الاستهلاك والتحويل الأخيرة"
    fun setupReenable(language: String?): String =
        if (isEnglish(language)) "Re-enable" else "إعادة تفعيل"
    fun setupCollabTeam(language: String?): String =
        if (isEnglish(language)) "Models collaborating on the unified work file" else "فريق النماذج المتعاونة على الملف الموحد"
    fun setupOpenCollabRoom(language: String?): String =
        if (isEnglish(language)) "Open collaboration room and shared workplan" else "فتح غرفة التعاون وملف العمل الموحد"
    fun setupAddAiAccount(language: String?): String =
        if (isEnglish(language)) "Add an AI account or key to the pool" else "إضافة حساب أو مفتاح AI جديد للمسبح"
    fun setupAiAccountName(language: String?): String =
        if (isEnglish(language)) "Account/key name (e.g. Gemini second account)" else "اسم الحساب/المفتاح (مثلاً: Gemini حسابي الثاني)"


    // --- Repo screens ---
    fun repoExplorerUnlinked(language: String?): String =
        if (isEnglish(language)) "File explorer is not connected" else "مستكشف الملفات غير مربوط"
    fun repoSearchFiles(language: String?, repo: String, branch: String): String =
        if (isEnglish(language)) "Search files in $repo ($branch)" else "بحث في ملفات $repo ($branch)"
    fun repoDownloadFileCd(language: String?): String =
        if (isEnglish(language)) "Download file to device" else "تنزيل الملف إلى الهاتف"
    fun repoNoFileSelected(language: String?): String =
        if (isEnglish(language)) "No file selected yet" else "لم يتم اختيار ملف بعد"
    fun repoOpenExplorer(language: String?): String =
        if (isEnglish(language)) "Open file explorer" else "فتح مستكشف الملفات"
    fun repoAiReview(language: String?): String =
        if (isEnglish(language)) "AI review" else "مراجعة AI"
    fun repoShowEditor(language: String?): String =
        if (isEnglish(language)) "Show editor" else "عرض المحرر"
    fun repoShowDiff(language: String?): String =
        if (isEnglish(language)) "Compare before/after" else "مقارنة قبل/بعد"
    fun repoDownloading(language: String?): String =
        if (isEnglish(language)) "Downloading…" else "جارٍ التحميل..."
    fun repoDownloadToPhone(language: String?): String =
        if (isEnglish(language)) "Download file directly to device" else "تنزيل الملف مباشرة إلى الهاتف"
    fun repoAiReviewResult(language: String?): String =
        if (isEnglish(language)) "AI review result" else "نتيجة مراجعة الذكاء الاصطناعي"
    fun repoPushRequiresApproval(language: String?): String =
        if (isEnglish(language)) "Push change (tier 2 — requires approval)" else "رفع التعديل (المستوى 2 — يتطلب موافقة)"
    fun repoCommitMessage(language: String?): String =
        if (isEnglish(language)) "Commit message" else "رسالة Commit"
    fun repoRequestPushApproval(language: String?): String =
        if (isEnglish(language)) "Request approval to push to GitHub" else "طلب موافقة Push إلى GitHub"
    fun repoGithubRequired(language: String?): String =
        if (isEnglish(language)) "GitHub connection required" else "ربط GitHub مطلوب"
    fun repoNewBranchTier1(language: String?): String =
        if (isEnglish(language)) "Create a new work branch (tier 1)" else "إنشاء فرع عمل جديد (المستوى 1)"
    fun repoNewBranchName(language: String?, from: String): String =
        if (isEnglish(language)) "New branch name (from $from)" else "اسم الفرع الجديد (تفرع من $from)"
    fun repoCreateBranch(language: String?): String =
        if (isEnglish(language)) "Create branch" else "إنشاء الفرع"
    fun repoBranchesCount(language: String?, n: Int): String =
        if (isEnglish(language)) "Branches ($n)" else "الفروع ($n)"
    fun repoLatestCommits(language: String?, n: Int): String =
        if (isEnglish(language)) "Latest commits ($n)" else "أحدث Commits ($n)"
    fun repoCreateDraftPr(language: String?): String =
        if (isEnglish(language)) "Create draft pull request (tier 2)" else "إنشاء Draft Pull Request (المستوى 2)"
    fun repoPrTitle(language: String?): String =
        if (isEnglish(language)) "PR title" else "عنوان PR"
    fun repoPrFromBranch(language: String?): String =
        if (isEnglish(language)) "From branch" else "من فرع"
    fun repoPrToBranch(language: String?): String =
        if (isEnglish(language)) "To branch" else "إلى فرع"
    fun repoPrSummary(language: String?): String =
        if (isEnglish(language)) "Change summary" else "ملخص التغييرات"
    fun repoRequestDraftPr(language: String?): String =
        if (isEnglish(language)) "Request approval to create draft PR" else "طلب موافقة إنشاء Draft PR"
    fun repoPrAiReview(language: String?): String =
        if (isEnglish(language)) "AI review of pull request" else "مراجعة الذكاء الاصطناعي لـ Pull Request"
    fun repoPrChangedFiles(language: String?): String =
        if (isEnglish(language)) "Changed files in selected PR" else "ملفات التغيير في PR المحدد"
    fun repoReviewFilesAi(language: String?): String =
        if (isEnglish(language)) "Review files & AI" else "مراجعة الملفات وAI"
    fun repoRequestMergeT3(language: String?): String =
        if (isEnglish(language)) "Request merge (T3)" else "طلب دمج (T3)"
    fun repoActionsLog(language: String?): String =
        if (isEnglish(language)) "GitHub Actions log" else "سجل GitHub Actions"
    fun repoNoActions(language: String?): String =
        if (isEnglish(language)) "No Actions runs available right now" else "لا توجد تشغيلات Actions متاحة حالياً"

    // --- Shared workspace ---
    fun workspaceTitle(language: String?): String =
        if (isEnglish(language)) "Collaboration room & unified work file" else "غرفة التعاون وملف العمل الموحد"
    fun workspaceContinuousOn(language: String?): String =
        if (isEnglish(language)) "Continuous follow-through" else "متابعة مستمرة"
    fun workspaceContinuousOff(language: String?): String =
        if (isEnglish(language)) "Manually stopped" else "متوقف يدوياً"
    fun workspaceFilePath(language: String?): String =
        if (isEnglish(language)) "Single work file path inside the repository" else "مسار الملف الواحد في المستودع (داخل مجلد)"
    fun workspaceRequirements(language: String?): String =
        if (isEnglish(language)) "Work requirements for the project" else "متطلبات العمل اللازمة للمشروع"
    fun workspaceRequirementsPh(language: String?): String =
        if (isEnglish(language))
            "Write requirements for the current or new project: screens, APIs, rules, quality criteria…"
        else
            "اكتب متطلبات المشروع الجديد أو الحالي: الشاشات، الواجهات البرمجية، القواعد، شروط الجودة..."
    fun workspaceAgentInstructions(language: String?): String =
        if (isEnglish(language)) "Instructions for the agent and models" else "التعليمات التي سيعمل بها الوكيل والنماذج"
    fun workspaceRoundGoal(language: String?): String =
        if (isEnglish(language)) "Goal or guidance for this round (optional)" else "توجيه أو هدف هذه الجولة (اختياري)"
    fun workspaceRoundGoalPh(language: String?): String =
        if (isEnglish(language))
            "Example: prepare the full work plan for the requirements and update coding instructions"
        else
            "مثال: جهّز خطة العمل الكاملة للمتطلبات وحدّث التعليمات البرمجية"
    fun workspaceStartCollab(language: String?): String =
        if (isEnglish(language)) "Start model collaboration and update shared file" else "بدء تعاون النماذج وتحديث الملف المشترك"
    fun workspaceModelContributions(language: String?): String =
        if (isEnglish(language)) "Model contributions in the last round" else "مساهمات النماذج في آخر جولة"
    fun workspaceDirectEdit(language: String?): String =
        if (isEnglish(language)) "Direct edit of the unified work file (Markdown)" else "تحرير مباشر لملف العمل الموحد (Markdown)"
    fun workspacePreviewChanges(language: String?): String =
        if (isEnglish(language)) "Preview file changes:" else "معاينة التغييرات على الملف:"
    fun workspaceCommitMsg(language: String?, repo: String, branch: String): String =
        if (isEnglish(language)) "Commit message to push the file to $repo ($branch)" else "رسالة Commit لرفع الملف إلى $repo ($branch)"
    fun workspaceRequestPush(language: String?): String =
        if (isEnglish(language)) "Request approval and push change to GitHub" else "طلب موافقة ورفع التعديل إلى GitHub"

    // --- AI conversations ---
    fun aiConvTitle(language: String?): String =
        if (isEnglish(language)) "ChatGPT & Gemini accounts and agent conversations" else "حسابات ChatGPT وGemini ومحادثات الوكلاء"
    fun aiConvAccountsCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n accounts added" else "$n حسابات مضافة"
    fun aiConvManageAccounts(language: String?): String =
        if (isEnglish(language)) "+ Manage and add ChatGPT/Gemini accounts and keys" else "+ إدارة وإضافة حسابات ChatGPT وGemini ومفاتيحهم"
    fun aiConvPickAccount(language: String?): String =
        if (isEnglish(language)) "Choose an account to view its info and past conversations:" else "اختر الحساب لعرض معلوماته ومحادثاته السابقة:"
    fun aiConvNoAccounts(language: String?): String =
        if (isEnglish(language)) "No AI accounts added yet." else "لا توجد حسابات AI مضافة حالياً."
    fun aiConvCopied(language: String?): String =
        if (isEnglish(language)) "Copied ✓" else "تم النسخ ✓"
    fun aiConvCopy(language: String?): String =
        if (isEnglish(language)) "Copy conversations" else "نسخ المحادثات"
    fun aiConvEmpty(language: String?): String =
        if (isEnglish(language)) "No past conversations recorded yet" else "لا توجد محادثات سابقة مسجلة بعد"


    fun workspaceAgentPlaceholder(language: String?): String =
        if (isEnglish(language))
            "Example: read the structure first, split work, test each change, and save results in the shared file."
        else
            "مثال: اقرأ البنية أولاً، قسّم العمل، اختبر كل تعديل، واحفظ النتائج في الملف المشترك."
    fun workspaceCollabMulti(language: String?): String =
        if (isEnglish(language)) "Multi-model collaboration" else "العمل التعاوني لعدة نماذج معاً"
    fun workspaceContinuousFollow(language: String?): String =
        if (isEnglish(language)) "Continuous follow-through to completion" else "المتابعة المستمرة للنهاية بدون توقف"
    fun workspaceSaveSpecs(language: String?): String =
        if (isEnglish(language)) "Save specifications" else "حفظ المواصفات"
    fun workspaceFetchGithub(language: String?): String =
        if (isEnglish(language)) "Fetch from GitHub" else "جلب من GitHub"
    fun workspaceTeamModels(language: String?, n: Int): String =
        if (isEnglish(language)) "Models in collaboration ($n)" else "النماذج المشاركة في التعاون ($n)"
    fun workspaceAddModels(language: String?): String =
        if (isEnglish(language)) "Add models/accounts" else "إضافة نماذج/حسابات"
    fun workspaceRunRoundNow(language: String?): String =
        if (isEnglish(language)) "Run a model collaboration round now" else "تشغيل جولة تعاون النماذج الآن"
    fun studioOpen(language: String?): String =
        if (isEnglish(language)) "Open" else "فتح"
    fun studioOpened(language: String?): String =
        if (isEnglish(language)) "Open" else "مفتوح"
    fun studioBuildCmds(language: String?): String =
        if (isEnglish(language)) "Build & setup commands:" else "أوامر البناء والتهيئة (Build):"
    fun studioRunCmds(language: String?): String =
        if (isEnglish(language)) "Run / start command:" else "أمر التشغيل المباشر (Run / Start):"
    fun studioTestCmds(language: String?): String =
        if (isEnglish(language)) "Automated test command:" else "أمر تشغيل الاختبارات الآلية (Test):"
    fun studioCopiedToast(language: String?): String =
        if (isEnglish(language)) "Code copied to clipboard" else "تم نسخ الكود للحافظة"
    fun logsCopyClipboard(language: String?): String =
        if (isEnglish(language)) "Copy to clipboard" else "نسخ للحافظة"
    fun logsCopied(language: String?): String =
        if (isEnglish(language)) "Copied ✓" else "تم النسخ ✓"
    fun securityGrantNotifPermission(language: String?): String =
        if (isEnglish(language)) "Grant notification permission on device" else "منح صلاحية الإشعارات على الهاتف"
    fun securityApprovalsLog(language: String?, n: Int): String =
        if (isEnglish(language)) "Approvals log ($n)" else "سجل الموافقات ($n)"
    fun securitySecretsSession(language: String?): String =
        if (isEnglish(language)) "Secrets & session management" else "إدارة الأسرار والجلسة"


    fun linkedRegisteredCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n registered" else "$n مسجل"
    fun linkedGithubLinkedCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n linked" else "$n مربوط"
    fun linkedGithubRemoveCd(language: String?): String =
        if (isEnglish(language)) "Remove GitHub account" else "إزالة حساب GitHub"
    fun linkedAutoRepo(language: String?): String =
        if (isEnglish(language)) "Auto-link repository (optional — owner/repo)" else "ربط بمستودع تلقائياً (اختياري — owner/repo)"
    fun linkedAddGithub(language: String?): String =
        if (isEnglish(language)) "Add and link GitHub account" else "إضافة وربط حساب GitHub"
    fun linkedAiBackendEnvs(language: String?): String =
        if (isEnglish(language)) "AI environments & Backend" else "بيئات الذكاء الاصطناعي وBackend"
    fun linkedSavedCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n saved" else "$n محفوظة"
    fun linkedActivateEnv(language: String?): String =
        if (isEnglish(language)) "Activate environment" else "تفعيل البيئة"
    fun linkedDeleteEnvCd(language: String?): String =
        if (isEnglish(language)) "Delete environment" else "حذف البيئة"
    fun linkedAddOrEditEnv(language: String?): String =
        if (isEnglish(language)) "Add or edit AI / Backend environment" else "إضافة أو تعديل بيئة AI / Backend جديدة"
    fun linkedBundlesTitle(language: String?): String =
        if (isEnglish(language)) "Unified link bundles (accounts together)" else "حزم الربط الموحدة (ربط الحسابات معاً)"
    fun linkedActiveBundle(language: String?): String =
        if (isEnglish(language)) "Active bundle" else "حزمة نشطة"
    fun linkedActivateBundle(language: String?): String =
        if (isEnglish(language)) "Activate bundle" else "تفعيل الحزمة"
    fun linkedSwitchGoogle(language: String?): String =
        if (isEnglish(language)) "Switch Google" else "تبديل Google"
    fun linkedDeleteBundleCd(language: String?): String =
        if (isEnglish(language)) "Delete bundle" else "حذف الحزمة"
    fun linkedBundleName(language: String?): String =
        if (isEnglish(language)) "Unified bundle name (e.g. METRA core)" else "اسم الحزمة الموحدة (مثلاً: حزمة METRA الأساسية)"
    fun linkedPickGithubForBundle(language: String?): String =
        if (isEnglish(language)) "Choose GitHub account for the bundle:" else "اختر حساب GitHub للحزمة:"
    fun linkedPickAiForBundle(language: String?): String =
        if (isEnglish(language)) "Choose AI / Backend environment for the bundle:" else "اختر بيئة AI / Backend للحزمة:"
    fun linkedBundleRepo(language: String?): String =
        if (isEnglish(language)) "Linked repository (owner/repo)" else "المستودع المرتبط (owner/repo)"
    fun linkedSaveBundle(language: String?): String =
        if (isEnglish(language)) "Save and activate unified link bundle" else "حفظ وتفعيل حزمة الربط الموحدة"
    fun setupAiAvailableCount(language: String?, n: Int): String =
        if (isEnglish(language)) "$n available" else "$n متاح"
    fun setupPriorityUpCd(language: String?): String =
        if (isEnglish(language)) "Raise priority" else "رفع الأولوية"
    fun setupPriorityDownCd(language: String?): String =
        if (isEnglish(language)) "Lower priority" else "خفض الأولوية"
    fun setupDeleteProviderCd(language: String?): String =
        if (isEnglish(language)) "Delete provider" else "حذف المزود"
    fun setupAccountNote(language: String?): String =
        if (isEnglish(language)) "Account note or key owner email (optional)" else "ملاحظة الحساب أو البريد المالك للمفتاح (اختياري)"
    fun setupModelId(language: String?): String =
        if (isEnglish(language)) "Model ID" else "اسم النموذج (Model ID)"
    fun setupBaseUrlOptional(language: String?): String =
        if (isEnglish(language)) "Base URL (optional customization)" else "Base URL (اختياري للتخصيص)"
    fun setupAddToAiPool(language: String?): String =
        if (isEnglish(language)) "Add to AI pool and project priority" else "إضافة إلى مسبح الذكاء الاصطناعي وأولوية المشروع"
    fun setupBackendRunnerTitle(language: String?): String =
        if (isEnglish(language)) "Execution server (Backend Runner)" else "خادم التنفيذ (Backend Runner)"
    fun setupConnected(language: String?): String =
        if (isEnglish(language)) "Connected" else "متصل"
    fun setupOptional(language: String?): String =
        if (isEnglish(language)) "Optional" else "اختياري"
    fun setupBackendUrl(language: String?): String =
        if (isEnglish(language)) "Backend server URL (HTTPS)" else "رابط خادم Backend (HTTPS)"
    fun setupSaveBackend(language: String?): String =
        if (isEnglish(language)) "Save Backend settings" else "حفظ إعدادات Backend"
    fun setupCheckConnection(language: String?): String =
        if (isEnglish(language)) "Check connection" else "فحص الاتصال"
    fun dbSecurityIsolationTitle(language: String?): String =
        if (isEnglish(language)) "Security isolation & database secrets" else "العزل الأمني وإدارة أسرار قواعد البيانات"
    fun dbSchemaRunnerOptional(language: String?): String =
        if (isEnglish(language)) "Optional Backend Schema Runner for live tables" else "Backend Schema Runner اختياري للجداول الحية"
    fun testsNeedBackend(language: String?): String =
        if (isEnglish(language)) "Running tests requires Backend Runner" else "تشغيل الاختبارات يتطلب Backend Runner"
    fun testsRunProject(language: String?, repo: String): String =
        if (isEnglish(language)) "Run project tests: $repo" else "تشغيل اختبارات المشروع: $repo"
    fun testsStartReal(language: String?): String =
        if (isEnglish(language)) "Start real tests" else "بدء الاختبارات الفعلية"
    fun testsSummaryMs(language: String?, ms: Long): String =
        if (isEnglish(language)) "Results summary (${ms} ms)" else "ملخص النتائج ($ms مللي ثانية)"
    fun testsSendFailuresToAgent(language: String?): String =
        if (isEnglish(language)) "Send failures to the agent for repair" else "إرسال الأخطاء للوكيل لإصلاحها"
    fun testsPassed(language: String?): String =
        if (isEnglish(language)) "Passed" else "نجح"
    fun testsFailed(language: String?): String =
        if (isEnglish(language)) "Failed" else "فشل"
    fun notifOnCommandDone(language: String?): String =
        if (isEnglish(language)) "When any command, task, or collaboration round finishes" else "عند إنهاء أي أمر أو مهمة أو جولة تعاون"
    fun notifOnPauseError(language: String?): String =
        if (isEnglish(language)) "On pause, error, or security block" else "عند التوقف المؤقت أو الخطأ أو الحظر الأمني"
    fun notifOnApprovalNeeded(language: String?): String =
        if (isEnglish(language)) "When action or approval is needed (tiers 2 & 3)" else "عند الحاجة لإجراء أو موافقة (المستوى 2 و3)"
    fun notifOnQuotaExhausted(language: String?): String =
        if (isEnglish(language)) "When an API quota is exhausted and failover runs" else "عند انتهاء حصة API والتحويل للبديل"
    fun languageArabic(language: String?): String =
        if (isEnglish(language)) "Arabic" else "العربية"
    fun languageEnglish(language: String?): String =
        if (isEnglish(language)) "English" else "English"
    fun aboutAppName(language: String?): String =
        if (isEnglish(language)) "App name: ai i.z" else "اسم التطبيق: ai i.z"
    fun aboutDeveloper(language: String?): String =
        if (isEnglish(language)) "Developer & owner: Ibrahim Zeinab" else "المطور والمالك: إبراهيم زينب (Ibrahim Zeinab)"
    fun aboutCopyright(language: String?): String =
        if (isEnglish(language)) "All rights reserved © 2026" else "جميع الحقوق محفوظة © 2026"
    fun aboutSecurity(language: String?): String =
        if (isEnglish(language))
            "Security & encryption: the app is protected with advanced encryption and ProGuard/R8 code shrinking to resist reverse engineering."
        else
            "الأمان والتشفير: التطبيق محمي بالكامل عبر خوارزميات التشفير المتقدمة وضغط وإخفاء الأكواد (ProGuard/R8) لمنع تفكيكه أو سرقة أفكاره."


    fun setupPatPlaceholder(language: String?): String =
        if (isEnglish(language)) "ghp_... or github_pat_..." else "ghp_... أو github_pat_..."
    fun dbHostPlaceholder(language: String?): String =
        if (isEnglish(language)) "https://xyzcompany.supabase.co or db.host:5432" else "https://xyzcompany.supabase.co أو db.host:5432"


    fun githubConnected(language: String?): String =
        if (isEnglish(language)) "GitHub connected" else "GitHub متصل"
    fun githubDisconnected(language: String?): String =
        if (isEnglish(language)) "GitHub disconnected" else "GitHub غير متصل"
    fun githubNotLinked(language: String?): String =
        if (isEnglish(language)) "GitHub not linked" else "GitHub غير مربوط"
    fun agentRunning(language: String?): String =
        if (isEnglish(language)) "Agent running" else "الوكيل يعمل"
    fun controlPanel(language: String?): String =
        if (isEnglish(language)) "Control panel" else "لوحة التحكم"
    fun overviewTitle(language: String?): String =
        if (isEnglish(language)) "Overview" else "نظرة عامة"
    fun allEnvsAndLanguages(language: String?): String =
        if (isEnglish(language)) "All environments & languages" else "كافة البيئات واللغات"
    fun githubRequiredLiveData(language: String?): String =
        if (isEnglish(language)) "GitHub link required for live data" else "ربط GitHub مطلوب للبيانات الحية"
    fun agentStatusLabel(language: String?, status: String): String =
        if (isEnglish(language)) "Agent status: $status" else "حالة الوكيل: $status"
    fun aiAvailableCount(language: String?, n: Int): String =
        if (isEnglish(language)) "AI: $n ready" else "AI: $n متاح"
    fun backendConnected(language: String?): String =
        if (isEnglish(language)) "Backend connected" else "Backend متصل"
    fun backendOptional(language: String?): String =
        if (isEnglish(language)) "Backend optional" else "Backend اختياري"
    fun lastCommitLine(language: String?, detail: String): String =
        if (isEnglish(language)) "Latest commit: $detail" else "آخر Commit: $detail"
    fun workspaceSmartSpace(language: String?): String =
        if (isEnglish(language)) "Your smart workspace to build and manage apps" else "مساحة عملك الذكية لبناء التطبيقات وإدارتها"
    fun contentDescSend(language: String?): String =
        if (isEnglish(language)) "Send" else "إرسال"
    fun contentDescExportZip(language: String?): String =
        if (isEnglish(language)) "Export project as ZIP" else "تصدير المشروع كملف ZIP"
    fun contentDescCopyCode(language: String?): String =
        if (isEnglish(language)) "Copy code" else "نسخ الكود"
    fun contentDescSaveEdits(language: String?): String =
        if (isEnglish(language)) "Save edits" else "حفظ التعديلات"
    fun contentDescRefresh(language: String?): String =
        if (isEnglish(language)) "Refresh" else "تحديث"
    fun contentDescAppLogo(language: String?): String =
        if (isEnglish(language)) "AI I.Z agent logo" else "شعار وكيل METRA"
    fun notifTaskBlocked(language: String?, title: String): String =
        if (isEnglish(language)) "Task stopped due to a security block: $title" else "توقفت المهمة بسبب حظر أمني: $title"
    fun notifTaskPaused(language: String?, title: String): String =
        if (isEnglish(language)) "Task paused: $title" else "توقف مؤقت للمهمة: $title"
    fun notifTaskCompleted(language: String?, title: String): String =
        if (isEnglish(language)) "Task completed: $title" else "اكتملت المهمة: $title"
    fun notifQuotaWarning(language: String?): String =
        if (isEnglish(language)) "AI API quota warning" else "⚠️ تنبيه حصة AI API"
    fun notifAgentWorking(language: String?): String =
        if (isEnglish(language)) "METRA Agent notifications are working" else "🔔 إشعارات METRA Agent تعمل"

}
