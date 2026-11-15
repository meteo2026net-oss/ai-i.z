package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackendUrlPolicyTest {


    @Test
    fun backendRunnerUsesOnlyAWellFormedAiIzAccessToken() {
        assertEquals(
            "Bearer header.payload.signature",
            BackendAuthPolicy.authorizationHeader(" header.payload.signature ")
        )
        assertThrows(IllegalArgumentException::class.java) {
            BackendAuthPolicy.authorizationHeader("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendAuthPolicy.authorizationHeader("legacy-static-token")
        }
    }

    @Test
    fun acceptsHttpsProductionUrl() {
        assertEquals(
            "https://runner.example.test/",
            BackendUrlPolicy.normalizeAndValidate(" https://runner.example.test ", false)
        )
    }

    @Test
    fun rejectsRemoteHttpUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendUrlPolicy.normalizeAndValidate("http://runner.example.test", true)
        }
    }

    @Test
    fun allowsLoopbackHttpOnlyDuringDebug() {
        assertEquals(
            "http://10.0.2.2:8080/",
            BackendUrlPolicy.normalizeAndValidate("http://10.0.2.2:8080", true)
        )
        assertThrows(IllegalArgumentException::class.java) {
            BackendUrlPolicy.normalizeAndValidate("http://localhost:8080", false)
        }
    }

    @Test
    fun rejectsCredentialsAndQueryParametersInBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendUrlPolicy.normalizeAndValidate("https://user:password@runner.example.test", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendUrlPolicy.normalizeAndValidate("https://runner.example.test?token=secret", false)
        }
    }
}
