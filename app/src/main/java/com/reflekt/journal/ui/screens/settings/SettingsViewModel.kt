package com.reflekt.journal.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.AppUsageLog
import com.reflekt.journal.data.db.AppUsageLogDao
import com.reflekt.journal.data.db.Goal
import com.reflekt.journal.data.db.GoalDao
import com.reflekt.journal.data.db.Habit
import com.reflekt.journal.data.db.HabitDao
import com.reflekt.journal.data.db.HabitLogDao
import com.reflekt.journal.data.db.InterventionDao
import com.reflekt.journal.data.db.Intervention
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import com.reflekt.journal.data.db.ReflektDatabase
import com.reflekt.journal.data.db.Todo
import com.reflekt.journal.data.db.TodoDao
import com.reflekt.journal.data.db.UserProfile
import com.reflekt.journal.data.db.UserProfileDao
import com.reflekt.journal.data.model.ExportAppUsageLog
import com.reflekt.journal.data.model.ExportData
import com.reflekt.journal.data.model.ExportGoal
import com.reflekt.journal.data.model.ExportHabit
import com.reflekt.journal.data.model.ExportHabitLog
import com.reflekt.journal.data.model.ExportIntervention
import com.reflekt.journal.data.model.ExportJournalEntry
import com.reflekt.journal.data.model.ExportMoodLog
import com.reflekt.journal.data.model.ExportProfile
import com.reflekt.journal.data.model.ExportTodo
import androidx.datastore.preferences.core.edit
import com.reflekt.journal.data.preferences.biometricEnabledFlow
import com.reflekt.journal.data.preferences.notificationsEnabledFlow
import com.reflekt.journal.data.preferences.setBiometricEnabled
import com.reflekt.journal.data.preferences.setNotificationsEnabled
import com.reflekt.journal.data.preferences.settingsDataStore
import com.reflekt.journal.security.CryptoUtils
import com.reflekt.journal.security.KeystoreManager
import com.reflekt.journal.ui.screens.crisis.UiState
import com.reflekt.journal.ui.theme.ThemePreference
import com.reflekt.journal.ui.theme.THEME_PREFERENCE_KEY
import com.reflekt.journal.ui.theme.themeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val journalEntryDao: JournalEntryDao,
    private val moodLogDao: MoodLogDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val todoDao: TodoDao,
    private val goalDao: GoalDao,
    private val appUsageLogDao: AppUsageLogDao,
    private val interventionDao: InterventionDao,
    private val database: ReflektDatabase,
    private val keystoreManager: KeystoreManager,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userProfileDao.getAll()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val themePreference: StateFlow<ThemePreference> = context.themeDataStore.data
        .map { prefs ->
            val raw = prefs[THEME_PREFERENCE_KEY] ?: ThemePreference.SYSTEM.name
            runCatching { ThemePreference.valueOf(raw) }.getOrDefault(ThemePreference.SYSTEM)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.SYSTEM)

    val biometricEnabled: StateFlow<Boolean> = context.biometricEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val notificationsEnabled: StateFlow<Boolean> = context.notificationsEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _exportState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val exportState: StateFlow<UiState<Unit>> = _exportState

    private val _importState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val importState: StateFlow<UiState<String>> = _importState

    fun updateThemePreference(pref: ThemePreference) {
        viewModelScope.launch {
            context.themeDataStore.edit { prefs ->
                prefs[THEME_PREFERENCE_KEY] = pref.name
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { context.setBiometricEnabled(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { context.setNotificationsEnabled(enabled) }
    }

    fun exportBackup(password: String) {
        _exportState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val data    = buildExportData()
                val json    = Json.encodeToString(data)
                val salt    = (data.userProfile?.uid ?: "reflekt").toByteArray()
                val key     = withContext(Dispatchers.Default) { CryptoUtils.deriveKey(password, salt) }
                val encrypted = withContext(Dispatchers.Default) {
                    CryptoUtils.encrypt(key, json.toByteArray(Charsets.UTF_8))
                }
                val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                writeToDownloads("reflekt_backup_$ts.enc", encrypted)
                _exportState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _exportState.value = UiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun importBackup(uri: Uri, password: String) {
        _importState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val encrypted = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }
                val salt = "reflekt".toByteArray() // We try with default first; override if profile available
                val key  = withContext(Dispatchers.Default) { CryptoUtils.deriveKey(password, salt) }
                val json = withContext(Dispatchers.Default) {
                    String(CryptoUtils.decrypt(key, encrypted), Charsets.UTF_8)
                }
                val data = Json.decodeFromString<ExportData>(json)
                applyImport(data)
                val summary = buildImportSummary(data)
                _importState.value = UiState.Success(summary)
            } catch (e: javax.crypto.AEADBadTagException) {
                _importState.value = UiState.Error("Incorrect password")
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                _importState.value = UiState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    context.getDatabasePath("reflekt.db").apply {
                        delete()
                        parentFile?.listFiles { f -> f.name.startsWith("reflekt.db") }
                            ?.forEach { it.delete() }
                    }
                    keystoreManager.deleteKey()
                    context.settingsDataStore.edit { it.clear() }
                    context.themeDataStore.edit { it.clear() }
                }
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Delete all failed", e)
            }
        }
    }

    fun resetExportState() { _exportState.value = UiState.Idle }
    fun resetImportState() { _importState.value = UiState.Idle }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            userProfileDao.upsert(profile.copy(name = name))
        }
    }

    fun updateOccupation(occupation: String) {
        viewModelScope.launch {
            val profile = userProfile.value ?: return@launch
            userProfileDao.upsert(profile.copy(occupation = occupation))
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private suspend fun buildExportData(): ExportData {
        val profile      = userProfileDao.getAll().firstOrNull()?.firstOrNull()
        val entries      = journalEntryDao.getAll().firstOrNull() ?: emptyList()
        val moods        = moodLogDao.getAll().firstOrNull() ?: emptyList()
        val habits       = habitDao.getAll().firstOrNull() ?: emptyList()
        val habitLogs    = habitLogDao.getAll().firstOrNull() ?: emptyList()
        val todos        = todoDao.getAll().firstOrNull() ?: emptyList()
        val goals        = goalDao.getAll().firstOrNull() ?: emptyList()
        val usageLogs    = appUsageLogDao.getAll().firstOrNull() ?: emptyList()
        val interventions = interventionDao.getAll().firstOrNull() ?: emptyList()

        return ExportData(
            exportedAt      = System.currentTimeMillis(),
            userProfile     = profile?.toExport(),
            journalEntries  = entries.map { it.toExport() },
            moodLogs        = moods.map { it.toExport() },
            habits          = habits.map { it.toExport() },
            habitLogs       = habitLogs.map { it.toExport() },
            todos           = todos.map { it.toExport() },
            goals           = goals.map { it.toExport() },
            appUsageLogs    = usageLogs.map { it.toExport() },
            interventions   = interventions.map { it.toExport() },
        )
    }

    private suspend fun applyImport(data: ExportData) {
        data.userProfile?.let { userProfileDao.upsert(it.fromExport()) }
        data.journalEntries.forEach { journalEntryDao.upsert(it.fromExport()) }
        data.moodLogs.forEach { moodLogDao.upsert(it.fromExport()) }
        data.habits.forEach { habitDao.upsert(it.fromExport()) }
        data.habitLogs.forEach { habitLogDao.upsert(it.fromExport()) }
        data.todos.forEach { todoDao.upsert(it.fromExport()) }
        data.goals.forEach { goalDao.upsert(it.fromExport()) }
        data.appUsageLogs.forEach { appUsageLogDao.upsert(it.fromExport()) }
        data.interventions.forEach { interventionDao.upsert(it.fromExport()) }
    }

    private fun buildImportSummary(data: ExportData) =
        "${data.journalEntries.size} entries, ${data.habits.size} habits, ${data.goals.size} goals imported"

    private suspend fun writeToDownloads(filename: String, data: ByteArray) =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE,    "application/octet-stream")
                    put(MediaStore.Downloads.IS_PENDING,   1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
                context.contentResolver.openOutputStream(uri)!!.use { it.write(data) }
                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, cv, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                java.io.File(dir, filename).writeBytes(data)
            }
        }
}

