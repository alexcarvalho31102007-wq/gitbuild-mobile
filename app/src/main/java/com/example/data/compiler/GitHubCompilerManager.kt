package com.example.data.compiler

import android.content.Context
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.BuildHistoryEntity
import com.example.data.remote.ArtifactItem
import com.example.data.remote.CreateBlobRequest
import com.example.data.remote.CreateCommitRequest
import com.example.data.remote.CreateRefRequest
import com.example.data.remote.CreateRepoRequest
import com.example.data.remote.CreateTreeRequest
import com.example.data.remote.GitHubApiService
import com.example.data.remote.TreeItem
import com.example.data.remote.UpdateRefRequest
import com.example.data.remote.WorkflowJob
import com.example.data.remote.WorkflowRun
import com.example.data.zip.ProjectValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

sealed class CompilerProgress {
    object Idle : CompilerProgress()
    data class Validating(val message: String) : CompilerProgress()
    data class PreparingRepo(val repoName: String) : CompilerProgress()
    data class UploadingFiles(val current: Int, val total: Int, val currentFile: String) : CompilerProgress()
    data class Committing(val message: String) : CompilerProgress()
    data class WaitingForWorkflow(val repoName: String) : CompilerProgress()
    data class Running(
        val runId: Long,
        val status: String,
        val currentStep: String?,
        val elapsedSeconds: Long,
        val htmlUrl: String?
    ) : CompilerProgress()
    data class DownloadingArtifact(val bytesDownloaded: Long, val totalBytes: Long) : CompilerProgress()
    data class Success(
        val historyId: Long,
        val runId: Long,
        val projectName: String,
        val apkFile: File?,
        val artifactSize: Long,
        val htmlUrl: String?
    ) : CompilerProgress()
    data class Failed(
        val historyId: Long?,
        val runId: Long?,
        val reason: String,
        val errorLogs: String?,
        val htmlUrl: String?
    ) : CompilerProgress()
}

