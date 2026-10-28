package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ApiCallOutcome
import com.example.ai.ConnectionDiagnostics
import com.example.data.JarvisDatabase
import com.example.data.entity.MessageEntity
import com.example.model.AppConfig
import com.example.model.AssistantState
import com.example.model.FeatureCatalog
import com.example.model.TaskStatus
import com.example.model.WorkTask
import com.example.util.PermissionStatusChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: JarvisDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppConfigDefaultGemini38FlashAndThemeValues() {
        val config = AppConfig.default()
        assertEquals("bn", config.language)
        assertEquals("GEMINI", config.aiProvider)
        assertEquals("gemini-3.8-flash", config.modelName)
        assertEquals("gemini-4-argon", AppConfig.ADVANCED_GEMINI_MODEL)
        assertTrue(AppConfig.OFFICIAL_GEMINI_MODELS.contains("gemini-3.8-flash"))
        assertTrue(AppConfig.OFFICIAL_GEMINI_MODELS.contains("gemini-4-argon"))
        assertEquals("DARK", config.themeMode)
        assertTrue(config.showCharacterAvatar)
        assertTrue(config.assistantVoiceEnabled)
    }

    @Test
    fun testFeatureCatalogContainsAll21NormalizedCategories() {
        val categories = FeatureCatalog.allCategories
        assertEquals(21, categories.size)
        assertEquals(1, categories.first().number)
        assertEquals("1. Home Screen & 3D Character", categories.first().titleEn)
        assertEquals(15, categories[14].number)
        assertEquals("15. Weather & Location", categories[14].titleEn)
        assertEquals(21, categories.last().number)
        assertEquals("21. Diagnostics, Testing & Future Features", categories.last().titleEn)
    }

    @Test
    fun testPermissionStatusCheckerReturns10RuntimeAnd5AdvancedCards() {
        val tenRuntime = PermissionStatusChecker.getTenRuntimePermissionCards(context)
        val fiveAdvanced = PermissionStatusChecker.getFiveAdvancedBackgroundCards(context)
        assertEquals(10, tenRuntime.size)
        assertEquals(5, fiveAdvanced.size)
        assertEquals("1. Microphone", tenRuntime[0].titleEn)
        assertEquals("10. Bluetooth", tenRuntime[9].titleEn)
        assertEquals("1. Battery — No Optimisation", fiveAdvanced[0].titleEn)
        assertEquals("5. Default Digital Assistant", fiveAdvanced[4].titleEn)
    }

    @Test
    fun testConnectionDiagnosticsAndTypedOutcomes() {
        val authErr = ApiCallOutcome.AuthenticationError(
            httpCode = 401,
            detail = "API Key rejected (HTTP 401)"
        )
        assertEquals(401, authErr.httpCode)

        val diag = ConnectionDiagnostics(
            provider = "GEMINI",
            model = "gemini-3.8-flash",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/interactions",
            keySaved = true,
            configValidated = true,
            networkReachable = true,
            authAccepted = true,
            modelReachable = true,
            generationSucceeded = true,
            outcome = ApiCallOutcome.Success("JARVIS_OK", 200, 142L),
            sampleOutput = "JARVIS_OK"
        )
        assertTrue(diag.generationSucceeded)
        assertEquals("gemini-3.8-flash", diag.model)
    }

    @Test
    fun testAssistantStates() {
        assertEquals("সক্রিয়", AssistantState.ACTIVE.labelBn)
        assertEquals("শুনছি...", AssistantState.LISTENING.labelBn)
        assertEquals("প্রসেসিং হচ্ছে", AssistantState.PROCESSING.labelBn)
    }

    @Test
    fun testRoomDatabaseMessageInsertion() = runBlocking {
        val dao = database.jarvisDao()
        dao.insertMessage(
            MessageEntity(
                sender = "USER",
                content = "Hello JARVIS",
                timestamp = System.currentTimeMillis()
            )
        )

        val messages = dao.getAllMessages().first()
        assertEquals(1, messages.size)
        assertEquals("Hello JARVIS", messages[0].content)
        assertEquals("USER", messages[0].sender)
    }

    @Test
    fun testWorkTaskStateTransition() {
        val task = WorkTask(
            id = "task-1",
            name = "Test Automation",
            description = "Test Description",
            taskType = "AUTOMATION",
            status = TaskStatus.QUEUED
        )
        assertEquals(TaskStatus.QUEUED, task.status)
        val running = task.copy(status = TaskStatus.RUNNING, progress = 50)
        assertEquals(TaskStatus.RUNNING, running.status)
        assertEquals(50, running.progress)
    }
}
