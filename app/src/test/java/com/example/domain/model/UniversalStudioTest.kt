package com.example.domain.model

import com.example.data.repository.UniversalStudioProjectGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalStudioTest {

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
}
