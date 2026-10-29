package com.sophia.data

import androidx.room.withTransaction

/** Additive versioned catalogue: no REPLACE, no deletion of personal records. */
object ExpandedCatalog {
    const val VERSION="2"
    private const val HINDU="https://sacred-texts.com/categories/hinduism"
    private const val SANSKRIT="https://sanskritdocuments.org/veda/"
    private const val CLASSICS="https://classics.mit.edu/Browse/index.html"
    private const val FEB="https://www.fergs.org.br/fenavida"
    private data class Reference(val id:String,val title:String,val tradition:String,val summary:String,val source:String,val person:String?=null,val name:String?=null,val role:String="AUTHOR",val category:String="religions")
    private data class Intro(val id:String,val name:String,val description:String,val history:String,val concepts:String,val source:String)
    private val introductions=listOf(
        Intro("spiritism","Espiritismo","Estudo da tradição espírita e da sua literatura.","Movimento sistematizado por Allan Kardec em França no século XIX, com diferentes desenvolvimentos, incluindo no Brasil. A sobrevivência do espírito e a mediunidade são ensinamentos desta tradição, não conclusões científicas estabelecidas.","Reencarnação; mediunidade; responsabilidade moral; caridade",FEB),
        Intro("buddhism","Budismo","Escolas e textos das tradições budistas.","Tradições originadas no sul da Ásia a partir dos ensinamentos associados ao Buda. Os cânones e as interpretações diferem entre Theravāda, Mahāyāna e outras escolas.","Dukkha; impermanência; caminho óctuplo; compaixão","https://sacred-texts.com/categories/buddhism"),
        Intro("christianity","Cristianismo","Textos e diversidade das tradições cristãs.","Família de tradições surgidas no contexto judaico do primeiro século, centradas em Jesus. Existem diferenças históricas de cânone, doutrina e prática entre comunidades.","Evangelhos; graça; amor ao próximo; contemplação","https://www.vatican.va/archive/bible/index_po.htm"),
        Intro("hermeticism","Hermetismo","Literatura hermética e sua receção histórica.","Textos greco-egípcios da Antiguidade tardia atribuídos a Hermes Trismegisto; a sua receção medieval e renascentista tem contextos próprios. A atribuição tradicional não identifica um autor histórico comprovado.","Nous; cosmos; conhecimento; correspondências","https://www.cambridge.org/core/books/hermetica/A2779DE70B173114CC8669BEB3CF507D"),
        Intro("hinduism","Hinduísmo e tradições védicas","Vedas, textos filosóficos, épicos e tradições de yoga.","Conjunto diverso de tradições do sul da Ásia, com transmissão oral védica e desenvolvimentos filosóficos e devocionais. Os Vedas têm estatuto sagrado nestas tradições; tal estatuto não constitui uma afirmação científica.","Dharma; karma; ātman; brahman; moksha; yoga",HINDU+"\n"+SANSKRIT),
        Intro("judaism","Judaísmo e Cabala","Textos judaicos e correntes de interpretação mística.","Tradições formadas em diferentes contextos do antigo Israel e da história judaica. A Cabala reúne desenvolvimentos místicos, sobretudo medievais, e não representa todo o judaísmo.","Torá; aliança; mitzvot; interpretação; sefirot","https://sacred-texts.com/jud/"),
        Intro("islam","Islão e sufismo","Alcorão e tradições de espiritualidade islâmica.","Religião surgida na Península Arábica no século VII. O Alcorão é entendido como revelação na tradição islâmica; o sufismo designa diversas correntes de prática espiritual, não uma escola única.","Tawhid; salat; ética; dhikr; ihsan","https://sacred-texts.com/isl/"),
        Intro("taoism","Taoismo","Textos taoistas e práticas em contextos chineses.","Tradições chinesas com dimensões filosóficas, religiosas e rituais. As atribuições do Dao De Jing e do Zhuangzi devem distinguir-se da história de composição dos textos.","Dao; de; wu wei; ziran","https://sacred-texts.com/tao/"),
        Intro("ancient-philosophy","Filosofia antiga","Filosofia grega e romana em perspetiva histórica.","Diversidade de escolas da Antiguidade, incluindo platonismo, aristotelismo e estoicismo. São propostas filosóficas em debate, não uma religião única nem consenso científico.","Ética; virtude; razão; conhecimento; vida boa",CLASSICS),
        Intro("confucianism","Confucionismo","Tradições de ética e aprendizagem na China.","Correntes associadas a Confúcio e aos seus intérpretes, desenvolvidas em diferentes contextos históricos da Ásia oriental.","Ren; li; aprendizagem; responsabilidade","https://sacred-texts.com/cfu/")
    )
    private val references=listOf(
        Reference("rigveda","Rigveda","hinduism","Coleção védica de hinos transmitidos em sânscrito védico. Autoria coletiva, sem atribuição a um autor histórico único.",HINDU),
        Reference("samaveda","Samaveda","hinduism","Coleção védica relacionada com o canto ritual, com diferentes tradições de transmissão.",HINDU),
        Reference("yajurveda","Yajurveda","hinduism","Textos e fórmulas rituais védicos, com tradições conhecidas como Yajurveda Branco e Negro.",HINDU),
        Reference("atharvaveda","Atharvaveda","hinduism","Coleção védica com hinos, fórmulas e reflexões de diferentes contextos da vida e do ritual.",HINDU),
        Reference("upanishads","Upanishads","hinduism","Designação coletiva de textos filosóficos com datas e tradições diversas; não é um livro de autor único.","https://sanskritdocuments.org/upanishhat/",category="philosophy"),
        Reference("bhagavad-gita","Bhagavad Gita","hinduism","Diálogo inserido no Mahabharata, estudado em diferentes escolas hindus.",HINDU,"vyasa","Vyāsa","TRADITIONAL_ATTRIBUTION","philosophy"),
        Reference("ramayana","Ramayana","hinduism","Tradição épica de que existem várias versões; a referência não escolhe uma tradução ou recensão.",HINDU,"valmiki","Vālmīki","TRADITIONAL_ATTRIBUTION"),
        Reference("mahabharata","Mahabharata","hinduism","Conjunto épico de composição e transmissão complexas, tradicionalmente associado a Vyāsa.",HINDU,"vyasa","Vyāsa","TRADITIONAL_ATTRIBUTION"),
        Reference("yoga-sutras","Yoga Sutras","hinduism","Texto de referência do yoga clássico; autoria tradicionalmente atribuída a Patañjali.","https://sanskritdocuments.org/yoga/","patanjali","Patañjali","TRADITIONAL_ATTRIBUTION","philosophy"),
        Reference("mediums","O Livro dos Médiuns","spiritism","Referência para o estudo da mediunidade segundo a doutrina espírita.",FEB,"kardec","Allan Kardec"),
        Reference("gospel-spiritism","O Evangelho segundo o Espiritismo","spiritism","Interpretação de ensinamentos cristãos no contexto espírita.",FEB,"kardec","Allan Kardec"),
        Reference("heaven-hell","O Céu e o Inferno","spiritism","Reflexões e relatos da literatura espírita sobre justiça e vida após a morte, apresentados como ensinamentos dessa tradição.",FEB,"kardec","Allan Kardec"),
        Reference("genesis-spiritism","A Génese","spiritism","Obra de Kardec sobre temas cosmológicos e religiosos no contexto intelectual do século XIX; não é uma fonte científica atual.",FEB,"kardec","Allan Kardec"),
        Reference("jesus-home","Jesus no Lar","spiritism","Literatura espírita psicografada por Chico Xavier e atribuída a Neio Lúcio.",FEB,"neio-lucio","Neio Lúcio","ATTRIBUTED_AUTHOR"),
        Reference("family-constellation","Constelação Familiar","spiritism","Obra da literatura espírita psicografada por Divaldo Franco e atribuída a Joanna de Ângelis. Não constitui orientação clínica.",FEB,"joanna","Joanna de Ângelis","ATTRIBUTED_AUTHOR"),
        Reference("lotus-sutra","Sutra do Lótus","buddhism","Texto de referência de diversas escolas Mahāyāna; nenhuma tradução específica é incluída.","https://sacred-texts.com/categories/buddhism"),
        Reference("heart-sutra","Sutra do Coração","buddhism","Texto da literatura Prajñāpāramitā, com várias tradições textuais e interpretativas.","https://sacred-texts.com/categories/buddhism"),
        Reference("diamond-sutra","Sutra do Diamante","buddhism","Texto Mahāyāna estudado em diferentes comunidades e tradições de tradução.","https://sacred-texts.com/categories/buddhism"),
        Reference("confessions","Confissões","christianity","Reflexão autobiográfica, filosófica e religiosa de Agostinho de Hipona.","https://www.newadvent.org/fathers/1101.htm","augustine","Agostinho de Hipona"),
        Reference("imitation-christ","Imitação de Cristo","christianity","Texto de espiritualidade cristã tradicionalmente associado a Tomás de Kempis.","https://sacred-texts.com/chr/","kempis","Tomás de Kempis","TRADITIONAL_ATTRIBUTION"),
        Reference("asclepius","Asclepius","hermeticism","Diálogo da tradição hermética, associado tradicionalmente a Hermes Trismegisto.","https://sacred-texts.com/chr/herm/","hermes","Hermes Trismegisto","TRADITIONAL_ATTRIBUTION","esotericism"),
        Reference("emerald-tablet","Tábua de Esmeralda","hermeticism","Texto de receção hermética e alquímica, com atribuição tradicional a Hermes; a história textual exige distinguir versões.","https://sacred-texts.com/alc/","hermes","Hermes Trismegisto","TRADITIONAL_ATTRIBUTION","esotericism"),
        Reference("tanakh","Tanakh","judaism","Conjunto de textos da Bíblia hebraica; nenhuma edição ou tradução específica é selecionada.","https://sacred-texts.com/jud/"),
        Reference("sefer-yetzirah","Sefer Yetzirah","judaism","Texto da tradição mística judaica, com diferentes recensões e atribuições; autoria não verificada.","https://sacred-texts.com/jud/",category="mysticism"),
        Reference("zohar","Zohar","judaism","Coleção central na história da Cabala. As atribuições tradicionais devem ser distinguidas da história medieval de composição.","https://sacred-texts.com/jud/",category="mysticism"),
        Reference("quran","Alcorão","islam","Texto central do Islão, entendido como revelação na respetiva tradição; não se atribui aqui um autor humano único.","https://sacred-texts.com/isl/"),
        Reference("masnavi","Masnavi","islam","Obra poética associada a Rumi e à literatura sufista.","https://sacred-texts.com/isl/","rumi","Jalāl al-Dīn Rūmī",category="mysticism"),
        Reference("dao-de-jing","Dao De Jing","taoism","Texto taoista tradicionalmente associado a Laozi; nenhuma tradução específica é escolhida.","https://sacred-texts.com/tao/","laozi","Laozi","TRADITIONAL_ATTRIBUTION","philosophy"),
        Reference("zhuangzi","Zhuangzi","taoism","Coleção taoista associada a Zhuang Zhou e a autores posteriores; a atribuição não implica autoria única de todo o texto.","https://sacred-texts.com/tao/","zhuangzhou","Zhuang Zhou","TRADITIONAL_ATTRIBUTION","philosophy"),
        Reference("republic","A República","ancient-philosophy","Diálogo de Platão sobre justiça, educação e organização da comunidade.",CLASSICS,"plato","Platão",category="philosophy"),
        Reference("nicomachean-ethics","Ética a Nicómaco","ancient-philosophy","Tratado associado a Aristóteles sobre virtude e vida ética.",CLASSICS,"aristotle","Aristóteles",category="philosophy"),
        Reference("meditations","Meditações","ancient-philosophy","Apontamentos de Marco Aurélio no contexto da tradição estoica.",CLASSICS,"marcus","Marco Aurélio",category="philosophy"),
        Reference("enchiridion","Manual de Epicteto","ancient-philosophy","Ensinamentos de Epicteto recolhidos por Arriano; a edição e a tradução permanecem por selecionar.",CLASSICS,"epictetus","Epicteto",category="philosophy"),
        Reference("analects","Analectos","confucianism","Coleção de ensinamentos associados a Confúcio, transmitidos e compilados pelos seus seguidores.","https://sacred-texts.com/cfu/","confucius","Confúcio","TRADITIONAL_ATTRIBUTION","philosophy")
    )
    suspend fun seed(db:SophiaDatabase)=db.withTransaction{
        val dao=db.catalog();if(dao.metadata("universal_catalog_version")==VERSION)return@withTransaction
        introductions.forEach{t->dao.insertTraditions(listOf(TraditionEntity(t.id,t.name,t.description,t.history,t.concepts,t.source)));db.library().updateTradition(t.id,t.history,t.concepts,t.source)}
        val note="Referência bibliográfica, sem texto integral. Edição, tradução e direitos não verificados, salvo campos explicitamente documentados. As fontes não concedem automaticamente autorização de reprodução."
        val now=System.currentTimeMillis()
        references.forEachIndexed{i,r->
            dao.insertWorks(listOf(WorkEntity(r.id,r.title,r.summary,sourceUrl=r.source,sourceLabel="Fonte bibliográfica identificada",metadataNote=note,addedAt=now+i,demo=false)))
            dao.insertWorkTraditions(listOf(WorkTraditionEntity(r.id,r.tradition)))
            dao.insertWorkCategories(listOf(WorkCategoryEntity(r.id,r.category)))
            if(r.person!=null){dao.insertPeople(listOf(PersonEntity(r.person,r.name!!,if(r.role=="TRADITIONAL_ATTRIBUTION")"TRADITIONAL_ATTRIBUTION" else if(r.role=="ATTRIBUTED_AUTHOR")"ATTRIBUTED_SPIRIT_AUTHOR" else "PERSON")));dao.insertCredits(listOf(WorkContributorEntity(r.id,r.person,r.role)))}
        }
        dao.insertCredits(listOf(WorkContributorEntity("jesus-home","xavier","PSYCHOGRAPHER"),WorkContributorEntity("family-constellation","divaldo","PSYCHOGRAPHER")))
        dao.insertPeople(listOf(PersonEntity("arriano","Arriano")));dao.insertCredits(listOf(WorkContributorEntity("enchiridion","arriano","COMPILER")))
        // Bibliographic records only. No downloads and no assertion of edition rights in Portugal.
        data class Historical(val work:String,val translator:String,val name:String,val year:Int,val label:String)
        listOf(Historical("rigveda","griffith","Ralph T. H. Griffith",1896,"The Rig-Veda · referência histórica"),Historical("samaveda","griffith","Ralph T. H. Griffith",1895,"The Sama-Veda · referência histórica"),Historical("yajurveda","keith","Arthur Berriedale Keith",1914,"Taittiriya Sanhita · Yajurveda Negro"),Historical("atharvaveda","bloomfield","Maurice Bloomfield",1897,"Hymns of the Atharva-Veda · seleção, não texto integral")).forEach{h->
            dao.insertPeople(listOf(PersonEntity(h.translator,h.name)))
            dao.insertTranslation(TranslationEntity("translation-${h.work}",h.work,"sa","en","Referência histórica, sem ficheiro incluído; verificação dos direitos da edição concreta pendente.",HINDU))
            dao.insertEdition(EditionEntity("historical-${h.work}",h.work,"translation-${h.work}",h.label,publicationYear=h.year,language="en",sourceUrl=HINDU))
            dao.insertEditionCredit(EditionContributorEntity("historical-${h.work}",h.translator,"TRANSLATOR"))
        }
        dao.saveMetadata(CatalogMetadataEntity("universal_catalog_version",VERSION))
    }
}
