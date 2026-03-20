package com.reflekt.journal.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.reflekt.journal.ai.engine.MoodTag
import com.reflekt.journal.data.db.JournalEntry
import com.reflekt.journal.data.db.JournalEntryDao
import com.reflekt.journal.data.db.MoodLog
import com.reflekt.journal.data.db.MoodLogDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
    private val moodLogDao: MoodLogDao,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val moodFilter  = MutableStateFlow<MoodTag?>(null)

    /** Last 7 days of mood logs — powers the weekly heatmap. */
    val heatmapData: StateFlow<List<MoodLog>> = moodLogDao
        .getLastNDays(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: StateFlow<JournalEntry?> = _selectedEntry.asStateFlow()

    /**
     * Paged journal entries, re-emitted whenever searchQuery (debounced 300 ms)
     * or moodFilter changes.  Four query paths cover all combinations:
     *   search + mood → FTS + mood filter
     *   search only  → FTS
     *   mood only    → mood filter
     *   none         → all entries
     */
    val entries: Flow<PagingData<JournalEntry>> = combine(
        searchQuery.debounce(300L),
        moodFilter,
    ) { q, m -> q to m }
        .flatMapLatest { (query, mood) ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                val ftsQuery = "${query.trim()}*"
                when {
                    query.isNotBlank() && mood != null ->
                        journalEntryDao.searchEntriesByMoodPaged(ftsQuery, mood.name)
                    query.isNotBlank() ->
                        journalEntryDao.searchEntriesPaged(ftsQuery)
                    mood != null ->
                        journalEntryDao.getAllByMoodPaged(mood.name)
                    else ->
                        journalEntryDao.getAllPaged()
                }
            }.flow
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(query: String) { searchQuery.value = query }
    fun onMoodFilterChanged(mood: MoodTag?)  { moodFilter.value  = mood  }
    fun onEntrySelected(entry: JournalEntry) { _selectedEntry.value = entry }
    fun onEntryDismissed()                   { _selectedEntry.value = null  }

    fun onDeleteEntry(entryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            journalEntryDao.softDelete(entryId)
            if (_selectedEntry.value?.entryId == entryId) _selectedEntry.value = null
        }
    }
}
