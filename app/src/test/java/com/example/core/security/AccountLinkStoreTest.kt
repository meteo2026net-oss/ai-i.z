package com.example.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountLinkStoreTest {

    private class FakeSecretStorage : SecretStorage {
        private val map = mutableMapOf<String, String>()
        override fun putSecret(key: String, value: String) {
            if (value.isBlank()) map.remove(key) else map[key] = value
        }
        override fun getSecret(key: String): String = map[key].orEmpty()
        override fun hasSecret(key: String): Boolean = !map[key].isNullOrBlank()
        override fun clearAll() = map.clear()
    }

    @Test
    fun multipleGitHubAccounts_switchAndAutoMatchByRepo() {
        val storage = FakeSecretStorage()
        val store = AccountLinkStore(storage)

        val gh1 = store.upsertGitHubAccount(
            label = "حساب METRA الأساسي",
            login = "gfa437926-tech",
            displayName = "METRA Tech",
            avatarUrl = "",
            defaultRepo = "gfa437926-tech/metra2027",
            token = "ghp_token_primary",
            setActive = true
        )
        val gh2 = store.upsertGitHubAccount(
            label = "حساب العمل",
            login = "work-dev",
            displayName = "Work Dev",
            avatarUrl = "",
            defaultRepo = "work-org/flutter-client",
            token = "ghp_token_work",
            setActive = true
        )

        assertEquals("ghp_token_work", storage.getSecret(SecretVault.KEY_GITHUB_PAT))
        val snap = store.loadSnapshot()
        assertEquals(2, snap.gitHubAccounts.size)
        assertFalse(snap.gitHubAccounts.first { it.id == gh1.id }.isActive)
        assertTrue(snap.gitHubAccounts.first { it.id == gh2.id }.isActive)

        val matched = store.findGitHubAccountForRepo("gfa437926-tech/metra2027")
        assertNotNull(matched)
        assertEquals(gh1.id, matched!!.id)

        store.activateGitHubAccount(gh1.id)
        assertEquals("ghp_token_primary", storage.getSecret(SecretVault.KEY_GITHUB_PAT))
    }

    @Test
    fun workspaceBundle_linksGoogleGitHubAndBackendTogether() {
        val storage = FakeSecretStorage()
        val store = AccountLinkStore(storage)

        store.recordGoogleAccount("uid_1", "dev1@gmail.com", "Dev One")
        store.recordGoogleAccount("uid_2", "dev2@gmail.com", "Dev Two")

        val gh1 = store.upsertGitHubAccount(
            label = "Personal",
            login = "dev1-gh",
            displayName = "Dev 1",
            avatarUrl = "",
            defaultRepo = "gfa437926-tech/metra2027",
            token = "ghp_dev1",
            setActive = true
        )
        val be1 = store.upsertBackendProfile(
            label = "Staging Runner",
            aiProvider = "openai",
            aiModel = "gpt-4o-mini",
            openAiKey = "sk-dev1",
            backendUrl = "https://staging.metra.example",
            backendToken = "be_tok_1",
            setActive = true
        )
        val gh2 = store.upsertGitHubAccount(
            label = "Secondary",
            login = "dev2-gh",
            displayName = "Dev 2",
            avatarUrl = "",
            defaultRepo = "gfa437926-tech/metra-weather-intelligence",
            token = "ghp_dev2",
            setActive = true
        )

        val bundle1 = store.upsertBundle(
            name = "حزمة METRA الرئيسية",
            googleUid = "uid_1",
            googleEmail = "dev1@gmail.com",
            gitHubAccountId = gh1.id,
            gitHubLogin = gh1.login,
            backendProfileId = be1.id,
            backendLabel = be1.label,
            repoSlug = "gfa437926-tech/metra2027",
            branch = "main",
            setActive = false
        )

        // Currently gh2 is active
        assertEquals("ghp_dev2", storage.getSecret(SecretVault.KEY_GITHUB_PAT))

        // Activating bundle1 switches back to gh1 + be1 together
        val activated = store.activateBundle(bundle1.id)
        assertNotNull(activated)
        assertEquals("ghp_dev1", storage.getSecret(SecretVault.KEY_GITHUB_PAT))
        assertEquals("sk-dev1", storage.getSecret(SecretVault.KEY_OPENAI_API))
        assertEquals("be_tok_1", storage.getSecret(SecretVault.KEY_BACKEND_TOKEN))
        assertEquals(2, store.loadSnapshot().googleAccounts.size)
        assertTrue(store.loadSnapshot().gitHubAccounts.first { it.id == gh1.id }.isActive)
        assertFalse(store.loadSnapshot().gitHubAccounts.first { it.id == gh2.id }.isActive)
    }
}
