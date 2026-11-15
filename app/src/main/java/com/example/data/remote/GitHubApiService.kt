package com.example.data.remote

import com.example.domain.model.GitHubBlobResponse
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
import com.example.domain.model.GitHubTreeResponse
import com.example.domain.model.GitHubUser
import com.example.domain.model.GitHubWorkflowRunsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): GitHubUser

    @GET("user/repos")
    suspend fun listUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String = "updated"
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 50
    ): List<GitHubBranch>

    @GET("repos/{owner}/{repo}/git/trees/{treeSha}")
    suspend fun getTree(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("treeSha") treeSha: String,
        @Query("recursive") recursive: Int = 1
    ): GitHubTreeResponse

    @GET("repos/{owner}/{repo}/contents/{filePath}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("filePath", encoded = true) filePath: String,
        @Query("ref") ref: String
    ): GitHubFileContent

    @GET("repos/{owner}/{repo}/git/blobs/{fileSha}")
    suspend fun getGitBlob(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("fileSha") fileSha: String
    ): GitHubBlobResponse

    @GET("repos/{owner}/{repo}/commits")
    suspend fun listCommits(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String,
        @Query("per_page") perPage: Int = 20
    ): List<GitHubCommit>

    @GET("repos/{owner}/{repo}/pulls")
    suspend fun listPullRequests(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 20
    ): List<GitHubPullRequest>

    @GET("repos/{owner}/{repo}/pulls/{pullNumber}/files")
    suspend fun listPullFiles(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pullNumber") pullNumber: Int
    ): List<GitHubPullFile>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): GitHubWorkflowRunsResponse

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createBranchRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitHubCreateRefRequest
    ): Any

    @PUT("repos/{owner}/{repo}/contents/{filePath}")
    suspend fun putFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("filePath", encoded = true) filePath: String,
        @Body body: GitHubPutFileRequest
    ): Any

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitHubCreatePrRequest
    ): GitHubPullRequest

    @PUT("repos/{owner}/{repo}/pulls/{pullNumber}/merge")
    suspend fun mergePullRequest(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pullNumber") pullNumber: Int,
        @Body body: GitHubMergePrRequest
    ): GitHubMergePrResponse
}
