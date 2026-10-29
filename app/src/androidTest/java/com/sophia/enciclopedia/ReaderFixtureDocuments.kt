package com.sophia.enciclopedia

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

/** System Photo/File picker source, packaged exclusively in the instrumentation APK. */
class ReaderFixtureDocuments:DocumentsProvider(){
    override fun onCreate():Boolean{seed();return true}
    private val directory get()=File(context!!.filesDir,"reader-fixtures").apply{mkdirs()}
    private fun file(id:String)=File(directory,id).also{require(it.canonicalFile.parentFile==directory.canonicalFile)}
    override fun queryRoots(projection:Array<out String>?):Cursor{
        val columns=projection?:arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID,DocumentsContract.Root.COLUMN_TITLE,DocumentsContract.Root.COLUMN_FLAGS,DocumentsContract.Root.COLUMN_MIME_TYPES)
        return MatrixCursor(columns).apply{addRow(columns.map{when(it){DocumentsContract.Root.COLUMN_ROOT_ID->"sophia-test";DocumentsContract.Root.COLUMN_DOCUMENT_ID->"root";DocumentsContract.Root.COLUMN_TITLE->"Sophia-Teste";DocumentsContract.Root.COLUMN_FLAGS->DocumentsContract.Root.FLAG_LOCAL_ONLY or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD;DocumentsContract.Root.COLUMN_MIME_TYPES->"*/*";else->null}}.toTypedArray())}
    }
    private fun document(id:String,projection:Array<out String>?):MatrixCursor{
        val columns=projection?:arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_FLAGS)
        return MatrixCursor(columns).apply{addRow(row(id,columns))}
    }
    private fun row(id:String,columns:Array<out String>)=columns.map{column->when(column){DocumentsContract.Document.COLUMN_DOCUMENT_ID->id;DocumentsContract.Document.COLUMN_DISPLAY_NAME->if(id=="root")"Sophia-Teste" else id;DocumentsContract.Document.COLUMN_MIME_TYPE->if(id=="root")DocumentsContract.Document.MIME_TYPE_DIR else when(file(id).extension){"epub"->"application/epub+zip";"pdf"->"application/pdf";else->"text/plain"};DocumentsContract.Document.COLUMN_SIZE->if(id=="root")0L else file(id).length();DocumentsContract.Document.COLUMN_FLAGS->0;DocumentsContract.Document.COLUMN_LAST_MODIFIED->if(id=="root")0L else file(id).lastModified();else->null}}.toTypedArray()
    override fun queryDocument(documentId:String,projection:Array<out String>?)=document(documentId,projection)
    override fun queryChildDocuments(parentDocumentId:String,projection:Array<out String>?,sortOrder:String?):Cursor{require(parentDocumentId=="root");val columns=projection?:arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_FLAGS);return MatrixCursor(columns).apply{directory.listFiles().orEmpty().sortedBy{it.name}.forEach{addRow(row(it.name,columns))}}}
    override fun openDocument(documentId:String,mode:String,signal:CancellationSignal?)=ParcelFileDescriptor.open(file(documentId),ParcelFileDescriptor.MODE_READ_ONLY)
    override fun isChildDocument(parentDocumentId:String,documentId:String)=parentDocumentId=="root"&&file(documentId).exists()
    private fun seed(){
        val dir=directory
        File(dir,"01-Sophia.txt").writeText("Texto técnico original Sophia, criado apenas para ensaio.\n".repeat(300))
        ZipOutputStream(File(dir,"02-Sophia.epub").outputStream()).use{zip->mapOf("mimetype" to "application/epub+zip","META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>","OPS/book.opf" to "<package><metadata><title>EPUB técnico Sophia</title><creator>Autor de ensaio</creator><language>pt</language></metadata><manifest><item id='a' href='a.xhtml' media-type='application/xhtml+xml'/><item id='b' href='b.xhtml' media-type='application/xhtml+xml'/></manifest><spine><itemref idref='a'/><itemref idref='b'/></spine></package>","OPS/a.xhtml" to "<html><body><h1>Primeiro recurso EPUB</h1><p>Conteúdo original de ensaio EPUB.</p></body></html>","OPS/b.xhtml" to "<html><body><h1>Segundo recurso EPUB</h1><p>Passagem de teste para notas.</p></body></html>").forEach{(name,text)->zip.putNextEntry(ZipEntry(name));zip.write(text.toByteArray());zip.closeEntry()}}
        val pdf=PdfDocument();try{repeat(2){i->val page=pdf.startPage(PdfDocument.PageInfo.Builder(500,700,i+1).create());page.canvas.drawText("Sophia - PDF de teste ${i+1}",30f,90f,Paint().apply{textSize=22f});pdf.finishPage(page)};File(dir,"03-Sophia.pdf").outputStream().use{pdf.writeTo(it)}}finally{pdf.close()}
    }
}
