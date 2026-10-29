package com.sophia.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

/** Test APK only. All texts are original synthetic fixtures, never catalogue books. */
class FixtureProvider:ContentProvider(){
    override fun onCreate()=true
    private fun file(uri:Uri):File{val dir=File(context!!.cacheDir,"fixtures");val f=File(dir,uri.lastPathSegment?:error("No file"));require(f.canonicalFile.parentFile==dir.canonicalFile);return f}
    override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor{val f=file(uri);return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE)).apply{addRow(arrayOf(f.name,f.length()))}}
    override fun getType(uri:Uri)=when(file(uri).extension){"txt"->"text/plain";"pdf"->"application/pdf";"epub"->"application/epub+zip";else->"application/octet-stream"}
    override fun openFile(uri:Uri,mode:String)=ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY)
    override fun insert(uri:Uri,values:ContentValues?):Uri?=throw UnsupportedOperationException()
    override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException()
    override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException()
}
