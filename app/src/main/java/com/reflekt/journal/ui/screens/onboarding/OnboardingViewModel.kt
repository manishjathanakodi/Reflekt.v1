package com.reflekt.journal.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.UserProfile
import com.reflekt.journal.data.db.UserProfileDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class RelationEntry(val name: String, val role: String, val emoji: String)

data class ProfileDraft(
    val uid: String = UUID.randomUUID().toString(),
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val occupation: String = "",
    val industry: String = "",
    val relationshipStatus: String = "",
    val familyStatus: String = "",
    val struggleAreas: Set<String> = emptySet(),
    val screenTimeGoalMinutes: Int = 120,
    val relationMap: List<RelationEntry> = emptyList(),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _draft = MutableStateFlow(ProfileDraft())
    val draft = _draft.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    fun updateDraft(update: ProfileDraft.() -> ProfileDraft) {
        _draft.value = _draft.value.update()
    }

    fun nextStep() {
        if (_currentStep.value < 4) _currentStep.value++
    }

    fun previousStep() {
        if (_currentStep.value > 1) _currentStep.value--
    }

    fun addRelation(entry: RelationEntry) {
        _draft.value = _draft.value.copy(
            relationMap = _draft.value.relationMap + entry,
        )
    }

    fun removeRelation(index: Int) {
        _draft.value = _draft.value.copy(
            relationMap = _draft.value.relationMap.toMutableList().also { it.removeAt(index) },
        )
    }

    fun saveProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            val d = _draft.value
            val relationJson = Json.encodeToString(
                d.relationMap.map { mapOf("name" to it.name, "role" to it.role, "emoji" to it.emoji) },
            )
            val struggleJson = Json.encodeToString(d.struggleAreas.toList())
            val profile = UserProfile(
                uid = d.uid,
                name = d.name.ifBlank { "User" },
                age = d.age.toIntOrNull() ?: 0,
                gender = d.gender,
                occupation = d.occupation,
                industry = d.industry,
                maritalStatus = d.relationshipStatus,
                relationMapJson = relationJson,
                struggleAreasJson = struggleJson,
                screenTimeGoalMinutes = d.screenTimeGoalMinutes,
                onboardingComplete = true,
                themePreference = "SYSTEM",
            )
            userProfileDao.upsert(profile)
            onComplete()
        }
    }
}
