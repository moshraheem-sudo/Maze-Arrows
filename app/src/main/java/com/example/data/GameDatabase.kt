package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val starsEarned: Int = 0,
    val bestScore: Int = 0,
    val bestTimeSeconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val savedRemainingArrowIdsCsv: String = "",
    val savedRemainingSeconds: Int = 0,
    val savedElapsedSeconds: Int = 0,
    val savedLivesRemaining: Int = 5,
    val hasSavedProgress: Boolean = false
)

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val highestUnlockedLevel: Int = 1,
    val currentActiveLevel: Int = 1,
    val playerDnaSeed: Long = 0x5F3759DF7C9E1234L,
    val totalScore: Int = 60,
    val totalStars: Int = 0,
    val hintsBalance: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val selectedThemeId: String = "amaze_classic",
    val themeMode: String = "LIGHT",
    val arrowTipStyle: String = "CLASSIC_TRIANGLE",
    val eyeProtectionMode: Boolean = false,
    val totalPurchasesCount: Int = 0,
    val purchaseTimestampsCsv: String = "",
    val lockedDifficultyTierName: String = "",
    val bonusTimeSecondsBank: Int = 0,
    val customLevelsSolved: Int = 0
)

@Dao
interface GameProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAllLevelProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevelProgress(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgress(progress: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevelProgressList(list: List<LevelProgressEntity>)

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun observePlayerProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getPlayerProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerProfile(profile: PlayerProfileEntity)
}

@Database(
    entities = [LevelProgressEntity::class, PlayerProfileEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AmazeDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AmazeDatabase? = null

        fun getInstance(context: Context): AmazeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmazeDatabase::class.java,
                    "amaze_arrows_progress.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
