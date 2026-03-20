package com.reflekt.journal.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wellbeing DI module.
 * [UsageStatsRepository] and [CorrelationEngine] are @Singleton @Inject constructor
 * classes that Hilt provides automatically — no explicit @Provides needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object WellbeingModule
