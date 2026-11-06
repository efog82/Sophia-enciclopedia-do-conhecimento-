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
