package com.sophia.enciclopedia;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Test APK only. Java avoids depending on the target APK's Kotlin runtime in this process. */
public final class ReaderFixtureDocuments extends DocumentsProvider {
    private File directory(){File dir=new File(getContext().getFilesDir(),"reader-fixtures");dir.mkdirs();return dir;}
    private File file(String id) throws FileNotFoundException {
        File f=new File(directory(),id);
        try{if(!directory().getCanonicalFile().equals(f.getCanonicalFile().getParentFile()))throw new FileNotFoundException("Invalid fixture");}catch(IOException e){throw new FileNotFoundException("Invalid fixture");}
        return f;
    }
    @Override public boolean onCreate(){try{seed();return true;}catch(IOException e){throw new IllegalStateException("Failed to create original test fixtures",e);}}
    @Override public Cursor queryRoots(String[] projection){
        String[] cols=projection!=null?projection:new String[]{DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID,DocumentsContract.Root.COLUMN_TITLE,DocumentsContract.Root.COLUMN_FLAGS,DocumentsContract.Root.COLUMN_MIME_TYPES};
        MatrixCursor cursor=new MatrixCursor(cols);Object[] row=new Object[cols.length];
        for(int i=0;i<cols.length;i++){switch(cols[i]){case DocumentsContract.Root.COLUMN_ROOT_ID:row[i]="sophia-test";break;case DocumentsContract.Root.COLUMN_DOCUMENT_ID:row[i]="root";break;case DocumentsContract.Root.COLUMN_TITLE:row[i]="Sophia-Teste";break;case DocumentsContract.Root.COLUMN_FLAGS:row[i]=DocumentsContract.Root.FLAG_LOCAL_ONLY|DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD;break;case DocumentsContract.Root.COLUMN_MIME_TYPES:row[i]="*/*";break;default:row[i]=null;}}
        cursor.addRow(row);return cursor;
    }
    private static final String[] DOC_COLS={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_FLAGS};
    private Object[] row(String id,String[] cols) throws FileNotFoundException {
        Object[] row=new Object[cols.length];boolean root=id.equals("root");File f=root?directory():file(id);
        for(int i=0;i<cols.length;i++){switch(cols[i]){case DocumentsContract.Document.COLUMN_DOCUMENT_ID:row[i]=id;break;case DocumentsContract.Document.COLUMN_DISPLAY_NAME:row[i]=root?"Sophia-Teste":id;break;case DocumentsContract.Document.COLUMN_MIME_TYPE:row[i]=root?DocumentsContract.Document.MIME_TYPE_DIR:id.endsWith(".epub")?"application/epub+zip":id.endsWith(".pdf")?"application/pdf":"text/plain";break;case DocumentsContract.Document.COLUMN_SIZE:row[i]=root?0L:f.length();break;case DocumentsContract.Document.COLUMN_FLAGS:row[i]=0;break;case DocumentsContract.Document.COLUMN_LAST_MODIFIED:row[i]=f.lastModified();break;default:row[i]=null;}}
        return row;
    }
    @Override public Cursor queryDocument(String id,String[] projection) throws FileNotFoundException {String[] cols=projection!=null?projection:DOC_COLS;MatrixCursor c=new MatrixCursor(cols);c.addRow(row(id,cols));return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String order) throws FileNotFoundException {if(!parent.equals("root"))throw new FileNotFoundException();String[] cols=projection!=null?projection:DOC_COLS;MatrixCursor c=new MatrixCursor(cols);File[] files=directory().listFiles();if(files!=null){Arrays.sort(files);for(File f:files)c.addRow(row(f.getName(),cols));}return c;}
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal) throws FileNotFoundException {return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public boolean isChildDocument(String parent,String child){try{return parent.equals("root")&&file(child).exists();}catch(FileNotFoundException e){return false;}}
    private void seed() throws IOException {
        try(FileOutputStream out=new FileOutputStream(new File(directory(),"01-Sophia.txt"))){for(int i=0;i<300;i++)out.write("Texto técnico original Sophia, criado apenas para ensaio.\n".getBytes(StandardCharsets.UTF_8));}
        Map<String,String> files=new LinkedHashMap<>();
        files.put("mimetype","application/epub+zip");
        files.put("META-INF/container.xml","<container><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>");
        files.put("OPS/book.opf","<package><metadata><title>EPUB técnico Sophia</title><creator>Autor de ensaio</creator><language>pt</language></metadata><manifest><item id='a' href='a.xhtml' media-type='application/xhtml+xml'/><item id='b' href='b.xhtml' media-type='application/xhtml+xml'/></manifest><spine><itemref idref='a'/><itemref idref='b'/></spine></package>");
        files.put("OPS/a.xhtml","<html><body><h1>Primeiro recurso EPUB</h1><p>Conteúdo original de ensaio EPUB.</p></body></html>");
        files.put("OPS/b.xhtml","<html><body><h1>Segundo recurso EPUB</h1><p>Passagem de teste para notas.</p></body></html>");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(new File(directory(),"02-Sophia.epub")))){for(Map.Entry<String,String> entry:files.entrySet()){out.putNextEntry(new ZipEntry(entry.getKey()));out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));out.closeEntry();}}
        PdfDocument pdf=new PdfDocument();try{for(int i=0;i<2;i++){PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(500,700,i+1).create());Paint paint=new Paint();paint.setTextSize(22f);page.getCanvas().drawText("Sophia - PDF de teste "+(i+1),30f,90f,paint);pdf.finishPage(page);}try(FileOutputStream out=new FileOutputStream(new File(directory(),"03-Sophia.pdf"))){pdf.writeTo(out);}}finally{pdf.close();}
    }
}
