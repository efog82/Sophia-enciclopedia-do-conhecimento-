# Validação — Sophia v0.1.0

## Plano executável

- 9 testes unitários de domínio: pesquisa sem acentos, pesquisa combinada, classificação, filtros, favoritos, vazio e validação de notas.
- 7 testes instrumentados de dados: catálogo idempotente sem ficheiros, favoritos após reabertura, CRUD de notas e preservação do texto, notas após remoção da obra, integridade da tradução/edição, progresso associado a ficheiro e persistência do tema.
- Percurso instrumentado Compose: cinco áreas, pesquisa, detalhes/favoritos, recriação da Activity, criar/editar/eliminar nota com confirmação, cancelamento, rascunho após recriação, tema e capturas reais do emulador.
- Compilação APK debug, Android Lint e validação da assinatura; emulador com Wi-Fi e dados móveis desativados.

## Estado

Ainda em execução. Este documento será atualizado com os resultados reais e a ligação ao workflow antes da entrega. A existência dos testes e uma compilação bem-sucedida, isoladamente, não provam o seu sucesso no Android.

## Limites da validação

O assistente IA não tem APIs nesta fase; não existe teste de geração nem integração simulada. Os testes de leitura de progresso usam apenas entidades de teste numa base isolada, não representam um leitor funcional. Não existe esquema anterior para testar migrações. Testes em emulador não substituem validação num telefone físico nem cobertura de todos os fabricantes/tamanhos de ecrã.
