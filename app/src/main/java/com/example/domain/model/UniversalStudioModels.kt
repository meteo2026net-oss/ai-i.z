package com.example.domain.model

/**
 * Universal Development Environment and AI Studio Models
 * Platform: ai i.z by Ibrahim Zeinab (إبراهيم زينب)
 */

enum class DevelopmentEnvironment(
    val title: String,
    val subtitle: String,
    val defaultLanguage: ProgrammingLanguage,
    val category: String,
    val buildTool: String,
    val defaultPort: Int = 3000
) {
    MOBILE_ANDROID(
        title = "تطوير أندرويد (Android Native)",
        subtitle = "Kotlin + Jetpack Compose + Material 3 + Coroutines",
        defaultLanguage = ProgrammingLanguage.KOTLIN,
        category = "موبايل (Mobile)",
        buildTool = "Gradle (Kotlin DSL)",
        defaultPort = 8080
    ),
    MOBILE_FLUTTER(
        title = "تطبيقات متعددة المنصات (Flutter)",
        subtitle = "Dart + Flutter SDK + Riverpod/Bloc + Cross-Platform",
        defaultLanguage = ProgrammingLanguage.DART,
        category = "موبايل (Mobile)",
        buildTool = "Flutter CLI / Pub",
        defaultPort = 8080
    ),
    MOBILE_REACT_NATIVE(
        title = "تطبيقات هجينة (React Native / Expo)",
        subtitle = "TypeScript + React Native + Expo Router",
        defaultLanguage = ProgrammingLanguage.TYPESCRIPT,
        category = "موبايل (Mobile)",
        buildTool = "npm / Metro Bundler",
        defaultPort = 8081
    ),
    WEB_REACT_NEXT(
        title = "تطبيقات الويب المتطورة (Next.js & React)",
        subtitle = "React 19 + Next.js App Router + Tailwind CSS + TypeScript",
        defaultLanguage = ProgrammingLanguage.TYPESCRIPT,
        category = "ويب (Web)",
        buildTool = "npm / Next CLI",
        defaultPort = 3000
    ),
    WEB_VUE_SVELTE(
        title = "واجهات الويب السريعة (Vue 3 / Svelte)",
        subtitle = "Vue 3 Composition API + Svelte 5 + Vite + TypeScript",
        defaultLanguage = ProgrammingLanguage.TYPESCRIPT,
        category = "ويب (Web)",
        buildTool = "Vite / npm",
        defaultPort = 5173
    ),
    BACKEND_PYTHON(
        title = "الخوادم والـ APIs (Python FastAPI / Django)",
        subtitle = "Python 3.12 + FastAPI + Pydantic + SQLAlchemy + AsyncIO",
        defaultLanguage = ProgrammingLanguage.PYTHON,
        category = "خوادم خلفية (Backend)",
        buildTool = "Uvicorn / Poetry / Pip",
        defaultPort = 8000
    ),
    BACKEND_NODE(
        title = "خوادم Node.js وNestJS السريعة",
        subtitle = "TypeScript + NestJS / Express + Prisma ORM + PostgreSQL",
        defaultLanguage = ProgrammingLanguage.TYPESCRIPT,
        category = "خوادم خلفية (Backend)",
        buildTool = "npm / tsx / Nest CLI",
        defaultPort = 4000
    ),
    BACKEND_GO(
        title = "الأنظمة السحابية المصغرة (Go Microservices)",
        subtitle = "Go 1.23 + Gin / Fiber + Concurrency + gRPC + Docker",
        defaultLanguage = ProgrammingLanguage.GO,
        category = "سحابي وأنظمة (Systems/Cloud)",
        buildTool = "go build / go test",
        defaultPort = 8080
    ),
    SYSTEMS_RUST(
        title = "الأنظمة فائقة الأداء والأمان (Rust Actix / Axum)",
        subtitle = "Rust 2024 + Tokio Async + Axum + Memory Safety + WebAssembly",
        defaultLanguage = ProgrammingLanguage.RUST,
        category = "سحابي وأنظمة (Systems/Cloud)",
        buildTool = "Cargo",
        defaultPort = 8080
    ),
    DATA_SCIENCE_AI(
        title = "الذكاء الاصطناعي وتحليل البيانات (AI / ML & PyTorch)",
        subtitle = "Python + PyTorch + HuggingFace + LangChain + Pandas",
        defaultLanguage = ProgrammingLanguage.PYTHON,
        category = "ذكاء اصطناعي (AI/Data)",
        buildTool = "Python Pip / Conda / Jupyter",
        defaultPort = 8888
    ),
    CLOUD_DEVOPS(
        title = "البنية التحتية والحوسبة السحابية (DevOps & CI/CD)",
        subtitle = "Docker Compose + Kubernetes + GitHub Actions + Terraform",
        defaultLanguage = ProgrammingLanguage.BASH,
        category = "سحابي وأنظمة (Systems/Cloud)",
        buildTool = "Docker / Kubectl / Bash",
        defaultPort = 80
    )
}

