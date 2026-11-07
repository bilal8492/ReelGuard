package com.reelguard.di

import android.content.Context
import androidx.room.Room
import com.reelguard.data.repo.FirebaseSelectorSource
import com.reelguard.data.repo.RemoteSelectorSource
import com.reelguard.data.room.AppDatabase
import com.reelguard.data.room.ReelDao
import com.reelguard.domain.WidgetRefresher
import com.reelguard.widget.GlanceWidgetRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "reelguard.db").build()

    @Provides
    fun reelDao(db: AppDatabase): ReelDao = db.reelDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Firebase Remote Config when google-services.json is present, otherwise a no-op (see docs/SELECTORS.md). */
    @Provides
    @Singleton
    fun remoteSelectors(
        @ApplicationContext context: Context,
    ): RemoteSelectorSource = FirebaseSelectorSource.create(context)

    @Provides
    @Singleton
    fun widgetRefresher(impl: GlanceWidgetRefresher): WidgetRefresher = impl
}
