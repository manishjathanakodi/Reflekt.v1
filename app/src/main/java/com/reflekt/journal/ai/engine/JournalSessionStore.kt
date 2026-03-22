package com.reflekt.journal.ai.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton store for passing transient session data between screens without nav args.
 * Currently used to carry the user's pre-journal mood check-in into JournalViewModel.
 */
@Singleton
class JournalSessionStore @Inject constructor() {
    @Volatile var pendingInitialMood: MoodTag? = null
}
