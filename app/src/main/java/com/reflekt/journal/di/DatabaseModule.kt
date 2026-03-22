package com.reflekt.journal.di

import android.content.Context
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.ManualAppLimitDao
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.InterventionDao
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.db.ReflektDatabase
import com.reflekt.journal.data.db.ResourceDao
import com.reflekt.journal.data.db.TodoDao
import com.reflekt.journal.data.db.UserProfileDao
import com.reflekt.journal.security.KeystoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKeystoreManager(@ApplicationContext context: Context): KeystoreManager =
        KeystoreManager(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager,
    ): ReflektDatabase = ReflektDatabase.create(context, keystoreManager.getOrCreatePassphrase())

    // ── DAO providers ──────────────────────────────────────────────────────────

    @Provides fun provideUserProfileDao(db: ReflektDatabase): UserProfileDao = db.userProfileDao()

    @Provides fun provideJournalEntryDao(db: ReflektDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides fun provideMoodLogDao(db: ReflektDatabase): MoodLogDao = db.moodLogDao()

    @Provides fun provideAppUsageLogDao(db: ReflektDatabase): AppUsageLogDao = db.appUsageLogDao()

    @Provides fun provideInterventionDao(db: ReflektDatabase): InterventionDao = db.interventionDao()

    @Provides fun provideHabitDao(db: ReflektDatabase): HabitDao = db.habitDao()

    @Provides fun provideHabitLogDao(db: ReflektDatabase): HabitLogDao = db.habitLogDao()

    @Provides fun provideTodoDao(db: ReflektDatabase): TodoDao = db.todoDao()

    @Provides fun provideGoalDao(db: ReflektDatabase): GoalDao = db.goalDao()

    @Provides fun provideResourceDao(db: ReflektDatabase): ResourceDao = db.resourceDao()

    @Provides fun provideManualAppLimitDao(db: ReflektDatabase): ManualAppLimitDao = db.manualAppLimitDao()
}
