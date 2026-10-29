# Sophia — Enciclopédia do Conhecimento Espiritual

Aplicação Android nativa, offline e em português de Portugal. **v0.1.0 · apenas fase 1.** Projeto independente do AI Hub e da Sonora.

## Funcionalidades

- Início com pesquisa, categorias e referências recentes; estado de leitura vazio explícito.
- Biblioteca com seis referências bibliográficas, detalhes, pesquisa por título/autor, categorias, tradições e favoritos persistentes.
- Investigar: pesquisa de metadados e filtros de autor/colaborador e tradição. Sem pesquisa integral ou semântica.
- Caderno: criar, editar, pesquisar e eliminar notas com confirmação, associadas opcionalmente a uma obra.
- Tema claro, escuro ou do sistema, guardado em DataStore.
- Assistente IA identificado como **em desenvolvimento**, sem modelos, respostas ou chamadas de API.

Não exige conta, ligação à Internet, publicidade, cartão bancário ou credenciais. Não solicita permissão de Internet. Dados guardados localmente; desinstalar/limpar dados elimina notas e favoritos. Backup e restauro ainda não existem; a cópia automática Android está desativada.

## Instalação

O workflow publica um APK debug apenas após compilação, lint e testes no Android. Consulte [Releases](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/releases). Instale o APK no Android 8.0 (API 26) ou superior, autorizando a instalação desta origem quando o Android o solicitar. É uma versão inicial de estudo, não uma publicação na Play Store.

## Compilar

Requisitos: JDK 17, Android SDK 35, Build Tools 35.0.0 e Gradle 8.10.2. Abra a raiz no Android Studio, indique o SDK em `local.properties` (`sdk.dir=...`) e sincronize. As dependências precisam de Internet na primeira compilação; a aplicação funciona offline.

```sh
# Caso o wrapper ainda não tenha sido obtido do artefacto de CI:
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
./gradlew :domain:test :app:assembleDebug :app:lintDebug :data:lintDebug
# Com dispositivo/emulador Android disponível:
./gradlew :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. No Windows use `gradlew.bat`. Não publique keystores, `local.properties`, dados pessoais ou credenciais. A assinatura debug é preservada pelo cache exclusivo deste projeto no GitHub Actions.

## Organização e documentação

- `app`: Compose/Material 3, ViewModel, navegação e apresentação.
- `domain`: modelos, contratos de repositories, pesquisa e validação sem Android.
- `data`: Room/SQLite, catálogo inicial e DataStore.
- [Arquitetura e dados](docs/ARQUITETURA.md)
- [Catálogo e proveniência](docs/CATALOGO.md)
- [Testes e limitações](docs/TESTES.md)

Versões fixadas para compatibilidade: AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.7.2/KSP2, DataStore 1.1.2. Não representam uma promessa de serem as versões mais recentes.

## Limites da fase 1

Não inclui textos integrais, capas de terceiros, ficheiros EPUB/PDF, leitor, importação, IA, RAG, citações extraídas, pesquisa textual/semântica, sincronização ou backup. A estrutura distingue obras, edições, traduções e ficheiros, preparando essas evoluções sem as implementar. Os registos de demonstração não concedem direitos de utilização das obras. **Não foi iniciada a fase 2.**
