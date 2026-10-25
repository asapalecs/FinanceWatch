package com.financewatch.traderapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.local.dao.SavedNewsDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de integrare pentru SavedNewsDao.
 * Foloseste Room in-memory database — nu scrie pe disk, se sterge dupa test.
 *
 * TREBUIE rulate ca Instrumented Tests (androidTest), nu JVM Unit Tests,
 * deoarece folosesc ApplicationProvider care necesita runtime Android.
 */
@RunWith(AndroidJUnit4::class)
class SavedNewsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SavedNewsDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.savedNewsDao()
    }

    @After
    fun tearDown() = database.close()

    /**
     * TC-SAVE-001: O stire salvata apare in lista de bookmarks.
     */
    @Test
    fun testSaveNews_appearsInList() = runTest {
        val saved = createSavedNewsEntity(id = "s1")
        dao.save(saved)

        val result = dao.getAllSaved().first()
        assertEquals(1, result.size)
        assertEquals("s1", result[0].id)
    }

    /**
     * TC-SAVE-002: isSaved returneaza true pentru o stire salvata.
     */
    @Test
    fun testIsSaved_returnsTrueForSaved() = runTest {
        val saved = createSavedNewsEntity(id = "s2")
        dao.save(saved)

        val isSaved = dao.isSaved("s2").first()
        val isNotSaved = dao.isSaved("nonexistent").first()

        assertTrue(isSaved)
        assertFalse(isNotSaved)
    }

    /**
     * TC-SAVE-003: Stergerea unei stiri o elimina din lista.
     */
    @Test
    fun testDeleteSaved_removesFromList() = runTest {
        dao.save(createSavedNewsEntity("s3"))
        dao.save(createSavedNewsEntity("s4"))

        dao.delete("s3")

        val result = dao.getAllSaved().first()
        assertEquals(1, result.size)
        assertEquals("s4", result[0].id)
    }

    /**
     * TC-SAVE-004: deleteAll curata complet tabelul.
     */
    @Test
    fun testDeleteAll_clearsTable() = runTest {
        dao.save(createSavedNewsEntity("s5"))
        dao.save(createSavedNewsEntity("s6"))
        dao.save(createSavedNewsEntity("s7"))

        dao.deleteAll()

        val result = dao.getAllSaved().first()
        assertTrue(result.isEmpty())
    }

    /**
     * TC-SAVE-005: Strategia IGNORE nu suprascrie o stire deja salvata.
     */
    @Test
    fun testSaveDuplicate_isIgnored() = runTest {
        dao.save(createSavedNewsEntity("dup", "Titlu original"))
        dao.save(createSavedNewsEntity("dup", "Titlu nou")) // Trebuie ignorat

        val result = dao.getAllSaved().first()
        assertEquals(1, result.size)
        assertEquals("Titlu original", result[0].title) // Pastreaza originalul
    }
}
