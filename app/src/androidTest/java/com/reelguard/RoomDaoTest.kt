package com.reelguard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.reelguard.data.room.AppDatabase
import com.reelguard.data.room.ReelDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ReelDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.reelDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun incrementCreatesThenBumps() =
        runBlocking {
            dao.increment("2026-03-10", "yt", 1000)
            dao.increment("2026-03-10", "yt", 2000)
            dao.increment("2026-03-10", "ig", 3000)
            val rows = dao.getDay("2026-03-10").associate { it.appId to it.count }
            assertEquals(mapOf("yt" to 2, "ig" to 1), rows)
        }

    @Test
    fun observeDayEmitsUpdates() =
        runBlocking {
            dao.observeDay("d").test {
                assertEquals(0, awaitItem().size)
                dao.increment("d", "yt", 1)
                assertEquals(1, awaitItem().single().count)
                dao.increment("d", "yt", 2)
                assertEquals(2, awaitItem().single().count)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun eventsPrunedAfterRetention() =
        runBlocking {
            dao.increment("d", "yt", 100)
            dao.increment("d", "yt", 5000)
            assertEquals(1, dao.pruneEvents(1000))
            assertEquals(listOf(5000L), dao.eventTimes(0))
        }

    @Test
    fun daysAreSeparate() =
        runBlocking {
            dao.increment("2026-03-10", "yt", 1)
            dao.increment("2026-03-11", "yt", 2)
            assertEquals(1, dao.getDay("2026-03-11").single().count)
            assertEquals(2, dao.observeSinceOnce("2026-03-10"))
        }

    private suspend fun ReelDao.observeSinceOnce(from: String): Int {
        var n = 0
        observeSince(from).test {
            n = awaitItem().size
            cancelAndIgnoreRemainingEvents()
        }
        return n
    }
}
