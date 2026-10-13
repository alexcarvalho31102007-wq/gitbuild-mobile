package com.example.data.zip

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipProjectInspector {

    private val IGNORED_PREFIXES = listOf(
        "__MACOSX/",
        ".idea/",
        ".gradle/",
        ".git/"
    )

    private val IGNORED_PATH_CONTAINS = listOf(
        "/build/",
        "/build_cache/",
        "/.gradle/",
        "/.idea/",
        "/.git/"
    )

    private val IGNORED_FILENAMES = setOf(
        ".ds_store",
        "thumbs.db",
        "local.properties"
    )

    suspend fun inspectZip(
        context: Context,
        zipUri: Uri,
        fileNameFallback: String = "AndroidProject"
    ): ProjectValidationResult = withContext(Dispatchers.IO) {
        val rawEntries = mutableMapOf<String, ByteArray>()
        var totalUncompressedBytes = 0L

        val rawStream = context.contentResolver.openInputStream(zipUri)
            ?: return@withContext ProjectValidationResult(
                isValid = false,
                projectName = "Desconhecido",
                rootPrefix = "",
                totalFiles = 0,
                totalSizeUncompressed = 0,
                hasSettingsGradle = false,
                hasRootBuildGradle = false,
                hasAppModule = false,
                hasAndroidManifest = false,
                hasGradleWrapper = false,
                validationIssues = listOf("Não foi possível abrir o arquivo ZIP selecionado.")
            )

        try {
            ZipInputStream(rawStream.buffered()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.replace('\\', '/')
                        val lower = name.lowercase()

                        val isIgnored = IGNORED_PREFIXES.any { lower.startsWith(it) } ||
                                IGNORED_PATH_CONTAINS.any { lower.contains(it) } ||
                                IGNORED_FILENAMES.contains(lower.substringAfterLast('/')) ||
                                lower.endsWith(".class") ||
                                lower.endsWith(".dex") ||
                                lower.endsWith(".apk") ||
                                lower.endsWith(".aab")

                        if (!isIgnored) {
                            val buffer = ByteArrayOutputStream()
                            val data = ByteArray(8192)
                            var read: Int
                            while (zis.read(data).also { read = it } != -1) {
                                buffer.write(data, 0, read)
                            }
                            val bytes = buffer.toByteArray()
                            rawEntries[name] = bytes
                            totalUncompressedBytes += bytes.size
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            return@withContext ProjectValidationResult(
                isValid = false,
                projectName = "Erro",
                rootPrefix = "",
                totalFiles = 0,
                totalSizeUncompressed = 0,
                hasSettingsGradle = false,
                hasRootBuildGradle = false,
                hasAppModule = false,
                hasAndroidManifest = false,
                hasGradleWrapper = false,
                validationIssues = listOf("Falha ao descompactar ou ler o arquivo ZIP: ${e.message}")
            )
        }

        if (rawEntries.isEmpty()) {
            return@withContext ProjectValidationResult(
                isValid = false,
                projectName = "Vazio",
                rootPrefix = "",
                totalFiles = 0,
                totalSizeUncompressed = 0,
                hasSettingsGradle = false,
                hasRootBuildGradle = false,
                hasAppModule = false,
                hasAndroidManifest = false,
                hasGradleWrapper = false,
                validationIssues = listOf("O arquivo ZIP está vazio ou não contém arquivos de código válidos.")
            )
        }

        // 1. Detect root directory
        val rootPrefix = findRootPrefix(rawEntries.keys)

        // Normalize paths relative to rootPrefix
        val normalizedFiles = mutableMapOf<String, ByteArray>()
        for ((path, bytes) in rawEntries) {
            if (path.startsWith(rootPrefix)) {
                val relPath = path.substring(rootPrefix.length).trimStart('/')
                if (relPath.isNotBlank()) {
                    normalizedFiles[relPath] = bytes
                }
            }
        }

        // 2. Validate essential Android Gradle files
        val hasSettingsGradle = normalizedFiles.keys.any {
            it == "settings.gradle" || it == "settings.gradle.kts"
        }

        val hasRootBuildGradle = normalizedFiles.keys.any {
            it == "build.gradle" || it == "build.gradle.kts"
        }

        val hasAndroidManifest = normalizedFiles.keys.any {
            it.endsWith("AndroidManifest.xml")
        }

        val hasAppModule = normalizedFiles.keys.any {
            it == "app/build.gradle" || it == "app/build.gradle.kts" ||
                    (it.endsWith("build.gradle") || it.endsWith("build.gradle.kts")) && it != "build.gradle" && it != "build.gradle.kts"
        }

        val hasGradleWrapper = normalizedFiles.keys.any {
            it == "gradle/wrapper/gradle-wrapper.properties"
        }

        // 3. Extract metadata
        var detectedProjectName: String? = null
        val settingsBytes = normalizedFiles["settings.gradle.kts"] ?: normalizedFiles["settings.gradle"]
        if (settingsBytes != null) {
            val content = String(settingsBytes, StandardCharsets.UTF_8)
            val matcher = Pattern.compile("rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(content)
            if (matcher.find()) {
                detectedProjectName = matcher.group(1)?.trim()
            }
        }

        if (detectedProjectName.isNullOrBlank()) {
            // Try fallback from rootPrefix or filename
            val cleanPrefix = rootPrefix.trimEnd('/').substringAfterLast('/')
            detectedProjectName = if (cleanPrefix.isNotBlank()) cleanPrefix else fileNameFallback.substringBeforeLast('.')
        }

        // Clean project name for repo naming
        val sanitizedProjectName = detectedProjectName.replace(Regex("[^a-zA-Z0-9._-]"), "-")

        // Parse Gradle wrapper version
        var detectedGradleVersion: String? = null
        val wrapperKey = "gradle/wrapper/gradle-wrapper.properties"
        val wrapperBytes = normalizedFiles[wrapperKey]
        if (wrapperBytes != null) {
            val content = String(wrapperBytes, StandardCharsets.UTF_8)
            val matcher = Pattern.compile("gradle-([0-9.]+)-(all|bin)\\.zip").matcher(content)
            if (matcher.find()) {
                detectedGradleVersion = matcher.group(1)
            }
        }

        // Check if project uses AGP 9.x
        val isAgp9OrNewer = normalizedFiles.values.any { bytes ->
            val text = String(bytes, StandardCharsets.UTF_8)
            text.contains("agp = \"9.") ||
            text.contains("\"com.android.application\"") && text.contains("9.") ||
            text.contains("com.android.tools.build:gradle:9.")
        }

        var autoUpgradedGradle = false
        if (isAgp9OrNewer) {
            val needsUpgrade = detectedGradleVersion == null ||
                    detectedGradleVersion.startsWith("7.") ||
                    detectedGradleVersion.startsWith("8.") ||
                    detectedGradleVersion.startsWith("9.0") ||
                    detectedGradleVersion.startsWith("9.1") ||
                    detectedGradleVersion.startsWith("9.2")

            if (needsUpgrade) {
                val newWrapperContent = """
                    distributionBase=GRADLE_USER_HOME
                    distributionPath=wrapper/dists
                    distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
                    zipStoreBase=GRADLE_USER_HOME
                    zipStorePath=wrapper/dists
                """.trimIndent()
                normalizedFiles[wrapperKey] = newWrapperContent.toByteArray(StandardCharsets.UTF_8)
                detectedGradleVersion = "9.3.1"
                autoUpgradedGradle = true
            }
        }

        // Parse build.gradle / build.gradle.kts for SDK & signing
        var detectedCompileSdk: String? = null
        var detectedMinSdk: String? = null
        var detectedTargetSdk: String? = null
        var detectedPackageName: String? = null
        var hasSigningConfig = false

        // Check build scripts
        for ((path, bytes) in normalizedFiles) {
            if (path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")) {
                val content = String(bytes, StandardCharsets.UTF_8)
                if (content.contains("signingConfigs") || content.contains("signingConfig")) {
                    hasSigningConfig = true
                }
                if (detectedCompileSdk == null) {
                    val m = Pattern.compile("(compileSdk|compileSdkVersion)\\s*=?\\s*([0-9]+)").matcher(content)
                    if (m.find()) detectedCompileSdk = m.group(2)
                }
                if (detectedMinSdk == null) {
                    val m = Pattern.compile("(minSdk|minSdkVersion)\\s*=?\\s*([0-9]+)").matcher(content)
                    if (m.find()) detectedMinSdk = m.group(2)
                }
                if (detectedTargetSdk == null) {
                    val m = Pattern.compile("(targetSdk|targetSdkVersion)\\s*=?\\s*([0-9]+)").matcher(content)
                    if (m.find()) detectedTargetSdk = m.group(2)
                }
                if (detectedPackageName == null) {
                    val m = Pattern.compile("(applicationId|namespace)\\s*=?\\s*['\"]([^'\"]+)['\"]").matcher(content)
                    if (m.find()) detectedPackageName = m.group(2)
                }
            }
        }

        // Check AndroidManifest if package name not found yet
        if (detectedPackageName == null) {
            for ((path, bytes) in normalizedFiles) {
                if (path.endsWith("AndroidManifest.xml")) {
                    val content = String(bytes, StandardCharsets.UTF_8)
                    val m = Pattern.compile("package\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(content)
                    if (m.find()) {
                        detectedPackageName = m.group(1)
                        break
                    }
                }
            }
        }

        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!hasSettingsGradle && !hasRootBuildGradle) {
            issues.add("Nenhum arquivo Gradle de raiz encontrado (settings.gradle ou build.gradle). Certifique-se de enviar a pasta do projeto Android.")
        }

        if (!hasAndroidManifest) {
            issues.add("Nenhum AndroidManifest.xml foi encontrado no ZIP.")
        }

        if (!hasAppModule && !hasRootBuildGradle) {
            issues.add("Nenhum módulo de aplicação Android detectado.")
        }

        if (!hasGradleWrapper) {
            warnings.add("Gradle Wrapper ausente no ZIP. O workflow do GitHub Actions gerará e usará o wrapper automaticamente.")
        }

        if (!hasSigningConfig) {
            warnings.add("Nenhuma configuração nativa de assinatura release detectada. O GitHub Actions assinará automaticamente o APK gerado com uma chave de release para permitir instalação imediata.")
        }

        if (autoUpgradedGradle) {
            warnings.add("Gradle Wrapper atualizado automaticamente para 9.3.1 para garantir compatibilidade com AGP 9.x.")
        }

        val isValid = issues.isEmpty()

        ProjectValidationResult(
            isValid = isValid,
            projectName = sanitizedProjectName,
            rootPrefix = rootPrefix,
            totalFiles = normalizedFiles.size,
            totalSizeUncompressed = totalUncompressedBytes,
            hasSettingsGradle = hasSettingsGradle,
            hasRootBuildGradle = hasRootBuildGradle,
            hasAppModule = hasAppModule,
            hasAndroidManifest = hasAndroidManifest,
            hasGradleWrapper = hasGradleWrapper,
            detectedGradleVersion = detectedGradleVersion,
            detectedCompileSdk = detectedCompileSdk,
            detectedMinSdk = detectedMinSdk,
            detectedTargetSdk = detectedTargetSdk,
            detectedPackageName = detectedPackageName,
            hasSigningConfig = hasSigningConfig,
            validationIssues = issues,
            validationWarnings = warnings,
            parsedFiles = normalizedFiles
        )
    }

    private fun findRootPrefix(paths: Set<String>): String {
        // Find if settings.gradle or settings.gradle.kts exists
        for (path in paths) {
            if (path.endsWith("settings.gradle") || path.endsWith("settings.gradle.kts")) {
                val idx = path.lastIndexOf('/')
                if (idx >= 0) {
                    return path.substring(0, idx + 1)
                }
                return ""
            }
        }
        // Fallback: look for root build.gradle
        for (path in paths) {
            if (path.endsWith("build.gradle") || path.endsWith("build.gradle.kts")) {
                val idx = path.lastIndexOf('/')
                if (idx >= 0) {
                    val prefix = path.substring(0, idx + 1)
                    // check if app module exists under this prefix
                    if (paths.any { it.startsWith(prefix) && it.contains("AndroidManifest.xml") }) {
                        return prefix
                    }
                }
            }
        }
        return ""
    }
}
