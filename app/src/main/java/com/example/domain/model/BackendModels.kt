package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackendHealthResponse(
    val status: String = "",
    val version: String = "",
    val dotnetAvailable: Boolean = false,
    val flutterAvailable: Boolean = false,
    val postgresConnected: Boolean = false,
    val activeDatabaseName: String = "",
    val lastCheckUtc: String = ""
)

@JsonClass(generateAdapter = true)
data class BackendTestRunRequest(
    val repo: String,
    val branch: String,
    val target: String = "all" // "dotnet", "flutter", "db_isolated", "all"
)

@JsonClass(generateAdapter = true)
data class BackendTestCaseResult(
    val name: String = "",
    val suite: String = "",
    val passed: Boolean = false,
    val durationMs: Long = 0L,
    val errorMessage: String = ""
)

@JsonClass(generateAdapter = true)
data class BackendTestRunResponse(
    val runId: String = "",
    val passedCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val durationMs: Long = 0L,
    val summary: String = "",
    val cases: List<BackendTestCaseResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DbColumnInfo(
    val name: String = "",
    val dataType: String = "",
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DbRelationInfo(
    val constraintName: String = "",
    val sourceColumn: String = "",
    val targetTable: String = "",
    val targetColumn: String = ""
)

@JsonClass(generateAdapter = true)
data class DbTableInfo(
    val schema: String = "public",
    val name: String = "",
    val rowEstimate: Long = 0L,
    val columns: List<DbColumnInfo> = emptyList(),
    val indexes: List<String> = emptyList(),
    val relations: List<DbRelationInfo> = emptyList(),
    val rlsEnabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DbSchemaResponse(
    val databaseName: String = "",
    val engineVersion: String = "",
    val isIsolatedTarget: Boolean = true,
    val tables: List<DbTableInfo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DbSqlRequest(
    val targetDatabase: String,
    val sql: String,
    val approvalId: String? = null
)

@JsonClass(generateAdapter = true)
data class DbSqlResponse(
    val executed: Boolean = false,
    val rowsAffected: Int = 0,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val impactSummary: String = "",
    val warnings: List<String> = emptyList()
)
