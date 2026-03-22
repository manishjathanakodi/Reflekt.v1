package com.reflekt.journal.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wellbeing DI module.
 * [UsageStatsRepository] is a @Singleton @Inject constructor class — Hilt provides it automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
object WellbeingModule