// ── Entity ↔ Export mappers ───────────────────────────────────────────────────

private fun UserProfile.toExport() = ExportProfile(uid, name, age, gender, occupation, industry,
    maritalStatus, relationMapJson, struggleAreasJson, screenTimeGoalMinutes)

private fun ExportProfile.fromExport() = UserProfile(uid, name, age, gender, occupation, industry,
    maritalStatus, relationMapJson, struggleAreasJson, screenTimeGoalMinutes, true, "SYSTEM")

private fun JournalEntry.toExport() = ExportJournalEntry(entryId, timestamp, rawText,
    conversationJson, aiSummary, moodTag, moodScore, triggersJson, triageTier,
    clinicalSummaryJson, totalScreenTimeMs)

private fun ExportJournalEntry.fromExport() = JournalEntry(entryId, timestamp, rawText,
    conversationJson, aiSummary, moodTag, moodScore, triggersJson, triageTier,
    clinicalSummaryJson, totalScreenTimeMs, false)

private fun MoodLog.toExport() = ExportMoodLog(logId, date, moodScore, dominantMood,
    primaryTrigger, screenTimeMs, entryCount)

private fun ExportMoodLog.fromExport() = MoodLog(logId, date, moodScore, dominantMood,
    primaryTrigger, screenTimeMs, entryCount)

