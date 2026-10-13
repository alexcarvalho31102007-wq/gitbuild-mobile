package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "total_private_repos") val totalPrivateRepos: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val private: Boolean = true,
    @Json(name = "auto_init") val autoInit: Boolean = false,
    val description: String = "Repositório privado gerado por Compilador GitHub Android"
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean,
    @Json(name = "default_branch") val defaultBranch: String? = "main",
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefObject(
    val sha: String,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefResponse(
    val ref: String,
    @Json(name = "object") val targetObject: GitRefObject
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class TreeItem(
    val path: String,
    val mode: String = "100644", // 100644 for regular file, 100755 for executable (e.g. gradlew)
    val type: String = "blob",
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    @Json(name = "base_tree") val baseTree: String? = null,
    val tree: List<TreeItem>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
    val sha: String,
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    val ref: String,
    val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    val status: String, // queued, in_progress, completed
    val conclusion: String? = null, // success, failure, neutral, cancelled, timed_out, action_required
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowJobsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val jobs: List<WorkflowJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WorkflowJob(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    val steps: List<WorkflowStep>? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowStep(
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val number: Int
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val artifacts: List<ArtifactItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArtifactItem(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false
)
