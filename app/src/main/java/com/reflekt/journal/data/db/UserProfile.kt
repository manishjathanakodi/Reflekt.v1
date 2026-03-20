package com.reflekt.journal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One record per device. uid = UUID. */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String,
    val name: String,
    val age: Int,
    val gender: String,
    val occupation: String,
    val industry: String,
    val maritalStatus: String,
    /** JSON array of relation objects {name, role, emoji} */
    val relationMapJson: String,
    /** JSON array of struggle area strings */
    val struggleAreasJson: String,
    val screenTimeGoalMinutes: Int,
    val onboardingComplete: Boolean,
    /** "SYSTEM" | "LIGHT" | "DARK" */
    val themePreference: String,
)
