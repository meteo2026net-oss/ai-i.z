package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentDao {
    @Query("SELECT * FROM task_checkpoints WHERE userId = :userId ORDER BY updatedEpochMs DESC")
    fun observeCheckpoints(userId: String): Flow<List<LocalTaskCheckpoint>>

    @Query("SELECT * FROM task_checkpoints WHERE taskId = :taskId LIMIT 1")
    suspend fun getCheckpoint(taskId: String): LocalTaskCheckpoint?

    @Query("SELECT * FROM task_checkpoints WHERE userId = :userId AND status IN ('queued', 'running', 'paused') ORDER BY updatedEpochMs DESC LIMIT 1")
    suspend fun getLatestResumableCheckpoint(userId: String): LocalTaskCheckpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckpoint(checkpoint: LocalTaskCheckpoint)

    @Query("DELETE FROM task_checkpoints WHERE taskId = :taskId")
    suspend fun deleteCheckpoint(taskId: String)
}

@Database(entities = [LocalTaskCheckpoint::class], version = 1, exportSchema = false)
abstract class MetraLocalDatabase : RoomDatabase() {
    abstract fun agentDao(): AgentDao

    companion object {
        @Volatile
        private var INSTANCE: MetraLocalDatabase? = null

        fun getInstance(context: Context): MetraLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MetraLocalDatabase::class.java,
                    "metra_agent_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
