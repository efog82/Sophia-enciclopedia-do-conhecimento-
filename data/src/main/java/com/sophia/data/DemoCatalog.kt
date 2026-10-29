package com.sophia.data

import androidx.room.withTransaction

/** Only bibliographic references. No editions, translations, files or reading progress are invented. */
object DemoCatalog {
    suspend fun seed(db:SophiaDatabase){db.withTransaction {
        val dao=db.catalog();if(dao.metadata("demo_catalog_version")=="1")return@withTransaction
        dao.insertTraditions(listOf(
            TraditionEntity("spiritism","Espiritismo","Tradição espírita; atribuições mediúnicas identificadas como tais."),
            TraditionEntity("hermeticism","Hermetismo","Estudo das tradições herméticas e da sua receção histórica."),
            TraditionEntity("christianity","Cristianismo","Diversidade de tradições, textos e interpretações cristãs."),
            TraditionEntity("buddhism","Budismo","Diversidade de escolas e tradições budistas.")
        ))
        dao.insertCategories(listOf(CategoryEntity("spirituality","Espiritualidade"),CategoryEntity("religions","Religiões"),CategoryEntity("philosophy","Filosofia"),CategoryEntity("mysticism","Misticismo"),CategoryEntity("esotericism","Esoterismo"),CategoryEntity("occultism","Ocultismo")))
        dao.insertPeople(listOf(PersonEntity("kardec","Allan Kardec"),PersonEntity("xavier","Francisco Cândido Xavier (Chico Xavier)"),PersonEntity("andre-luiz","André Luiz","ATTRIBUTED_SPIRIT_AUTHOR"),PersonEntity("divaldo","Divaldo Pereira Franco"),PersonEntity("joanna","Joanna de Ângelis","ATTRIBUTED_SPIRIT_AUTHOR"),PersonEntity("hermes","Hermes Trismegisto","TRADITIONAL_FIGURE"),PersonEntity("buddha","Buda","TRADITIONAL_ATTRIBUTION")))
        val now=System.currentTimeMillis()
        val rights="Referência de demonstração. Edição e tradução não selecionadas; direitos de ficheiros não verificados. Nenhum texto integral ou capa incluído."
        dao.insertWorks(listOf(
            WorkEntity("spirits","O Livro dos Espíritos","Referência para o estudo dos fundamentos da tradição espírita.",originalYear=1857,sourceUrl="https://www.febnet.org.br/portal/2025/04/18/lancamento-de-o-livro-dos-espiritos-18-de-abril-de-1857/",sourceLabel="Federação Espírita Brasileira",metadataNote=rights+" O ano refere-se à publicação original, não a uma edição incluída.",addedAt=now+6),
            WorkEntity("nosso-lar","Nosso Lar","Narrativa da literatura espírita atribuída a André Luiz e psicografada por Chico Xavier; a atribuição é apresentada no contexto dessa tradição.",sourceUrl="https://books.google.com/books/about/Nosso_Lar.html?id=-1460gEACAAJ",sourceLabel="Registo bibliográfico Google Books",metadataNote=rights,addedAt=now+5),
            WorkEntity("jesus-atualidade","Jesus e Atualidade","Referência de estudo associada à série psicológica atribuída a Joanna de Ângelis, pela psicografia de Divaldo Franco.",sourceUrl="https://livraria.joanna.org.br/jesus-e-atualidade-2022-04-07-20-12-20",sourceLabel="Catálogo da Livraria Joanna de Ângelis",metadataNote=rights,addedAt=now+4),
            WorkEntity("corpus-hermeticum","Corpus Hermeticum","Conjunto de tratados da tradição hermética. A associação a Hermes Trismegisto é uma atribuição tradicional, não autoria histórica comprovada.",sourceUrl="https://www.cambridge.org/core/books/hermetica/A2779DE70B173114CC8669BEB3CF507D",sourceLabel="Cambridge University Press · referência sobre os Hermetica",metadataNote=rights+" A fonte trata de uma tradução moderna; essa edição não está incluída no catálogo.",addedAt=now+3),
            WorkEntity("bible","Bíblia","Designação coletiva de textos fundamentais das tradições cristãs. Os cânones, edições e traduções variam; nenhuma edição específica é representada aqui.",sourceUrl="https://www.vatican.va/archive/bible/index_po.htm",sourceLabel="Arquivo do Vaticano · Bíblia",metadataNote=rights+" Não existe atribuição a um autor único.",addedAt=now+2),
            WorkEntity("dhammapada","Dhammapada","Coletânea de versos da tradição budista, com ensinamentos tradicionalmente atribuídos ao Buda. Nenhuma tradução é reproduzida.",sourceUrl="https://accesstoinsight.org/tipitaka/kn/dhp/",sourceLabel="Access to Insight · referência ao Dhammapada",metadataNote=rights,addedAt=now+1)
        ))
        dao.insertCredits(listOf(WorkContributorEntity("spirits","kardec","AUTHOR"),WorkContributorEntity("nosso-lar","xavier","PSYCHOGRAPHER"),WorkContributorEntity("nosso-lar","andre-luiz","ATTRIBUTED_AUTHOR"),WorkContributorEntity("jesus-atualidade","divaldo","PSYCHOGRAPHER"),WorkContributorEntity("jesus-atualidade","joanna","ATTRIBUTED_AUTHOR"),WorkContributorEntity("corpus-hermeticum","hermes","TRADITIONAL_ATTRIBUTION"),WorkContributorEntity("dhammapada","buddha","TRADITIONAL_ATTRIBUTION")))
        dao.insertWorkTraditions(listOf(WorkTraditionEntity("spirits","spiritism"),WorkTraditionEntity("nosso-lar","spiritism"),WorkTraditionEntity("jesus-atualidade","spiritism"),WorkTraditionEntity("corpus-hermeticum","hermeticism"),WorkTraditionEntity("bible","christianity"),WorkTraditionEntity("dhammapada","buddhism")))
        dao.insertWorkCategories(listOf(WorkCategoryEntity("spirits","spirituality"),WorkCategoryEntity("nosso-lar","spirituality"),WorkCategoryEntity("jesus-atualidade","spirituality"),WorkCategoryEntity("jesus-atualidade","philosophy"),WorkCategoryEntity("corpus-hermeticum","philosophy"),WorkCategoryEntity("corpus-hermeticum","esotericism"),WorkCategoryEntity("corpus-hermeticum","mysticism"),WorkCategoryEntity("bible","religions"),WorkCategoryEntity("dhammapada","religions"),WorkCategoryEntity("dhammapada","philosophy")))
        dao.saveMetadata(CatalogMetadataEntity("demo_catalog_version","1"))
    }}
}
