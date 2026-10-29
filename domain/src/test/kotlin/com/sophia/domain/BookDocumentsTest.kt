package com.sophia.domain

import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import kotlin.test.*

class BookDocumentsTest {
    private fun fixture(extra:Map<String,String> = emptyMap(),spine:String="c1"):File{
        val f=Files.createTempFile("sophia-fixture-",".epub").toFile()
        ZipOutputStream(f.outputStream()).use{z->val files=mapOf("mimetype" to "application/epub+zip","META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>","OPS/book.opf" to "<package><metadata><dc:title xmlns:dc='urn:dc'>Documento técnico Sophia</dc:title><dc:creator xmlns:dc='urn:dc'>Autor de teste</dc:creator><dc:language xmlns:dc='urn:dc'>pt</dc:language></metadata><manifest><item id='c1' href='c1.xhtml' media-type='application/xhtml+xml'/></manifest><spine><itemref idref='$spine'/></spine></package>","OPS/c1.xhtml" to "<html><head><title>Título declarado</title></head><body><h1>Texto de teste</h1><p>Conteúdo técnico original.</p><script>alert('x')</script><img src='image.png'/><a href='javascript:alert(1)'>Ligação</a></body></html>")+extra;files.forEach{(n,v)->z.putNextEntry(ZipEntry(n));z.write(v.toByteArray());z.closeEntry()}}
        return f
    }
    @Test fun readsActualPackageMetadataAndSpine(){val f=fixture();try{val d=BookDocuments.epub(f);assertEquals("Documento técnico Sophia",d.metadata.title);assertEquals(listOf("Autor de teste"),d.metadata.authors);assertEquals("pt",d.metadata.language);assertEquals("OPS/c1.xhtml",d.units.single().href)}finally{f.delete()}}
    @Test fun rejectsPathTraversal(){assertFailsWith<IllegalArgumentException>{BookDocuments.normalizedPath("OPS/a.xhtml","../../outside")};assertFailsWith<IllegalArgumentException>{BookDocuments.normalizedPath("","%2e%2e/private")};assertEquals("image.png",BookDocuments.normalizedPath("OPS/a.xhtml","../image.png"))}
    @Test fun rejectsExternalAndWindowsPaths(){listOf("https://example.org/a","//example.org/a","C:\\file","/etc/passwd").forEach{assertFails{BookDocuments.normalizedPath("",it)}}}
    @Test fun doesNotRemoveDrm(){val f=fixture(mapOf("META-INF/encryption.xml" to "<encryption/>"));try{assertFailsWith<IllegalArgumentException>{BookDocuments.epub(f)}}finally{f.delete()}}
    @Test fun rejectsBrokenSpine(){val f=fixture(spine="missing");try{assertFails{BookDocuments.epub(f)}}finally{f.delete()}}
    @Test fun stripsScriptsAndKeepsRealText(){val f=fixture();try{val html=BookDocuments.safeHtml(f,"OPS/c1.xhtml",true,24);assertFalse(html.contains("<script"));assertFalse(html.contains("javascript:"));assertTrue(html.contains("Conteúdo técnico original."));assertTrue(html.contains("24px"));assertTrue(html.contains("image.png"))}finally{f.delete()}}
    @Test fun textBlocksPreserveExactText(){val f=Files.createTempFile("sophia-",".txt").toFile();val text="  início\n"+"Reflexão 🌿\n".repeat(1200);f.writeText(text);try{val d=BookDocuments.text(f);assertEquals(text,d.units.joinToString(""){it.text.orEmpty()});assertTrue(d.units.size>1);assertTrue(d.units.all{!it.title.contains("Capítulo")})}finally{f.delete()}}
    @Test fun supportsUtf16Bom(){val f=Files.createTempFile("sophia-",".txt").toFile();f.writeBytes(byteArrayOf(0xff.toByte(),0xfe.toByte())+"Ação e reflexão".toByteArray(Charsets.UTF_16LE));try{assertEquals("Ação e reflexão",BookDocuments.text(f).units.single().text)}finally{f.delete()}}
    @Test fun rejectsBinaryAndInvalidEncoding(){val f=Files.createTempFile("sophia-",".txt").toFile();try{f.writeBytes(byteArrayOf(0,1,2));assertFails{BookDocuments.text(f)};f.writeBytes(byteArrayOf(0xc3.toByte(),0x28));assertFails{BookDocuments.text(f)}}finally{f.delete()}}
    @Test fun detectsEpub2AndEpub3DeclaredCovers(){
        listOf("<meta name='cover' content='cover'/>" to "", "" to " properties='cover-image'").forEach{(meta,properties)->
            val f=fixture(mapOf("OPS/book.opf" to "<package><metadata>$meta</metadata><manifest><item id='c1' href='c1.xhtml' media-type='application/xhtml+xml'/><item id='cover' href='images/cover.png' media-type='image/png'$properties/></manifest><spine><itemref idref='c1'/></spine></package>","OPS/images/cover.png" to "fixture image bytes"))
            try{assertEquals("OPS/images/cover.png",BookDocuments.epub(f).coverHref);assertEquals("fixture image bytes",BookDocuments.resource(f,BookDocuments.epub(f).coverHref!!).toString(Charsets.UTF_8))}finally{f.delete()}
        }
    }
    @Test fun externalCoverIsIgnoredWithoutBreakingReading(){
        val f=fixture(mapOf("OPS/book.opf" to "<package><manifest><item id='c1' href='c1.xhtml' media-type='application/xhtml+xml'/><item id='cover' href='https://example.org/cover.jpg' media-type='image/jpeg' properties='cover-image'/></manifest><spine><itemref idref='c1'/></spine></package>"))
        try{val doc=BookDocuments.epub(f);assertNull(doc.coverHref);assertEquals(1,doc.units.size)}finally{f.delete()}
    }
}
