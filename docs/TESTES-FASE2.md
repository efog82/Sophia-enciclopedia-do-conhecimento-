# Testes — Sophia v0.2.0

Estado: validação em curso na branch `feature/fase-2-biblioteca`. Este documento será atualizado com resultados reais antes da entrega. Compilar não prova que a importação/leitura funciona.

## Testes preparados

- Domínio: pesquisa/validação anteriores e parser EPUB/TXT (OPF/spine/metadados, limites, caminhos inseguros, DRM, scripts, codificação e conservação do texto).
- Persistência anterior: favoritos, notas, chaves estrangeiras e preferências, incluindo tamanho de letra do leitor.
- Migração Room real 1→2 com `MigrationTestHelper`, validação do esquema e conservação de favoritos/notas existentes; catálogo aditivo e idempotente.
- Importações reais de ficheiros técnicos originais via ContentProvider: TXT, EPUB e PDF; deduplicação, posição, marcador, nota ligada, metadados declarados, renderização PDF, formatos inválidos e autorização em falta.
- Coleções: adicionar sem duplicar, ordenar e eliminar a coleção conservando obras.
- UI anterior: navegação, catálogo/pesquisa/favoritos/notas/tema, adaptada à expansão.
- UI nova: seletor Android SAF verdadeiro com DocumentsProvider apenas no APK de testes; leitura TXT/EPUB/PDF, navegação, fonte, marcador, nota por posição e retorno à passagem; secção de tradições.
- Instalação do APK original v0.1.0 e atualização com `adb install -r` para v0.2.0; arranque e capturas reais.

## O que não é testado por estes casos

Os EPUB/PDF/TXT usados são fixtures técnicas originais, não downloads nem edições de obras religiosas. Testam os formatos e a implementação, não compatibilidade com todos os EPUB/PDF existentes. O fornecedor de documentos de teste não integra o APK de produção.

Sem testes num telefone físico ou garantia de cobertura de todos os fabricantes. Sem testes de IA, pesquisa integral, OCR ou funcionalidades não implementadas. Resultados concretos e ligações ao workflow serão acrescentados após execução.
