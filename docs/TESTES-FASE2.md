# Testes — Sophia v0.2.0

## Estado da entrega — 29/10/2026

**Validação final aprovada e release publicada:** [execução 40918470947](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/actions/runs/40918470947), código `a7c98b8e6061b0fe08a7299059c6dd07fb37860f`, versão `0.2.0`, código Android `2`, pacote `com.sophia.enciclopedia`.

| Verificação executada | Resultado |
|---|---|
| Compilação `:app:assembleDebug` | Aprovada |
| `:app:lintDebug`, `:data:lintDebug` | 0 erros; 19 avisos (12 app, 7 data) |
| `:domain:test` | 18 testes, 0 falhas |
| `:data:connectedDebugAndroidTest` | 13 testes, 0 falhas, Android API 35 |
| `:app:connectedDebugAndroidTest` | 2 percursos UI, 0 falhas, Android API 35 |
| `python3 -m unittest discover -s tools/tests -v` | 6 testes de segurança, aprovados localmente e no CI; ADB simulado |
| Migração assistida com ADB real/emulador | Aprovada; APK original v0.1.0, nota, favorito e preferência real preservados |
| Revisão visual das capturas finais | TXT, segunda página EPUB com letra 22, PDF e instalação após migração confirmados |
| Integridade da entrega | ZIP íntegro; SHA-256 do APK e ZIP de migração coincidem com a release e `SHA256SUMS.txt` |

**Total: 39 testes automatizados aprovados**, mais a instalação/migração assistida real no emulador. Nenhum teste ignorado nos relatórios JUnit Android/JVM. A instalação num telefone físico não foi testada.

Os avisos de lint referem sobretudo versões mais recentes de dependências, target SDK, regras de backup e ausência de ícone monocromático. Não foram ocultados com baseline nem tratados como erros corrigidos; mantiveram-se as ferramentas compatíveis da arquitetura existente.

Capturas e relatórios XML/logcat estão no artefacto `sophia-device-tests` da execução final; relatórios JVM/lint em `sophia-build-reports`. O teste final valida o texto efetivo do EPUB depois de mudar de página e de regressar através de uma nota, e o índice `2/2`. A sincronização entre Compose e UIAutomator foi tornada explícita antes de observar o texto/estado da atividade. Uma execução aprovada não garante ausência de problemas em todos os WebViews.

### APK publicado

- [Sophia-v0.2.0-debug.apk](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/releases/download/sophia-v0.2.0/Sophia-v0.2.0-debug.apk)
- SHA-256: `ef3de297fed2b5880d8e6963549fa2b7a8dc8c41ef70337099925935a83c0e20`.
- Certificado SHA-256: `9788c0e73c9653cdf3e3dbbb1ae362bd740408b47f32c5290da40f5fa5de8b1e`.
- ZIP de migração SHA-256: `302cfd8710ca17ee40b6bd7aabd27da504e159f48a9a3485c9e870ee849a8802`.
- [Release e verificações](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/releases/tag/sophia-v0.2.0).


## Testes já executados na fase 2

Na execução [40910424988](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/actions/runs/40910424988), passaram 18 testes JVM, 13 testes de persistência Android e 2 testes de interface. Passaram também 6 testes Python de segurança da ferramenta de migração, repetidos localmente em 29/10/2026. **Os testes Python usam ADB simulado**: não constituem prova de instalação num dispositivo.

Separadamente, nesse workflow foi executada a instalação real do APK original publicado v0.1.0 num emulador Android API 35, seguida da ferramenta de migração assistida para o APK novo. Foram verificados uma nota com Unicode, um favorito e um ficheiro real de preferências alterado pela interface. O conteúdo e os hashes foram preservados após a migração Room 1→2. Nenhum dado de utilizadores reais foi usado.

A revisão humana das capturas dessa execução detetou um erro de recarregamento EPUB que os primeiros testes não apanhavam. O carregamento local foi corrigido e o teste reforçado para exigir texto real após mudar de página, alterar a letra e regressar a uma passagem. A execução final e as capturas estão identificadas acima. Uma execução intermédia reforçada (40913885768) detetou ainda falha do processo de renderização do WebView na transição. A correção seguinte conserva uma só vista entre recursos e tamanhos de letra, com carregamento local explícito e proteção contra callbacks antigos. Execuções posteriores ainda falharam na observação imediata da transição; foram acrescentados registos técnicos e sincronização do teste com o estado real da atividade. Só a execução final acima é a referência da entrega.

## Cobertura

- Domínio: pesquisa/validação anteriores e parser EPUB/TXT (OPF/spine/metadados, limites, caminhos inseguros, DRM, scripts, codificação e conservação exata do texto).
- Room: favoritos, notas, chaves estrangeiras; preferências DataStore, incluindo tamanho de letra.
- Migração real 1→2 com `MigrationTestHelper`, validação do esquema e conservação de favoritos/notas; catálogo aditivo e idempotente.
- Importações reais de fixtures originais via ContentProvider: TXT, EPUB e PDF; deduplicação, cópia privada independente do original, reabertura da base de dados, posição, marcador, nota ligada, metadados declarados, bitmap PDF, formatos inválidos e autorização em falta.
- Coleções: adicionar sem duplicar, ordenar e eliminar a coleção conservando obras.
- UI anterior: navegação, catálogo/pesquisa/favoritos/notas/tema, adaptada à expansão.
- UI nova: seletor Android SAF verdadeiro com DocumentsProvider só no APK de testes; leitura TXT/EPUB/PDF, navegação, fonte, marcador, nota por posição e retorno à passagem; secção de tradições.
- Rede Wi-Fi e dados móveis desativados no emulador durante estes fluxos.

## Defeito de assinatura da versão anterior

A instalação direta sobre o APK publicado falhou com `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: a keystore original não tinha sido conservada pelo antigo workflow. Não é um erro de migração Room nem pode ser corrigido recuperando a chave do certificado público. Não declarar compatibilidade de atualização direta.

A alternativa implementada é a [migração assistida](MIGRACAO-V01.md), com backup validado, confirmação explícita, reinstalação, restauro, verificação e tentativa de rollback. Os testes Python verificam ainda cancelamento, modo apenas backup, rejeição de caminhos inseguros e rollback após falha simulada. A assinatura nova usa caminho explícito e bloqueia futuras compilações se a chave publicada não puder ser restaurada.

## Limites da validação

Os EPUB/PDF/TXT são fixtures técnicas originais, não downloads nem edições religiosas. Não provam compatibilidade com todos os livros existentes. O fornecedor de documentos de teste não integra o APK entregue.

Sem testes em telefone físico, Android API 26, computador Windows real ou todos os fabricantes/WebViews. A ferramenta foi testada com ADB real num emulador Linux/API 35; o lançador Windows não foi executado num Windows real. A nova instalação e migração foram testadas, mas não a recuperação física após perda do dispositivo/computador.

Readium foi avaliado, não integrado. EPUB refluível sem DRM, com formatação simplificada; sem disposição fixa, TOC hierárquica completa ou CFI. PDF sem OCR ou seleção semântica de passagens. Pesquisa integral, semântica, RAG e IA continuam por implementar e são identificadas como tal na aplicação. Não há testes nem promessas de funcionalidades futuras.
