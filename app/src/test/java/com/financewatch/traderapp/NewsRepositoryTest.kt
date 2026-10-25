package com.financewatch.traderapp

import com.financewatch.traderapp.data.local.dao.NewsDao
import com.financewatch.traderapp.data.local.dao.SavedNewsDao
import com.financewatch.traderapp.data.local.entity.NewsEntity
import com.financewatch.traderapp.data.remote.NewsApiService
import com.financewatch.traderapp.repository.FirestoreRepository
import com.financewatch.traderapp.repository.NewsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response

/**
 * Teste unitare pentru NewsRepository.
 * Foloseste Mockito-Kotlin pentru a simula DAO-urile si API-ul.
 * Testeaza logica offline-first a repository-ului.
 *
 * NOTA: refreshNews apeleaza apiService.getTopHeadlines(), nu getEverything().
 */
class NewsRepositoryTest {

    // Mock-uri — simulam dependentele, nu le folosim pe cele reale
    private lateinit var mockNewsDao: NewsDao
    private lateinit var mockSavedNewsDao: SavedNewsDao
    private lateinit var mockApiService: NewsApiService
    private lateinit var repository: NewsRepository

    @Before
    fun setup() {
        mockNewsDao = mock()
        mockSavedNewsDao = mock()
        mockApiService = mock()

        repository = NewsRepository(mockNewsDao, mockSavedNewsDao, mockApiService, mock<FirestoreRepository>())
    }

    /**
     * TC-REPO-001: refreshNews reusit → insereaza articolele in DAO.
     * refreshNews apeleaza getTopHeadlines (nu getEverything).
     */
    @Test
    fun testRefreshNews_whenApiSuccess_insertsToDao() = runTest {
        // Arrange: API returneaza 2 articole
        val articles = listOf(createArticle("Article 1"), createArticle("Article 2"))
        val response = Response.success(createNewsResponse(articles))
        // refreshNews apeleaza getTopHeadlines(category, language, pageSize)
        whenever(mockApiService.getTopHeadlines(any(), any(), any()))
            .thenReturn(response)

        // Act
        val result = repository.refreshNews("general")

        // Assert: succesul e semnalat si DAO-ul primeste datele
        assertTrue(result.isSuccess)
        verify(mockNewsDao).replaceCategory(eq("general"), any())
    }

    /**
     * TC-REPO-002: refreshNews cu exceptie de retea → returneaza failure.
     */
    @Test
    fun testRefreshNews_whenNetworkError_returnsFailure() = runTest {
        // Arrange: simulam absenta internetului
        whenever(mockApiService.getTopHeadlines(any(), any(), any()))
            .thenThrow(RuntimeException("No network connection"))

        // Act
        val result = repository.refreshNews("general")

        // Assert: returneaza failure
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    /**
     * TC-REPO-003: refreshNews cu raspuns 401 → returneaza eroare API key.
     */
    @Test
    fun testRefreshNews_when401_returnsApiKeyError() = runTest {
        // Arrange: API returneaza 401 Unauthorized
        val errorResponse = Response.error<com.financewatch.traderapp.data.remote.model.NewsResponse>(
            401,
            okhttp3.ResponseBody.create(null, "Unauthorized")
        )
        whenever(mockApiService.getTopHeadlines(any(), any(), any()))
            .thenReturn(errorResponse)

        // Act
        val result = repository.refreshNews("general")

        // Assert: returneaza failure cu mentionarea codului 401
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue("Mesajul trebuie sa contina '401'", message.contains("401"))
    }

    /**
     * TC-REPO-004: saveNews apeleaza SavedNewsDao.save cu datele corecte.
     */
    @Test
    fun testSaveNews_callsSavedNewsDao() = runTest {
        // Arrange
        val news = createNewsEntity("save-test", category = "gold")

        // Act
        repository.saveNews(news)

        // Assert: DAO-ul de salvare a fost apelat
        verify(mockSavedNewsDao).save(argThat { id == "save-test" })
    }

    /**
     * TC-REPO-005: unsaveNews apeleaza SavedNewsDao.delete cu ID-ul corect.
     */
    @Test
    fun testUnsaveNews_callsDeleteWithCorrectId() = runTest {
        // Act
        repository.unsaveNews("delete-me")

        // Assert
        verify(mockSavedNewsDao).delete("delete-me")
    }

    /**
     * TC-REPO-006: clearOldCache calculeaza corect threshold-ul de 24h.
     */
    @Test
    fun testClearOldCache_callsDeleteWithCorrectThreshold() = runTest {
        val beforeCall = System.currentTimeMillis()

        // Act
        repository.clearOldCache()

        val afterCall = System.currentTimeMillis()

        // Assert: deleteOlderThan a fost apelat cu un threshold in intervalul corect
        val expectedMin = beforeCall - (24 * 60 * 60 * 1000L)
        val expectedMax = afterCall - (24 * 60 * 60 * 1000L)

        val captor = argumentCaptor<Long>()
        verify(mockNewsDao).deleteOlderThan(captor.capture())
        assertTrue(captor.firstValue in expectedMin..expectedMax)
    }

    @Test
    fun testRefreshNews_forMarketCategory_usesEverythingEndpoint() = runTest {
        val response = Response.success(createNewsResponse(listOf(createArticle("Gold rallies"))))
        whenever(mockApiService.getEverything(any(), any(), any(), any(), any())).thenReturn(response)

        val result = repository.refreshNews("gold")

        assertTrue(result.isSuccess)
        assertEquals("gold", result.getOrThrow().first().category)
        verify(mockApiService, never()).getTopHeadlines(any(), any(), any())
    }
}
