package com.reflekt.journal.di

import com.reflekt.journal.ai.accountability.AccountabilityEngine
import com.reflekt.journal.ai.accountability.AccountabilityParser
import com.reflekt.journal.ai.engine.AiResponseParser
import com.reflekt.journal.ai.engine.LlmEngine
import com.reflekt.journal.ai.engine.LlmEngineImpl
import com.reflekt.journal.ai.prompt.PromptBuilder
import com.reflekt.journal.ai.triage.TriageEngine
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.db.TodoDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindLlmEngine(impl: LlmEngineImpl): LlmEngine

    companion object {

        @Provides
        @Singleton
        fun providePromptBuilder(): PromptBuilder = PromptBuilder()

        @Provides
        @Singleton
        fun provideAiResponseParser(): AiResponseParser = AiResponseParser()

        @Provides
        @Singleton
        fun provideTriageEngine(moodLogDao: MoodLogDao): TriageEngine =
            TriageEngine(moodLogDao)

        @Provides
        @Singleton
        fun provideAccountabilityEngine(
            habitDao: HabitDao,
            habitLogDao: HabitLogDao,
            todoDao: TodoDao,
            goalDao: GoalDao,
        ): AccountabilityEngine = AccountabilityEngine(habitDao, habitLogDao, todoDao, goalDao)

        @Provides
        @Singleton
        fun provideAccountabilityParser(
            llmEngine: LlmEngine,
            promptBuilder: PromptBuilder,
            aiResponseParser: AiResponseParser,
        ): AccountabilityParser = AccountabilityParser(llmEngine, promptBuilder, aiResponseParser)
    }
}
