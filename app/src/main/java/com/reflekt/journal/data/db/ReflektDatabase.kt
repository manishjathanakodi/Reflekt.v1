package com.reflekt.journal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        UserProfile::class,
        JournalEntry::class,
        JournalEntryFts::class,
        MoodLog::class,
        AppUsageLog::class,
        Intervention::class,
        Habit::class,
        HabitLog::class,
        Todo::class,
        Goal::class,
        Resource::class,
        ManualAppLimit::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ReflektDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun moodLogDao(): MoodLogDao
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun interventionDao(): InterventionDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun todoDao(): TodoDao
    abstract fun goalDao(): GoalDao
    abstract fun resourceDao(): ResourceDao
    abstract fun manualAppLimitDao(): ManualAppLimitDao

    companion object {
        /**
         * Builds the encrypted database.
         * [passphrase] must be the 32-byte key returned by [com.reflekt.journal.security.KeystoreManager].
         */
        fun create(context: Context, passphrase: ByteArray): ReflektDatabase =
            Room.databaseBuilder(context, ReflektDatabase::class.java, "reflekt.db")
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .fallbackToDestructiveMigration()
                .build()
    }
}
