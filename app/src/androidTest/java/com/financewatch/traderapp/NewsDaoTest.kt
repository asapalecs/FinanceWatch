package com.financewatch.traderapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.financewatch.traderapp.data.local.AppDatabase
import com.financewatch.traderapp.data.local.dao.NewsDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de integrare pentru NewsDao.
 * Foloseste Room in-memory database — nu scrie pe disk, se sterge dupa test.
 *
 * @RunWith(AndroidJUnit4) este necesar pentru a rula pe emulator sau dispozitiv real.
 * Aceste teste TREBUIE rulate ca Instrumented Tests (androidTest), nu ca Unit Tests.
 */
@RunWith(AndroidJUnit4::class)
class NewsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: NewsDao

    @Before
    fun setup() {
        // Creeaza o baza de date in memorie pentru teste (se reseteaza la fiecare test)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.newsDao()
    }

    @After
    fun tearDown() {
        database.close() // Elibereaza resursele dupa fiecare test
    }

    /**
     * TC-DB-001: Inserarea si recuperarea stirilor functioneaza corect.
     */
    @Test
    fun testInsertAndRetrieveNews() = runTest {
        // Arrange: pregatim o stire de test
        val news = createNewsEntity(id = "test-1", category = "nasdaq")

        // Act: inserez in baza de date
        dao.insertAll(listOf(news))

        // Assert: o recuperez si verific ca datele sunt corecte
        val result = dao.getAllNews().first()
        assertEquals(1, result.size)
        assertEquals("test-1", result[0].id)
        assertEquals("nasdaq", result[0].category)
    }

    /**
     * TC-DB-002: Strategia REPLACE suprascrie stirile cu acelasi ID.
     */
    @Test
    fun testInsertReplace_updatesExistingEntry() = runTest {
        // Arrange
        val original = createNewsEntity(id = "dup-1", title = "Titlu original")
        val updated = createNewsEntity(id = "dup-1", title = "Titlu actualizat")

        // Act
        dao.insertAll(listOf(original))
        dao.insertAll(listOf(updated)) // Acelasi ID → REPLACE

        // Assert: trebuie sa existe un singur element cu titlul actualizat
        val result = dao.getAllNews().first()
        assertEquals(1, result.size)
        assertEquals("Titlu actualizat", result[0].title)
    }

    /**
     * TC-DB-003: Filtrarea pe categorie returneaza doar stirile din acea categorie.
     */
    @Test
    fun testGetNewsByCategory_filtersCorrectly() = runTest {
        // Arrange: inserez stiri din 3 categorii diferite
        dao.insertAll(listOf(
            createNewsEntity(id = "n1", category = "nasdaq"),
            createNewsEntity(id = "n2", category = "nasdaq"),
            createNewsEntity(id = "g1", category = "gold"),
            createNewsEntity(id = "f1", category = "forex")
        ))

        // Act: filtrez pe "nasdaq"
        val nasdaqNews = dao.getNewsByCategory("nasdaq").first()
        val goldNews = dao.getNewsByCategory("gold").first()

        // Assert
        assertEquals(2, nasdaqNews.size)
        assertEquals(1, goldNews.size)
        assertTrue(nasdaqNews.all { it.category == "nasdaq" })
    }

    /**
     * TC-DB-004: deleteOlderThan sterge stirile mai vechi de un anumit timestamp.
     */
    @Test
    fun testDeleteOlderThan_removesCorrectEntries() = runTest {
        // Arrange: doua stiri — una veche (de ieri), una noua
        val oneHourMs = 60 * 60 * 1000L
        val oldNews = createNewsEntity(id = "old", cachedAt = System.currentTimeMillis() - 2 * oneHourMs)
        val newNews = createNewsEntity(id = "new", cachedAt = System.currentTimeMillis())
        dao.insertAll(listOf(oldNews, newNews))

        // Act: sterg stirile mai vechi de 1 ora
        val threshold = System.currentTimeMillis() - oneHourMs
        dao.deleteOlderThan(threshold)

        // Assert: ramane doar stirea noua
        val result = dao.getAllNews().first()
        assertEquals(1, result.size)
        assertEquals("new", result[0].id)
    }

    /**
     * TC-DB-005: Numarul de stiri este corect calculat.
     */
    @Test
    fun testGetNewsCount_returnsCorrectCount() = runTest {
        // Arrange
        dao.insertAll(listOf(
            createNewsEntity("c1"), createNewsEntity("c2"), createNewsEntity("c3")
        ))

        // Assert
        val count = dao.getNewsCount().first()
        assertEquals(3, count)
    }

    @Test
    fun testReplaceCategory_dropsStaleEntriesOfThatCategoryOnly() = runTest {
        dao.insertAll(listOf(
            createNewsEntity(id = "old-gold", category = "gold"),
            createNewsEntity(id = "fx", category = "forex")
        ))

        dao.replaceCategory("gold", listOf(createNewsEntity(id = "new-gold", category = "gold")))

        assertEquals(listOf("new-gold"), dao.getNewsByCategory("gold").first().map { it.id })
        assertEquals(1, dao.getNewsByCategory("forex").first().size)
    }
}
