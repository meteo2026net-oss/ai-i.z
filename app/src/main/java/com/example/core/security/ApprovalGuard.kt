package com.example.core.security

enum class PermissionTier(val level: Int, val arabicLabel: String) {
    TIER_1_AUTO(1, "المستوى 1 — تنفيذ تلقائي"),
    TIER_2_APPROVAL(2, "المستوى 2 — موافقة قبل التنفيذ"),
    TIER_3_EXPLICIT(3, "المستوى 3 — موافقة صريحة مستقلة")
}

enum class AgentActionType(
    val wireKey: String,
    val tier: PermissionTier,
    val arabicTitle: String
) {
    READ_FILES("read_files", PermissionTier.TIER_1_AUTO, "قراءة الملفات"),
    ANALYZE_CODE("analyze_code", PermissionTier.TIER_1_AUTO, "تحليل الشفرة"),
    STAGE_BRANCH_EDIT("stage_branch_edit", PermissionTier.TIER_1_AUTO, "تعديل داخل فرع عمل"),
    RUN_TESTS("run_tests", PermissionTier.TIER_1_AUTO, "تشغيل الاختبارات"),
    GENERATE_REPORT("generate_report", PermissionTier.TIER_1_AUTO, "إنشاء التقرير"),

    GIT_PUSH("git_push", PermissionTier.TIER_2_APPROVAL, "رفع commits إلى GitHub"),
    CREATE_PR("create_pr", PermissionTier.TIER_2_APPROVAL, "إنشاء Pull Request"),
    STAGING_DEPLOY("staging_deploy", PermissionTier.TIER_2_APPROVAL, "تعديل بيئة Staging"),
    SERVICE_CONFIG("service_config", PermissionTier.TIER_2_APPROVAL, "تعديل إعدادات الخدمات"),

    DB_SQL("db_sql", PermissionTier.TIER_3_EXPLICIT, "تنفيذ SQL على قاعدة مستضافة"),
    DB_RLS_GRANTS("db_rls_grants", PermissionTier.TIER_3_EXPLICIT, "تعديل RLS وGrants"),
    DB_MIGRATION("db_migration", PermissionTier.TIER_3_EXPLICIT, "تطبيق Migrations"),
    MERGE_PR("merge_pr", PermissionTier.TIER_3_EXPLICIT, "دمج Pull Request"),
    PROD_DEPLOY("prod_deploy", PermissionTier.TIER_3_EXPLICIT, "النشر إلى Production");

    companion object {
        fun fromWireKey(key: String): AgentActionType? = entries.find { it.wireKey == key }
    }
}

sealed interface GuardDecision {
    data object AllowedAuto : GuardDecision
    data class RequiresApproval(val tier: PermissionTier, val reason: String) : GuardDecision
    data class Blocked(val reason: String) : GuardDecision
}

/**
 * Deterministic security & policy guard.
 * AI prompts or chat instructions cannot bypass Tier 2/3 gates or legacy DB isolation.
 */
object ApprovalGuard {

    private val legacyDbBlockedPatterns = listOf(
        "metra_old",
        "metra_legacy",
        "legacy_metra",
        "old_metra",
        "metra_v1",
        "metra2025",
        "metra2026_legacy",
        "prod_legacy"
    )

    private val mutatingSqlKeywords = Regex(
        """\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE|REPLACE|EXEC|EXECUTE|CALL)\b""",
        RegexOption.IGNORE_CASE
    )

    fun isLegacyDatabaseTarget(
        target: String,
        extraProtectedTargets: List<String> = emptyList()
    ): Boolean {
        val normalized = target.lowercase().trim()
        if (normalized.isEmpty()) return false
        val legacyHit = legacyDbBlockedPatterns.any { normalized.contains(it) } ||
            (normalized.contains("قديمة") && normalized.contains("metra"))
        if (legacyHit) return true
        return extraProtectedTargets
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }
            .any { normalized.contains(it) }
    }

    fun isMutatingSql(sql: String): Boolean = mutatingSqlKeywords.containsMatchIn(sql)

    fun evaluateAction(
        action: AgentActionType,
        autoExecuteTier1: Boolean,
        hasExplicitApproval: Boolean,
        targetResource: String = "",
        extraProtectedTargets: List<String> = emptyList()
    ): GuardDecision {
        if (isLegacyDatabaseTarget(targetResource, extraProtectedTargets)) {
            return GuardDecision.Blocked(
                "محظور أمنياً: قاعدة البيانات المستهدفة محمية ومعزولة خارج نطاق التنفيذ."
            )
        }
        return when (action.tier) {
            PermissionTier.TIER_1_AUTO -> {
                if (autoExecuteTier1 || hasExplicitApproval) {
                    GuardDecision.AllowedAuto
                } else {
                    GuardDecision.RequiresApproval(
                        PermissionTier.TIER_1_AUTO,
                        "التنفيذ التلقائي للمستوى الأول معطل في إعدادات الأمان."
                    )
                }
            }
            PermissionTier.TIER_2_APPROVAL -> {
                if (hasExplicitApproval) {
                    GuardDecision.AllowedAuto
                } else {
                    GuardDecision.RequiresApproval(
                        PermissionTier.TIER_2_APPROVAL,
                        "عملية من المستوى الثاني تتطلب موافقتك قبل التنفيذ: ${action.arabicTitle}"
                    )
                }
            }
            PermissionTier.TIER_3_EXPLICIT -> {
                if (hasExplicitApproval) {
                    GuardDecision.AllowedAuto
                } else {
                    GuardDecision.RequiresApproval(
                        PermissionTier.TIER_3_EXPLICIT,
                        "عملية حرجة من المستوى الثالث تتطلب موافقة صريحة مستقلة: ${action.arabicTitle}"
                    )
                }
            }
        }
    }

    fun validateSqlExecution(
        sql: String,
        targetDatabase: String,
        hasExplicitTier3Approval: Boolean,
        extraProtectedTargets: List<String> = emptyList()
    ): GuardDecision {
        if (isLegacyDatabaseTarget(targetDatabase, extraProtectedTargets) ||
            isLegacyDatabaseTarget(sql, extraProtectedTargets)
        ) {
            return GuardDecision.Blocked(
                "ممنوع تماماً الاتصال بقاعدة بيانات محمية أو قديمة أو تعديلها أو حذفها."
            )
        }
        if (isMutatingSql(sql) && !hasExplicitTier3Approval) {
            return GuardDecision.RequiresApproval(
                PermissionTier.TIER_3_EXPLICIT,
                "يحتوي الاستعلام على أوامر تعديل أو DDL/RLS/Grants. يتطلب معاينة الأثر وموافقة صريحة من المستوى الثالث."
            )
        }
        return GuardDecision.AllowedAuto
    }
}
