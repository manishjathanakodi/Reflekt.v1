package com.reflekt.journal.ui.screens.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.UserProfileDao
import com.reflekt.journal.data.preferences.biometricEnabledFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    object Onboarding : SplashDestination
    object Auth : SplashDestination
    object Home : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = userProfileDao.getAll().first().firstOrNull()
            _destination.value = when {
                profile == null || !profile.onboardingComplete -> SplashDestination.Onboarding
                context.biometricEnabledFlow().first() -> SplashDestination.Auth
                else -> SplashDestination.Home
            }
        }
    }
}
