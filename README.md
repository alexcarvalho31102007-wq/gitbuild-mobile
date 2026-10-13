# Compilador GitHub - Android Native

Aplicativo nativo Android desenvolvido em **Kotlin** e **Jetpack Compose** que permite compilar projetos Android Gradle diretamente do seu celular em APK Release utilizando a infraestrutura do **GitHub Actions**.

---

## 🚀 Como Funciona

1. **Configuração de Credenciais**: O usuário insere seu GitHub Personal Access Token (PAT) com escopo `repo`. O app valida o token em tempo real contra a API do GitHub (`GET /user`) e o armazena de forma segura no dispositivo.
2. **Seleção e Inspeção do ZIP**: O usuário toca no botão central **"Selecionar arquivo ZIP"**. O app lê o arquivo streaming, identifica a raiz real do projeto, valida se é um projeto Android Gradle válido (`settings.gradle`, `build.gradle`, `AndroidManifest.xml`), detecta versão do Gradle, SDKs e configuração de assinatura.
3. **Criação do Repositório Privado**: O app cria um repositório privado exclusivo para o projeto (ou reutiliza se já existente, sem duplicidades) via API do GitHub.
4. **Envio Limpo do Código via Git Data API**: Utilizando a API de Blobs e Trees do Git, o app envia os arquivos do código-fonte e injeta o arquivo de workflow `.github/workflows/android-release.yml`. Pastas pesadas de build e caches locais são automaticamente ignoradas.
5. **Execução Real no GitHub Actions**:
   - Compilação estritamente em **Release** (`./gradlew assembleRelease`).
   - Se o projeto não possuir chave de release própria configurada, o runner gera e assina o APK com keystore seguro via `apksigner`/`jarsigner`.
   - Gera e publica o artefato `app-release-apk`.
6. **Acompanhamento Transparente**: O app exibe o status real da API do GitHub (`queued`, `in_progress` com a etapa atual do job e cronômetro em tempo real, `completed`). Sem simulações ou porcentagens falsas.
7. **Logs de Erro Inteligentes**: Em caso de falha, os logs exatos do Gradle são baixados e apresentados com opção de cópia em 1 clique para a área de transferência.
8. **Download e Instalação do APK**: Após confirmar que o artefato existe, o app baixa o APK real e o abre através do instalador do Android (`FileProvider`).
9. **Histórico Completo**: O app mantém histórico local em banco de dados Room com status, tamanho do APK, download e opção de exclusão (com opção de excluir também o repositório remoto do GitHub).

---

## 🛠️ Tecnologias Utilizadas

- **Kotlin 2.2**
- **Jetpack Compose & Material 3** (Design moderno no estilo developer do GitHub)
- **Room Database** (Persistência local do histórico de compilações)
- **Retrofit 2 & OkHttp** (Comunicação com a API REST v3 do GitHub)
- **Moshi** (Serialização JSON segura e performática)
- **Coroutines & StateFlow** (Arquitetura reativa MVVM)
- **FileProvider & SAF** (Seleção de arquivos e instalação nativa de APKs)
- **GitHub Actions** (Ambiente de compilação em nuvem Ubuntu com JDK 17 e Android SDK)

---

## 🔒 Segurança

- O token do GitHub nunca é compartilhado com terceiros ou enviado para servidores intermediários; a comunicação é 100% direta entre o seu aparelho e os endpoints oficiais `https://api.github.com`.
- Todos os repositórios criados são configurados com visibilidade **Privada** (`private: true`).
- O token é armazenado com segurança e exibido de forma mascarada (`ghp_••••••••xxxx`) na interface.

---

## 📖 Guia de Regras

Consulte o arquivo [`REGRAS_DO_PROJETO_ANDROID.txt`](./REGRAS_DO_PROJETO_ANDROID.txt) ou acesse a aba **"Regras"** dentro do aplicativo para orientações completas sobre como compactar e preparar seus projetos Android.
