# Validação — v0.3.0

## Verificação local

Executados localmente: seis testes Python da ferramenta de migração v0.1.0 → v0.2.0, aprovados; usam ADB simulado.

Tentativa local de `:domain:test :app:assembleDebug :app:lintDebug :data:lintDebug`: bloqueada antes da compilação, no download do Gradle 8.10.2 (`SocketException: Operation not permitted`). Não se declara aprovação Android/JVM local. O ambiente fornece JDK 21; o CI usa JDK 17 e SDK 35 conforme o projeto.

## Verificações no GitHub Actions

**Execução aprovada:** [40958248556](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/actions/runs/40958248556), branch `feature/v0.3.0-historia`, código `d01917759c7e42c2df0f03558ce7a9f0868af8d3`. A integração acrescenta documentação dos resultados; o workflow de `main` repete os controlos antes de publicar a release.

| Verificação executada | Resultado observado |
|---|---|
| `:app:assembleDebug` e compilação dos testes Android | Aprovados |
| `:domain:test` | 20 testes, zero falhas/erros/ignorados |
| `:data:connectedDebugAndroidTest` | 15 testes, zero falhas/erros/ignorados, Android API 35 |
| `:app:connectedDebugAndroidTest` | 2 percursos, zero falhas/erros/ignorados, Android API 35 |
| Testes Python da ferramenta de migração | 6 aprovados, ADB simulado |
| Lint app/data | Zero erros; 13 avisos app e 7 data |
| Certificado do APK | SHA-256 igual ao da v0.2.0 publicada |
| Migração assistida original v0.1.0 → v0.2.0 | Aprovada no emulador com APK original e dados de ensaio |
| Atualização normal v0.2.0 → v0.3.0 | `adb install -r` aprovado, sem desinstalar; tabelas, ficheiro e preferências iguais |
| Revisão visual | Início claro, categorias em escuro, referências/capas e história do hermetismo inspecionados |

**Total: 43 testes automatizados**, mais dois percursos reais de instalação/atualização no emulador. Os relatórios XML, capturas e folha de revisão visual estão nos artefactos `sophia-device-tests` e `sophia-build-reports` da execução. Os avisos de lint não foram ocultados por baseline.

A primeira execução foi bloqueada por uma regra de lint na utilização de `produceState`; o carregamento passou para estado memorizado e `LaunchedEffect`. Uma execução seguinte detetou uma expectativa errada de biblioteca vazia no teste de interface, depois de outro percurso já ter importado ficheiros; a asserção foi corrigida para os ficheiros reais e a ausência de cartões de referência na Biblioteca. Só a execução aprovada acima fundamenta os resultados desta entrega.

Cobertura adicionada: seleção de capas OPF2/OPF3; rejeição de capa remota com leitura preservada; descodificação da capa local e reabertura após remover o original; integridade das ligações e números de fontes dos artigos; pesquisa histórica; início sem lista de livros; biblioteca sem referências sem ficheiro; navegação entre histórias e restauro após recriação. Os percursos de importação SAF/leitura TXT/EPUB/PDF, notas, favoritos e temas foram conservados e adaptados à navegação nova.

O teste de atualização direta instala a v0.3.0 por cima da v0.2.0 publicada num emulador e compara todas as tabelas de dados, o ficheiro pessoal e as preferências. Inclui nota ligada, posição, marcador, coleção e favorito de ensaio. Exige o certificado da release v0.2.0. A migração assistida antiga continua a ser testada com destino v0.2.0; não foi alterada para aceitar silenciosamente outro APK.

Limites: não foram testados telefone físico, API 26, todos os WebViews, todas as codificações/formatações EPUB ou bibliotecas pessoais reais. Os testes de integridade editorial verificam referências internas, não substituem a verificação histórica humana de cada afirmação. Não se afirma uma história exaustiva de todas as tradições.
