package com.sophia.enciclopedia

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File

/** System Photo/File picker source, packaged exclusively in the instrumentation APK. */
class ReaderFixtureDocuments:DocumentsProvider(){
    override fun onCreate()=true
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
}
