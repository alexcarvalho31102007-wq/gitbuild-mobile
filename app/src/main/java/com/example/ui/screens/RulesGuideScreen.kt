package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GitHubGreen

val AI_RULES_PROMPT = """
Você é um especialista sênior em desenvolvimento Android e automação de builds com Gradle e GitHub Actions.

Por favor, analise, ajuste e estruture o meu projeto Android para que ele atenda a 100% dos requisitos do 'Compilador GitHub' e compile com sucesso no GitHub Actions gerando um APK Release instalável.

Aplique rigorosamente as seguintes regras no código e na estrutura do projeto:

1. ESTRUTURA GRADLE OBRIGATÓRIA:
   - Garanta o arquivo 'settings.gradle' (ou 'settings.gradle.kts') na raiz contendo:
     * rootProject.name definido com um nome válido para o app.
     * include ':app' (e outros módulos se houver).
     * Repositórios declarados em pluginManagement e dependencyResolutionManagement com google(), mavenCentral() e gradlePluginPortal().
   - Garanta o 'build.gradle' (ou 'build.gradle.kts') da raiz do projeto com os plugins mapeados.
   - Garanta o módulo 'app/build.gradle' (ou 'app/build.gradle.kts') aplicando o plugin 'com.android.application'.

2. COMPATIBILIDADE JDK, SDK E GRADLE:
   - Configure compatibilidade de Java e Kotlin para Java 17 (JavaVersion.VERSION_17).
   - Defina compileSdk (34 ou 35), targetSdk (34 ou 35) e minSdk (21 ou superior).
   - Configure 'gradle/wrapper/gradle-wrapper.properties' com uma versão estável (ex: Gradle 8.4 ou superior com AGP correspondente).

3. REQUISITOS DO APK RELEASE:
   - A compilação é executada estritamente com o comando './gradlew assembleRelease'.
   - O projeto deve compilar limpo sem erros de sintaxe ou dependências não resolvidas.
   - Não utilize flavor exclusivo de debug na compilação release.

4. MANIFEST E RECURSOS:
   - Certifique-se de que 'app/src/main/AndroidManifest.xml' está presente com applicationId/namespace correspondente e a Activity principal configurada com o intent-filter MAIN e LAUNCHER.
   - Garanta que ícones, temas e strings básicos estejam devidamente mapeados sem referências quebradas.

5. REMOÇÃO DE ARQUIVOS LOCAIS E CACHES:
   - Remova arquivos locais 'local.properties', pastas 'build/', 'app/build/', '.gradle/' e '.idea/'.
   - O projeto não pode conter caminhos absolutos locais do computador.

Entregue o código ajustado e a lista dos arquivos prontos para serem compactados na raiz de um arquivo .zip para compilação direta no Compilador GitHub.
""".trimIndent()

@Composable
fun RulesGuideScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val copyPromptToClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prompt IA - Compilador GitHub", AI_RULES_PROMPT)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Prompt copiado com sucesso!", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("rules_guide_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
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
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Regras do Projeto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Copie o prompt e envie para uma IA aplicar todas as regras no seu projeto Android.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = copyPromptToClipboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("copy_ai_prompt_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copiar prompt para IA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
