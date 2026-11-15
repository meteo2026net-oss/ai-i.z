package com.example.data.repository

import com.example.domain.model.ProgrammingLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioAiProjectResponseParserTest {

    @Test
    fun parsesFencedJsonAndPreservesEntrypoint() {
        val response = """
            ```json
            {
              "files": [
                {
                  "path": "src/main.py",
                  "language": "PYTHON",
                  "purpose": "Application entry point",
                  "isEntrypoint": true,
                  "content": "print(\\\"hello\\\")"
                },
                {
                  "path": "README.md",
                  "language": "PYTHON",
                  "purpose": "Project guide",
                  "isEntrypoint": false,
                  "content": "# Sample"
                }
              ]
            }
            ```
        """.trimIndent()

        val files = StudioAiProjectResponseParser.parseFiles(response, ProgrammingLanguage.PYTHON)

        assertEquals(2, files.size)
        assertEquals("src/main.py", files.first { it.isEntrypoint }.path)
        assertTrue(files.any { it.path == "README.md" })
    }

    @Test
    fun rejectsParentDirectoryTraversal() {
        val response = manifest("../outside.txt", "safe")
        assertThrows(IllegalArgumentException::class.java) {
            StudioAiProjectResponseParser.parseFiles(response, ProgrammingLanguage.KOTLIN)
        }
    }

    @Test
    fun rejectsDuplicatePathsIgnoringCase() {
        val response = """
            {"files":[
              {"path":"src/Main.kt","content":"one","isEntrypoint":true},
              {"path":"src/main.kt","content":"two","isEntrypoint":false}
            ]}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            StudioAiProjectResponseParser.parseFiles(response, ProgrammingLanguage.KOTLIN)
        }
    }

    @Test
    fun requiresExactlyOneEntrypoint() {
        val response = """
            {"files":[
              {"path":"README.md","content":"guide","isEntrypoint":false}
            ]}
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            StudioAiProjectResponseParser.parseFiles(response, ProgrammingLanguage.KOTLIN)
        }
    }

    @Test
    fun rejectsSecretKeyFiles() {
        val response = manifest("keys/upload-key.jks", "private-key-material")
        assertThrows(IllegalArgumentException::class.java) {
            StudioAiProjectResponseParser.parseFiles(response, ProgrammingLanguage.KOTLIN)
        }
    }

    private fun manifest(path: String, source: String): String {
        val escapedPath = path.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedSource = source.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"files":[{"path":"$escapedPath","content":"$escapedSource","isEntrypoint":true}]}"""
    }
}
