# Sophia — Biblioteca Universal do Conhecimento Espiritual

**v0.2.0 · fase 2.** Android nativo em Kotlin/Jetpack Compose/Material 3, offline e em português de Portugal. Mantém os módulos `app`, `domain` e `data`, com MVVM, Repository, Room e DataStore.

A v0.1.0 e a sua release permanecem disponíveis. O desenvolvimento da fase 2 está na branch `feature/fase-2-biblioteca`, com compilação e testes no GitHub Actions. Consulte o [relatório da fase 2](docs/TESTES-FASE2.md) para o estado efetivamente verificado.

## O que inclui

- 40 referências bibliográficas e 10 tradições: Espiritismo, Budismo, Cristianismo, Hermetismo, Hinduísmo e tradições védicas, Judaísmo e Cabala, Islão e sufismo, Taoismo, Filosofia antiga e Confucionismo.
- Os quatro Vedas, Upanishads, Bhagavad Gita, Ramayana, Mahabharata e Yoga Sutras; referências adicionais das tradições existentes.
- Introduções históricas neutras, conceitos fundamentais, textos/autores e ligação à investigação por tradição. Ensinamentos religiosos são apresentados no seu contexto, não como factos científicos.
- Importação de EPUB, PDF e TXT pelo **seletor Android / Storage Access Framework**. Cópia privada para leitura offline, associação a uma obra existente ou criação de uma referência pessoal. O original não é alterado.
- Leitor integrado, posição persistente, marcadores e retoma no Início. Tema claro/escuro, letra ajustável em EPUB/TXT e zoom em PDF.
- Notas associadas ao ficheiro e à posição; excertos opcionais copiados pelo utilizador. Abrir uma nota permite regressar à passagem.
- Coleções e listas de leitura ordenadas; favoritos e caderno existentes preservados.
- Pesquisa de metadados: títulos, autores/colaboradores, tradições, categorias e descrições. **Pesquisa integral/FTS e semântica ainda não implementadas.**
- Assistente IA continua explicitamente em desenvolvimento, sem modelos ou respostas simuladas.

## Instalar e usar

Consulte [Releases](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/releases) e os artefactos do [GitHub Actions](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/actions). Android 8.0 / API 26 ou superior. O APK é debug, para avaliação, não uma publicação na Play Store.

1. **Biblioteca → Importar EPUB, PDF ou TXT.** Escolhe um ficheiro a que tenhas acesso legítimo.
2. Seleciona a obra do catálogo ou cria uma referência a partir do ficheiro; confirma a autorização de uso pessoal.
3. Abre **Ler offline**, nos detalhes ou na lista de ficheiros. A cópia fica disponível mesmo que o original seja movido.
4. Usa as setas e o índice, guarda marcadores e cria uma nota na posição atual. Para um excerto exato, seleciona e copia o texto e cola-o no campo de excerto.
5. No Caderno, **Ler passagem associada** regressa ao ficheiro. Na Biblioteca, cria coleções/listas; adiciona obras nos respetivos detalhes.

Sem contas, publicidade ou permissões de Internet/armazenamento alargado. Um fornecedor de ficheiros na nuvem pode precisar de descarregar o ficheiro durante a seleção; a leitura posterior usa a cópia local. Não há downloads automáticos de livros.

**Atualização:** instalar o APK com a mesma assinatura e `versionCode=2` conserva os dados. O workflow reutiliza o cache de assinatura debug do projeto e testa instalar a v0.2.0 sobre o APK original v0.1.0. Não desinstales a versão antiga para atualizar: desinstalar ou limpar dados elimina as notas, favoritos e cópias locais. Ainda não existe backup/restauro; a cópia automática Android está desativada.

## Formatos e limites reais

| Formato | Suporte desta fase | Limites |
|---|---|---|
| EPUB | Pacote OPF, ordem `spine`, XHTML sanitizado, texto e imagens locais, títulos declarados dos recursos, letra/tema e posição dentro do recurso | Leitor de texto refluível; não reproduz fielmente CSS da editora. Sem DRM, fontes cifradas, EPUB de disposição fixa, áudio/vídeo, scripts, MathML ou navegação TOC hierárquica. Ligações entre recursos usam a ordem real; notas/locadores não são CFI Readium. |
| PDF | Páginas renderizadas pelo `PdfRenderer` Android, navegação, zoom/deslocação, posição/página e marcadores | Sem palavra-passe/DRM, OCR, extração ou seleção textual em PDF. Letra fixa; modo escuro inverte as cores da página. |
| TXT | UTF-8 e UTF-16 com BOM; seleção de texto, letra/tema e posição | Blocos técnicos de leitura preservam o texto integral; não são capítulos inventados. Outras codificações devem ser convertidas previamente. |

Limites de segurança: ficheiro importado até 200 MiB; TXT até 16 MiB; recurso EPUB até 8 MiB, 10 000 entradas e tamanho total descomprimido até 512 MiB. São limites contra ficheiros incompatíveis e consumo excessivo de memória, não quotas comerciais.

O Readium foi [avaliado](docs/ARQUITETURA-FASE2.md). A integração do navegador Readium completo fica por realizar; esta versão usa um leitor local de âmbito explícito. **Não se apresenta como um leitor Readium completo.**

## Catálogo e direitos

O catálogo **não contém os textos integrais**. Uma referência não implica que exista um ficheiro para leitura. As quatro traduções históricas dos Vedas são registos bibliográficos, com tradução/edição separadas da obra e direitos por verificar; nenhuma é descarregada. [Fontes e critérios](docs/CATALOGO-FASE2.md).

Metadados do EPUB são declarações do ficheiro, não verificação bibliográfica independente. Não se infere a língua original da obra a partir da língua da edição. Editoras, traduções e datas desconhecidas ficam vazias. A autorização assinalada pelo utilizador não é certificação de domínio público/licença pela Sophia e não autoriza redistribuição.

## Compilar e testar

JDK 17, Android SDK/target 35, Build Tools 35.0.0, Gradle Wrapper 8.10.2. As dependências exigem Internet na primeira compilação; a aplicação funciona offline. No Android Studio, abre a raiz e indica o SDK em `local.properties`.

```sh
./gradlew :domain:test :app:assembleDebug :app:lintDebug :data:lintDebug
./gradlew :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

No Windows: `gradlew.bat`. APK em `app/build/outputs/apk/debug/app-debug.apk`.

- `app`: ecrãs/estados Compose, ViewModels e leitor WebView local/PDF.
- `domain`: contratos, modelos, pesquisa, parser EPUB/TXT e validação.
- `data`: Room, migração explícita 1→2, catálogo incremental, cópias privadas e DataStore.
- [Arquitetura da fase 2](docs/ARQUITETURA-FASE2.md), [catálogo](docs/CATALOGO-FASE2.md), [testes](docs/TESTES-FASE2.md).

Não publiques keystores, `local.properties`, ficheiros pessoais, bases de dados ou credenciais. Projeto independente do AI Hub e da Sonora.
