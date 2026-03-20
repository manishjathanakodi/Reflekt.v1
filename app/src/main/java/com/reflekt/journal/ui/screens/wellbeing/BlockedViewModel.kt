package com.reflekt.journal.ui.screens.wellbeing

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.Intervention
import com.reflekt.journal.data.db.InterventionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BlockedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val interventionDao: InterventionDao,
) : ViewModel() {

    val packageName: String = savedStateHandle["packageName"] ?: ""

    private val _appLabel = MutableStateFlow("")
    val appLabel: StateFlow<String> = _appLabel

    private val _appIcon = MutableStateFlow<Drawable?>(null)
    val appIcon: StateFlow<Drawable?> = _appIcon

    val activeIntervention: StateFlow<Intervention?> =
        interventionDao.getPending().map { list ->
            list.firstOrNull { it.packageName == packageName }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        try {
            val pm   = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            _appLabel.value = pm.getApplicationLabel(info).toString()
            _appIcon.value  = pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            _appLabel.value = packageName
        }
    }

    // AppBlockerService creates the Intervention before launching this Activity.
    // No additional setup needed here.

    fun overrideWithPin(pin: String, storedPin: String, onSuccess: () -> Unit) {
        if (pin == storedPin) {
            viewModelScope.launch {
                val intervention = activeIntervention.value
                if (intervention != null) {
                    interventionDao.upsert(
                        intervention.copy(
                            overrideUsed = true,
                            status       = "OVERRIDDEN",
                            resolvedAt   = System.currentTimeMillis(),
                        )
                    )
                }
                onSuccess()
            }
        }
    }

    fun selectMicrotask(microtaskType: String) {
        viewModelScope.launch {
            val intervention = activeIntervention.value ?: run {
                // Create one if it doesn't exist
                val newIntervention = Intervention(
                    id                 = UUID.randomUUID().toString(),
                    timestamp          = System.currentTimeMillis(),
                    triggerType        = "SCREEN_TIME_LIMIT",
                    actionTaken        = "MICROTASK_ASSIGNED",
                    packageName        = packageName,
                    microtaskType      = microtaskType,
                    microtaskCompleted = false,
                    overrideUsed       = false,
                    status             = "PENDING",
                    resolvedAt         = null,
                )
                interventionDao.insert(newIntervention)
                return@launch
            }
            interventionDao.upsert(intervention.copy(microtaskType = microtaskType))
        }
    }
}
