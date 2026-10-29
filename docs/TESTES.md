# Relatório de validação — Sophia v0.1.0 (fase 1)

## Ambiente e versão

- Aplicação: `com.sophia.enciclopedia`, versão `0.1.0`, código `1`.
- Android mínimo 8.0 / API 26; compilação e target API 35.
- Compilação remota em GitHub Actions, Ubuntu 24.04, JDK 17 e Gradle Wrapper 8.10.2. O ambiente local não tinha Android SDK instalado; não se afirma uma compilação Android local.
- Testes instrumentados num emulador Android 15 / API 35, x86_64, perfil Pixel 2, com Wi-Fi e dados móveis desativados.
- [Workflow de entrega](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/actions/runs/40879940666).
- Commit de código e testes: `c82ddf05f260040a9aa9bc5262f74951cabe0f31`. Documentação pode ter commits posteriores sem alterações no executável.

## Resultados

**Execução de entrega concluída com sucesso em 29/10/2026.** Os jobs de compilação, testes Android e publicação terminaram com sucesso. Foram confirmados 17 testes automatizados sem falhas, a instalação e o arranque do APK de distribuição e seis capturas reais do emulador. A recolha de capturas após a limpeza dos testes foi corrigida durante a validação.

| Verificação | Resultado confirmado |
|---|---|
| Testes unitários de domínio | 9 testes, 0 falhas, 0 erros |
| Room / DataStore no Android | 7 testes, 0 falhas, 0 erros |
| Percurso Compose no Android | 1 teste com vários fluxos, 0 falhas, 0 erros |
| Compilação APK debug e testes Android compilados | Sucesso |
| Android Lint app/data | Sem erros; existem avisos descritos abaixo |
| Integridade ZIP, hash e assinatura v2 do APK | Verificadas |
| Instalação do artefacto entregue, arranque e recolha de capturas | Sucesso: `adb install` → `Success`, arranque → `Status: ok`, XML de interface e 6 PNG recolhidos |

Os 17 testes acima são testes efetivamente executados, não simulações de serviços. Os XML JUnit, relatórios lint, capturas do emulador e dados da instalação são disponibilizados nos artefactos do workflow. A publicação da release depende de ambos os jobs de compilação e Android terminarem com sucesso.

## Cobertura funcional

**Domínio (9):** pesquisa sem distinção de acentos/maiúsculas; termos de título e autor combinados; metadados de classificação; interseção de filtros; exclusão de não favoritos; pesquisa vazia; título de nota obrigatório; limites de 160 caracteres no título e 50 000 no corpo; preservação do texto e indentação do corpo.

**Persistência (7):** inserção idempotente das seis referências sem ficheiros; favoritos após fechar/reabrir a base; criar/editar/reabrir/eliminar notas sem alterar a sua identidade/data de criação; conservar notas quando a associação à obra é removida; rejeitar uma edição com tradução de outra obra; progresso associado ao ficheiro e intervalo válido; tema após fechar/reabrir DataStore. São bases de teste isoladas, sem dados pessoais.

**Interface (1 percurso):** cinco áreas; pesquisa por autor; detalhes e favorito; favorito e filtros após recriar a Activity; pesquisa por tradição e filtro de autor sem resultados; estado inicial de investigação; indicação explícita de IA em desenvolvimento; validação de nota sem título; criação e edição de nota; conservação após recriação; cancelamento e confirmação da eliminação; rascunho após recriação e confirmação de descarte; tema claro/escuro persistente; categoria vazia e limpeza de filtros. Verifica também que a aplicação não tem permissão de Internet.

A instalação final usa o APK descarregado do job de distribuição, além do APK construído e instalado pelo executor de testes. As capturas são reais, não maquetes.

## Avisos e limites

- Lint sem erros: avisos de versões mais recentes de dependências/target SDK, compatibilidade de regras de backup em Android anterior a 12 e deteção do ícone monocromático na variante base. A cópia automática está desativada (`allowBackup=false`); existem exclusões explícitas para Android 12+ e ícone monocromático próprio em API 33+. Não se afirma ausência total de avisos.
- O compilador também assinala dois ícones Material antigos que têm variantes AutoMirrored. Não impedem compilação ou funcionamento em português.
- Não testado num telefone físico, em API 26 nem em todos os fabricantes/tamanhos de ecrã. A compatibilidade mínima declarada não equivale a teste físico em todas as versões.
- Não há versão anterior do esquema para testar uma migração: está guardado o esquema inicial Room versão 1, sem restauro destrutivo.
- Rascunhos usam o estado Android, não são um mecanismo de backup. Notas/favoritos guardados são locais; limpar dados ou desinstalar elimina-os.
- Não existem leitor, livros integrais, importação, pesquisa textual/semântica, IA/RAG, backup ou sincronização. Os registos de ficheiros/progresso usados nos testes não representam um leitor implementado. Nenhuma integração de IA foi simulada.

## Artefacto

[Descarregar APK debug](https://github.com/efog82/Sophia-enciclopedia-do-conhecimento-/releases/download/sophia-v0.1.0/Sophia-v0.1.0-debug.apk), assinado para instalação direta.

Tamanho: 16 842 094 bytes. SHA-256: `ba8fab5ffa40d5bb003b94f3feaf07a3ace226f47d2f99d3bd5972f4de0bd4d2`. O ficheiro descarregado foi comparado com o hash do workflow e com o digest do asset publicado no GitHub. A assinatura APK v2 foi validada por `apksigner` no CI; a integridade ZIP foi também verificada localmente.

 O APK, o `SHA256SUMS.txt` e os dados públicos do certificado estão na release. A assinatura debug não é uma chave de publicação da Play Store. Nenhuma keystore ou credencial foi incluída no código ou nos artefactos.

**Apenas a fase 1 foi implementada. A fase 2 não foi iniciada.**