private fun Habit.toExport() = ExportHabit(habitId, title, emoji, frequency, customDaysJson,
    targetTime, goalId, color, streak, longestStreak, isArchived, createdAt)

private fun ExportHabit.fromExport() = Habit(habitId, title, emoji, frequency, customDaysJson,
    targetTime, goalId, color, streak, longestStreak, isArchived, createdAt)

private fun com.reflekt.journal.data.db.HabitLog.toExport() = ExportHabitLog(logId, habitId,
    date, status, completedViaJournal, note, moodAtCompletion)

private fun ExportHabitLog.fromExport() = com.reflekt.journal.data.db.HabitLog(logId, habitId,
    date, status, completedViaJournal, note, moodAtCompletion)

private fun Todo.toExport() = ExportTodo(todoId, title, description, dueDate, priority,
    goalId, isCompleted, completedAt, completedViaJournal, isArchived, createdAt)

private fun ExportTodo.fromExport() = Todo(todoId, title, description, dueDate, priority,
    goalId, isCompleted, completedAt, completedViaJournal, isArchived, createdAt)

private fun Goal.toExport() = ExportGoal(goalId, title, description, emoji, targetDate,
    color, milestonesJson, status, progressPercent, createdAt)

private fun ExportGoal.fromExport() = Goal(goalId, title, description, emoji, targetDate,
    color, milestonesJson, status, progressPercent, createdAt)

private fun AppUsageLog.toExport() = ExportAppUsageLog(logId, date, packageName, appLabel,
    category, durationMs, launchCount, impactScore, isTriggerApp)

private fun ExportAppUsageLog.fromExport() = AppUsageLog(logId, date, packageName, appLabel,
    category, durationMs, launchCount, impactScore, isTriggerApp)

private fun Intervention.toExport() = ExportIntervention(id, timestamp, triggerType, actionTaken,
    packageName, microtaskType, microtaskCompleted, overrideUsed, status, resolvedAt)

private fun ExportIntervention.fromExport() = Intervention(id, timestamp, triggerType, actionTaken,
    packageName, microtaskType, microtaskCompleted, overrideUsed, status, resolvedAt)
