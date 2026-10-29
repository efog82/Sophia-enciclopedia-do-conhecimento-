# Sophia — Norma editorial histórica v0.4.0 (proposta)

## Finalidade
A Sophia apresenta história e conhecimento das religiões, tradições espirituais, correntes místicas e perspetivas não religiosas de modo acessível, documentado, profundo e imparcial. O objetivo não é apologética nem refutação religiosa.

## Estrutura obrigatória por tradição
1. **Origens e contexto**: espaço, sociedade, culturas anteriores, limites da documentação.
2. **Cronologia**: etapas ordenadas, datas aproximadas claramente assinaladas, mudanças e consequências.
3. **Pessoas**: biografias contextualizadas; distinguir personagens historicamente documentadas de personagens da memória religiosa.
4. **Textos fundamentais**: composição, língua, transmissão, edições, autoria atribuída versus apurada.
5. **Ensinamentos e práticas**: descrever com fidelidade a diversidade interna, sem confundir convicção com prova independente.
6. **Movimentos e expansão**: ramos, diferenças, geografia, relações com poder e instituições, contemporaneidade.
7. **Fontes e discussão**: bibliografia por afirmação ou secção, divergências significativas e incerteza.

## Política de evidência
- **História documentada:** identificar documentos, inscrições, publicações, testemunhos independentes ou bibliografia académica.
- **Memória/tradição:** explicar «segundo a tradição», sem transformar narrativa sagrada numa biografia documental.
- **Doutrina:** «a corrente X ensina ...», sem afirmar ou desmentir automaticamente a realidade metafísica.
- **Hipótese:** expor plausibilidade, alternativas e grau de consenso; não inventar precisão.
- **Fonte confessional:** útil para autodescrição e história institucional; comparar com historiografia independente para afirmações controvertidas.
- **Fontes:** URLs verificáveis, título, entidade, data/edição quando disponível. Evitar links quebrados e atribuições fictícias. Não alegar verificação de uma fonte ainda não examinada.
- Não repetir a advertência sobre prova científica em todas as secções: colocar uma nota metodológica curta no início do artigo e discutir evidência apenas quando pertinente.

## Qualidade narrativa
A cronologia deve ser contínua, incluir transições e explicar causas, não apenas enumerar nomes. Evitar repetições e saltos geográficos que sugiram falsamente o desaparecimento de outras comunidades. Explicar diversidade e omissões editoriais. Usar português europeu, precisão terminológica e respeito por todas as convicções. Contextualizar escravidão, violência, desigualdades ou discriminação sem branqueamento nem caricatura.

## Dados estruturados futuros
Proposta: `id`, `traditionId`, `kind` (origens|cronologia|personagem|obra|doutrina|movimento|fontes), `title`, `body`, `periodStart`, `periodEnd`, `datePrecision`, `places`, `personIds`, `workIds`, `sourceIds`, `evidenceStatus`, `relatedIds`. Manter IDs de artigos históricos e compatibilidade com notas, favoritos e arquivos pessoais. Na fase piloto, guardar conteúdo editorial em ficheiros separados; só integrar na UI após revisão e testes.

## Porta de publicação
1. Revisão editorial humana e verificação das afirmações históricas e fontes.
2. Revisão de imparcialidade e cobertura cronológica.
3. Testes de integridade de IDs, ligações e conteúdo offline.
4. Testes de regressão, atualização sem perdas e leitura em dispositivos.
5. Só então integrar e publicar nova versão.

**Estado:** especificação editorial, não implementação de interface nem aprovação factual de todos os rascunhos.
