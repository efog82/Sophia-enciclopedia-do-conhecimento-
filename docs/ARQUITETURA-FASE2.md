# Arquitetura e decisões — fase 2

Mantidos `app`, `domain`, `data`, Kotlin, Compose, MVVM/Repository, Room, Flow/StateFlow e DataStore. `MainActivity` continua a ligar ViewModels à interface. `ReaderActivity` é privada, com `ReaderViewModel`; a importação e organização têm `LibraryViewModel`. Operações de ficheiros/parser/PDF decorrem em `Dispatchers.IO`. O PDF é renderizado página a página, com dimensão limitada.

## Esquema Room 2

`DatabaseMigrations.V1_TO_V2` altera o esquema inicial explicitamente. Sem `REPLACE` dos dados pessoais e sem `fallbackToDestructiveMigration`.

- Tradições ganham contextualização histórica, conceitos e fontes.
- Edições ganham estado dos direitos; continuam separadas de obras/traduções.
- Ficheiros ganham nome original e data técnica de importação.
- Notas ganham `fileId`, `locatorJson` e `excerpt`. A ligação ao ficheiro tem `SET_NULL`, conservando o texto da nota.
- Novas tabelas: `bookmarks`, `study_collections`, `collection_works`; chaves estrangeiras, posição ordenada e índices explícitos.
- O esquema 1 original é preservado em `data/schemas`; o esquema 2 é exportado pelo KSP e disponibilizado ao teste de migração antes de empacotar assets.

O catálogo inicial mantém `demo_catalog_version=1`. `ExpandedCatalog` usa separadamente `universal_catalog_version=2`, em transação. Novas referências/ligações são adicionadas; as introduções das tradições são atualizadas por ID. Favoritos e notas não são regravados nem eliminados. O marcador só é gravado se toda a atualização concluir.

## Importação e segurança

`OpenDocument`/SAF concede acesso ao ficheiro escolhido; não se exige acesso geral ao armazenamento. `LocalLibraryRepository` copia por blocos para uma pasta privada, calcula SHA-256 e valida conteúdo. Uma transação associa obra, edição, colaboradores declarados e ficheiro. Em caso de falha normal ou cancelamento, remove a cópia incompleta. Um encerramento abrupto do processo pode deixar um ficheiro privado órfão; não existe ainda ferramenta automática de limpeza/gestão de espaço.

Importações idênticas para a mesma obra são deduplicadas por hash. Não se apaga nem modifica o ficheiro original. Depois da cópia não depende do URI original, de uma licença temporária do fornecedor SAF ou de rede.

EPUB é lido diretamente do ZIP, sem extração. São rejeitados caminhos absolutos/traversal, entradas duplicadas, recursos cifrados/DRM, disposição fixa e limites excessivos. XML e XHTML usam jsoup, sem resolução de entidades externas. O XHTML é sanitizado; scripts, formulários e frames são removidos. O WebView tem JavaScript, acesso a ficheiros/conteúdo e rede desativados; só o domínio virtual `https://sophia.invalid/` é servido por interceção a partir desse EPUB. Esse domínio não é um serviço externo. Não existe permissão `INTERNET` na aplicação.

TXT valida UTF-8 ou UTF-16 com BOM, preservando os caracteres e quebras de linha. Blocos são técnicos, não representam capítulos reais. PDF usa a implementação do sistema, sem binário nativo externo ou serviço remoto.

## Readium: avaliação, sem falsa integração

Consultados [guia oficial](https://readium.org/kotlin-toolkit/latest/) e [API/guia da versão 3.0.0](https://github.com/readium/kotlin-toolkit/tree/3.0.0/docs/guides). O Readium fornece parser, modelos de publicações/locadores e navegador EPUB, exigindo ainda UI e persistência da aplicação. Versões recentes exigem SDK/ferramentas diferentes das fixadas; 3.0.0 é uma base anterior compatível a avaliar para integração completa.

Foi escolhido nesta fase um leitor EPUB refluível local com capacidades explicitamente limitadas, evitando atualizar toda a base ou anunciar compatibilidade não testada. **Readium não é uma dependência integrada.** Há trabalho futuro para fidelidade editorial, navegação TOC hierárquica, CFI, destaques semânticos e EPUB de disposição fixa.

## Posições, notas e evolução

Posição versionada por ficheiro: índice de recurso `spine`/bloco TXT/página PDF e fração de deslocação quando aplicável. A posição acompanha a cópia privada imutável identificada por hash; não se promete que possa ser aplicada a outra edição. Tema e tamanho de letra são preferências DataStore. Marcadores e notas ficam em Room. O excerto é texto escolhido e colado pelo utilizador, não uma citação gerada/verificada automaticamente.

As relações ficheiro/edição, origem, texto sanitizado e locadores permitem preparar uma futura indexação FTS; nesta versão não há indexação integral, OCR, pesquisa semântica, RAG ou IA. O esquema/contratos poderão evoluir por migração; não se instala uma API fictícia de pesquisa.

## Assinatura e migração da instalação antiga

O teste de instalação sobre o APK publicado revelou `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. O workflow v0.1.0 não tinha guardado a keystore: a ação de cache devolvia `Path Validation Error`, apesar de o APK ter sido assinado por uma chave gerada no processo de compilação. A chave privada não está no APK e não pode ser recuperada do certificado público. Não se tenta contornar a validação de assinaturas do Android.

O identificador `com.sophia.enciclopedia` foi conservado. `tools/migrate_v01.py` suporta uma reinstalação assistida com backup validado do APK e dados originais, confirmação explícita e rollback. Só usa `run-as` de uma aplicação debug num dispositivo ADB autorizado; não usa root nem serviços externos. O caminho de assinatura no novo CI é explícito e partilhado com a workspace; após a publicação uma ausência da chave bloqueia o workflow. Ver [procedimento](MIGRACAO-V01.md).
