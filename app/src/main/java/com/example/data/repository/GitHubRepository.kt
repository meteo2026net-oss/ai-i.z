package com.example.data.repository

import android.util.Base64
import com.example.core.security.SecretVault
import com.example.data.remote.GitHubApiService
import com.example.domain.model.GitHubBranch
import com.example.domain.model.GitHubCommit
import com.example.domain.model.GitHubCreatePrRequest
import com.example.domain.model.GitHubCreateRefRequest
import com.example.domain.model.GitHubFileContent
import com.example.domain.model.GitHubMergePrRequest
import com.example.domain.model.GitHubMergePrResponse
import com.example.domain.model.GitHubPullFile
import com.example.domain.model.GitHubPullRequest
import com.example.domain.model.GitHubPutFileRequest
import com.example.domain.model.GitHubRepo
import com.example.domain.model.GitHubTreeItem
import com.example.domain.model.GitHubUser
import com.example.domain.model.GitHubWorkflowRun
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GitHubRepository(private val vault: SecretVault) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    fun hasToken(): Boolean = vault.hasSecret(SecretVault.KEY_GITHUB_PAT)

    private fun authHeader(): String {
        val pat = vault.getSecret(SecretVault.KEY_GITHUB_PAT)
        require(pat.isNotBlank()) {
            "لم يتم ربط رمز GitHub الشخصي (PAT) بعد. يُرجى إدخاله في شاشة ربط GitHub."
        }
        return "Bearer $pat"
    }

    private fun splitSlug(repoSlug: String): Pair<String, String> {
        val parts = repoSlug.trim().split("/")
        require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            "صيغة المستودع غير صالحة: $repoSlug"
        }
        return parts[0] to parts[1]
    }

    suspend fun verifyAndFetchUser(patOverride: String? = null): Result<GitHubUser> = runCatching {
        val header = if (!patOverride.isNullOrBlank()) "Bearer ${patOverride.trim()}" else authHeader()
        api.getAuthenticatedUser(header)
    }.rethrowCancellation()

    suspend fun listRepos(): Result<List<GitHubRepo>> = runCatching {
        api.listUserRepos(authHeader())
    }.rethrowCancellation()

    suspend fun getRepoInfo(repoSlug: String): Result<GitHubRepo> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.getRepository(authHeader(), owner, repo)
    }.rethrowCancellation()

    suspend fun listBranches(repoSlug: String): Result<List<GitHubBranch>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.listBranches(authHeader(), owner, repo)
    }.rethrowCancellation()

    suspend fun getTree(repoSlug: String, branchOrSha: String): Result<List<GitHubTreeItem>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.getTree(authHeader(), owner, repo, branchOrSha).tree
    }.rethrowCancellation()

    suspend fun getFileDecoded(
        repoSlug: String,
        filePath: String,
        branch: String
    ): Result<Pair<GitHubFileContent, String>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        val file = api.getFileContent(authHeader(), owner, repo, filePath, branch)
        val decoded = if (!file.content.isNullOrBlank()) {
            val cleaned = file.content.replace("\n", "")
            String(Base64.decode(cleaned, Base64.DEFAULT), Charsets.UTF_8)
        } else {
            ""
        }
        file to decoded
    }.rethrowCancellation()

    suspend fun downloadRawFileBytes(
        repoSlug: String,
        filePath: String,
        branch: String
    ): Result<ByteArray> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        val file = api.getFileContent(authHeader(), owner, repo, filePath, branch)
        val inlineContent = file.content?.replace("\n", "")
        if (!inlineContent.isNullOrBlank()) {
            Base64.decode(inlineContent, Base64.DEFAULT)
        } else if (file.sha.isNotBlank()) {
            val blob = api.getGitBlob(authHeader(), owner, repo, file.sha)
            val blobContent = blob.content?.replace("\n", "")
                ?: throw IllegalStateException("تعذر تنزيل محتوى الملف من GitHub.")
            Base64.decode(blobContent, Base64.DEFAULT)
        } else {
            ByteArray(0)
        }
    }.rethrowCancellation()

    suspend fun listCommits(repoSlug: String, branch: String): Result<List<GitHubCommit>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.listCommits(authHeader(), owner, repo, branch)
    }.rethrowCancellation()

    suspend fun listPullRequests(repoSlug: String): Result<List<GitHubPullRequest>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.listPullRequests(authHeader(), owner, repo)
    }.rethrowCancellation()

    suspend fun listPullFiles(repoSlug: String, pullNumber: Int): Result<List<GitHubPullFile>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.listPullFiles(authHeader(), owner, repo, pullNumber)
    }.rethrowCancellation()

    suspend fun listWorkflowRuns(repoSlug: String): Result<List<GitHubWorkflowRun>> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.listWorkflowRuns(authHeader(), owner, repo).workflowRuns
    }.rethrowCancellation()

    suspend fun createWorkingBranch(
        repoSlug: String,
        newBranchName: String,
        fromSha: String
    ): Result<Unit> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.createBranchRef(
            authHeader(),
            owner,
            repo,
            GitHubCreateRefRequest(ref = "refs/heads/${newBranchName.trim()}", sha = fromSha)
        )
        Unit
    }.rethrowCancellation()

    suspend fun commitFileEdit(
        repoSlug: String,
        filePath: String,
        newContent: String,
        commitMessage: String,
        branch: String,
        existingSha: String?
    ): Result<Unit> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        val encoded = Base64.encodeToString(newContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        api.putFileContent(
            authHeader(),
            owner,
            repo,
            filePath,
            GitHubPutFileRequest(
                message = commitMessage,
                content = encoded,
                sha = existingSha,
                branch = branch
            )
        )
        Unit
    }.rethrowCancellation()

    suspend fun createDraftPullRequest(
        repoSlug: String,
        title: String,
        headBranch: String,
        baseBranch: String,
        body: String
    ): Result<GitHubPullRequest> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.createPullRequest(
            authHeader(),
            owner,
            repo,
            GitHubCreatePrRequest(
                title = title,
                head = headBranch,
                base = baseBranch,
                body = body,
                draft = true
            )
        )
    }.rethrowCancellation()

    suspend fun mergePullRequest(
        repoSlug: String,
        pullNumber: Int,
        commitTitle: String
    ): Result<GitHubMergePrResponse> = runCatching {
        val (owner, repo) = splitSlug(repoSlug)
        api.mergePullRequest(
            authHeader(),
            owner,
            repo,
            pullNumber,
            GitHubMergePrRequest(commitTitle = commitTitle)
        )
    }.rethrowCancellation()

    private fun <T> Result<T>.rethrowCancellation(): Result<T> {
        val ex = exceptionOrNull()
        if (ex is CancellationException) throw ex
        return this
    }
}
