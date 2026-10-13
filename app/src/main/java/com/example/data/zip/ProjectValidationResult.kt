package com.example.data.zip

data class ProjectValidationResult(
    val isValid: Boolean,
    val projectName: String,
    val rootPrefix: String, // Prefix inside the ZIP (e.g. "" or "my-app-master/")
    val totalFiles: Int,
    val totalSizeUncompressed: Long,
    val hasSettingsGradle: Boolean,
    val hasRootBuildGradle: Boolean,
    val hasAppModule: Boolean,
    val hasAndroidManifest: Boolean,
    val hasGradleWrapper: Boolean,
    val detectedGradleVersion: String? = null,
    val detectedCompileSdk: String? = null,
    val detectedMinSdk: String? = null,
    val detectedTargetSdk: String? = null,
    val detectedPackageName: String? = null,
    val hasSigningConfig: Boolean = false,
    val validationIssues: List<String> = emptyList(),
    val validationWarnings: List<String> = emptyList(),
    val parsedFiles: Map<String, ByteArray> = emptyMap()
)
