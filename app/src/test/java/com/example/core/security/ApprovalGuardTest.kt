package com.example.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalGuardTest {
    @Test
    fun tier1Actions_autoExecuteWhenEnabled() {
        val decision = ApprovalGuard.evaluateAction(
            action = AgentActionType.READ_FILES,
            autoExecuteTier1 = true,
            hasExplicitApproval = false
        )
        assertEquals(GuardDecision.AllowedAuto, decision)
    }

    @Test
    fun tier1Actions_requireApprovalWhenAutoDisabled() {
        val decision = ApprovalGuard.evaluateAction(
            action = AgentActionType.RUN_TESTS,
            autoExecuteTier1 = false,
            hasExplicitApproval = false
        )
        assertTrue(decision is GuardDecision.RequiresApproval)
        assertEquals(PermissionTier.TIER_1_AUTO, (decision as GuardDecision.RequiresApproval).tier)
    }

    @Test
    fun tier2Actions_alwaysRequireApprovalUnlessExplicit() {
        val withoutApproval = ApprovalGuard.evaluateAction(
            action = AgentActionType.GIT_PUSH,
            autoExecuteTier1 = true,
            hasExplicitApproval = false
        )
        assertTrue(withoutApproval is GuardDecision.RequiresApproval)
        assertEquals(PermissionTier.TIER_2_APPROVAL, (withoutApproval as GuardDecision.RequiresApproval).tier)
        assertEquals(
            GuardDecision.AllowedAuto,
            ApprovalGuard.evaluateAction(
                action = AgentActionType.CREATE_PR,
                autoExecuteTier1 = true,
                hasExplicitApproval = true
            )
        )
    }

    @Test
    fun tier3Actions_requireIndependentExplicitApproval() {
        val decision = ApprovalGuard.evaluateAction(
            action = AgentActionType.DB_MIGRATION,
            autoExecuteTier1 = true,
            hasExplicitApproval = false
        )
        assertTrue(decision is GuardDecision.RequiresApproval)
        assertEquals(PermissionTier.TIER_3_EXPLICIT, (decision as GuardDecision.RequiresApproval).tier)
    }

    @Test
    fun legacyDatabaseTargets_areStrictlyBlockedEvenWithApproval() {
        val blockedTargets = listOf("metra_old", "legacy_metra_prod", "metra_legacy", "metra2025")
        for (target in blockedTargets) {
            assertTrue("Expected $target to be identified as legacy", ApprovalGuard.isLegacyDatabaseTarget(target))
            val decision = ApprovalGuard.evaluateAction(
                action = AgentActionType.READ_FILES,
                autoExecuteTier1 = true,
                hasExplicitApproval = true,
                targetResource = target
            )
            assertTrue("Expected $target to be blocked", decision is GuardDecision.Blocked)
        }
    }

    @Test
    fun validateSqlExecution_allowsOnlySingleReadOnlySelectOrWith() {
        assertFalse(ApprovalGuard.isMutatingSql("SELECT id, station_name FROM weather_stations LIMIT 20;"))
        val statements = listOf(
            "SELECT * FROM metra_forecasts WHERE region = 'north';",
            "WITH recent AS (SELECT id FROM forecasts) SELECT * FROM recent"
        )
        statements.forEach { sql ->
            assertEquals(
                GuardDecision.AllowedAuto,
                ApprovalGuard.validateSqlExecution(sql, "metra_2027_isolated", false)
            )
        }
    }

    @Test
    fun validateSqlExecution_requiresTier3ForMutationsUnknownOrMultipleStatements() {
        val restricted = listOf(
            "DROP TABLE users;",
            "ALTER TABLE forecasts ADD COLUMN confidence double precision;",
            "GRANT SELECT ON forecasts TO anon;",
            "INSERT INTO logs(msg) VALUES ('x');",
            "VACUUM ANALYZE forecasts;",
            "SELECT * FROM forecasts; DELETE FROM forecasts;",
            "SELECT 1; SELECT 2;"
        )
        restricted.forEach { sql ->
            val decision = ApprovalGuard.validateSqlExecution(sql, "metra_2027_staging", false)
            assertTrue("Expected Tier 3 approval for: $sql", decision is GuardDecision.RequiresApproval)
            assertEquals(PermissionTier.TIER_3_EXPLICIT, (decision as GuardDecision.RequiresApproval).tier)
        }
    }

    @Test
    fun validateSqlExecution_blocksLegacyDbInSqlOrTarget() {
        val decision = ApprovalGuard.validateSqlExecution(
            sql = "SELECT * FROM metra_old.stations;",
            targetDatabase = "metra_2027_staging",
            hasExplicitTier3Approval = true
        )
        assertTrue(decision is GuardDecision.Blocked)
    }
}