enum class ProgrammingLanguage(
    val displayName: String,
    val fileExtension: String,
    val syntaxName: String,
    val defaultLinter: String
) {
    KOTLIN("Kotlin", ".kt", "kotlin", "ktlint / detekt"),
    JAVA("Java", ".java", "java", "checkstyle"),
    PYTHON("Python", ".py", "python", "ruff / flake8 / black"),
    TYPESCRIPT("TypeScript", ".ts", "typescript", "eslint / tsc"),
    JAVASCRIPT("JavaScript", ".js", "javascript", "eslint"),
    GO("Go", ".go", "go", "golangci-lint"),
    RUST("Rust", ".rs", "rust", "cargo clippy"),
    DART("Dart", ".dart", "dart", "dart analyze"),
    CPP("C++", ".cpp", "cpp", "clang-tidy"),
    CSHARP("C# (.NET)", ".cs", "csharp", "dotnet format"),
    PHP("PHP", ".php", "php", "phpcs / phpstan"),
    SQL("SQL", ".sql", "sql", "sqlfluff"),
    SWIFT("Swift", ".swift", "swift", "swiftlint"),
    BASH("Bash / Shell", ".sh", "bash", "shellcheck")
}

data class AiModelRecommendation(
    val modelId: String,
    val modelName: String,
    val provider: String,
    val matchScore: Int,
    val strengthBadge: String,
    val contextWindow: String,
    val strengths: List<String>,
    val teamRole: String,
    val rationale: String,
    val isPrimaryPick: Boolean = false
)

data class StudioFileItem(
    val path: String,
    val language: ProgrammingLanguage,
    val content: String,
    val purpose: String,
    val isEntrypoint: Boolean = false
)

data class StudioProjectSpec(
    val id: String,
    val projectName: String,
    val environment: DevelopmentEnvironment,
    val primaryLanguage: ProgrammingLanguage,
    val description: String,
    val recommendedModel: AiModelRecommendation,
    val alternateModels: List<AiModelRecommendation>,
    val files: List<StudioFileItem>,
    val buildInstructions: String,
    val runInstructions: String,
    val testInstructions: String,
    val securityAuditScore: Int = 100,
    val createdAtFormatted: String = "الآن"
)

object ModelAdvisorEngine {

