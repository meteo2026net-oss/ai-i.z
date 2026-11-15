package com.example.core.security

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LinkedGoogleAccount(
    val uid: String,
    val email: String,
    val displayName: String,
    val linkedBundleId: String = "",
    val lastUsedEpochMs: Long = 0L
)

data class LinkedGitHubAccount(
    val id: String,
    val label: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val defaultRepo: String,
    val token: String,
    val isActive: Boolean
)

data class LinkedBackendProfile(
    val id: String,
    val label: String,
    val aiProvider: String,
    val aiModel: String,
    val openAiKey: String,
    val backendUrl: String,
    val backendToken: String,
    val isActive: Boolean
)

data class WorkspaceAccountBundle(
    val id: String,
    val name: String,
    val googleUid: String,
    val googleEmail: String,
    val gitHubAccountId: String,
    val gitHubLogin: String,
    val backendProfileId: String,
    val backendLabel: String,
    val repoSlug: String,
    val branch: String,
    val isActive: Boolean
)

data class LinkedAccountsSnapshot(
    val googleAccounts: List<LinkedGoogleAccount> = emptyList(),
    val gitHubAccounts: List<LinkedGitHubAccount> = emptyList(),
    val backendProfiles: List<LinkedBackendProfile> = emptyList(),
    val bundles: List<WorkspaceAccountBundle> = emptyList()
)

/**
 * Encrypted multi-account & cross-service bundle store backed by Android Keystore AES-GCM (SecretStorage).
 * Allows linking multiple Google, GitHub, AI Provider, and Backend Runner accounts together.
 */
class AccountLinkStore(private val storage: SecretStorage) {

    fun loadSnapshot(): LinkedAccountsSnapshot {
        val raw = storage.getSecret(SecretVault.KEY_LINKED_ACCOUNTS_JSON)
        if (raw.isBlank()) return LinkedAccountsSnapshot()
        return try {
            val root = JSONObject(raw)
            val googleList = parseArray(root.optJSONArray("googleAccounts")) { obj ->
                LinkedGoogleAccount(
                    uid = obj.optString("uid"),
                    email = obj.optString("email"),
                    displayName = obj.optString("displayName"),
                    linkedBundleId = obj.optString("linkedBundleId"),
                    lastUsedEpochMs = obj.optLong("lastUsedEpochMs", 0L)
                )
            }.filter { it.uid.isNotBlank() }

            val gitHubList = parseArray(root.optJSONArray("gitHubAccounts")) { obj ->
                LinkedGitHubAccount(
                    id = obj.optString("id"),
                    label = obj.optString("label"),
                    login = obj.optString("login"),
                    displayName = obj.optString("displayName"),
                    avatarUrl = obj.optString("avatarUrl"),
                    defaultRepo = obj.optString("defaultRepo"),
                    token = obj.optString("token"),
                    isActive = obj.optBoolean("isActive", false)
                )
            }.filter { it.id.isNotBlank() && it.token.isNotBlank() }

            val backendList = parseArray(root.optJSONArray("backendProfiles")) { obj ->
                LinkedBackendProfile(
                    id = obj.optString("id"),
                    label = obj.optString("label"),
                    aiProvider = obj.optString("aiProvider", "gemini"),
                    aiModel = obj.optString("aiModel", "gemini-3.8-flash"),
                    openAiKey = obj.optString("openAiKey"),
                    backendUrl = obj.optString("backendUrl"),
                    backendToken = obj.optString("backendToken"),
                    isActive = obj.optBoolean("isActive", false)
                )
            }.filter { it.id.isNotBlank() }

            val bundleList = parseArray(root.optJSONArray("bundles")) { obj ->
                WorkspaceAccountBundle(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    googleUid = obj.optString("googleUid"),
                    googleEmail = obj.optString("googleEmail"),
                    gitHubAccountId = obj.optString("gitHubAccountId"),
                    gitHubLogin = obj.optString("gitHubLogin"),
                    backendProfileId = obj.optString("backendProfileId"),
                    backendLabel = obj.optString("backendLabel"),
                    repoSlug = obj.optString("repoSlug"),
                    branch = obj.optString("branch", "main"),
                    isActive = obj.optBoolean("isActive", false)
                )
            }.filter { it.id.isNotBlank() }

            LinkedAccountsSnapshot(
                googleAccounts = googleList,
                gitHubAccounts = gitHubList,
                backendProfiles = backendList,
                bundles = bundleList
            )
        } catch (_: Exception) {
            LinkedAccountsSnapshot()
        }
    }

