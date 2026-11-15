package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_checkpoints")
data class LocalTaskCheckpoint(
    @PrimaryKey val taskId: String,
    val userId: String,
    val repo: String,
    val branch: String,
    val title: String,
    val prompt: String,
    val status: String,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val progressPercent: Int,
    val lastCompletedStep: String,
    val lastResultSummary: String,
    val lastError: String,
    val modifiedFilesCsv: String,
    val nextStepsCsv: String,
    val stepsJson: String,
    val updatedEpochMs: Long = System.currentTimeMillis()
)
