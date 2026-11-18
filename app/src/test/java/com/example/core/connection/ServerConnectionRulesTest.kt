package com.example.core.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerConnectionRulesTest {

    @Test
    fun httpsRequiredInReleaseStyleChecks() {
        assertTrue(ServerConnectionRules.isAcceptableBaseUrl("https://api.example.com", debugBuild = false))
        assertFalse(ServerConnectionRules.isAcceptableBaseUrl("http://api.example.com", debugBuild = false))
        assertTrue(ServerConnectionRules.isAcceptableBaseUrl("http://10.0.2.2:8080", debugBuild = true))
        assertFalse(ServerConnectionRules.isAcceptableBaseUrl("", debugBuild = true))
    }

    @Test
    fun compatibilityRequiresProtocolCapabilitiesAndOauth() {
        val good = ServerDiscoveryInfo(
            service = "ai-iz-api",
            apiVersion = "1.0.0",
            protocol = "ai-iz-identity/v1",
            minClientProtocol = 1,
            capabilities = listOf("auth.google_oidc", "sessions.refresh", "user_records"),
            googleClientId = "123.apps.googleusercontent.com",
            accessTokenAudience = "ai-iz-android",
            accessTokenIssuer = "https://api.example.com",
            authModes = listOf("google_oidc")
        )
        assertTrue(ServerConnectionRules.isCompatible(good))
        assertEquals(null, ServerConnectionRules.incompatibilityCode(good))

        val badProtocol = good.copy(protocol = "other")
        assertEquals("INCOMPATIBLE_PROTOCOL", ServerConnectionRules.incompatibilityCode(badProtocol))

        val missingOauth = good.copy(googleClientId = "")
        assertEquals("OAUTH_CLIENT_MISSING", ServerConnectionRules.incompatibilityCode(missingOauth))

        val missingCaps = good.copy(capabilities = listOf("user_records"))
        assertEquals("MISSING_CAPABILITIES", ServerConnectionRules.incompatibilityCode(missingCaps))
    }

    @Test
    fun fingerprintChangesWhenHostOrClientChanges() {
        val a = ServerConnectionRules.serverFingerprint("https://a.example", "client-1")
        val b = ServerConnectionRules.serverFingerprint("https://b.example", "client-1")
        val c = ServerConnectionRules.serverFingerprint("https://a.example", "client-2")
        assertTrue(a != b)
        assertTrue(a != c)
        assertEquals(
            ServerConnectionRules.serverFingerprint("https://a.example/", "client-1"),
            ServerConnectionRules.serverFingerprint("https://a.example", "client-1")
        )
    }

    @Test
    fun parseMetaFromDiscoveryClient() {
        val json = """
        {
          "service":"ai-iz-api",
          "apiVersion":"1.0.0",
          "protocol":"ai-iz-identity/v1",
          "minClientProtocol":1,
          "capabilities":["auth.google_oidc","sessions.refresh"],
          "oauth":{"googleClientId":"abc.apps.googleusercontent.com","accessTokenAudience":"ai-iz-android","accessTokenIssuer":"https://api.example"},
          "authModes":["google_oidc"]
        }
        """.trimIndent()
        val info = ServerDiscoveryClient().parseMeta(json)
        requireNotNull(info)
        assertTrue(ServerConnectionRules.isCompatible(info))
        assertEquals("ai-iz-api", info.service)
    }
}