class GitHubCompilerManager(
    private val context: Context,
    private val api: GitHubApiService,
    private val database: AppDatabase
) {
    private val historyDao = database.buildHistoryDao()

    companion object {
        const val WORKFLOW_PATH = ".github/workflows/android-release.yml"
        val WORKFLOW_CONTENT = """
name: Android Release Build

on:
  workflow_dispatch:
  push:
    branches: [ main, master ]

jobs:
  build-release:
    name: Compilar APK Release
    runs-on: ubuntu-latest
    steps:
      - name: Checkout do Código-Fonte
        uses: actions/checkout@v4

      - name: Configurar JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Configurar Android SDK
        uses: android-actions/setup-android@v3

      - name: Garantir Gradle Wrapper e Permissões
        run: |
          if [ ! -f "gradlew" ]; then
            echo "Gerando Gradle Wrapper..."
            gradle wrapper --gradle-version 9.3.1 || true
          fi
          # Auto-ajuste para projetos com AGP 9.x
          if grep -r -E "agp = \"9\.|com\.android\.application.*9\." . 2>/dev/null; then
            echo "AGP 9.x detectado. Garantindo Gradle 9.3.1 no wrapper..."
            if [ -f "gradle/wrapper/gradle-wrapper.properties" ]; then
              sed -i -E 's/gradle-[0-8]\.[0-9.]*-(bin|all)\.zip/gradle-9.3.1-bin.zip/g' gradle/wrapper/gradle-wrapper.properties
              sed -i -E 's/gradle-9\.[0-2](\.[0-9]+)?-(bin|all)\.zip/gradle-9.3.1-bin.zip/g' gradle/wrapper/gradle-wrapper.properties
            fi
          fi
          chmod +x gradlew

      - name: Compilar APK Release
        run: |
          set +e
          ./gradlew assembleRelease --stacktrace --no-daemon 2>&1 | tee build.log
          STATUS=${'$'}{PIPESTATUS[0]}
          if [ ${'$'}STATUS -ne 0 ]; then
            REQUIRED_GRADLE=${'$'}(grep -oE "Minimum supported Gradle version is [0-9.]+" build.log | awk '{print ${'$'}NF}' | head -n 1)
            if [ -n "${'$'}REQUIRED_GRADLE" ]; then
              echo ">>> AUTO-CORREÇÃO: Atualizando Gradle Wrapper para ${'$'}REQUIRED_GRADLE e recompilando..."
              sed -i -E "s/gradle-[0-9.]*-(bin|all)\.zip/gradle-${'$'}{REQUIRED_GRADLE}-bin.zip/g" gradle/wrapper/gradle-wrapper.properties
              ./gradlew assembleRelease --stacktrace --no-daemon
              STATUS=${'$'}?
            fi
          fi
          exit ${'$'}STATUS

      - name: Verificar e Assinar APK Release
        id: sign_step
        run: |
          set -e
          RELEASE_APK=$(find . -path "*/build/outputs/apk/release/*.apk" | head -n 1)
          if [ -z "${'$'}RELEASE_APK" ]; then
            echo "::error::Nenhum APK de release foi encontrado na pasta build/outputs/apk/release!"
            exit 1
          fi
          echo "APK detectado: ${'$'}RELEASE_APK"

          mkdir -p release-output

          if [[ "${'$'}RELEASE_APK" == *"-unsigned"* ]] || [[ "${'$'}RELEASE_APK" == *"-unaligned"* ]]; then
            echo "Assinando APK não assinado com chave de release gerada..."
            KEYSTORE_PATH="/tmp/release-build.keystore"
            keytool -genkey -v -keystore "${'$'}KEYSTORE_PATH" -alias release_key -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=CompiladorGitHub, OU=Build, O=Mobile, L=City, S=State, C=US"
            
            SIGNED_APK="/tmp/app-release-signed.apk"
            if command -v apksigner &> /dev/null; then
              cp "${'$'}RELEASE_APK" "${'$'}SIGNED_APK"
              apksigner sign --ks "${'$'}KEYSTORE_PATH" --ks-pass pass:android --key-pass pass:android "${'$'}SIGNED_APK"
            else
              jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore "${'$'}KEYSTORE_PATH" -storepass android -keypass android -signedjar "${'$'}SIGNED_APK" "${'$'}RELEASE_APK" release_key
            fi
            cp "${'$'}SIGNED_APK" release-output/app-release.apk
          else
            echo "APK já assinado pelo projeto."
            cp "${'$'}RELEASE_APK" release-output/app-release.apk
          fi

      - name: Validar Integridade do APK Final
        run: |
          if [ ! -f "release-output/app-release.apk" ]; then
            echo "::error::Falha crítica: o arquivo release-output/app-release.apk não existe!"
            exit 1
          fi
          echo "Sucesso! Tamanho do APK gerado:"
          ls -lh release-output/app-release.apk

      - name: Publicar Artefato APK Release
        uses: actions/upload-artifact@v4
        with:
          name: app-release-apk
          path: release-output/app-release.apk
          retention-days: 14
""".trimIndent()
    }

    suspend fun executeCompilation(
        validationResult: ProjectValidationResult,
        onProgress: (CompilerProgress) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        var currentHistoryId: Long = 0
        var currentRunId: Long? = null
        var repoName = "ghc-${validationResult.projectName.lowercase()}"

        try {
            onProgress(CompilerProgress.Validating("Verificando credenciais e usuário GitHub..."))
            val userResp = api.getUser()
            if (!userResp.isSuccessful || userResp.body() == null) {
                val err = "Falha de autenticação no GitHub: Código ${userResp.code()}. Verifique seu Personal Access Token."
                onProgress(CompilerProgress.Failed(null, null, err, null, null))
                return@withContext 0L
            }
            val owner = userResp.body()!!.login

            // 1. Prepare / Reuse Private Repository
            onProgress(CompilerProgress.PreparingRepo(repoName))
            var repoCheck = api.getRepo(owner, repoName)
            if (!repoCheck.isSuccessful) {
                // Create private repo
                val createResp = api.createRepo(
                    CreateRepoRequest(
                        name = repoName,
                        private = true,
                        description = "Repositório privado para compilação Android - ${validationResult.projectName}"
                    )
                )
                if (!createResp.isSuccessful) {
                    val errMsg = "Erro ao criar repositório privado '$repoName': ${createResp.errorBody()?.string() ?: createResp.message()}"
                    onProgress(CompilerProgress.Failed(null, null, errMsg, null, null))
                    return@withContext 0L
                }
            }

            // Create initial History Record
            val historyEntity = BuildHistoryEntity(
                projectName = validationResult.projectName,
                repoName = repoName,
                status = "QUEUED",
                projectRootPathInZip = validationResult.rootPrefix
            )
            currentHistoryId = historyDao.insert(historyEntity)

            // 2. Upload source files to GitHub via Git Blobs & Tree
            val filesToUpload = HashMap(validationResult.parsedFiles)
            // Add workflow file
            filesToUpload[WORKFLOW_PATH] = WORKFLOW_CONTENT.toByteArray(StandardCharsets.UTF_8)

            val totalFiles = filesToUpload.size
            val treeItems = mutableListOf<TreeItem>()
            var fileCounter = 0

            for ((relPath, contentBytes) in filesToUpload) {
                fileCounter++
                onProgress(CompilerProgress.UploadingFiles(fileCounter, totalFiles, relPath))

                val base64Content = Base64.encodeToString(contentBytes, Base64.NO_WRAP)
                val blobResp = api.createBlob(owner, repoName, CreateBlobRequest(content = base64Content, encoding = "base64"))
                if (!blobResp.isSuccessful || blobResp.body() == null) {
                    val err = "Falha ao enviar arquivo '$relPath' para o GitHub: ${blobResp.errorBody()?.string() ?: blobResp.message()}"
                    historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                    onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                    return@withContext currentHistoryId
                }
                val blobSha = blobResp.body()!!.sha
                val mode = if (relPath == "gradlew") "100755" else "100644"
                treeItems.add(TreeItem(path = relPath, mode = mode, type = "blob", sha = blobSha))
            }

            // 3. Create Tree & Commit
            onProgress(CompilerProgress.Committing("Criando árvore de arquivos Git..."))
            val treeResp = api.createTree(owner, repoName, CreateTreeRequest(tree = treeItems))
            if (!treeResp.isSuccessful || treeResp.body() == null) {
                val err = "Erro ao gerar árvore Git: ${treeResp.errorBody()?.string() ?: treeResp.message()}"
                historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                return@withContext currentHistoryId
            }
            val treeSha = treeResp.body()!!.sha

            // Check if main branch exists
            val refResp = api.getRef(owner, repoName, "heads/main")
            val parentShas = if (refResp.isSuccessful && refResp.body() != null) {
                listOf(refResp.body()!!.targetObject.sha)
            } else {
                emptyList()
            }

            onProgress(CompilerProgress.Committing("Criando commit de compilação release..."))
            val commitResp = api.createCommit(
                owner,
                repoName,
                CreateCommitRequest(
                    message = "Compilação Release Android: ${validationResult.projectName} [ci skip: false]",
                    tree = treeSha,
                    parents = parentShas
                )
            )
            if (!commitResp.isSuccessful || commitResp.body() == null) {
                val err = "Erro ao criar commit: ${commitResp.errorBody()?.string() ?: commitResp.message()}"
                historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                return@withContext currentHistoryId
            }
            val newCommitSha = commitResp.body()!!.sha

            // Update or Create branch ref
            if (parentShas.isNotEmpty()) {
                val upd = api.updateRef(owner, repoName, "heads/main", UpdateRefRequest(sha = newCommitSha, force = true))
                if (!upd.isSuccessful) {
                    val err = "Erro ao atualizar branch main: ${upd.errorBody()?.string() ?: upd.message()}"
                    historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                    onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                    return@withContext currentHistoryId
                }
            } else {
                val cr = api.createRef(owner, repoName, CreateRefRequest(ref = "refs/heads/main", sha = newCommitSha))
                if (!cr.isSuccessful) {
                    val err = "Erro ao criar branch main: ${cr.errorBody()?.string() ?: cr.message()}"
                    historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                    onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                    return@withContext currentHistoryId
                }
            }

            historyDao.update(historyEntity.copy(id = currentHistoryId, commitSha = newCommitSha, status = "QUEUED"))

            // 4. Wait for GitHub Actions Workflow to Start
            onProgress(CompilerProgress.WaitingForWorkflow(repoName))
            var targetRun: WorkflowRun? = null
            val startTimeMs = System.currentTimeMillis()

            // Wait up to 60 seconds for GitHub to trigger the push run
            for (i in 0 until 20) {
                delay(3000)
                val runsResp = api.getWorkflowRuns(owner, repoName, perPage = 5)
                if (runsResp.isSuccessful && runsResp.body() != null) {
                    val runs = runsResp.body()!!.workflowRuns
                    // Match run triggered around or after start time
                    val matched = runs.firstOrNull()
                    if (matched != null) {
                        targetRun = matched
                        currentRunId = matched.id
                        break
                    }
                }
            }

            if (targetRun == null) {
                val err = "O GitHub Actions não iniciou a compilação a tempo. Verifique se o GitHub Actions está habilitado nas configurações do repositório."
                historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                onProgress(CompilerProgress.Failed(currentHistoryId, null, err, null, null))
                return@withContext currentHistoryId
            }

            historyDao.update(historyEntity.copy(id = currentHistoryId, runId = targetRun.id, status = "IN_PROGRESS"))

            // 5. Poll Workflow Execution until Completed (Max 20 minutes)
            val maxWaitMs = 20 * 60 * 1000L
            var completedRun: WorkflowRun? = null

            while (System.currentTimeMillis() - startTimeMs < maxWaitMs) {
                delay(4000)
                val runResp = api.getWorkflowRun(owner, repoName, targetRun.id)
                if (!runResp.isSuccessful || runResp.body() == null) {
                    continue
                }
                val run = runResp.body()!!
                val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000

                // Get current job step
                var activeStepName: String? = null
                val jobsResp = api.getWorkflowJobs(owner, repoName, run.id)
                if (jobsResp.isSuccessful && jobsResp.body() != null) {
                    val job = jobsResp.body()!!.jobs.firstOrNull()
                    if (job != null) {
                        val activeStep = job.steps?.firstOrNull { it.status == "in_progress" }
                            ?: job.steps?.lastOrNull { it.status == "completed" }
                        activeStepName = activeStep?.name
                    }
                }

                onProgress(
                    CompilerProgress.Running(
                        runId = run.id,
                        status = run.status,
                        currentStep = activeStepName,
                        elapsedSeconds = elapsedSec,
                        htmlUrl = run.htmlUrl
                    )
                )

                if (run.status == "completed") {
                    completedRun = run
                    break
                }
            }

            if (completedRun == null) {
                val err = "Tempo limite de compilação excedido (20 minutos)."
                historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                onProgress(CompilerProgress.Failed(currentHistoryId, targetRun.id, err, null, targetRun.htmlUrl))
                return@withContext currentHistoryId
            }

            // 6. Inspect Completion Status
            if (completedRun.conclusion != "success") {
                // Compilation Failed! Fetch failure logs as mandated by rules
                val failureLogs = fetchFailureLogs(owner, repoName, completedRun.id)
                val conclusionDesc = when (completedRun.conclusion) {
                    "cancelled" -> "Compilação cancelada no GitHub Actions."
                    else -> "Falha na compilação do Gradle no GitHub Actions."
                }
                historyDao.update(
                    historyEntity.copy(
                        id = currentHistoryId,
                        runId = completedRun.id,
                        status = "FAILURE",
                        failureLog = failureLogs,
                        completedAt = System.currentTimeMillis()
                    )
                )
                onProgress(
                    CompilerProgress.Failed(
                        historyId = currentHistoryId,
                        runId = completedRun.id,
                        reason = conclusionDesc,
                        errorLogs = failureLogs,
                        htmlUrl = completedRun.htmlUrl
                    )
                )
                return@withContext currentHistoryId
            }

            // 7. Verify Artifact Existence - "Não declarar sucesso sem confirmar que o APK foi gerado corretamente"
            val artifactsResp = api.getArtifacts(owner, repoName, completedRun.id)
            if (!artifactsResp.isSuccessful || artifactsResp.body() == null || artifactsResp.body()!!.artifacts.isEmpty()) {
                val err = "O job concluiu mas nenhum artefato foi publicado pelo GitHub Actions."
                historyDao.update(historyEntity.copy(id = currentHistoryId, status = "FAILURE", failureLog = err, completedAt = System.currentTimeMillis()))
                onProgress(CompilerProgress.Failed(currentHistoryId, completedRun.id, err, null, completedRun.htmlUrl))
                return@withContext currentHistoryId
            }

            val apkArtifact = artifactsResp.body()!!.artifacts.firstOrNull { it.name == "app-release-apk" }
                ?: artifactsResp.body()!!.artifacts.first()

            historyDao.update(
                historyEntity.copy(
                    id = currentHistoryId,
                    runId = completedRun.id,
                    status = "SUCCESS",
                    artifactDownloadUrl = apkArtifact.archiveDownloadUrl,
                    apkSizeBytes = apkArtifact.sizeInBytes,
                    completedAt = System.currentTimeMillis()
                )
            )

            onProgress(
                CompilerProgress.Success(
                    historyId = currentHistoryId,
                    runId = completedRun.id,
                    projectName = validationResult.projectName,
                    apkFile = null,
                    artifactSize = apkArtifact.sizeInBytes,
                    htmlUrl = completedRun.htmlUrl
                )
            )

            return@withContext currentHistoryId
        } catch (e: Exception) {
            val err = "Erro inesperado durante o pipeline de compilação: ${e.message}"
            if (currentHistoryId > 0) {
                historyDao.update(
                    BuildHistoryEntity(
                        id = currentHistoryId,
                        projectName = validationResult.projectName,
                        repoName = repoName,
                        runId = currentRunId,
                        status = "FAILURE",
                        failureLog = err,
                        completedAt = System.currentTimeMillis()
                    )
                )
            }
            onProgress(CompilerProgress.Failed(currentHistoryId.takeIf { it > 0 }, currentRunId, err, null, null))
            return@withContext currentHistoryId
        }
    }

    private suspend fun fetchFailureLogs(owner: String, repo: String, runId: Long): String {
        return try {
            val jobsResp = api.getWorkflowJobs(owner, repo, runId)
            if (jobsResp.isSuccessful && jobsResp.body() != null) {
                val failedJob = jobsResp.body()!!.jobs.firstOrNull { it.conclusion == "failure" }
                    ?: jobsResp.body()!!.jobs.firstOrNull()

                if (failedJob != null) {
                    val logStream = api.downloadJobLogs(owner, repo, failedJob.id)
                    if (logStream.isSuccessful && logStream.body() != null) {
                        val fullLog = logStream.body()!!.string()
                        return extractRelevantErrorTrace(fullLog)
                    }
                }
            }
            "Logs não puderam ser recuperados automaticamente da API de jobs do GitHub."
        } catch (e: Exception) {
            "Erro ao carregar logs de falha: ${e.message}"
        }
    }

    private fun extractRelevantErrorTrace(fullLog: String): String {
        val lines = fullLog.lines()
        // If the log is very long, find the failing step and compile errors
        val failureIndices = lines.mapIndexedNotNull { index, line ->
            if (line.contains("FAILURE: Build failed") || line.contains("ERROR:") || line.contains("::error::") || line.contains("Execution failed for task")) {
                index
            } else null
        }

        return if (failureIndices.isNotEmpty()) {
            val start = (failureIndices.first() - 10).coerceAtLeast(0)
            val end = (failureIndices.last() + 60).coerceAtMost(lines.size)
            lines.subList(start, end).joinToString("\n")
        } else {
            // Return last 80 lines of the job log
            lines.takeLast(80).joinToString("\n")
        }
    }

    suspend fun downloadAndExtractApk(
        historyId: Long,
        archiveUrl: String,
        projectName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val resp = api.downloadArtifact(archiveUrl)
            if (!resp.isSuccessful || resp.body() == null) {
                return@withContext null
            }

            val body = resp.body()!!
            val totalBytes = body.contentLength()
            val byteStream = body.byteStream()

            // The artifact download from GitHub Actions is a ZIP containing app-release.apk
            val releasesDir = File(context.getExternalFilesDir(null), "releases")
            if (!releasesDir.exists()) releasesDir.mkdirs()

            val outputFile = File(releasesDir, "${projectName}-release.apk")
            var downloadedBytes = 0L

            ZipInputStream(byteStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".apk")) {
                        FileOutputStream(outputFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                downloadedBytes += read
                                onProgress(downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)
                            }
                        }
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                // Update history entity
                val current = historyDao.getById(historyId)
                if (current != null) {
                    historyDao.update(
                        current.copy(
                            localApkPath = outputFile.absolutePath,
                            apkSizeBytes = outputFile.length()
                        )
                    )
                }
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
