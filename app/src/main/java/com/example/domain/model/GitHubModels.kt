package com.example.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String = "",
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val private: Boolean = false,
    val language: String? = null,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBranchCommitRef(
    val sha: String = "",
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String = "",
    val commit: GitHubBranchCommitRef = GitHubBranchCommitRef(),
    val protected: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val sha: String = "",
    val truncated: Boolean = false,
    val tree: List<GitHubTreeItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubTreeItem(
    val path: String = "",
    val mode: String = "",
    val type: String = "", // "blob" or "tree"
    val sha: String = "",
    val size: Long? = null
)

@JsonClass(generateAdapter = true)
data class GitHubFileContent(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0L,
    val encoding: String? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBlobResponse(
    val sha: String = "",
    val size: Long = 0L,
    val encoding: String? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitAuthor(
    val name: String = "",
    val email: String = "",
    val date: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubCommitDetail(
    val message: String = "",
    val author: GitHubCommitAuthor = GitHubCommitAuthor()
)

@JsonClass(generateAdapter = true)
data class GitHubCommit(
    val sha: String = "",
    val commit: GitHubCommitDetail = GitHubCommitDetail(),
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubHeadRef(
    val ref: String = "",
    val sha: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubPullRequest(
    val id: Long = 0L,
    val number: Int = 0,
    val title: String = "",
    val state: String = "",
    val draft: Boolean = false,
    val body: String? = null,
    val user: GitHubUser = GitHubUser(),
    val head: GitHubHeadRef = GitHubHeadRef(),
    val base: GitHubHeadRef = GitHubHeadRef(),
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubPullFile(
    val filename: String = "",
    val status: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
    val patch: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRun(
    val id: Long = 0L,
    val name: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @Json(name = "run_number") val runNumber: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubCreateRefRequest(
    val ref: String,
    val sha: String
)

@JsonClass(generateAdapter = true)
data class GitHubPutFileRequest(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String
)

@JsonClass(generateAdapter = true)
data class GitHubCreatePrRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String,
    val draft: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubMergePrRequest(
    @Json(name = "commit_title") val commitTitle: String,
    @Json(name = "merge_method") val mergeMethod: String = "squash"
)

@JsonClass(generateAdapter = true)
data class GitHubMergePrResponse(
    val merged: Boolean = false,
    val message: String = "",
    val sha: String? = null
)
