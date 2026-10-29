# História e organização — v0.3.0

A v0.3.0 parte da v0.2.0 publicada, sem alterar o esquema Room 2, os identificadores das obras ou os marcadores de atualização idempotente do catálogo. Não elimina obras: remove as referências sem ficheiro da apresentação da Biblioteca pessoal. A eliminação física quebraria notas, favoritos e coleções existentes. O catálogo continua acessível nas categorias e em Investigar.

## Conteúdo editorial

`data/HistoricalEncyclopedia.kt` contém 17 artigos offline: seis áreas existentes e onze tradições/perspetivas, incluindo ateísmo/agnosticismo. São cerca de 6 000 palavras originais, divididas em secções, com datas/lugares, personagens, conceitos, relações e fontes numeradas localmente. `domain/History.kt` define os modelos e a pesquisa sem acentos. O Repository fornece os artigos ao ViewModel; a interface não escreve conteúdo histórico na base pessoal.

Cada secção identifica os números das fontes usadas. As ligações abrem no navegador externo; a aplicação mantém-se sem permissão de Internet. Os textos da Sophia são redação editorial, não capítulos ou excertos apresentados como sendo de livros antigos. Não se reproduzem artigos completos de terceiros.

Critérios: separar narrativa sagrada e evidência histórica; indicar datas aproximadas/tradicionais; não tratar atribuições textuais como autoria demonstrada; distinguir fontes institucionais confessionais de estudos históricos. Não se afirma que os artigos esgotam todas as épocas, escolas e comunidades. A expansão futura exige revisão documental e fontes para novas afirmações. A existência histórica de uma prática não certifica eficácia terapêutica ou afirmações sobrenaturais.

As fontes incluem Stanford Encyclopedia of Philosophy, Metropolitan Museum, Columbia University, St Andrews, PBS, enciclopédias históricas, estudos sobre Ficino e instituições da tradição espírita identificadas como tais. Os arquivos Sacred Text Archive/Sanskrit Documents servem de referências bibliográficas; não autorizam automaticamente a reprodução de uma edição.

## Biblioteca e capas

A Biblioteca mostra os ficheiros locais que correspondem aos filtros. Cada ficheiro abre o leitor e oferece acesso aos detalhes/favoritos/notas. Importar continua a permitir associar a uma referência existente ou criar uma obra pessoal. Referências e artigos não são mostrados como livros completos. Não são fornecidos livros integrais nesta entrega.

Capas de referência: composição tipográfica original da Sophia, com aviso «Capa ilustrativa». Não se inventa uma capa histórica nem se descarrega arte de editoras. EPUB: imagem declarada como `cover-image` no OPF3 ou referenciada por `meta name=cover` no OPF2. PDF: primeira página real. TXT, EPUB sem capa ou imagem incompatível: composição tipográfica. O símbolo é um elemento de orientação, não uma representação exclusiva ou universal de uma tradição.

A extração respeita os caminhos locais/limites já aplicados ao EPUB; não lê imagens remotas, SVG ativo ou recursos fora do pacote. Imagens raster são verificadas pelos limites dimensionais e descodificadas com amostragem. Miniaturas são derivadas em cache privada; podem ser recriadas a partir do ficheiro original offline. Não há colunas Room novas. Falhas de capa não bloqueiam a leitura. A seleção manual de imagem fica por implementar.

## Atualização e validação

A versão Android passa a `versionCode=3`, `versionName=0.3.0`. O workflow valida explicitamente o certificado SHA-256 usado na v0.2.0 antes de permitir a entrega. O percurso de atualização no emulador conserva tabelas Room, ficheiros, posição, marcadores, notas, favoritos, coleções e preferências através de `adb install -r`, sem desinstalar.

Consultar [TESTES-v0.3.0.md](TESTES-v0.3.0.md) para resultados efetivamente executados e limitações.
