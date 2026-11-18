package com.example.core.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerSessionIsolationTest {

    @Test
    fun switchingServersRequiresClearingSession() {
        val a = ServerConnectionRules.serverFingerprint("https://a.example", "client-a")
        val b = ServerConnectionRules.serverFingerprint("https://b.example", "client-b")
        assertTrue(ServerSessionIsolation.mustClearSessionOnBind(a, b))
        assertFalse(ServerSessionIsolation.mustClearSessionOnBind(a, a))
        assertFalse(ServerSessionIsolation.mustClearSessionOnBind("", b))
    }

    @Test
    fun storedSessionOnlyValidForBoundFingerprint() {
        val a = ServerConnectionRules.serverFingerprint("https://a.example", "client-a")
        val b = ServerConnectionRules.serverFingerprint("https://b.example", "client-a")
        assertTrue(ServerSessionIsolation.mayUseStoredSession(a, a))
        assertFalse(ServerSessionIsolation.mayUseStoredSession(a, b))
        assertFalse(ServerSessionIsolation.mayUseStoredSession(null, a))
        assertFalse(ServerSessionIsolation.mayUseStoredSession(a, null))
    }

    @Test
    fun repeatedSwitchBetweenTwoServersAlwaysMismatchPrevious() {
        val s1 = ServerConnectionRules.serverFingerprint("https://one.test", "cid-1")
        val s2 = ServerConnectionRules.serverFingerprint("https://two.test", "cid-2")
        assertTrue(ServerSessionIsolation.mustClearSessionOnBind(s1, s2))
        assertTrue(ServerSessionIsolation.mustClearSessionOnBind(s2, s1))
        assertTrue(ServerSessionIsolation.mustClearSessionOnBind(s1, s2))
    }

    @Test
    fun metaIncompatibilityCodes() {
        val base = ServerDiscoveryInfo(
            service = "ai-iz-api",
            apiVersion = "1.0.0",
            protocol = "ai-iz-identity/v1",
            minClientProtocol = 1,
            capabilities = listOf("auth.google_oidc", "sessions.refresh"),
            googleClientId = "x.apps.googleusercontent.com",
            accessTokenAudience = "ai-iz-android",
            accessTokenIssuer = "https://one.test",
            authModes = listOf("google_oidc")
        )
        assertEquals(null, ServerConnectionRules.incompatibilityCode(base))
        assertEquals(
            "INCOMPATIBLE_PROTOCOL",
            ServerConnectionRules.incompatibilityCode(base.copy(protocol = "nope"))
        )
        assertEquals(
            "MISSING_CAPABILITIES",
            ServerConnectionRules.incompatibilityCode(base.copy(capabilities = emptyList()))
        )
        assertEquals(
            "OAUTH_CLIENT_MISSING",
            ServerConnectionRules.incompatibilityCode(base.copy(googleClientId = ""))
        )
        assertEquals(
            "CLIENT_TOO_OLD",
            ServerConnectionRules.incompatibilityCode(base.copy(minClientProtocol = 99))
        )
    }

    @Test
    fun discoveryClientRejectsMalformedAndNetworkStyleFailuresViaParse() {
        val client = ServerDiscoveryClient()
        assertEquals(null, client.parseMeta("{not-json"))
        assertEquals(null, client.parseMeta("{}"))
        val ok = client.parseMeta(
            """{"service":"ai-iz-api","apiVersion":"1","protocol":"ai-iz-identity/v1","minClientProtocol":1,"capabilities":["auth.google_oidc","sessions.refresh"],"oauth":{"googleClientId":"c.apps.googleusercontent.com","accessTokenAudience":"aud","accessTokenIssuer":"https://x"},"authModes":["google_oidc"]}"""
        )
        requireNotNull(ok)
        assertTrue(ServerConnectionRules.isCompatible(ok))
        // Probe without network: invalid URL path
        val bad = client.probe("http://evil.example", debugBuild = false)
        assertFalse(bad.ok)
        assertEquals("INVALID_BASE_URL", bad.errorCode)
    }
}
