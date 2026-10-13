package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_history")
data class BuildHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectName: String,
    val repoName: String,
    val commitSha: String? = null,
    val runId: Long? = null,
    val status: String, // QUEUED, IN_PROGRESS, SUCCESS, FAILURE, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val artifactDownloadUrl: String? = null,
    val localApkPath: String? = null,
    val apkSizeBytes: Long? = null,
    val failureLog: String? = null,
    val projectRootPathInZip: String? = null
)
