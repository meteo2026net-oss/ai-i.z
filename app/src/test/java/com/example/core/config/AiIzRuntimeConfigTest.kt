package com.example.core.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiIzRuntimeConfigTest {

    @Test
    fun rejectsBlankAndNonHttpsHosts() {
        assertFalse(AiIzRuntimeConfig.isAcceptableApiBaseUrl("", debugBuild = true))
        assertFalse(AiIzRuntimeConfig.isAcceptableApiBaseUrl("http://evil.example", debugBuild = true))
        assertFalse(AiIzRuntimeConfig.isAcceptableApiBaseUrl("http://10.0.2.2:8080", debugBuild = false))
        assertTrue(AiIzRuntimeConfig.isAcceptableApiBaseUrl("https://api.example.com", debugBuild = false))
        assertTrue(AiIzRuntimeConfig.isAcceptableApiBaseUrl("http://10.0.2.2:8080", debugBuild = true))
    }

    @Test
    fun googleClientIdMustLookLikeWebOauthClient() {
        assertFalse(AiIzRuntimeConfig.isConfiguredGoogleWebClientId(""))
        assertFalse(AiIzRuntimeConfig.isConfiguredGoogleWebClientId("not-a-client"))
        assertTrue(
            AiIzRuntimeConfig.isConfiguredGoogleWebClientId(
                "1234567890-abcdef.apps.googleusercontent.com"
            )
        )
    }

    @Test
    fun cloudSignInRequiresBothApiAndOauth() {
        assertFalse(
            AiIzRuntimeConfig.isCloudSignInReady(
                apiBaseUrl = "",
                googleWebClientId = "123.apps.googleusercontent.com",
                debugBuild = true
            )
        )
        assertFalse(
            AiIzRuntimeConfig.isCloudSignInReady(
                apiBaseUrl = "https://api.example.com",
                googleWebClientId = "",
                debugBuild = true
            )
        )
        assertTrue(
            AiIzRuntimeConfig.isCloudSignInReady(
                apiBaseUrl = "https://api.example.com",
                googleWebClientId = "123.apps.googleusercontent.com",
                debugBuild = false
            )
        )
    }
}
