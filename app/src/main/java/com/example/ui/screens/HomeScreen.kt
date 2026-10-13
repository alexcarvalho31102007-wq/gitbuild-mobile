package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.compiler.CompilerProgress
import com.example.ui.CompilerViewModel
import com.example.ui.components.CentralZipPicker
import com.example.ui.components.LogViewerDialog
import com.example.ui.components.PreflightValidationCard
import com.example.ui.components.RealtimeProgressCard
import com.example.ui.components.TokenConfigCard

@Composable
fun HomeScreen(
    viewModel: CompilerViewModel,
    onPickZipClick: () -> Unit,
    onInstallApk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokenState by viewModel.tokenState.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val isAnalyzingZip by viewModel.isAnalyzingZip.collectAsState()
    val compilerProgress by viewModel.compilerProgress.collectAsState()

    var activeLogsToView by remember { mutableStateOf<String?>(null) }

    val isCompiling = compilerProgress is CompilerProgress.Validating ||
            compilerProgress is CompilerProgress.PreparingRepo ||
            compilerProgress is CompilerProgress.UploadingFiles ||
            compilerProgress is CompilerProgress.Committing ||
            compilerProgress is CompilerProgress.WaitingForWorkflow ||
            compilerProgress is CompilerProgress.Running

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. GitHub Token Configuration Card
            TokenConfigCard(
                tokenState = tokenState,
                onSaveToken = { viewModel.saveToken(it) },
                onClearToken = { viewModel.clearToken() }
            )

            // 2. Loading state while inspecting ZIP
            if (isAnalyzingZip) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Analisando estrutura do projeto Android...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3. Central ZIP Picker Button (shown when no validation result is open or ready to select new)
            if (validationResult == null && !isAnalyzingZip && compilerProgress is CompilerProgress.Idle) {
                CentralZipPicker(
                    hasToken = tokenState.hasToken,
                    onPickZipClick = onPickZipClick
                )
            }

            // 4. Preflight Validation Report Card
            validationResult?.let { result ->
                if (compilerProgress is CompilerProgress.Idle) {
                    PreflightValidationCard(
                        result = result,
                        onStartCompilation = { viewModel.startCompilation() },
                        onCancel = { viewModel.clearSelectedZip() },
                        isCompiling = isCompiling
                    )
                }
            }

            // 5. Real-time Progress Card (when build is running, success, or failed)
            if (compilerProgress !is CompilerProgress.Idle) {
                RealtimeProgressCard(
                    progress = compilerProgress,
                    onViewLogs = { logs -> activeLogsToView = logs },
                    onInstallApk = onInstallApk,
                    onReset = { viewModel.resetCompilerState() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Error Logs viewer dialog
        activeLogsToView?.let { logs ->
            LogViewerDialog(
                logs = logs,
                onDismiss = { activeLogsToView = null }
            )
        }
    }
}
