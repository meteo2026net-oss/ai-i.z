package com.example.domain.model

import com.example.data.repository.UniversalStudioProjectGenerator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalStudioTest {

    @Test
    fun savedProjectSummaryDeserializesWithoutSourceFiles() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(StudioProjectSummary::class.java)
        val summary = adapter.fromJson(
            """
            {
              "id": "project_123",
              "projectName": "Weather dashboard",
              "environment": "WEB_REACT_NEXT",
              "primaryLanguage": "TYPESCRIPT",
              "description": "A saved web project",
              "fileCount": 7
            }
            """.trimIndent()
        )

        assertNotNull(summary)
        assertEquals("project_123", summary!!.id)
        assertEquals("Weather dashboard", summary.projectName)
        assertEquals(DevelopmentEnvironment.WEB_REACT_NEXT, summary.environment)
        assertEquals(ProgrammingLanguage.TYPESCRIPT, summary.primaryLanguage)
        assertEquals(7, summary.fileCount)
    }

    @Test
    fun testModelAdvisorRecommendsGeminiForAndroid() {
        val recommendations = ModelAdvisorEngine.recommendModels(
            environment = DevelopmentEnvironment.MOBILE_ANDROID,
            language = ProgrammingLanguage.KOTLIN
        )
        assertTrue(recommendations.isNotEmpty())
        val topPick = recommendations.first()
        assertTrue(topPick.modelId.contains("gemini") || topPick.modelId.contains("claude"))
        assertTrue(topPick.matchScore >= 95)
    }

    @Test
    fun testModelAdvisorRecommendsClaudeForNextJs() {
        val recommendations = ModelAdvisorEngine.recommendModels(
            environment = DevelopmentEnvironment.WEB_REACT_NEXT,
            language = ProgrammingLanguage.TYPESCRIPT
        )
        assertTrue(recommendations.isNotEmpty())
        val topPick = recommendations.first()
        assertEquals("claude-3-7-sonnet", topPick.modelId)
    }

    @Test
    fun testUniversalProjectGeneratorGeneratesFastApiFiles() {
        val project = UniversalStudioProjectGenerator.generateProject(
            name = "fintech_service",
            prompt = "Secure payment processing API",
            environment = DevelopmentEnvironment.BACKEND_PYTHON,
            language = ProgrammingLanguage.PYTHON
        )
        assertEquals("fintech_service", project.projectName)
        assertEquals(DevelopmentEnvironment.BACKEND_PYTHON, project.environment)
        assertTrue(project.files.any { it.path == "main.py" })
        assertTrue(project.files.any { it.path == "requirements.txt" })
        val mainPy = project.files.find { it.path == "main.py" }
        assertNotNull(mainPy)
        assertTrue(mainPy!!.content.contains("FastAPI"))
    }


    @Test
    fun unsupportedEnvironmentDoesNotFabricatePassingTests() {
        val project = UniversalStudioProjectGenerator.generateProject(
            name = "react_native_project",
            prompt = "A mobile project",
            environment = DevelopmentEnvironment.MOBILE_REACT_NATIVE,
            language = ProgrammingLanguage.TYPESCRIPT
        )

        assertTrue(project.testInstructions.contains("لم يتم تحديد أمر اختبار"))
        assertTrue(project.buildInstructions.contains("لم يتم تحديد أمر بناء"))
        assertTrue(project.runInstructions.contains("لم يتم تحديد أمر تشغيل"))
        assertTrue(!project.buildInstructions.contains("make build"))
        assertTrue(!project.runInstructions.contains("./run.sh"))
        assertTrue(!project.testInstructions.contains("All tests passed"))
    }

    @Test
    fun generatedProjectDoesNotClaimAnAuditBeforeEvidenceExists() {
        val project = UniversalStudioProjectGenerator.generateProject(
            name = "audit_boundary_check",
            prompt = "A sample project",
            environment = DevelopmentEnvironment.BACKEND_PYTHON,
            language = ProgrammingLanguage.PYTHON
        )

        assertEquals(null, project.securityAuditScore)
        assertTrue(project.files.isNotEmpty())
    }
}
