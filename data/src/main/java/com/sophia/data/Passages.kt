package com.sophia.data

import android.content.Context
import com.sophia.domain.BaseBook
import com.sophia.domain.BookDocuments
import com.sophia.domain.Passage
import com.sophia.domain.PassagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

/** Passagens dos livros-base com texto integral incluído (hoje: os cinco de Kardec, em domínio público). */
class PassageRepository(private val context:Context) {
    private val cache=mutableMapOf<String,List<Passage>>()
    private val books:List<BaseBook> get()=BaseBooks.all.filter{it.fullTextAsset!=null}

    suspend fun passagesFor(book:BaseBook):List<Passage> = withContext(Dispatchers.IO) {
        cache[book.id]?.let{return@withContext it}
        val asset=book.fullTextAsset?:return@withContext emptyList()
        val dir=File(context.cacheDir,"passages").apply{mkdirs()}
        val file=File(dir,"${book.id}.epub")
        if(!file.exists()||file.length()==0L){
            context.assets.open(asset).use{input->file.outputStream().use{out->input.copyTo(out)}}
        }
        val units=runCatching{BookDocuments.epubUnitTexts(file)}.getOrDefault(emptyList())
            .mapIndexed{index,(title,text)->Triple(index,title,text)}.filter{it.second!="Edição e licença"}
        val result=PassagePicker.fromIndexedUnitTexts(units).map{(unitIndex,unitTitle,text)->Passage(book.id,book.title,unitTitle,text,unitIndex)}
        cache[book.id]=result
        result
    }

    /** Passagens de uma tradição: amostra estável e repartida dos seus livros-base com texto integral. */
    suspend fun passagesForTradition(traditionId:String,count:Int=6):List<Passage> {
        val pool=BaseBooks.all.filter{it.traditionId==traditionId && it.fullTextAsset!=null}
        if(pool.isEmpty()) return emptyList()
        val all=pool.flatMap{passagesFor(it)}
        if(all.size<=count) return all
        val step=all.size.toDouble()/count
        val offset=kotlin.math.abs(traditionId.hashCode())%all.size
        return (0 until count).map{all[((offset+it*step).toInt())%all.size]}
    }

    /** Passagem do dia: determinística por data — o mesmo texto na app e na notificação; os livros rodam dia a dia. */
    suspend fun dailyPassage(date:LocalDate=LocalDate.now()):Passage? {
        val pool=books; if(pool.isEmpty()) return null
        val book=pool[(date.toEpochDay()%pool.size).toInt()]
        val passages=passagesFor(book); if(passages.isEmpty()) return null
        return passages[PassagePicker.pickIndex(passages.size,date.toEpochDay()*31L+book.id.hashCode())]
    }

    suspend fun randomPassage():Passage? {
        val pool=books; if(pool.isEmpty()) return null
        val book=pool[Random.nextInt(pool.size)]
        val passages=passagesFor(book); if(passages.isEmpty()) return null
        return passages[Random.nextInt(passages.size)]
    }
}
