package com.reflekt.journal.ui.screens.crisis

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.UserProfileDao
import com.reflekt.journal.security.CryptoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "CrisisViewModel"

@Serializable
data class ClinicalSummary(
    val durationOfConcern: String = "",
    val primaryMood: String = "",
    val keyTriggers: List<String> = emptyList(),
    val riskLevel: String = "",
)

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

@HiltViewModel
class CrisisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val journalEntryDao: JournalEntryDao,
    private val userProfileDao: UserProfileDao,
) : ViewModel() {

    private val _clinicalSummary = MutableStateFlow<ClinicalSummary?>(null)
    val clinicalSummary: StateFlow<ClinicalSummary?> = _clinicalSummary

    private val _exportState = MutableStateFlow<UiState<Uri>>(UiState.Idle)
    val exportState: StateFlow<UiState<Uri>> = _exportState

    init {
        loadLatestTier3()
    }

    private fun loadLatestTier3() {
        viewModelScope.launch {
            val entry = journalEntryDao.getLatestTier3() ?: return@launch
            val json  = entry.clinicalSummaryJson
            if (!json.isNullOrBlank()) {
                _clinicalSummary.value = runCatching {
                    Json.decodeFromString<ClinicalSummary>(json)
                }.getOrElse {
                    // Fallback: build from available entry data
                    ClinicalSummary(
                        primaryMood       = entry.moodTag,
                        riskLevel         = "High — review",
                        durationOfConcern = "recent",
                    )
                }
            } else {
                _clinicalSummary.value = ClinicalSummary(
                    primaryMood       = entry.moodTag,
                    riskLevel         = "High — review",
                    durationOfConcern = "recent",
                )
            }
        }
    }

    fun exportClinicalSummary(password: String) {
        val summary = _clinicalSummary.value ?: return
        _exportState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val salt    = (userProfileDao.getAll().firstOrNull()?.firstOrNull()?.uid
                    ?: "reflekt_default_salt").toByteArray()
                val key     = withContext(Dispatchers.Default) { CryptoUtils.deriveKey(password, salt) }
                val json    = Json.encodeToString(summary)
                val encrypted = withContext(Dispatchers.Default) {
                    CryptoUtils.encrypt(key, json.toByteArray(Charsets.UTF_8))
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val filename  = "reflekt_clinical_$timestamp.enc"
                val uri       = writeToDownloads(filename, encrypted)
                _exportState.value = UiState.Success(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _exportState.value = UiState.Error(e.message ?: "Export failed")
            }
        }
    }

    private suspend fun writeToDownloads(filename: String, data: ByteArray): Uri =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val cv = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE,    "application/octet-stream")
                    put(MediaStore.Downloads.IS_PENDING,   1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?: throw IllegalStateException("MediaStore insert returned null")
                resolver.openOutputStream(uri)!!.use { it.write(data) }
                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, cv, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val file = java.io.File(dir, filename)
                file.writeBytes(data)
                Uri.fromFile(file)
            }
        }

    fun resetExportState() { _exportState.value = UiState.Idle }
}