    private fun saveSnapshot(snapshot: LinkedAccountsSnapshot) {
        val root = JSONObject().apply {
            put("googleAccounts", JSONArray().apply {
                snapshot.googleAccounts.forEach { g ->
                    put(JSONObject().apply {
                        put("uid", g.uid)
                        put("email", g.email)
                        put("displayName", g.displayName)
                        put("linkedBundleId", g.linkedBundleId)
                        put("lastUsedEpochMs", g.lastUsedEpochMs)
                    })
                }
            })
            put("gitHubAccounts", JSONArray().apply {
                snapshot.gitHubAccounts.forEach { gh ->
                    put(JSONObject().apply {
                        put("id", gh.id)
                        put("label", gh.label)
                        put("login", gh.login)
                        put("displayName", gh.displayName)
                        put("avatarUrl", gh.avatarUrl)
                        put("defaultRepo", gh.defaultRepo)
                        put("token", gh.token)
                        put("isActive", gh.isActive)
                    })
                }
            })
            put("backendProfiles", JSONArray().apply {
                snapshot.backendProfiles.forEach { bp ->
                    put(JSONObject().apply {
                        put("id", bp.id)
                        put("label", bp.label)
                        put("aiProvider", bp.aiProvider)
                        put("aiModel", bp.aiModel)
                        put("openAiKey", bp.openAiKey)
                        put("backendUrl", bp.backendUrl)
                        put("backendToken", bp.backendToken)
                        put("isActive", bp.isActive)
                    })
                }
            })
            put("bundles", JSONArray().apply {
                snapshot.bundles.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("googleUid", b.googleUid)
                        put("googleEmail", b.googleEmail)
                        put("gitHubAccountId", b.gitHubAccountId)
                        put("gitHubLogin", b.gitHubLogin)
                        put("backendProfileId", b.backendProfileId)
                        put("backendLabel", b.backendLabel)
                        put("repoSlug", b.repoSlug)
                        put("branch", b.branch)
                        put("isActive", b.isActive)
                    })
                }
            })
        }
        storage.putSecret(SecretVault.KEY_LINKED_ACCOUNTS_JSON, root.toString())
    }

    fun recordGoogleAccount(
        uid: String,
        email: String,
        displayName: String,
        nowMs: Long = System.currentTimeMillis()
    ): LinkedAccountsSnapshot {
        if (uid.isBlank()) return loadSnapshot()
        val current = loadSnapshot()
        val existing = current.googleAccounts.find { it.uid == uid }
        val updatedEntry = LinkedGoogleAccount(
            uid = uid,
            email = email.ifBlank { existing?.email.orEmpty() },
            displayName = displayName.ifBlank { existing?.displayName ?: email.ifBlank { "مستخدم Google" } },
            linkedBundleId = existing?.linkedBundleId.orEmpty(),
            lastUsedEpochMs = nowMs
        )
        val merged = (current.googleAccounts.filterNot { it.uid == uid } + updatedEntry)
            .sortedByDescending { it.lastUsedEpochMs }
        val next = current.copy(googleAccounts = merged)
        saveSnapshot(next)
        return next
    }

    fun upsertGitHubAccount(
        label: String,
        login: String,
        displayName: String,
        avatarUrl: String,
        defaultRepo: String,
        token: String,
        setActive: Boolean = true
    ): LinkedGitHubAccount {
        val cleanToken = token.trim()
        val cleanLogin = login.trim()
        val current = loadSnapshot()
        val existing = current.gitHubAccounts.find {
            it.login.equals(cleanLogin, ignoreCase = true) && cleanLogin.isNotBlank()
        } ?: current.gitHubAccounts.find { it.token == cleanToken }

        val targetId = existing?.id ?: "gh_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val account = LinkedGitHubAccount(
            id = targetId,
            label = label.ifBlank { existing?.label ?: cleanLogin.ifBlank { "حساب GitHub" } },
            login = cleanLogin.ifBlank { existing?.login.orEmpty() },
            displayName = displayName.ifBlank { existing?.displayName.orEmpty() },
            avatarUrl = avatarUrl.ifBlank { existing?.avatarUrl.orEmpty() },
            defaultRepo = defaultRepo.trim().ifBlank { existing?.defaultRepo.orEmpty() },
            token = cleanToken,
            isActive = setActive || current.gitHubAccounts.none { it.isActive }
        )
        val updatedList = current.gitHubAccounts
            .filterNot { it.id == targetId }
            .map { if (account.isActive) it.copy(isActive = false) else it } + account

        if (account.isActive) {
            storage.putSecret(SecretVault.KEY_GITHUB_PAT, account.token)
        }
        saveSnapshot(current.copy(gitHubAccounts = updatedList))
        return account
    }

    fun activateGitHubAccount(accountId: String): LinkedGitHubAccount? {
        val current = loadSnapshot()
        val target = current.gitHubAccounts.find { it.id == accountId } ?: return null
        val updatedList = current.gitHubAccounts.map { it.copy(isActive = it.id == accountId) }
        storage.putSecret(SecretVault.KEY_GITHUB_PAT, target.token)
        saveSnapshot(current.copy(gitHubAccounts = updatedList))
        return target.copy(isActive = true)
    }

    fun removeGitHubAccount(accountId: String): LinkedAccountsSnapshot {
        val current = loadSnapshot()
        val remaining = current.gitHubAccounts.filterNot { it.id == accountId }
        val nextActive = remaining.firstOrNull { it.isActive } ?: remaining.firstOrNull()
        val normalized = if (nextActive != null) {
            storage.putSecret(SecretVault.KEY_GITHUB_PAT, nextActive.token)
            remaining.map { it.copy(isActive = it.id == nextActive.id) }
        } else {
            storage.putSecret(SecretVault.KEY_GITHUB_PAT, "")
            emptyList()
        }
        val cleanedBundles = current.bundles.filterNot { it.gitHubAccountId == accountId }
        val next = current.copy(gitHubAccounts = normalized, bundles = cleanedBundles)
        saveSnapshot(next)
        return next
    }

    fun findGitHubAccountForRepo(repoSlug: String): LinkedGitHubAccount? {
        val normalized = repoSlug.trim()
        if (normalized.isBlank()) return null
        return loadSnapshot().gitHubAccounts.firstOrNull {
            it.defaultRepo.equals(normalized, ignoreCase = true)
        }
    }

    fun upsertBackendProfile(
        label: String,
        aiProvider: String,
        aiModel: String,
        openAiKey: String,
        backendUrl: String,
        backendToken: String,
        setActive: Boolean = true
    ): LinkedBackendProfile {
        val current = loadSnapshot()
        val cleanUrl = backendUrl.trim()
        val cleanLabel = label.ifBlank {
            if (cleanUrl.isNotBlank()) "بيئة ${cleanUrl.removePrefix("https://").removePrefix("http://").take(24)}"
            else if (aiProvider == "openai") "OpenAI ($aiModel)" else "Gemini ($aiModel)"
        }
        val existing = current.backendProfiles.find {
            it.label.equals(cleanLabel, ignoreCase = true) ||
                (cleanUrl.isNotBlank() && it.backendUrl.equals(cleanUrl, ignoreCase = true))
        }
        val id = existing?.id ?: "be_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val resolvedOpenAiKey = openAiKey.trim().ifBlank { existing?.openAiKey ?: storage.getSecret(SecretVault.KEY_OPENAI_API) }
        val resolvedBackendToken = backendToken.trim().ifBlank { existing?.backendToken ?: storage.getSecret(SecretVault.KEY_BACKEND_TOKEN) }

        val profile = LinkedBackendProfile(
            id = id,
            label = cleanLabel,
            aiProvider = if (aiProvider == "openai") "openai" else "gemini",
            aiModel = aiModel.ifBlank { "gemini-3.8-flash" },
            openAiKey = resolvedOpenAiKey,
            backendUrl = cleanUrl,
            backendToken = resolvedBackendToken,
            isActive = setActive || current.backendProfiles.none { it.isActive }
        )
        val updated = current.backendProfiles
            .filterNot { it.id == id }
            .map { if (profile.isActive) it.copy(isActive = false) else it } + profile

        if (profile.isActive) {
            if (profile.openAiKey.isNotBlank()) storage.putSecret(SecretVault.KEY_OPENAI_API, profile.openAiKey)
            if (profile.backendToken.isNotBlank()) storage.putSecret(SecretVault.KEY_BACKEND_TOKEN, profile.backendToken)
        }
        saveSnapshot(current.copy(backendProfiles = updated))
        return profile
    }

    fun activateBackendProfile(profileId: String): LinkedBackendProfile? {
        val current = loadSnapshot()
        val target = current.backendProfiles.find { it.id == profileId } ?: return null
        val updated = current.backendProfiles.map { it.copy(isActive = it.id == profileId) }
        storage.putSecret(SecretVault.KEY_OPENAI_API, target.openAiKey)
        storage.putSecret(SecretVault.KEY_BACKEND_TOKEN, target.backendToken)
        saveSnapshot(current.copy(backendProfiles = updated))
        return target.copy(isActive = true)
    }

    fun removeBackendProfile(profileId: String): LinkedAccountsSnapshot {
        val current = loadSnapshot()
        val remaining = current.backendProfiles.filterNot { it.id == profileId }
        val nextActive = remaining.firstOrNull { it.isActive } ?: remaining.firstOrNull()
        val normalized = if (nextActive != null) {
            storage.putSecret(SecretVault.KEY_OPENAI_API, nextActive.openAiKey)
            storage.putSecret(SecretVault.KEY_BACKEND_TOKEN, nextActive.backendToken)
            remaining.map { it.copy(isActive = it.id == nextActive.id) }
        } else {
            emptyList()
        }
        val cleanedBundles = current.bundles.filterNot { it.backendProfileId == profileId }
        val next = current.copy(backendProfiles = normalized, bundles = cleanedBundles)
        saveSnapshot(next)
        return next
    }

    fun upsertBundle(
        name: String,
        googleUid: String,
        googleEmail: String,
        gitHubAccountId: String,
        gitHubLogin: String,
        backendProfileId: String,
        backendLabel: String,
        repoSlug: String,
        branch: String,
        setActive: Boolean = true
    ): WorkspaceAccountBundle {
        val current = loadSnapshot()
        val cleanName = name.trim().ifBlank { "حزمة ${repoSlug.substringAfter("/")}" }
        val id = current.bundles.find { it.name.equals(cleanName, ignoreCase = true) }?.id
            ?: "bnd_${UUID.randomUUID().toString().replace("-", "").take(10)}"

        val bundle = WorkspaceAccountBundle(
            id = id,
            name = cleanName,
            googleUid = googleUid,
            googleEmail = googleEmail,
            gitHubAccountId = gitHubAccountId,
            gitHubLogin = gitHubLogin,
            backendProfileId = backendProfileId,
            backendLabel = backendLabel,
            repoSlug = repoSlug.trim(),
            branch = branch.trim().ifBlank { "main" },
            isActive = setActive
        )

        val updatedBundles = current.bundles
            .filterNot { it.id == id }
            .map { if (setActive) it.copy(isActive = false) else it } + bundle

        val updatedGoogle = current.googleAccounts.map { g ->
            if (g.uid == googleUid && setActive) g.copy(linkedBundleId = id) else g
        }
        saveSnapshot(current.copy(googleAccounts = updatedGoogle, bundles = updatedBundles))
        if (setActive) {
            if (gitHubAccountId.isNotBlank()) activateGitHubAccount(gitHubAccountId)
            if (backendProfileId.isNotBlank()) activateBackendProfile(backendProfileId)
        }
        return bundle
    }

    fun activateBundle(bundleId: String): WorkspaceAccountBundle? {
        val current = loadSnapshot()
        val target = current.bundles.find { it.id == bundleId } ?: return null
        val updatedBundles = current.bundles.map { it.copy(isActive = it.id == bundleId) }
        saveSnapshot(current.copy(bundles = updatedBundles))
        if (target.gitHubAccountId.isNotBlank()) activateGitHubAccount(target.gitHubAccountId)
        if (target.backendProfileId.isNotBlank()) activateBackendProfile(target.backendProfileId)
        return target.copy(isActive = true)
    }

    fun removeBundle(bundleId: String): LinkedAccountsSnapshot {
        val current = loadSnapshot()
        val next = current.copy(bundles = current.bundles.filterNot { it.id == bundleId })
        saveSnapshot(next)
        return next
    }

    fun clearAll() {
        storage.clearAll()
    }

    private inline fun <T> parseArray(array: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(mapper(obj))
        }
        return result
    }
}
