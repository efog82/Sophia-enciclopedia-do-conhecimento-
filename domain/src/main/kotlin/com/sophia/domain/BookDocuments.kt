package com.sophia.domain

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipFile

data class FileMetadata(val title:String?=null,val authors:List<String> = emptyList(),val language:String?=null,val publisher:String?=null,val year:Int?=null)
data class ReadingUnit(val href:String,val title:String,val text:String?=null)
data class BookDocument(val metadata:FileMetadata,val units:List<ReadingUnit>,val epub:Boolean)

/** Bounded, local-only parsing. ZIP contents are never extracted onto the filesystem. */
object BookDocuments {
    const val MAX_IMPORT_BYTES=200L*1024*1024
    private const val MAX_RESOURCE_BYTES=8*1024*1024
    fun normalizedPath(base:String,relative:String):String {
        require(!relative.contains('\\') && !relative.contains('\u0000')){"Caminho inválido no EPUB."}
        val uri=URI(relative)
        require(!uri.isAbsolute && uri.rawAuthority==null && !relative.startsWith("/")){"O EPUB contém um recurso externo ou um caminho absoluto não suportado."}
        val decoded=uri.path.orEmpty();val stack=base.substringBeforeLast('/',"").split('/').filter{it.isNotEmpty()}.toMutableList()
        decoded.split('/').forEach{part->when(part){"","."->Unit;".."->{require(stack.isNotEmpty()){"Caminho fora do EPUB."};stack.removeAt(stack.lastIndex)};else->{require(!part.contains('\\')&&!part.contains('\u0000'));stack+=part}}}
        return stack.joinToString("/")
    }
    fun resource(file:File,path:String):ByteArray=ZipFile(file).use{zip->
        require(normalizedPath("",path)==path){"Caminho de recurso inválido."}
        val entry=zip.getEntry(path)?:throw IllegalArgumentException("Recurso em falta no EPUB: $path")
        require(entry.size<=MAX_RESOURCE_BYTES){"Recurso EPUB demasiado grande (máximo 8 MiB)."}
        zip.getInputStream(entry).use{it.readNBytesCompat(MAX_RESOURCE_BYTES)}
    }
    private fun java.io.InputStream.readNBytesCompat(limit:Int):ByteArray{
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=0
        while(true){val n=read(buffer);if(n<0)break;count+=n;require(count<=limit){"Recurso demasiado grande."};out.write(buffer,0,n)}
        return out.toByteArray()
    }
    fun epub(file:File):BookDocument {
        ZipFile(file).use{zip->
            val entries=zip.entries().asSequence().toList();require(entries.size<=10000){"EPUB com demasiados recursos."}
            require(entries.all{e->e.name.trimEnd('/').let{it.isEmpty()||normalizedPath("",it)==it}}){"O EPUB contém caminhos inseguros."}
            require(entries.all{it.size>=0} && entries.sumOf{it.size}<=512L*1024*1024){"O EPUB descomprimido excede os limites de segurança."}
            require(entries.distinctBy{it.name}.size==entries.size){"O EPUB contém recursos duplicados."}
            require(entries.none{it.name=="META-INF/encryption.xml"}){"EPUB cifrado ou com recursos protegidos: importe uma edição sem DRM. Não são removidas proteções."}
        }
        require(resource(file,"mimetype").toString(Charsets.US_ASCII).trim()=="application/epub+zip"){"O ZIP não é um EPUB válido."}
        val container=Jsoup.parse(resource(file,"META-INF/container.xml").toString(Charsets.UTF_8),"",Parser.xmlParser())
        val opfPath=container.getAllElements().firstOrNull{it.normalName().substringAfter(':')=="rootfile"}?.attr("full-path")?.takeIf{it.isNotBlank()}?:error("O EPUB não contém um pacote OPF identificado.")
        val opf=Jsoup.parse(resource(file,normalizedPath("",opfPath)).toString(Charsets.UTF_8),"",Parser.xmlParser())
        fun field(name:String)=opf.getAllElements().firstOrNull{it.normalName().substringAfterLast(':')==name}?.text()?.trim()?.takeIf{it.isNotEmpty()}
        require(opf.getAllElements().none{it.attr("property")=="rendition:layout"&&it.text().trim()=="pre-paginated"}){"EPUB de disposição fixa não suportado neste leitor. Utilize PDF ou EPUB de texto refluível."}
        val manifest=opf.getAllElements().filter{it.normalName().substringAfterLast(':')=="item"}.associateBy{it.attr("id")}
        val units=opf.getAllElements().filter{it.normalName().substringAfterLast(':')=="itemref"}.mapIndexed{index,spine->
            val item=manifest[spine.attr("idref")]?:error("O índice de leitura refere um recurso inexistente.")
            require(item.attr("media-type") in listOf("application/xhtml+xml","text/html")){"Recurso de leitura EPUB incompatível: ${item.attr("media-type")}"}
            val href=normalizedPath(opfPath,item.attr("href"));val body=Jsoup.parse(resource(file,href).toString(Charsets.UTF_8))
            val label=body.selectFirst("h1,h2,title")?.text()?.takeIf{it.isNotBlank()}?:"Recurso ${index+1} · ${href.substringAfterLast('/')}"
            ReadingUnit(href,label)
        }
        require(units.isNotEmpty()){"O EPUB não tem recursos de leitura."}
        val authors=opf.getAllElements().filter{it.normalName().substringAfterLast(':')=="creator"}.map{it.text().trim()}.filter{it.isNotEmpty()}.distinct()
        return BookDocument(FileMetadata(field("title"),authors,field("language"),field("publisher"),field("date")?.take(4)?.toIntOrNull()),units,true)
    }
    fun text(file:File):BookDocument {
        require(file.length()<=16L*1024*1024){"TXT demasiado grande para este leitor (máximo 16 MiB)."}
        val bytes=file.readBytes()
        val charset=when{bytes.size>=2&&bytes[0]==0xff.toByte()&&bytes[1]==0xfe.toByte()->Charsets.UTF_16LE;bytes.size>=2&&bytes[0]==0xfe.toByte()&&bytes[1]==0xff.toByte()->Charsets.UTF_16BE;else->Charsets.UTF_8}
        val text=try{charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")}catch(e:java.nio.charset.CharacterCodingException){throw IllegalArgumentException("Codificação TXT não suportada. Guarde o ficheiro como UTF-8 ou UTF-16 com BOM.")}
        require(text.isNotBlank()&&!text.contains('\u0000')){"O TXT está vazio ou contém dados binários."}
        val units=mutableListOf<ReadingUnit>();var start=0
        while(start<text.length){var end=(start+4000).coerceAtMost(text.length);if(end<text.length){val breakAt=text.lastIndexOf('\n',end);if(breakAt>start+2000)end=breakAt+1;if(end<text.length&&Character.isLowSurrogate(text[end]))end--};units+=ReadingUnit("offset:$start","Bloco de leitura ${units.size+1}",text.substring(start,end));start=end}
        return BookDocument(FileMetadata(),units,false)
    }
    fun safeHtml(file:File,href:String,dark:Boolean,fontSize:Int):String {
        val doc=Jsoup.parse(resource(file,href).toString(Charsets.UTF_8),"https://sophia.invalid/$href");doc.select("script,style,iframe,object,embed,form,link,base,meta").remove()
        val safe=Cleaner(Safelist.relaxed().addTags("h1","h2","h3","h4","h5","h6","hr","section","article","sup","sub").addAttributes(":all","id").preserveRelativeLinks(true)).clean(doc)
        // Relative image and link paths are resolved only against the app's virtual local origin.
        val background=if(dark)"#111827" else "#fcfaf5";val foreground=if(dark)"#e5e7eb" else "#1f2937"
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; img-src https://sophia.invalid data:; style-src 'unsafe-inline';\"><style>body{background:$background;color:$foreground;font-family:serif;font-size:${fontSize.coerceIn(12,36)}px;line-height:1.7;padding:20px;overflow-wrap:break-word}img{max-width:100%;height:auto}a{color:inherit}pre{white-space:pre-wrap}table{max-width:100%;border-collapse:collapse}</style></head><body>"+safe.body().html()+"</body></html>"
    }
}
