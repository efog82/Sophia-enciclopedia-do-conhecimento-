# Validação — v0.3.0

## Estado inicial

Executados localmente: seis testes Python da ferramenta de migração v0.1.0 → v0.2.0, aprovados; usam ADB simulado.

Tentativa local de `:domain:test :app:assembleDebug :app:lintDebug :data:lintDebug`: bloqueada antes da compilação, no download do Gradle 8.10.2 (`SocketException: Operation not permitted`). Não se declara aprovação Android/JVM local. O ambiente fornece JDK 21; o CI usa JDK 17 e SDK 35 conforme o projeto.

## Verificações no GitHub Actions

O workflow da branch `feature/v0.3.0-historia` executa compilação, lint, testes JVM e Android antes da integração. O resultado e o link da execução final serão registados depois de observados.

Cobertura adicionada: seleção de capas OPF2/OPF3; rejeição de capa remota com leitura preservada; descodificação da capa local e reabertura após remover o original; integridade das ligações e números de fontes dos artigos; pesquisa histórica; início sem lista de livros; biblioteca sem referências sem ficheiro; navegação entre histórias e restauro após recriação. Os percursos de importação SAF/leitura TXT/EPUB/PDF, notas, favoritos e temas foram conservados e adaptados à navegação nova.

O teste de atualização direta instala a v0.3.0 por cima da v0.2.0 publicada num emulador e compara todas as tabelas de dados, o ficheiro pessoal e as preferências. Inclui nota ligada, posição, marcador, coleção e favorito de ensaio. Exige o certificado da release v0.2.0. A migração assistida antiga continua a ser testada com destino v0.2.0; não foi alterada para aceitar silenciosamente outro APK.

Limites: não foram testados telefone físico, API 26, todos os WebViews, todas as codificações/formatações EPUB ou bibliotecas pessoais reais. Os testes de integridade editorial verificam referências internas, não substituem a verificação histórica humana de cada afirmação. Não se afirma uma história exaustiva de todas as tradições.
