package com.reelguard.data.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_count", primaryKeys = ["dayKey", "appId"])
data class DailyCount(
    val dayKey: String,
    val appId: String,
    val count: Int,
    val updatedAt: Long,
)

@Entity(tableName = "reel_event", indices = [Index("timestamp")])
data class ReelEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appId: String,
)

@Dao
abstract class ReelDao {
    @Query("UPDATE daily_count SET count = count + 1, updatedAt = :now WHERE dayKey = :day AND appId = :app")
    protected abstract suspend fun bump(
        day: String,
        app: String,
        now: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertCount(row: DailyCount): Long

    @Insert
    protected abstract suspend fun insertEvent(event: ReelEvent): Long

    @Transaction
    open suspend fun increment(
        day: String,
        app: String,
        now: Long,
    ) {
        if (bump(day, app, now) == 0) insertCount(DailyCount(day, app, 1, now))
        insertEvent(ReelEvent(timestamp = now, appId = app))
    }

    @Query("SELECT * FROM daily_count WHERE dayKey = :day")
    abstract fun observeDay(day: String): Flow<List<DailyCount>>

    @Query("SELECT * FROM daily_count WHERE dayKey = :day")
    abstract suspend fun getDay(day: String): List<DailyCount>

    @Query("SELECT * FROM daily_count WHERE dayKey >= :fromDay ORDER BY dayKey")
    abstract fun observeSince(fromDay: String): Flow<List<DailyCount>>

    @Query("SELECT * FROM daily_count ORDER BY dayKey LIMIT 1")
    abstract suspend fun firstDay(): DailyCount?

    @Query("SELECT timestamp FROM reel_event WHERE timestamp >= :since")
    abstract suspend fun eventTimes(since: Long): List<Long>

    @Query("DELETE FROM reel_event WHERE timestamp < :before")
    abstract suspend fun pruneEvents(before: Long): Int
}

@Database(entities = [DailyCount::class, ReelEvent::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reelDao(): ReelDao
}
