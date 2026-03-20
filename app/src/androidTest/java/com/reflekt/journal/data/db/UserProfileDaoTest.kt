package com.reflekt.journal.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileDaoTest {

    private lateinit var db: ReflektDatabase
    private lateinit var dao: UserProfileDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReflektDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.userProfileDao()
    }

    @After
    fun tearDown() = db.close()

    private fun profile(uid: String = "uid-1") = UserProfile(
        uid = uid, name = "Alice", age = 30, gender = "Female",
        occupation = "Engineer", industry = "Tech", maritalStatus = "Single",
        relationMapJson = "[]", struggleAreasJson = "[]",
        screenTimeGoalMinutes = 120, onboardingComplete = true, themePreference = "SYSTEM",
    )

    @Test
    fun insertAndGetById() = runTest {
        val p = profile()
        dao.insert(p)
        assertEquals(p, dao.getById(p.uid))
    }

    @Test
    fun upsertUpdatesExisting() = runTest {
        dao.insert(profile())
        val updated = profile().copy(name = "Bob")
        dao.upsert(updated)
        assertEquals("Bob", dao.getById("uid-1")?.name)
    }

    @Test
    fun deleteRemovesRecord() = runTest {
        val p = profile()
        dao.insert(p)
        dao.delete(p)
        assertNull(dao.getById(p.uid))
    }

    @Test
    fun getAllReturnsInserted() = runTest {
        dao.insert(profile("uid-1"))
        dao.insert(profile("uid-2"))
        val all = dao.getAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getByIdReturnsNullWhenMissing() = runTest {
        assertNull(dao.getById("ghost"))
    }
}
