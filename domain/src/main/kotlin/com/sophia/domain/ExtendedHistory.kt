package com.sophia.domain

/**
 * OPTIONAL v0.4.0 pilot supplement.
 * The v0.3.0 article IDs and contents remain unchanged. No Room migration is needed.
 * Draft narrative: further historical and source review required before publication.
 */
data class ExtendedHistorySection(val heading: String, val narrative: String)

object ExtendedHistory {
    val pilot: Map<String, List<ExtendedHistorySection>> = mapOf(
        "spiritism" to listOf(
            ExtendedHistorySection("Contexto europeu e formação",
                "O Espiritismo de Allan Kardec tomou forma em França na década de 1850, num ambiente marcado pela circulação de relatos de mesas girantes, magnetismo e manifestações atribuídas a agentes invisíveis. Hippolyte Léon Denizard Rivail (1804–1869), educador que adotou o nome Allan Kardec, organizou materiais recolhidos em reuniões e apresentou a sua interpretação em publicações sucessivas. A existência dessas reuniões e obras pertence ao registo histórico; a atribuição das comunicações a Espíritos faz parte da compreensão doutrinária dos participantes."),
            ExtendedHistorySection("A codificação entre 1857 e 1868",
                "A sequência editorial ajuda a acompanhar o desenvolvimento da doutrina: O Livro dos Espíritos (1857), O Livro dos Médiuns (1861), O Evangelho segundo o Espiritismo (1864), O Céu e o Inferno (1865) e A Génese (1868). Em 1858 foram fundadas a Revue Spirite e a Sociedade Parisiense de Estudos Espíritas. A Revista Espírita mostra debates, relatos e respostas que acompanharam a construção do movimento, pelo que a sua leitura acrescenta contexto às cinco obras fundamentais."),
            ExtendedHistorySection("Depois de Kardec: expansão e diversidade",
                "A morte de Kardec em Paris, em 1869, não terminou a circulação das suas obras. Traduções, grupos locais, jornais e associações criaram percursos distintos em França, Portugal, Brasil e noutros países. A Federação Espírita Brasileira, fundada em 1884, ganhou importância no Brasil, mas não representa todas as organizações espíritas. No século XX, Chico Xavier e Divaldo Franco tornaram-se figuras de especial influência na literatura e divulgação brasileiras; a evolução europeia e portuguesa requer tratamento próprio."),
            ExtendedHistorySection("Ensinamentos e métodos",
                "A doutrina kardeciana articula sobrevivência espiritual, reencarnação, comunicabilidade dos Espíritos, progresso moral e caridade. As reuniões de estudo, práticas mediúnicas e atividades assistenciais assumiram formas diferentes entre grupos. Para compreender a tradição é necessário apresentar os argumentos e o vocabulário dos seus autores sem confundir a descrição de uma crença com a sua comprovação independente.")
        ),
        "judaism" to listOf(
            ExtendedHistorySection("Origens no antigo Levante",
                "As tradições israelitas desenvolveram-se no antigo Levante ao longo de vários séculos. O relato bíblico de patriarcas, Moisés e êxodo tem significado central na memória religiosa, mas não constitui por si só confirmação arqueológica dos episódios narrados. A história dos reinos de Israel e Judá e a transformação gradual das práticas de culto ajudam a compreender a formação das identidades judaicas."),
            ExtendedHistorySection("Exílio, período persa e Segundo Templo",
                "A conquista assíria do reino setentrional de Israel no século VIII a.C. e a destruição babilónica de Jerusalém e do Primeiro Templo em 587/586 a.C. marcaram momentos distintos. Sob domínio persa, grupos de exilados regressaram e o templo foi reconstruído. O judaísmo do Segundo Templo conheceu instituições, textos e correntes diversas. Em 70 d.C. os romanos destruíram o Segundo Templo, desencadeando transformações profundas sem interromper a continuidade das comunidades judaicas."),
            ExtendedHistorySection("Literatura rabínica e diásporas",
                "A Mishná e os Talmudes preservam debates jurídicos, interpretações e tradições de diferentes épocas. O desenvolvimento do judaísmo rabínico não ocorreu num único momento nem apagou todas as outras experiências comunitárias. Ao longo da Idade Média, comunidades judaicas viveram em vários territórios europeus, norte-africanos e médio-orientais, com tradições intelectuais, litúrgicas e linguísticas diversas."),
            ExtendedHistorySection("Cabala e interpretações místicas",
                "A Cabala compreende correntes místicas judaicas, sem equivaler ao judaísmo no seu conjunto. O Zohar circulou na Península Ibérica medieval, embora a tradição o atribua a uma figura de época anterior. No século XVI, Isaac Luria ensinou em Safed; discípulos, especialmente Chaim Vital, transmitiram muitos dos ensinamentos associados ao seu nome. É importante distinguir datas de composição, atribuições religiosas e história dos manuscritos."),
            ExtendedHistorySection("Épocas moderna e contemporânea",
                "A história judaica posterior inclui transformações religiosas, emancipação, migrações, movimentos reformistas e tradicionais, perseguições antissemitas, o Holocausto e a fundação do Estado de Israel em 1948. Cada processo exige uma abordagem própria, fontes diferenciadas e atenção à pluralidade de comunidades e posições. O judaísmo contemporâneo não pode ser reduzido a uma escola única ou a uma única região.")
        )
    )
}