    fun recommendModels(
        environment: DevelopmentEnvironment,
        language: ProgrammingLanguage,
        priority: String = "balanced" // "accuracy", "speed", "huge_context", "balanced", "open_source"
    ): List<AiModelRecommendation> {
        val list = mutableListOf<AiModelRecommendation>()

        when (environment) {
            DevelopmentEnvironment.MOBILE_ANDROID -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-2.0-flash",
                        modelName = "Google Gemini 2.0 Flash",
                        provider = "Google DeepMind",
                        matchScore = 99,
                        strengthBadge = "النموذج الرسمي الأقوى لبيئة أندرويد وCompose",
                        contextWindow = "1,048,576 Tokens",
                        strengths = listOf(
                            "مدرب بشكل كامل على أحدث مكتبات Jetpack Compose وMaterial 3",
                            "توليد فوري فائق السرعة لكافة ملفات المشروع مع المعاينة",
                            "دعم أصيل لميزات الوسائط والمدخلات المتعددة (Multimodal)"
                        ),
                        teamRole = "المهندس المعماري وكبير مطوري Android",
                        rationale = "أفضل نموذج على الإطلاق لإنشاء تطبيقات Kotlin وJetpack Compose المتوافقة مع أحدث معايير Google Play 2026.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 97,
                        strengthBadge = "الأقوى في المنطق المعماري وClean Architecture",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf(
                            "كتابة كود Clean MVVM مع فحص دقيق للـ StateFlow",
                            "بناء اختبارات Robolectric وUnit Tests خالية من العيوب",
                            "معالجة المشاكل المعقدة في Concurrency وCoroutines"
                        ),
                        teamRole = "مراجع الأمان وجودة الأكواد (Code Auditor)",
                        rationale = "يتميز بأعلى دقة في منع أخطاء الذاكرة والـ Race Conditions في تطبيقات الهواتف."
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "gpt-4o",
                        modelName = "OpenAI GPT-4o",
                        provider = "OpenAI",
                        matchScore = 94,
                        strengthBadge = "متعدد القدرات والتكاملات الخارجية",
                        contextWindow = "128,000 Tokens",
                        strengths = listOf("تكامل سلس مع REST APIs وRetrofit", "توليد توثيق كامل للـ SDKs"),
                        teamRole = "مهندس تكامل الواجهات والـ APIs",
                        rationale = "ممتاز في تحويل مواصفات الـ APIs إلى Retrofit Data Models."
                    )
                )
            }

            DevelopmentEnvironment.WEB_REACT_NEXT, DevelopmentEnvironment.WEB_VUE_SVELTE -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 99,
                        strengthBadge = "الأقوى عالمياً في فرونت إند وNext.js وتنسيق Tailwind",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf(
                            "فهم استثنائي لـ Server Components وHydration في React 19",
                            "توليد واجهات مستخدم مذهلة بتنسيقات Tailwind CSS متناسقة",
                            "حل أخطاء الـ TypeScript المعقدة بدقة متناهية"
                        ),
                        teamRole = "كبير مهندسي الويب والواجهات التفاعلية",
                        rationale = "يعد المعيار الذهبي لتوليد كود Next.js نظيف وجاهز للإنتاج دون أي أخطاء ترجمة.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-2.0-flash",
                        modelName = "Google Gemini 2.0 Flash",
                        provider = "Google DeepMind",
                        matchScore = 96,
                        strengthBadge = "الأسرع في توليد صفحات ومكونات كاملة فوراً",
                        contextWindow = "1,048,576 Tokens",
                        strengths = listOf("إنشاء عشرات الملفات والصفحات في ثوان معدودة", "سياق ضخم يستوعب مكتبات الويب بالكامل"),
                        teamRole = "المبرمج المولد السريع (Rapid Prototyper)",
                        rationale = "يتيح بناء تطبيقات ويب كاملة متعددة المسارات بضغطة زر واحدة."
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "deepseek-r1",
                        modelName = "DeepSeek-R1 (Reasoning)",
                        provider = "DeepSeek",
                        matchScore = 93,
                        strengthBadge = "النموذج المفتوح الأقوى في المنطق والخوارزميات",
                        contextWindow = "64,000 Tokens",
                        strengths = listOf("سلاسل تفكير منطقية عميقة", "تكلفة صفرية / كفاءة حسابية فائقة"),
                        teamRole = "مدقق الخوارزميات وتحسين الأداء",
                        rationale = "يتفوق في حل مشاكل الحسابات والتصيير الثقيل في واجهات الويب."
                    )
                )
            }

            DevelopmentEnvironment.BACKEND_PYTHON, DevelopmentEnvironment.DATA_SCIENCE_AI -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-1.5-pro",
                        modelName = "Google Gemini 1.5 Pro",
                        provider = "Google DeepMind",
                        matchScore = 99,
                        strengthBadge = "الأفضل لتحليل مجموعات البيانات والأوراق البحثية الضخمة",
                        contextWindow = "2,097,152 Tokens (الأكبر في العالم)",
                        strengths = listOf(
                            "يستوعب أوراق بحثية وكتب وبيانات كاملة في نافذة سياق واحدة",
                            "إتقان كامل لمكتبات PyTorch, LangChain, Transformers, Pandas",
                            "استخراج وتحليل وتوليد خطوط معالجة البيانات (Data Pipelines)"
                        ),
                        teamRole = "عالم البيانات والمهندس المعماري للـ AI",
                        rationale = "بفضل نافذة السياق التي تتسع لـ 2 مليون رمز، يستطيع فحص وتحليل نماذج البيانات ومستودعات بايثون بأكملها.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 97,
                        strengthBadge = "الأقوى في خوادم FastAPI وSQLAlchemy النظيفة",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf("بناء APIs غير متزامنة (AsyncIO) فائقة الأمان", "منع ثغرات الحقن والتحقق الصارم بـ Pydantic v2"),
                        teamRole = "كبير مهندسي Backend وMicroservices",
                        rationale = "يضمن أعلى كفاءة في الأمان وإدارة الجلسات واتصالات قواعد البيانات."
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "o3-mini",
                        modelName = "OpenAI o3-mini (High Reasoning)",
                        provider = "OpenAI",
                        matchScore = 95,
                        strengthBadge = "الأقوى في الخوارزميات الحسابية والرياضية",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf("تفكير منطقي رياضي فائق في خوارزميات الـ ML", "حل معادلات الـ Vector Embeddings والبحث الدلالي"),
                        teamRole = "مهندس خوارزميات الذكاء والبحث",
                        rationale = "أفضل نموذج لحل المشاكل الرياضية والمنطقية المعقدة في بايثون."
                    )
                )
            }

            DevelopmentEnvironment.SYSTEMS_RUST, DevelopmentEnvironment.BACKEND_GO -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 99,
                        strengthBadge = "النموذج الرائد عالمياً في أمان الذاكرة في Rust وGo",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf(
                            "فهم عبقري لـ Borrow Checker وLifetimes في لغة Rust",
                            "بناء Concurrency آمن مع Tokio Channels وGoroutines",
                            "كتابة كود خال تماماً من Panic أو Deadlocks"
                        ),
                        teamRole = "كبير مهندسي الأنظمة المنخفضة المستوى (Systems Architect)",
                        rationale = "النموذج الوحيد القادر على كتابة برامج Rust وGo معقدة بدون أخطاء تجميع أو مشاكل ملكية الذاكرة.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "deepseek-r1",
                        modelName = "DeepSeek-R1",
                        provider = "DeepSeek",
                        matchScore = 96,
                        strengthBadge = "دقة تفكير استثنائية في خوارزميات الأنظمة المنخفضة",
                        contextWindow = "64,000 Tokens",
                        strengths = listOf("تحليل متعمق لتخصيص الذاكرة وأداء المعالج", "كتابة دوال سريعة جداً بدون Overhead"),
                        teamRole = "مهندس تحسين الأداء وKernel/Systems",
                        rationale = "قوي جداً في كتابة خوارزميات منخفضة المستوى بلغة C/C++ وRust."
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-2.0-flash",
                        modelName = "Google Gemini 2.0 Flash",
                        provider = "Google DeepMind",
                        matchScore = 92,
                        strengthBadge = "السرعة والتوسع السحابي",
                        contextWindow = "1,048,576 Tokens",
                        strengths = listOf("توليد Dockerfiles وملفات Kubernetes متكاملة لـ Go وRust"),
                        teamRole = "مهندس DevOps والنشر السحابي",
                        rationale = "ممتاز في إعداد بيئات التجميع والكونتينر وتوزيع الـ Binaries."
                    )
                )
            }

            DevelopmentEnvironment.MOBILE_FLUTTER, DevelopmentEnvironment.MOBILE_REACT_NATIVE -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 98,
                        strengthBadge = "الأقوى في شجرة عناصر Flutter وReact Native",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf("بناء Widgets معقدة في Flutter مع Riverpod", "فصل الطبقات والـ State Management بكفاءة"),
                        teamRole = "مهندس التطبيقات متعددة المنصات",
                        rationale = "أفضل نموذج لتوليد كود Dart وFlutter نظيف يعمل على iOS وAndroid والويب بنقرة واحدة.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-2.0-flash",
                        modelName = "Google Gemini 2.0 Flash",
                        provider = "Google DeepMind",
                        matchScore = 97,
                        strengthBadge = "توليد فوري لكافة الشاشات والمكونات",
                        contextWindow = "1,048,576 Tokens",
                        strengths = listOf("إنشاء حزم التطبيق والملفات الكاملة بسرعة هائلة", "سياق عملاق يستوعب كافة حزم pubspec.yaml"),
                        teamRole = "المطور المنفذ السريع",
                        rationale = "نموذج Google الأمثل لبيئة Flutter لكون كلاهما من تطوير Google."
                    )
                )
            }

            else -> {
                list.add(
                    AiModelRecommendation(
                        modelId = "gemini-2.0-flash",
                        modelName = "Google Gemini 2.0 Flash",
                        provider = "Google DeepMind",
                        matchScore = 98,
                        strengthBadge = "النموذج الشامل لجميع اللغات والبيئات",
                        contextWindow = "1,048,576 Tokens",
                        strengths = listOf("تغطية لأكثر من 50 لغة برمجة وبيئة عمل", "سرعة استجابة فائقة مع سياق ضخم"),
                        teamRole = "المهندس الشامل (Full-Stack Architect)",
                        rationale = "أفضل نموذج عام للتطوير البرمجي متعدد اللغات.",
                        isPrimaryPick = true
                    )
                )
                list.add(
                    AiModelRecommendation(
                        modelId = "claude-3-7-sonnet",
                        modelName = "Anthropic Claude 3.7 Sonnet",
                        provider = "Anthropic",
                        matchScore = 97,
                        strengthBadge = "المنطق البرمجي الدقيق والخالي من الأخطاء",
                        contextWindow = "200,000 Tokens",
                        strengths = listOf("تحليل عميق ومعماريات برمجية احترافية"),
                        teamRole = "مدقق الكود والجودة",
                        rationale = "المعيار الأعلى للجودة في هندسة البرمجيات."
                    )
                )
            }
        }

        // Adjust order if user prioritized specific criteria
        return when (priority) {
            "huge_context" -> list.sortedByDescending { if (it.modelId.contains("gemini-1.5")) 100 else if (it.modelId.contains("gemini-2.0")) 95 else 80 }
            "accuracy" -> list.sortedByDescending { if (it.modelId.contains("claude") || it.modelId.contains("o3") || it.modelId.contains("o1")) 100 else 85 }
            "open_source" -> list.sortedByDescending { if (it.modelId.contains("deepseek") || it.modelId.contains("qwen")) 100 else 80 }
            else -> list
        }
    }
}
