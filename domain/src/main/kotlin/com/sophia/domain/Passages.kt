package com.sophia.domain

import kotlin.random.Random

/**
 * Selecção de passagens curtas para reflexão. As passagens são construídas a partir de frases
 * completas do texto (janelas de 1 a 3 frases, 140–560 caracteres): nunca começam nem acabam
 * a meio de uma frase, mesmo quando a tipografia do livro parte os parágrafos.
 */
object PassagePicker {
    private val SENTENCE_START=Regex("(?<=[.!?])\\s+(?=[A-ZÁÉÍÓÚÂÊÔÃÕÇ«\\\"])")

    fun isPassage(text:String):Boolean {
        val p=text.trim()
        if(p.length !in 140..640) return false
        if(p.first().isDigit()) return false
        if(!(p.first().isUpperCase()||p.first()=='«'||p.first()=='"')) return false
        if(p.startsWith("CAPÍTULO",ignoreCase=true)||p.startsWith("LIVRO",ignoreCase=true)||p.startsWith("PARTE",ignoreCase=true)) return false
        if(p.contains("pág.")||p.contains("Nota desta edição")) return false
        if(p.contains(Regex("\\d+\\s*[–—-]\\s*(O Livro|A Génese|O Evangelho|O Céu)"))) return false
        if(p.count{it==' '} < 15) return false
        return p.endsWith('.')||p.endsWith('!')||p.endsWith('»')
    }

    fun fromUnitTexts(units:List<Pair<String,String>>):List<Pair<String,String>> =
        fromIndexedUnitTexts(units.mapIndexed{index,(title,text)->Triple(index,title,text)}).map{it.second to it.third}

    /** Igual a [fromUnitTexts], mas cada passagem conserva o índice real da sua unidade no livro (para abrir o leitor no capítulo certo). */
    fun fromIndexedUnitTexts(units:List<Triple<Int,String,String>>):List<Triple<Int,String,String>> =
        units.flatMap{(index,title,text)->
            val sentences=text.split(SENTENCE_START).map{it.trim()}.filter{it.length>=40}
            val out=mutableListOf<Triple<Int,String,String>>()
            var i=0
            while(i<sentences.size){
                var combined=sentences[i];var j=i
                while(combined.length<170&&j+1<sentences.size&&combined.length+1+sentences[j+1].length<=560){j++;combined+=" "+sentences[j]}
                if(isPassage(combined)) out+=Triple(index,title,combined)
                i=j+1
            }
            out
        }

    fun pickIndex(size:Int,seed:Long):Int = if(size<=0) -1 else Random(seed).nextInt(size)
}
