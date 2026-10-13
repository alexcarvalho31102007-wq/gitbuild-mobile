package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.compiler.CompilerProgress
import com.example.data.zip.ProjectValidationResult
import com.example.ui.TokenUiState
import com.example.ui.theme.GitHubBlue
import com.example.ui.theme.GitHubGreen
import com.example.ui.theme.GitHubOrange
import com.example.ui.theme.GitHubRed

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "SUCCESS" -> Triple(GitHubGreen.copy(alpha = 0.2f), GitHubGreen, "Concluído com Sucesso")
        "FAILURE", "FAILED" -> Triple(GitHubRed.copy(alpha = 0.2f), GitHubRed, "Falha na Compilação")
        "IN_PROGRESS", "RUNNING" -> Triple(GitHubBlue.copy(alpha = 0.2f), GitHubBlue, "Compilando no GitHub...")
        "QUEUED" -> Triple(GitHubOrange.copy(alpha = 0.2f), GitHubOrange, "Na Fila")
        else -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TokenConfigCard(
    tokenState: TokenUiState,
    onSaveToken: (String) -> Unit,
    onClearToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(!tokenState.hasToken) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("token_config_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Token PAT",
                        tint = if (tokenState.hasToken) GitHubGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "GitHub Personal Access Token (PAT)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (tokenState.hasToken && !isEditing) {
                    TextButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.testTag("edit_token_button")
                    ) {
                        Text("Alterar", fontSize = 12.sp)
                    }
                }
            }

            if (tokenState.hasToken && !isEditing) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Conectado",
                                tint = GitHubGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Autenticado como: @${tokenState.username ?: "GitHub"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Token: ${tokenState.maskedToken}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onClearToken,
                        modifier = Modifier.testTag("disconnect_token_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GitHubRed)
                    ) {
                        Text("Sair", fontSize = 11.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Necessário token clássico com escopo 'repo' ou Fine-Grained com permissões de repositório (Contents & Workflows: Read and write).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    label = { Text("Cole seu token (ghp_... ou github_pat_...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("token_input_field"),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (tokenState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tokenState.errorMessage,
                        color = GitHubRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tokenState.hasToken) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            onSaveToken(rawInput)
                            isEditing = false
                        },
                        enabled = rawInput.isNotBlank() && !tokenState.isValidating,
                        modifier = Modifier.testTag("save_token_button")
                    ) {
                        if (tokenState.isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validando...")
                        } else {
                            Text("Validar e Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CentralZipPicker(
    hasToken: Boolean,
    onPickZipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("zip_picker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = "ZIP Android",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Compilação Nativa em Nuvem",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Envie o arquivo .zip com seu projeto Android Gradle. O app detecta a estrutura, valida o projeto e compila um APK Release pelo GitHub Actions.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPickZipClick,
                enabled = hasToken,
                modifier = Modifier
                    .height(52.dp)
                    .fillMaxWidth(0.85f)
                    .testTag("select_zip_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Selecionar arquivo ZIP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!hasToken) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure seu token PAT do GitHub acima para habilitar o envio.",
                    color = GitHubOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PreflightValidationCard(
    result: ProjectValidationResult,
    onStartCompilation: () -> Unit,
    onCancel: () -> Unit,
    isCompiling: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("validation_report_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Validação do Projeto Android",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Projeto: ${result.projectName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onCancel, enabled = !isCompiling) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ValidationItem(
                    label = "Estrutura Gradle",
                    statusOk = result.hasSettingsGradle || result.hasRootBuildGradle,
                    detail = if (result.hasSettingsGradle) "settings.gradle localizado na raiz" else "Detectado build.gradle"
                )

                ValidationItem(
                    label = "Módulo Application",
                    statusOk = result.hasAppModule,
                    detail = if (result.hasAppModule) "Módulo app detectado" else "Módulo padrão"
                )

                ValidationItem(
                    label = "AndroidManifest",
                    statusOk = result.hasAndroidManifest,
                    detail = if (result.hasAndroidManifest) "Manifest presente" else "Manifest ausente"
                )

                ValidationItem(
                    label = "Gradle Wrapper",
                    statusOk = result.hasGradleWrapper,
                    detail = if (result.hasGradleWrapper) "Versão ${result.detectedGradleVersion ?: "detectada"}" else "Será gerado no GitHub"
                )

                ValidationItem(
                    label = "Assinatura Release",
                    statusOk = true,
                    detail = if (result.hasSigningConfig) "Configurada no build.gradle" else "Auto-assinatura no GitHub Actions ativada"
                )

                ValidationItem(
                    label = "Arquivos / Tamanho",
                    statusOk = true,
                    detail = "${result.totalFiles} arquivos (~${result.totalSizeUncompressed / 1024} KB descompactado)"
                )
            }

            if (result.validationIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GitHubRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = GitHubRed, modifier = Modifier.size(16.dp))
                        Text(text = "Erros de Validação:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GitHubRed)
                    }
                    result.validationIssues.forEach { issue ->
                        Text(text = "• $issue", fontSize = 12.sp, color = GitHubRed)
                    }
                }
            }

            if (result.validationWarnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GitHubOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = GitHubOrange, modifier = Modifier.size(16.dp))
                        Text(text = "Observações:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GitHubOrange)
                    }
                    result.validationWarnings.forEach { warning ->
                        Text(text = "• $warning", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartCompilation,
                enabled = result.isValid && !isCompiling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("compile_release_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GitHubGreen
                )
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Iniciando Compilação...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compilar Release no GitHub", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ValidationItem(
    label: String,
    statusOk: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = if (statusOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (statusOk) GitHubGreen else GitHubOrange,
                modifier = Modifier.size(14.dp)
            )
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(text = detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RealtimeProgressCard(
    progress: CompilerProgress,
    onViewLogs: (String) -> Unit,
    onInstallApk: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_progress_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status da Compilação Real",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (progress) {
                    is CompilerProgress.Success -> StatusBadge("SUCCESS")
                    is CompilerProgress.Failed -> StatusBadge("FAILURE")
                    is CompilerProgress.Running -> StatusBadge("IN_PROGRESS")
                    is CompilerProgress.WaitingForWorkflow -> StatusBadge("QUEUED")
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (progress) {
                is CompilerProgress.Validating -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = progress.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is CompilerProgress.PreparingRepo -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configurando repositório privado: ${progress.repoName}...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is CompilerProgress.UploadingFiles -> {
                    val progressFrac = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f
                    LinearProgressIndicator(progress = { progressFrac }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enviando arquivo ${progress.current} de ${progress.total}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(progressFrac * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = progress.currentFile,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                is CompilerProgress.Committing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = progress.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is CompilerProgress.WaitingForWorkflow -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Aguardando o GitHub Actions iniciar a compilação...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is CompilerProgress.Running -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = GitHubBlue,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Executando no GitHub Actions",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            val min = progress.elapsedSeconds / 60
                            val sec = progress.elapsedSeconds % 60
                            Text(
                                text = String.format("%02d:%02d min", min, sec),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!progress.currentStep.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Etapa Atual:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = progress.currentStep,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "Status real da API: ${progress.status}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is CompilerProgress.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GitHubGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GitHubGreen)
                            Text(
                                text = "APK Release Compilado com Sucesso!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GitHubGreen
                            )
                        }

                        Text(
                            text = "O artefato oficial foi gerado e verificado pelo pipeline. Tamanho: ${progress.artifactSize / (1024 * 1024)} MB.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onInstallApk,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("install_apk_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Instalar APK", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(onClick = onReset) {
                                Text("Novo")
                            }
                        }
                    }
                }

                is CompilerProgress.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GitHubRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = GitHubRed)
                            Text(
                                text = "Falha na Compilação",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GitHubRed
                            )
                        }

                        Text(
                            text = progress.reason,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!progress.errorLogs.isNullOrBlank()) {
                                Button(
                                    onClick = { onViewLogs(progress.errorLogs) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("view_failure_logs_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GitHubRed)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exibir e Copiar Logs")
                                }
                            }

                            OutlinedButton(onClick = onReset) {
                                Text("Fechar")
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun LogViewerDialog(
    logs: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("log_viewer_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Logs de Falha do Gradle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GitHubRed
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D1117))
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = logs,
                        color = Color(0xFFF0F6FC),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Logs de Compilação GitHub", logs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Logs copiados para a área de transferência!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_logs_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copiar Logs Completos", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
