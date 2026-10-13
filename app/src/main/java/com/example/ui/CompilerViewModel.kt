package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.compiler.CompilerProgress
import com.example.data.compiler.GitHubCompilerManager
import com.example.data.local.AppDatabase
import com.example.data.local.BuildHistoryEntity
import com.example.data.local.TokenStorage
import com.example.data.remote.GitHubClient
import com.example.data.zip.ProjectValidationResult
import com.example.data.zip.ZipProjectInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class TokenUiState(
    val hasToken: Boolean = false,
    val maskedToken: String = "",
    val username: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val isValidating: Boolean = false,
    val errorMessage: String? = null
)

class CompilerViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val database = AppDatabase.getInstance(application)
    private val historyDao = database.buildHistoryDao()

    private val _tokenState = MutableStateFlow(
        TokenUiState(
            hasToken = tokenStorage.hasToken(),
            maskedToken = tokenStorage.getMaskedToken(),
            username = tokenStorage.getUsername(),
            name = tokenStorage.getUserName(),
            avatarUrl = tokenStorage.getAvatarUrl()
        )
    )
    val tokenState: StateFlow<TokenUiState> = _tokenState.asStateFlow()

    private val _selectedZipUri = MutableStateFlow<Uri?>(null)
    val selectedZipUri: StateFlow<Uri?> = _selectedZipUri.asStateFlow()

    private val _isAnalyzingZip = MutableStateFlow(false)
    val isAnalyzingZip: StateFlow<Boolean> = _isAnalyzingZip.asStateFlow()

    private val _validationResult = MutableStateFlow<ProjectValidationResult?>(null)
    val validationResult: StateFlow<ProjectValidationResult?> = _validationResult.asStateFlow()

    private val _compilerProgress = MutableStateFlow<CompilerProgress>(CompilerProgress.Idle)
    val compilerProgress: StateFlow<CompilerProgress> = _compilerProgress.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Compilador, 1: Histórico, 2: Regras
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _isDownloadingApk = MutableStateFlow(false)
    val isDownloadingApk: StateFlow<Boolean> = _isDownloadingApk.asStateFlow()

    val historyList: StateFlow<List<BuildHistoryEntity>> = historyDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var compilerManager: GitHubCompilerManager = createCompilerManager()

    private fun createCompilerManager(): GitHubCompilerManager {
        val api = GitHubClient.create { tokenStorage.getToken() }
        return GitHubCompilerManager(getApplication(), api, database)
    }

    init {
        // If token exists, refresh user profile silently
        if (tokenStorage.hasToken()) {
            validateTokenSilent()
        }
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun saveToken(rawToken: String) {
        val token = rawToken.trim()
        if (token.isBlank()) {
            _tokenState.value = _tokenState.value.copy(errorMessage = "O token não pode estar vazio.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _tokenState.value = _tokenState.value.copy(isValidating = true, errorMessage = null)
            try {
                val tempApi = GitHubClient.create { token }
                val response = tempApi.getUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    tokenStorage.saveToken(token)
                    tokenStorage.saveUserProfile(user.login, user.name, user.avatarUrl)
                    compilerManager = createCompilerManager()

                    _tokenState.value = TokenUiState(
                        hasToken = true,
                        maskedToken = tokenStorage.getMaskedToken(),
                        username = user.login,
                        name = user.name,
                        avatarUrl = user.avatarUrl,
                        isValidating = false,
                        errorMessage = null
                    )
                } else {
                    val err = when (response.code()) {
                        401 -> "Token inválido ou expirado (HTTP 401 Unauthorized)."
                        403 -> "Acesso negado. Verifique os escopos do token (necessário escopo 'repo')."
                        else -> "Erro ao validar token: Código HTTP ${response.code()}"
                    }
                    _tokenState.value = _tokenState.value.copy(isValidating = false, errorMessage = err)
                }
            } catch (e: Exception) {
                _tokenState.value = _tokenState.value.copy(
                    isValidating = false,
                    errorMessage = "Erro de conexão ao validar token: ${e.message}"
                )
            }
        }
    }

    private fun validateTokenSilent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = GitHubClient.create { tokenStorage.getToken() }
                val response = api.getUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    tokenStorage.saveUserProfile(user.login, user.name, user.avatarUrl)
                    _tokenState.value = _tokenState.value.copy(
                        username = user.login,
                        name = user.name,
                        avatarUrl = user.avatarUrl
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun clearToken() {
        tokenStorage.clear()
        compilerManager = createCompilerManager()
        _tokenState.value = TokenUiState()
        _validationResult.value = null
        _selectedZipUri.value = null
        _compilerProgress.value = CompilerProgress.Idle
    }

    fun selectZip(uri: Uri, fallbackName: String = "ProjetoAndroid") {
        _selectedZipUri.value = uri
        _isAnalyzingZip.value = true
        _validationResult.value = null
        _compilerProgress.value = CompilerProgress.Idle

        viewModelScope.launch {
            val result = ZipProjectInspector.inspectZip(getApplication(), uri, fallbackName)
            _validationResult.value = result
            _isAnalyzingZip.value = false
        }
    }

    fun clearSelectedZip() {
        _selectedZipUri.value = null
        _validationResult.value = null
        _compilerProgress.value = CompilerProgress.Idle
    }

    fun startCompilation() {
        val validation = _validationResult.value ?: return
        if (!validation.isValid) return

        // Prevent duplicate concurrent builds: "Executar apenas uma compilação por solicitação, sem jobs duplicados."
        val currentProgress = _compilerProgress.value
        if (currentProgress is CompilerProgress.PreparingRepo ||
            currentProgress is CompilerProgress.UploadingFiles ||
            currentProgress is CompilerProgress.Committing ||
            currentProgress is CompilerProgress.WaitingForWorkflow ||
            currentProgress is CompilerProgress.Running
        ) {
            return
        }

        viewModelScope.launch {
            compilerManager.executeCompilation(validation) { progress ->
                _compilerProgress.value = progress
            }
        }
    }

    fun downloadAndInstallApk(
        historyItem: BuildHistoryEntity,
        onReadyToInstall: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = historyItem.artifactDownloadUrl
        if (url.isNullOrBlank()) {
            onError("URL do artefato não disponível.")
            return
        }

        // If local APK file already exists, open it directly
        val existingPath = historyItem.localApkPath
        if (!existingPath.isNullOrBlank()) {
            val file = File(existingPath)
            if (file.exists() && file.length() > 0) {
                onReadyToInstall(file)
                return
            }
        }

        viewModelScope.launch {
            _isDownloadingApk.value = true
            val downloadedFile = compilerManager.downloadAndExtractApk(
                historyId = historyItem.id,
                archiveUrl = url,
                projectName = historyItem.projectName
            ) { downloaded, total ->
                // Progress
            }
            _isDownloadingApk.value = false

            if (downloadedFile != null && downloadedFile.exists()) {
                onReadyToInstall(downloadedFile)
            } else {
                onError("Falha ao baixar e extrair o arquivo APK de release.")
            }
        }
    }

    fun deleteHistoryItem(item: BuildHistoryEntity, deleteRemoteRepo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.delete(item)
            // Delete local apk file if exists
            item.localApkPath?.let { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) {}
            }
            if (deleteRemoteRepo) {
                try {
                    val user = tokenStorage.getUsername()
                    if (!user.isNullOrBlank()) {
                        val api = GitHubClient.create { tokenStorage.getToken() }
                        api.deleteRepo(user, item.repoName)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun resetCompilerState() {
        _compilerProgress.value = CompilerProgress.Idle
    }
}
