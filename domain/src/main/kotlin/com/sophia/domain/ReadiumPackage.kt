package com.sophia.domain

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** A derivative cache preserves text, CSS and locators while removing publication scripts.
 * The original imported/bundled edition and its hash never change.
 */
object ReadiumPackage {
    const val POLICY="default-src 'none'; img-src 'self' data: https://readium; style-src 'self' 'unsafe-inline' https://readium; script-src https://readium 'unsafe-inline'; font-src 'self' data: https://readium; connect-src 'none'; media-src 'none'; frame-src 'none'; object-src 'none'; form-action 'none'; base-uri 'none'"
    fun prepare(source:File,target:File){
        val doc=BookDocuments.epub(source);val readingPaths=doc.units.map{it.href}.toSet()
        val temp=File(target.parentFile,target.name+".part")
        try{
            ZipFile(source).use{zip->ZipOutputStream(temp.outputStream()).use{out->
                val entries=zip.entries().asSequence().filterNot{it.isDirectory}.sortedBy{if(it.name=="mimetype")0 else 1}.toList()
                var total=0L
                for(entry in entries){
                    val path=BookDocuments.normalizedPath("",entry.name)
                    val bytes=BookDocuments.resource(source,path);total+=bytes.size;require(total<=512L*1024*1024)
                    val active=path in readingPaths||path.substringAfterLast('.').lowercase() in setOf("html","xhtml","htm","svg")
                    val data=if(active){
                        val svg=path.endsWith(".svg",true)
                        val html=Jsoup.parse(bytes.inputStream(),null,"",if(svg)org.jsoup.parser.Parser.xmlParser()else org.jsoup.parser.Parser.htmlParser())
                        html.select("script,iframe,frame,frameset,object,embed,form,base,meta[http-equiv]").remove()
                        html.allElements.forEach{node->node.attributes().asList().filter{a->a.key.startsWith("on",true)||a.key.equals("srcdoc",true)||a.value.trim().startsWith("javascript:",true)}.forEach{node.removeAttr(it.key)}}
                        if(!svg)html.head().prependElement("meta").attr("http-equiv","Content-Security-Policy").attr("content",POLICY)
                        html.outputSettings().syntax(Document.OutputSettings.Syntax.xml).charset(Charsets.UTF_8)
                        html.outerHtml().toByteArray(Charsets.UTF_8)
                    }else bytes
                    val dest=ZipEntry(path)
                    if(path=="mimetype"){dest.method=ZipEntry.STORED;dest.size=data.size.toLong();dest.crc=java.util.zip.CRC32().apply{update(data)}.value}
                    out.putNextEntry(dest);out.write(data);out.closeEntry()
                }
            }}
            require(temp.renameTo(target)){"Não foi possível preparar a cópia segura do leitor."}
        }finally{temp.delete()}
    }
}
