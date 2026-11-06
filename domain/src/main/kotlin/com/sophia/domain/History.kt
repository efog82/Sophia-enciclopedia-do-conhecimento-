package com.sophia.domain

/** Editorial history is bundled offline; it never changes a user's Room records. */
data class HistorySource(val title:String,val url:String,val kind:String="Estudo histórico")
data class HistorySection(val title:String,val text:String,val sources:List<Int>)
data class HistoryFigure(val name:String,val dates:String,val text:String)
data class HistoryEvent(val period:String,val title:String,val text:String)
data class HistoryArticle(
    val id:String,
    val title:String,
    val symbol:String,
    val description:String,
    val area:Boolean=false,
    val sections:List<HistorySection>,
    val sources:List<HistorySource>,
    val related:List<String>,
    val concepts:String="",
    val figures:List<HistoryFigure> = emptyList(),
    val timeline:List<HistoryEvent> = emptyList(),
    val practices:List<HistorySection> = emptyList()
) {
    fun matches(query:String):Boolean {
        val haystack=CatalogSearch.normalized(title+" "+description+" "+concepts+" "+sections.joinToString(" "){it.title+" "+it.text})
        return CatalogSearch.normalized(query).split(Regex("\\s+")).filter{it.isNotBlank()}.all{it in haystack}
    }
}

/** Palavras distintivas de um nome de pessoa (5+ letras): servem para ligar a figura a textos e a autoria de livros. */
fun HistoryFigure.matchTokens():List<String> =
    CatalogSearch.normalized(name).split(Regex("[^a-z]+")).filter{it.length>=5}

/** A pessoa é mencionada neste texto? Pelo nome completo, ou por uma palavra distintiva do nome como palavra inteira. */
fun HistoryFigure.isMentionedIn(text:String):Boolean {
    val folded=CatalogSearch.normalized(text)
    if(folded.contains(CatalogSearch.normalized(name)))return true
    return matchTokens().any{token->Regex("(^|[^a-z])"+Regex.escape(token)+"([^a-z]|$)").containsMatchIn(folded)}
}

/** Secções do artigo que mencionam a pessoa — o material do «percurso» na página da pessoa. */
fun HistoryArticle.sectionsMentioning(figure:HistoryFigure):List<HistorySection> =
    sections.filter{figure.isMentionedIn(it.title+" "+it.text)}

/** Marcos da linha do tempo que mencionam a pessoa. */
fun HistoryArticle.eventsMentioning(figure:HistoryFigure):List<HistoryEvent> =
    timeline.filter{figure.isMentionedIn(it.title+" "+it.text)}
